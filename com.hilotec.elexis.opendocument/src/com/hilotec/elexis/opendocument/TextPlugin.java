package com.hilotec.elexis.opendocument;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.odftoolkit.odfdom.OdfFileDom;
import org.odftoolkit.odfdom.OdfXMLFactory;
import org.odftoolkit.odfdom.doc.OdfDocument;
import org.odftoolkit.odfdom.doc.OdfTextDocument;
import org.odftoolkit.odfdom.doc.draw.OdfDrawFrame;
import org.odftoolkit.odfdom.doc.draw.OdfDrawTextBox;
import org.odftoolkit.odfdom.doc.office.OdfOfficeAutomaticStyles;
import org.odftoolkit.odfdom.doc.office.OdfOfficeFontFaceDecls;
import org.odftoolkit.odfdom.doc.office.OdfOfficeText;
import org.odftoolkit.odfdom.doc.style.OdfStyle;
import org.odftoolkit.odfdom.doc.style.OdfStyleBackgroundImage;
import org.odftoolkit.odfdom.doc.style.OdfStyleColumns;
import org.odftoolkit.odfdom.doc.style.OdfStyleFontFace;
import org.odftoolkit.odfdom.doc.style.OdfStyleGraphicProperties;
import org.odftoolkit.odfdom.doc.style.OdfStyleTableColumnProperties;
import org.odftoolkit.odfdom.doc.style.OdfStyleTextProperties;
import org.odftoolkit.odfdom.doc.text.OdfTextLineBreak;
import org.odftoolkit.odfdom.doc.text.OdfTextParagraph;
import org.odftoolkit.odfdom.doc.text.OdfTextTab;
import org.odftoolkit.odfdom.dom.element.draw.DrawFrameElement;
import org.odftoolkit.odfdom.dom.element.draw.DrawTextBoxElement;
import org.odftoolkit.odfdom.dom.element.style.StyleBackgroundImageElement;
import org.odftoolkit.odfdom.dom.element.style.StyleColumnsElement;
import org.odftoolkit.odfdom.dom.element.style.StyleFontFaceElement;
import org.odftoolkit.odfdom.dom.element.style.StyleGraphicPropertiesElement;
import org.odftoolkit.odfdom.dom.element.style.StyleStyleElement;
import org.odftoolkit.odfdom.dom.element.style.StyleTableColumnPropertiesElement;
import org.odftoolkit.odfdom.dom.element.style.StyleTextPropertiesElement;
import org.odftoolkit.odfdom.dom.element.table.TableTableCellElement;
import org.odftoolkit.odfdom.dom.element.table.TableTableColumnElement;
import org.odftoolkit.odfdom.dom.element.table.TableTableElement;
import org.odftoolkit.odfdom.dom.element.table.TableTableRowElement;
import org.odftoolkit.odfdom.dom.element.text.TextPElement;
import org.odftoolkit.odfdom.dom.element.text.TextSpanElement;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import ch.elexis.Desk;
import ch.elexis.Hub;
import ch.elexis.text.ITextPlugin;
import ch.elexis.text.ReplaceCallback;
import ch.elexis.util.SWTHelper;

public class TextPlugin implements ITextPlugin {
	private Process editor_process;

	private File file;

	private OdfTextDocument odt;
	private String curStyle;

	private Composite comp;
	private Label filename_label;
	private Button open_button;
	private Button import_button;

 	private boolean editorRunning() {
		return (editor_process != null);
	}

	private synchronized void odtSync() {
		if (file == null || odt == null || editorRunning()) {
			return;
		}

		try {
			odt.save(file);
		} catch (Exception e) {
			// TODO
			e.printStackTrace();
		}
	}

	/**
	 * Sicherstellen dass kein Editor geoeffnet ist. Falls einer geoeffnet ist,
	 * wird eine Fehlermeldung mit einem entsprechenden Hinweis angezeigt.
	 * 
	 * @return True wenn keine Instanz mehr geoeffnet ist.
	 */
	private boolean ensureClosed() {
		if (editorRunning()) {
			SWTHelper.showError("Editor bereits geöffnet", "Es ist bereits ein"
					+ "Editor geöffnet, diesen bitte erst schliessen.");
			return false;
		}
		return true;
	}

	private void openEditor() {
		if (file == null || !ensureClosed()) {
			return;
		}

		odtSync();

		String editor = Hub.localCfg.get(Preferences.P_EDITOR, "");
		String argstr = Hub.localCfg.get(Preferences.P_EDITARGS, "");

		if (editor.length() == 0) {
			SWTHelper.showError("Kein Editor gesetzt",
					"In den Einstellungen wurde kein Editor konfiguriert.");
			return;
		}

		String args[] = (editor + "\n" + argstr + "\n" + file.getAbsolutePath())
				.split("\n");
		ProcessBuilder pb = new ProcessBuilder(args);

		try {
			editor_process = pb.start();
			odt = null;
			(new Thread() {
				public void run() {
					try {
						editor_process.waitFor();
						odt = (OdfTextDocument) OdfTextDocument
								.loadDocument(file);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} finally {
						editor_process = null;
					}
				}
			}).start();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void importFile() {
		if (file == null || !ensureClosed()) {
			return;
		}

		odtSync();
		
		FileDialog fd = new FileDialog(Desk.getTopShell(), SWT.OPEN);
		fd.setFilterExtensions(new String[]{"*.odt"});
		String path = fd.open();
		
		if (path != null) {
			try {
				OdfTextDocument ndoc = (OdfTextDocument)
					OdfTextDocument.loadDocument(path);
				if (ndoc != null) {
					odt = ndoc;
					fileValid();
				}
				
				odtSync();
				
			} catch (Exception e) {
				SWTHelper.showError("Fehler beim Import", e.getMessage());
			}
		}
	}
		
	public boolean print(String toPrinter, String toTray, boolean wait) {
		if (file == null || !ensureClosed()) {
			return false;
		}

		odtSync();
		String editor = Hub.localCfg.get(Preferences.P_EDITOR, "oowriter");
		String argstr = Hub.localCfg.get(Preferences.P_PRINTARGS, "");
		String args[] = (editor + "\n" + argstr + "\n" + file.getAbsolutePath())
				.split("\n");
		ProcessBuilder pb = new ProcessBuilder(args);

		try {
			editor_process = pb.start();
			editor_process.waitFor();
			editor_process = null;
		} catch (Exception e) {
			e.printStackTrace();
		}

		return true;
	}

	@Override
	public Composite createContainer(Composite parent, ICallback handler) {
		if (comp == null) {
			comp = new Composite(parent, SWT.NONE);
			comp.setLayout(new GridLayout());
			comp.setLayoutData(SWTHelper.getFillGridData(1, true, 1, true));

			Label fn_label = new Label(comp, SWT.NONE);
			fn_label.setText("Dateiname:");
			filename_label = new Label(comp, SWT.NONE);

			new Label(comp, SWT.NONE);
			open_button = new Button(parent, SWT.PUSH);
			open_button.setText("Editor Öffnen");
			open_button.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event event) {
					openEditor();
				}
			});
			
			import_button = new Button(parent, SWT.PUSH);
			import_button.setText("Datei Importieren");
			import_button.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event event) {
					importFile();
				}
			});
			/* open_button.setEnabled(false); */
		}

		return comp;
	}

	private void fileValid() {
		open_button.setEnabled(true);
		filename_label.setText(file.getAbsolutePath());
		curStyle = "Standard";
	}

	@Override
	public void dispose() {
		//System.out.println("dispose()");
		// TODO Auto-generated method stub

	}

	private void closeFile() {
		//System.out.println("closeFile()");
		odtSync();
		file.delete();
		file = null;
	}

	@Override
	public boolean clear() {
		//System.out.println("clear()");
		SWTHelper.showError("TODO", "TODO: clear()");
		return false;
	}

	@Override
	public boolean createEmptyDocument() {
		//System.out.println("createEmptyDocument()");
		if (!ensureClosed()) {
			return false;
		}

		if (file != null) {
			closeFile();
		}

		try {
			file = File.createTempFile("elexis_", ".odt");
			file.deleteOnExit();

			odt = OdfTextDocument.newTextDocument();
			odt.save(file);
			fileValid();
		} catch (Exception e) {
			file = null;
			return false;
		}
		return true;
	}

	@Override
	public byte[] storeToByteArray() {
		if (!ensureClosed() || file == null) {
			return null;
		}

		ByteArrayOutputStream stream = new ByteArrayOutputStream();

		try {
			odt.save(stream);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

		return stream.toByteArray();
	}

	@Override
	public boolean loadFromByteArray(byte[] bs, boolean asTemplate) {
		ByteArrayInputStream stream = new ByteArrayInputStream(bs);
		return loadFromStream(stream, asTemplate);
	}

	@Override
	public boolean loadFromStream(InputStream is, boolean asTemplate) {
		//System.out.println("loadFromStream()");
		if (!ensureClosed()) {
			return false;
		}

		if (file != null) {
			closeFile();
		}

		try {
			file = File.createTempFile("elexis_", ".odt");
			file.deleteOnExit();

			odt = (OdfTextDocument) OdfDocument.loadDocument(is);
			odt.save(file);
			fileValid();
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}

		return true;
	}

	/**
	 * Ersetzt Tabs in einem Text-Node
	 * 
	 * @see formatText
	 * 
	 * @return Letzter enstandener Knoten
	 */
	private Text replaceTabs(OdfFileDom dom, Text text) {
		Node parent = text.getParentNode();
		Text cur = text;
		int i;

		while ((i = cur.getTextContent().indexOf('\t')) >= 0) {
			Text next = cur.splitText(i);
			next.setTextContent(next.getTextContent().substring(1));

			OdfTextTab tab = (OdfTextTab) OdfXMLFactory.newOdfElement(dom,
					OdfTextTab.ELEMENT_NAME);
			parent.insertBefore(tab, next);
			cur = next;
		}

		return cur;
	}

	/**
	 * Text-Node formatieren. Dabei werden Newlines und Tabs ersetzt. Der Knoten
	 * kann unter Umstaenden aufgespalten werden. {text} entspricht in dem Fall
	 * dem ersten Stueck.
	 * 
	 * @return Letzter Knoten, der beim aufsplitten entstanden ist
	 */
	private Text formatText(OdfFileDom dom, Text text) {
		Node parent = text.getParentNode();
		Text cur = text;
		int i;

		while ((i = cur.getTextContent().indexOf('\n')) >= 0) {
			Text next = cur.splitText(i);
			next.setTextContent(next.getTextContent().substring(1));

			OdfTextLineBreak lbrk = (OdfTextLineBreak) OdfXMLFactory
					.newOdfElement(dom, OdfTextLineBreak.ELEMENT_NAME);
			parent.insertBefore(lbrk, next);

			replaceTabs(dom, cur);
			cur = next;
		}

		return replaceTabs(dom, cur);
	}

	private boolean searchNode(Node n, Pattern pat, List<Text> matches,
			boolean onlyFirst) {
		boolean result = false;
		Node child = n.getFirstChild();
		while (child != null) {
			if (child instanceof Text) {
				Text bit = (Text) child;
				String content = bit.getTextContent();
				Matcher m = pat.matcher(content);

				if (m.find()) {
					int start = m.start();
					int end = m.end();

					// Wenn noetig fuehrendes Stueck abschneiden
					if (start != 0) {
						bit = bit.splitText(start);
						end -= start;
					}

					// Wenn noetig nachfolgendes Stueck abschneiden
					if (end != bit.getTextContent().length()) {
						bit.splitText(end);
					}

					result = true;
					matches.add(bit);
					child = bit;

					if (onlyFirst) {
						return true;
					}
				}
			}
			child = child.getNextSibling();
		}

		return result;
	}

	/**
	 * Text-Nodes finden deren Inhalt das uebergebene Pattern matcht. Dabei
	 * werden die Folgenden Elementtypen durchsucht: - text:p - text:span Es
	 * koennen vorhandene Text-Knoten aufgespalten werden.
	 */
	private List<Text> findTextNode(OdfFileDom dom, XPath xpath, Pattern pat,
			boolean onlyFirst) throws Exception {
		List<Text> result = new ArrayList<Text>();

		String types[] = { "//text:p", "//text:span" };
		for (String t : types) {
			NodeList bits = (NodeList) xpath.evaluate(t, dom,
					XPathConstants.NODESET);
			for (int i = 0; i < bits.getLength(); i++) {
				Node n = bits.item(i);
				if (searchNode(n, pat, result, onlyFirst) && onlyFirst) {
					return result;
				}
			}
		}
		return result;
	}

	/**
	 * Tabelle erstellen, an der stelle an der match steht. match wird aus dem
	 * Dokument entfernt.
	 * 
	 * Bei den Breiten werden Prozentwerte erwartet, oder null, falls die Breite
	 * auf alle Spalten gleichmaessig verteilt werden soll.
	 */
	private void makeTableAt(OdfFileDom dom, Text match, String[][] content,
			int[] widths) throws Exception {
		Node tableParent = match.getParentNode();

		// Find Parent-Node for table
		while (tableParent instanceof TextSpanElement) {
			tableParent = tableParent.getParentNode();
		}
		Node before = tableParent;
		tableParent = tableParent.getParentNode();

		// Create table
		TableTableElement table = (TableTableElement) OdfXMLFactory
				.newOdfElement(dom, TableTableElement.ELEMENT_NAME);
		tableParent.insertBefore(table, before);

		// Remove reference node
		// FIXME: There is probably a better solution
		before.getParentNode().removeChild(before);

		// Initialize columns
		if (content.length == 0) {
			return;
		}
		int colcount = content[0].length;
		if (widths == null) {
			// Create a column declaration for all columns
			TableTableColumnElement ttc = (TableTableColumnElement) OdfXMLFactory
					.newOdfElement(dom, TableTableColumnElement.ELEMENT_NAME);
			ttc.setTableNumberColumnsRepeatedAttribute(colcount);
			table.appendChild(ttc);
		} else {
			float percentval = 65535f / 100f;

			XPath xpath = odt.getXPath();
			OdfOfficeAutomaticStyles autost = (OdfOfficeAutomaticStyles) xpath
					.evaluate("//office:automatic-styles", dom,
							XPathConstants.NODE);

			for (int i = 0; i < widths.length; i++) {
				// Create Style for this column
				String stname = generateStyleName("col", odt.getContentDom(),
						odt.getStylesDom(), xpath);
				OdfStyle cst = (OdfStyle) OdfXMLFactory.newOdfElement(dom,
						StyleStyleElement.ELEMENT_NAME);
				cst.setStyleNameAttribute(stname);
				cst.setStyleFamilyAttribute("table-column");
				autost.appendChild(cst);

				OdfStyleTableColumnProperties stcp = (OdfStyleTableColumnProperties) OdfXMLFactory
						.newOdfElement(dom,
								StyleTableColumnPropertiesElement.ELEMENT_NAME);
				stcp.setStyleRelColumnWidthAttribute(Integer
						.toString((int) (widths[i] * percentval)));
				cst.appendChild(stcp);

				// Create Column declaration for this column
				TableTableColumnElement ttc = (TableTableColumnElement) OdfXMLFactory
						.newOdfElement(dom,
								TableTableColumnElement.ELEMENT_NAME);
				ttc.setStyleName(stname);
				table.appendChild(ttc);
			}
		}

		for (String[] row : content) {
			// Create row
			TableTableRowElement ttre = table.newTableTableRowElement();
			table.appendChild(ttre);
			for (String col : row) {
				if (col == null) {
					col = "";
				}

				// Create cell
				TableTableCellElement ttce = ttre.newTableTableCellElement();
				ttce.setOfficeValueTypeAttribute("string");
				ttre.appendChild(ttce);

				TextPElement tp = (TextPElement) OdfXMLFactory.newOdfElement(
						dom, TextPElement.ELEMENT_NAME);
				tp.setStyleName(curStyle);
				tp.setTextContent(col);
				ttce.appendChild(tp);

				// Format cell content
				Text t = (Text) tp.getFirstChild();
				if (t != null) {
					formatText(dom, t);
				}
			}
		}

	}

	private int findOrReplaceIn(OdfFileDom dom, Pattern pat,
			ReplaceCallback cb, XPath xpath) throws Exception {
		List<Text> matches = findTextNode(dom, xpath, pat, false);

		for (Text match : matches) {
			String text = match.getTextContent();
			Object replacement = cb.replace(text);
			String replstr;

			if (replacement == null) {
			} else if (replacement instanceof String) {
				replstr = (String) replacement;
				if (replstr.compareTo(text) != 0) {
					match.setTextContent(replstr);
					formatText(dom, match);
				}
			} else if (replacement instanceof String[][]) {
				try {
					makeTableAt(dom, match, (String[][]) replacement, null);
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else {
				replstr = "???Unbekannter Typ???";
				if (replstr.compareTo(text) != 0) {
					match.setTextContent(replstr);
				}
			}

		}

		return matches.size();
	}

	@Override
	public boolean findOrReplace(String pattern, ReplaceCallback cb) {
		if (editorRunning() || file == null) {
			return false;
		}

		int count = 0;
		try {
			Pattern pat = Pattern.compile(pattern);
			OdfFileDom contentDom = odt.getContentDom();
			OdfFileDom styleDom = odt.getStylesDom();
			XPath xpath = odt.getXPath();

			count += findOrReplaceIn(contentDom, pat, cb, xpath);
			count += findOrReplaceIn(styleDom, pat, cb, xpath);

			odtSync();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return count > 0;
	}

	@Override
	public boolean insertTable(String place, int properties,
			String[][] contents, int[] columnSizes) {
		// System.out.println("insertTable()" + this.hashCode());
		if (!ensureClosed() || file == null) {
			return false;
		}

		try {
			OdfFileDom contentDom = odt.getContentDom();
			XPath xpath = odt.getXPath();

			List<Text> texts = findTextNode(contentDom, xpath, Pattern
					.compile(Pattern.quote(place)), true);

			if (texts.size() == 0) {
				return false;
			}

			Text txt = texts.get(0);
			makeTableAt(contentDom, txt, contents, columnSizes);

			// TODO: Style
			odtSync();
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	@Override
	public Object insertText(String marke, String text, int adjust) {
		if (!ensureClosed() || file == null) {
			return null;
		}

		// System.out.println("insertText('"+marke+"', '"+text+"')");
		try {
			OdfFileDom contentDom = odt.getContentDom();
			XPath xpath = odt.getXPath();

			List<Text> texts = findTextNode(contentDom, xpath, Pattern
					.compile(Pattern.quote(marke)), true);

			if (texts.size() == 0) {
				return null;
			}

			Text txt = texts.get(0);
			txt.setTextContent(text);
			txt = formatText(contentDom, txt);

			// TODO: Style
			odtSync();
			return txt;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public Object insertText(Object pos, String text, int adjust) {
		if (!ensureClosed() || file == null || pos == null) {
			return null;
		}

		// System.out.println("insertText2('" + text + "')");
		try {
			OdfFileDom contentDom = odt.getContentDom();
			Text prev = (Text) pos;

			Text txt = prev.splitText(prev.getLength());
			txt.setTextContent(text);
			txt = formatText(contentDom, txt);

			// TODO: Style

			return txt;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Finde einen nicht benutzten Style-Namen der mit prefix beginnt, und mit
	 * einer beliebigen Zahl endet.
	 */
	private String generateStyleName(String prefix, OdfFileDom contentDom,
			OdfFileDom styleDom, XPath xpath) {
		NodeList nl;

		for (int i = 0;; i++) {
			String cur = prefix + i;

			String xp = "//*[@style:name='" + cur + "']";
			try {
				nl = (NodeList) xpath.evaluate(xp, contentDom,
						XPathConstants.NODESET);
				if (nl.getLength() > 0) {
					continue;
				}

				nl = (NodeList) xpath.evaluate(xp, styleDom,
						XPathConstants.NODESET);
				if (nl.getLength() > 0) {
					continue;
				}

				return cur;
			} catch (XPathExpressionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	@Override
	public Object insertTextAt(int x, int y, int w, int h, String text,
			int adjust) {
		if (!ensureClosed() || file == null) {
			return null;
		}

		try {
			OdfFileDom contentDom = odt.getContentDom();
			OdfFileDom styleDom = odt.getStylesDom();
			XPath xpath = odt.getXPath();

			// Generate Styles
			String frstyle = generateStyleName("fr", contentDom, styleDom,
					xpath);
			OdfOfficeAutomaticStyles autost = (OdfOfficeAutomaticStyles) xpath
					.evaluate("//office:automatic-styles", contentDom,
							XPathConstants.NODE);

			OdfStyle frst = (OdfStyle) OdfXMLFactory.newOdfElement(contentDom,
					StyleStyleElement.ELEMENT_NAME);
			frst.setStyleNameAttribute(frstyle);
			frst.setStyleFamilyAttribute("graphic");
			frst.setStyleParentStyleNameAttribute("Frame");
			autost.appendChild(frst);

			OdfStyleGraphicProperties gsp = (OdfStyleGraphicProperties) OdfXMLFactory
					.newOdfElement(contentDom,
							StyleGraphicPropertiesElement.ELEMENT_NAME);
			gsp.setStyleRunThroughAttribute("foreground");
			gsp.setStyleWrapAttribute("dynamic");
			gsp.setStyleNumberWrappedParagraphsAttribute("no-limit");
			gsp.setStyleVerticalPosAttribute("from-top");
			gsp.setStyleVerticalRelAttribute("page-content");
			gsp.setStyleHorizontalPosAttribute("from-left");
			gsp.setStyleHorizontalRelAttribute("page-content");
			gsp.setStyleBackgroundTransparencyAttribute("100%");
			gsp.setStyleShadowAttribute("none");
			// Strange, if transparent is chosen OO3 doesent display it
			// transparently
			gsp.setFoBackgroundColorAttribute("#ffffff");
			gsp.setFoPaddingAttribute("0cm");
			gsp.setFoBorderAttribute("none");
			frst.appendChild(gsp);

			OdfStyleBackgroundImage bgimg = (OdfStyleBackgroundImage) OdfXMLFactory
					.newOdfElement(contentDom,
							StyleBackgroundImageElement.ELEMENT_NAME);
			gsp.appendChild(bgimg);

			OdfStyleColumns scols = (OdfStyleColumns) OdfXMLFactory
					.newOdfElement(contentDom, StyleColumnsElement.ELEMENT_NAME);
			scols.setFoColumnCountAttribute(1);
			scols.setFoColumnGapAttribute("0cm");
			gsp.appendChild(scols);

			// Generate Content
			OdfOfficeText officeText = (OdfOfficeText) xpath.evaluate(
					"//office:text", contentDom, XPathConstants.NODE);

			OdfDrawFrame frame = (OdfDrawFrame) OdfXMLFactory.newOdfElement(
					contentDom, DrawFrameElement.ELEMENT_NAME);
			frame.setSvgXAttribute(x + "mm");
			frame.setSvgYAttribute(y + "mm");
			frame.setSvgWidthAttribute(w + "mm");
			// FIXME: Unschoener Workaround fuer platzproblem bei
			// Einzahlungsschein
			frame.setSvgHeightAttribute((h + 1) + "mm");
			frame.setTextAnchorTypeAttribute("page");
			frame.setTextAnchorPageNumberAttribute(1);
			frame.setDrawZIndexAttribute(0);
			frame.setDrawStyleNameAttribute(frstyle);
			frame.setDrawNameAttribute("Frame" + frstyle);
			officeText.insertBefore(frame, officeText.getFirstChild());

			OdfDrawTextBox textbox = (OdfDrawTextBox) OdfXMLFactory
					.newOdfElement(contentDom, DrawTextBoxElement.ELEMENT_NAME);
			frame.appendChild(textbox);

			OdfTextParagraph para = (OdfTextParagraph) OdfXMLFactory
					.newOdfElement(contentDom, TextPElement.ELEMENT_NAME);
			para.setTextContent(text);
			para.setStyleName(curStyle);
			textbox.appendChild(para);

			// TODO: Sauber?
			Text txt = (Text) para.getChildNodes().item(0);
			formatText(contentDom, txt);

			odtSync();
		} catch (Exception e) {
			e.printStackTrace();
		}

		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PageFormat getFormat() {
		// System.out.println("getFormat()");
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getMimeType() {
		return "application/vnd.oasis.opendocument.text";
	}

	@Override
	public void setFocus() {
		// TODO Auto-generated method stub

	}

	private String styleHash(String font, int style, float size) {
		MessageDigest m;
		try {
			String pass = font + "_" + style + "_" + size;
			m = MessageDigest.getInstance("MD5");
			byte[] data = pass.getBytes();
			m.update(data, 0, data.length);
			BigInteger i = new BigInteger(1, m.digest());
			return String.format("%1$032X", i);
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public boolean setFont(String name, int style, float size) {
		if (!ensureClosed() || file == null) {
			return false;
		}

		String sname = styleHash(name, style, size);

		try {
			OdfFileDom contentDom = odt.getContentDom();
			XPath xpath = odt.getXPath();

			// Declare font face
			OdfOfficeFontFaceDecls ffdec = (OdfOfficeFontFaceDecls) xpath
					.evaluate("//office:font-face-decls", contentDom,
							XPathConstants.NODE);

			OdfStyleFontFace fface = (OdfStyleFontFace) ffdec.getFirstChild();
			NodeList nl = (NodeList) xpath.evaluate(
					"//office:font-face-decls/style:font-face[@style:name='"
							+ name + "']", contentDom, XPathConstants.NODESET);
			if (nl.getLength() == 0) {
				fface = (OdfStyleFontFace) OdfXMLFactory.newOdfElement(
						contentDom, StyleFontFaceElement.ELEMENT_NAME);
				fface.setStyleNameAttribute(name);
				fface.setStyleFontPitchAttribute("variable");
				fface.setSvgFontFamilyAttribute("'" + name + "'");
				ffdec.appendChild(fface);
			}

			// Create Style
			nl = (NodeList) xpath.evaluate("//style:style[@style:name='"
					+ sname + "']", contentDom, XPathConstants.NODESET);
			if (nl.getLength() == 0) {
				OdfOfficeAutomaticStyles autost = (OdfOfficeAutomaticStyles) xpath
						.evaluate("//office:automatic-styles", contentDom,
								XPathConstants.NODE);

				OdfStyle frst = (OdfStyle) OdfXMLFactory.newOdfElement(
						contentDom, StyleStyleElement.ELEMENT_NAME);
				frst.setStyleNameAttribute(sname);
				frst.setStyleFamilyAttribute("paragraph");
				frst.setStyleParentStyleNameAttribute("Standard");
				frst.setStyleParentStyleNameAttribute("Frame");
				autost.appendChild(frst);

				OdfStyleTextProperties stp = (OdfStyleTextProperties) OdfXMLFactory
						.newOdfElement(contentDom,
								StyleTextPropertiesElement.ELEMENT_NAME);
				stp.setStyleFontNameAttribute(name);
				stp.setFoFontSizeAttribute(size + "pt");
				stp.setStyleFontSizeAsianAttribute(size + "pt");
				stp.setStyleFontSizeComplexAttribute(size + "pt");
				frst.appendChild(stp);
			}

			curStyle = sname;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}

		return true;
	}

	@Override
	public void setFormat(PageFormat f) {
		// System.out.println("setFormat");
		// TODO Auto-generated method stub

	}

	@Override
	public void setSaveOnFocusLost(boolean bSave) {
		// System.out.println("setSaveOnFocusLost");
		// TODO Auto-generated method stub

	}

	@Override
	public boolean setStyle(int style) {
		// System.out.println("setStyle");
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void showMenu(boolean b) {
		// TODO Auto-generated method stub

	}

	@Override
	public void showToolbar(boolean b) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setInitializationData(IConfigurationElement config,
			String propertyName, Object data) throws CoreException {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean isDirectOutput() {
		// TODO: Make sure that false is what we want here...
		return false;
	}

}
