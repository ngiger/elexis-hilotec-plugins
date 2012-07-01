package com.hilotec.elexis.kgview.diagnoseliste;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.custom.TreeEditor;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.HTMLTransfer;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.part.ViewPart;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import com.hilotec.elexis.kgview.diagnoseliste.DiagnoselisteItem;

import ch.elexis.Desk;
import ch.elexis.actions.ElexisEvent;
import ch.elexis.actions.ElexisEventDispatcher;
import ch.elexis.actions.ElexisEventListener;
import ch.elexis.data.Patient;
import ch.elexis.data.PersistentObject;
import ch.elexis.util.PersistentObjectDragSource;
import ch.elexis.util.PersistentObjectDropTarget;
import ch.elexis.util.SWTHelper;
import ch.elexis.util.ViewMenus;

/*
 * Helper class to draw multiline tree items, kind of ugly, but according
 * to the eclipse forum there is no other way at the moment.
 *
 * Derived From: http://git.eclipse.org/c/platform/
 *  + eclipse.platform.swt.git/tree/examples/org.eclipse.swt.snippets/
 *  + src/org/eclipse/swt/snippets/Snippet227.java
 * Copyright (c) 2000, 2006 IBM Corporation and others (EPL)
 */
class MultilinePaintListener implements Listener {
	public void handleEvent(Event event) {
		switch(event.type) {
			case SWT.MeasureItem: {
				TreeItem item = (TreeItem)event.item;
				String text = getText(item, event.index);
				Point size = event.gc.textExtent(text);
				event.width = size.x;
				event.height = Math.max(event.height, size.y);
				break;
			}
			case SWT.PaintItem: {
				TreeItem item = (TreeItem)event.item;
				String text = getText(item, event.index);
				Point size = event.gc.textExtent(text);
				int offset2 = event.index == 0 ? Math.max(0, (event.height - size.y) / 2) : 0;
				event.gc.drawText(text, event.x, event.y + offset2, true);
				break;
			}
			case SWT.EraseItem: {
				event.detail &= ~SWT.FOREGROUND;
				break;
			}
		}
	}
	String getText(TreeItem item, int column) {
		String text = item.getText(column);
		/*if (column != 0) {
			TreeItem parent = item.getParentItem();
			int index = parent == null ? tree.indexOf(item) : parent.indexOf(item);
			if ((index+column) % 3 == 1){
				text +="\nnew line";
			}
			if ((index+column) % 3 == 2) {
				text +="\nnew line\nnew line";
			}
		}*/
		return text;
	}
}

/**
 * Nicht-persistente Representation eines Diagnosebaums.
 */
class DNode {
	public String text = "";
	public ArrayList<DNode> children = new ArrayList<DNode>();

	/**
	 * Neues Kindelement erstellen.
	 */
	public DNode newChild() {
		DNode dn = new DNode();
		children.add(dn);
		return dn;
	}

	/**
	 * Text an diesen Knoten anhaengen
	 */
	public void append(String s) {
		text += s;
	}

	/**
	 * Text aufgeraeumt zurueckgeben
	 */
	public String getClean() {
		return text.trim()
					.replaceAll("[ \t]+", " ")
					.replaceAll("\n+", "\n");
	}

	/**
	 * Unter-Nodes in DiagnoselisteItem persistent abspeichern
	 */
	public void storeChildren(DiagnoselisteItem it) {
		for (DNode c: children) {
			DiagnoselisteItem ci = it.createChild();
			ci.setText(c.getClean());
			c.storeChildren(ci);
		}
	}
}


/**
 * Dialog zum anzeigen eines Baums aus DNodes.
 */
class DLDialog extends TitleAreaDialog {
	private DNode di;
	private Tree tree;

	public DLDialog(Shell parentShell, DNode di) {
		super(parentShell);
		this.di = di;
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		ScrolledComposite scroll = new ScrolledComposite(parent, SWT.BORDER | SWT.V_SCROLL);
		Composite comp = new Composite(scroll, SWT.NONE);

		scroll.setLayoutData(SWTHelper.getFillGridData(1, true, 1, true));
		scroll.setContent(comp);
		comp.setLayout(new GridLayout());
		comp.setLayoutData(SWTHelper.getFillGridData(1, true, 1, false));

		tree = new Tree(comp, SWT.NONE);
		tree.setLayoutData(SWTHelper.getFillGridData(1, true, 1, false));
		final TreeEditor editor = new TreeEditor(tree);
		editor.grabHorizontal = true;
		editor.grabVertical = true;

		MultilinePaintListener mlListener = new MultilinePaintListener();
		tree.addListener(SWT.MeasureItem, mlListener);
		tree.addListener(SWT.PaintItem, mlListener);
		tree.addListener(SWT.EraseItem, mlListener);

		addNodes(di, null);

		comp.setSize(comp.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		return scroll;
	}

	private void addNodes(DNode dn, TreeItem parent) {
		TreeItem ti;
		for (DNode n: dn.children) {
			if (parent == null)
				ti = new TreeItem(tree, 0);
			else
				ti = new TreeItem(parent, 0);

			ti.setText(n.getClean());
			addNodes(n, ti);
			ti.setExpanded(true);
		}
	}

	@Override
	public void create(){
		super.create();
		getShell().setText("Diagnosen Importieren");
	}

	@Override
	protected void okPressed() {
		close();
	}
}

/**
 * Parser um copy&paste-Daten aus OpenOffice zu uebernehmen. Sehr hacky.
 */
class DLParser {
	/**
	 * Versucht den String in eine DNode-Repraesentation zu parsen
	 */
	public DNode parse(String html) {
		DNode dn = parseItems(html);
		if (dn == null) {
			dn = parseManualList(html);
		}
		System.out.println(dn);
		if (dn != null) {
			for (DNode c: dn.children) {
				System.out.println(c.text);
			}
		}
		return dn;
	}

	/**
	 * Root Element fuer XML Dokument geparst aus code holen
	 */
	private Element parseXML(String code) {
		try {
			byte[] bytes = code.getBytes();
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();

			dbFactory.setValidating(false);
			dbFactory.setNamespaceAware(true);
			dbFactory.setIgnoringComments(false);
			dbFactory.setIgnoringElementContentWhitespace(false);
			dbFactory.setExpandEntityReferences(false);

			DocumentBuilder dBuilder;
			dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(new ByteArrayInputStream(bytes));
			return doc.getDocumentElement();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * HTML UL-Aufzaehlungsliste parsen
	 */
	private  DNode parseItems(String html) {
		System.out.println("{{" + html + "}}");

		Element e = parseXML(html);
		if (e == null) return null;

		DNode dn = new DNode();
		parseUL(e, dn);
		return dn;
	}

	/**
	 * Konkretes UL-Element parsen, indirekt rekursiv. Dabei wird dn benutzt um
	 * die entstehenden Kindknoten einzutragen.
	 */
	private void parseUL(Element e, DNode dn) {
		if (!e.getNodeName().equals("ul")) return;
		NodeList nl = e.getChildNodes();
		for (int i = 0; i < nl.getLength(); i++) {
			Node n = nl.item(i);
			if (n instanceof Element && n.getNodeName().equals("li")) {
				Element li = (Element) n;
				li.normalize();
				parseLI(li, dn.newChild());
			}
		}
	}

	/**
	 * Einzelnes LI-Element einer UL-Liste parsen, rekursiv. Der Text und
	 * allfaellige Kindknoten werden in dn eingetragen.
	 */
	private void parseLI(Element li, DNode dn) {
		NodeList nl = li.getChildNodes();

		for (int i = 0; i < nl.getLength(); i++) {
			Node n = nl.item(i);

			if (n instanceof Element) {
				Element e = (Element) n;
				e.normalize();

				if (e.getNodeName().equals("ul")) {
					parseUL(e, dn);
				} else if (e.getNodeName().equals("br")) {
					dn.append("\n");
				} else if (e.getNodeName().equals("p")) {
					dn.append("\n");
					parseLI(e, dn);
					dn.append("\n");
				} else {
					parseLI(e, dn);
				}
			} else if (n instanceof Text) {
				Text t = (Text) n;
				dn.append(t.getTextContent().trim());
			}
		}
	}

	/**
	 * Parsen der manuell erstellten Aufzaehlungslisten (mit - und tabulatoren)
	 * Gar nicht allgemein.
	 */
	private DNode parseManualList(String html) {
		try {
			// Versuchen html in gueltiges XML umzuwandeln
			String enc = "utf-8";
			if(System.getProperty("os.name").startsWith("Windows"))
				enc = "iso-8859-1";
			html = "<?xml version=\"1.0\" encoding=\""+enc+"\"?>" +
					"<root>\n" + html + "\n</root>";
			html = html.replaceAll("<br>", "<br/>");
			html = html.replaceAll("<BR>", "<br/>");
			html = html.replaceAll("ALIGN=JUSTIFY", "");
			html = html.replaceAll("&shy;", "-");

			System.out.println("Cleaned xml:\n{{" + html + "}}");

			// Parsen
			Element root = parseXML(html);
			NodeList nl = root.getChildNodes();

			Stack<DNode> stack = new Stack<DNode>();
			stack.push(new DNode());
			for (int i = 0; i < nl.getLength(); i++) {
				Node n = nl.item(i);
				if (!(n instanceof Element)) continue;
				Element e = (Element) n;

				// Wir wollen nur P-elemente
				String txt = e.getTextContent();
				if (!e.getNodeName().equalsIgnoreCase("p") ||
					!txt.startsWith("-"))
				{
					throw new Exception("Nicht p-Element");
				}

				// Pruefen ob das class-Attribut mit list-n- anfaengt
				String c = e.getAttribute("class");
				if (c.isEmpty()) c = e.getAttribute("CLASS");
				c = c.toLowerCase();
				if (!c.matches("liste?-[0-9]-.*")) {
					throw new Exception("p-Element ohne passende class");
				}

				// level Zahl aus class parsen
				int lvl = Integer.parseInt(
						c.replaceAll("^liste?-", "").substring(0, 1));
				while (stack.size() > lvl) stack.pop();
				if (stack.size() < lvl) {
					throw new Exception("Stack underflow beim parsen");
				}

				// Knoten erstellen
				DNode dn = stack.peek().newChild();
				parseManualP(dn, e);
				// Text etwas aufraeumen
				dn.text = dn.text.substring(1).replaceAll("[ \t\n\r]+", " ");
				dn.text = dn.text.trim();
				stack.push(dn);
			}
			while (stack.size() > 1) stack.pop();
			return stack.pop();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Text aus P-Element in manuell erstellter Liste parsen
	 * @throws Exception 
	 */
	private void parseManualP(DNode dn, Element e) throws Exception {
		e.normalize();
		NodeList nl = e.getChildNodes();
		for (int i = 0; i < nl.getLength(); i++) {
			Node n = nl.item(i);
			if (n instanceof Text) {
				dn.append(n.getNodeValue());
			} else if (n instanceof Element &&
					n.getNodeName().equalsIgnoreCase("br"))
			{
				dn.append("\n");
			} else {
				throw new Exception("Ungueltigen Node angetroffen: {" +
						n.getNodeName()+"}");
			}
		}
	}
}


public abstract class DiagnoselisteBaseView extends ViewPart
		implements ElexisEventListener
{
	/** Typ der anzuzeigenden items */
	protected final int typ;

	/** Sollen neue eintraege erstellt werden koenenn? */
	protected boolean canAdd = true;

	/** Sollen alle Eintraege geloescht werden koennen? */
	protected boolean canClear = false;

	/** Soll das Datum bei Eintraegen angezeigt werden? */
	protected boolean showDate = true;

	/** Koennen Eintraege anderes Typs importiert werden (SA, PA) */
	protected boolean allowImport = false;
	
	/** Koennen Eintraege aus der Diagnoseliste importiert werden (uebergang) */
	protected boolean allowImportDL = true;
	
	/** Koennen Eintraege aus der Zwischenablage importiert werden? */
	protected boolean allowImportCB = true;


	private Tree tree;
	Action actAdd;
	Action actEdit;
	Action actAddChild;
	Action actDel;
	Action actClear;
	Action actMoveUp;
	Action actMoveDown;
	Action actImportPA;
	Action actImportSA;
	Action actImportDL;
	Action actImportCB;

	/**
	 * Diagnoseliste Anzeige fuer Items eines bestimmten Typs initialisieren.
	 *
	 * @param typ Typ der anzuzeigenden Elemente
	 */
	public DiagnoselisteBaseView(int typ) {
		super();
		this.typ = typ;
	}


	private void setupTI(TreeItem ti, DiagnoselisteItem di) {
		if (showDate) {
			ti.setText(di.getText() + " (" + di.getDatum() + ")");
		} else {
			ti.setText(di.getText());
		}
		ti.setData(di);
	}

	/**
	 * Neues TreeItem für die übergebene Diagnose am angegebenen Index
	 * erstellen.
	 */
	private TreeItem createTI(DiagnoselisteItem di, TreeItem tip, int index) {
		TreeItem ti;
		if (tip == null)
			ti = new TreeItem(tree, SWT.NONE, index);
		else
			ti = new TreeItem(tip, SWT.NONE, index);
		setupTI(ti, di);
		return ti;
	}

	private void insertSubtree(DiagnoselisteItem dip, TreeItem tip) {
		TreeItem ti;
		for (DiagnoselisteItem di: dip.getChildren()) {
			ti = createTI(di, tip, di.getPosition());
			insertSubtree(di, ti);
		}
	}

	private void updateTree() {
		updateTree(ElexisEventDispatcher.getSelectedPatient());
	}

	private void updateTree(Patient pat) {
		tree.removeAll();
		boolean en = (pat != null);

		actAdd.setEnabled(en && canAdd);
		actAddChild.setEnabled(en && canAdd);

		actEdit.setEnabled(en);
		actDel.setEnabled(en);
		actMoveUp.setEnabled(en);
		actMoveDown.setEnabled(en);

		actImportPA.setEnabled(en && allowImport);
		actImportSA.setEnabled(en && allowImport);
		actImportDL.setEnabled(en && allowImportDL);
		actImportCB.setEnabled(en && allowImportCB);
		actClear.setEnabled(en && canClear);

		if (pat == null) return;

		insertSubtree(DiagnoselisteItem.getRoot(pat, typ), null);
	}

	/**
	 * Root-Item eines bestimmten Typs fuer den aktuellen Patienten suchen
	 */
	private static DiagnoselisteItem getRoot(int typ) {
		Patient pat = ElexisEventDispatcher.getSelectedPatient();
		return DiagnoselisteItem.getRoot(pat, typ);
	}

	@Override
	public void createPartControl(Composite parent) {
		tree = new Tree(parent, SWT.NONE);

		final TreeEditor editor = new TreeEditor(tree);
		editor.horizontalAlignment = SWT.LEFT;
		editor.grabHorizontal = true;

		// SelectionListener um Eintraege inline bearbeiten zu koennen.
		/*tree.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) { }
			public void widgetSelected(SelectionEvent e) {
				TreeItem diagItem = (TreeItem) e.item;
				if (diagItem == null) return;

	            Control old = editor.getEditor();
	            if (old != null) old.dispose();

	            Text diagEditor = new Text(tree, SWT.MULTI | SWT.WRAP);
	            diagEditor.setData(diagItem);
	            diagEditor.setText(diagItem.getText());

	            // ModifyListener um Aenderungen zu speichern
	            diagEditor.addModifyListener(new ModifyListener() {
					public void modifyText(ModifyEvent e) {
						Text te = (Text) e.widget;
						TreeItem ti = (TreeItem) te.getData();
						DiagnoselisteItem di = (DiagnoselisteItem) ti.getData();
						ti.setText(te.getText());
						di.setText(te.getText());
					}
				});
	            //diagEditor.selectAll();
	            diagEditor.setFocus();
	            editor.setEditor(diagEditor, diagItem);
			}
		});*/
		MultilinePaintListener mlListener = new MultilinePaintListener();
		tree.addListener(SWT.MeasureItem, mlListener);
		tree.addListener(SWT.PaintItem, mlListener);
		tree.addListener(SWT.EraseItem, mlListener);

		// Drop Target um neue Eintraege zu erstellen
		new PersistentObjectDropTarget(tree,
				new PersistentObjectDropTarget.IReceiver()
		{
					@Override
					public void dropped(PersistentObject o, DropTargetEvent e) {
						DiagnoselisteItem d = (DiagnoselisteItem) o;
						DiagnoselisteItem root = getRoot(typ);
						if (root == null) return;

						importItemTo(d, root);
						updateTree();
					}

					@Override
					public boolean accept(PersistentObject o) {
						if (!(o instanceof DiagnoselisteItem)) return false;
						DiagnoselisteItem d = (DiagnoselisteItem) o;

						// Droppen nur erlauben wenn es sich um ein Toplevel
						// Element handelt
						DiagnoselisteItem root = getRoot(d.getTyp());
						if (root == null || !d.getParent().equals(root))
							return false;

						// Pruefen ob der Typ importiert werden kann
						int typ = d.getTyp();
						if (typ == DiagnoselisteItem.TYP_DIAGNOSELISTE && allowImportDL)
							return true;
						else if (allowImport &&
								(typ == DiagnoselisteItem.TYP_PERSANAMNESE ||
								 typ == DiagnoselisteItem.TYP_SYSANAMNESE))
							return true;

						return false;
					}
		});

		new PersistentObjectDragSource(tree,
				new PersistentObjectDragSource.ISelectionRenderer()
		{
			public List<PersistentObject> getSelection() {
				TreeItem[] tis = tree.getSelection();
				if (tis == null) return null;

				DiagnoselisteItem root = getRoot(typ);
				ArrayList<PersistentObject> res =
					new ArrayList<PersistentObject>(tis.length);
				// Auswahl in Liste von Items umwandeln
				for (TreeItem ti: tis) {
					DiagnoselisteItem di = (DiagnoselisteItem) ti.getData();
					res.add(di);

					// Wenn es sich nicht um ein Toplevel-Item handelt, brechen
					// wir ab
					if (!di.getParent().equals(root)) {
						return null;
					}
				}
				return res;
			}
		});

		makeActions();
		tree.addMouseListener(new MouseListener() {
			public void mouseUp(MouseEvent e) {}
			public void mouseDown(MouseEvent e) {}
			public void mouseDoubleClick(MouseEvent e) {
				actEdit.run();
			}
		});



		// Menus oben rechts in der View
		ViewMenus menus = new ViewMenus(getViewSite());
		menus.createToolbar(actAdd, actMoveUp, actMoveDown);
		menus.createMenu(actImportPA, actImportSA, actImportDL, actImportCB,
				actClear);
		menus.createControlContextMenu(tree, actAddChild, actDel);

		ElexisEventDispatcher.getInstance().addListeners(this);
		updateTree();
	}

	/**
	 * Importiert alle Kind-Items von sp nach dp.
	 */
	private void importFrom(DiagnoselisteItem sp, DiagnoselisteItem dp) {
		for (DiagnoselisteItem si: sp.getChildren()) {
			importItemTo(si, dp);
		}
	}

	/**
	 * Importiert item als Kind-Item in newParent item falls es noch nicht
	 * enthalten ist. Sonst werden nur die Unterelemente rekursiv eingefuegt.
	 *
	 * @param item      Quell-Item
	 * @param newParent Neues Ziel-Parent Item
	 */
	private void importItemTo(DiagnoselisteItem item,
			DiagnoselisteItem newParent)
	{
		DiagnoselisteItem di = newParent.getChildBySrc(item);
		if (di == null) {
			di = newParent.createChildFrom(item);
		}

		// Rekursiv Kind-Item behandeln
		importFrom(item, di);
	}


	/**
	 * Action zum Importieren aus Views eines anderen Typs.
	 */
	private class ImportAction extends Action {
		int fromTyp;

		public ImportAction(int typ) {
			super();
			if (typ == DiagnoselisteItem.TYP_PERSANAMNESE) {
				setText("Import Pers. Anamnese");
			} else if (typ == DiagnoselisteItem.TYP_SYSANAMNESE){
				setText("Import Systemanamnese");
			} else if (typ == DiagnoselisteItem.TYP_DIAGNOSELISTE) {
				setText("Import Diagnoseliste");
			}
			setImageDescriptor(Desk.getImageDescriptor(Desk.IMG_IMPORT));

			fromTyp = typ;
		}

		@Override
		public void run() {
			Patient pat = ElexisEventDispatcher.getSelectedPatient();
			importFrom(DiagnoselisteItem.getRoot(pat, fromTyp),
					   DiagnoselisteItem.getRoot(pat, typ));
			updateTree(pat);
		}
	}

	private void makeActions() {
		actEdit = new Action("Bearbeiten") { {
				setImageDescriptor(Desk.getImageDescriptor(Desk.IMG_EDIT));
			}
			@Override
			public void run() {
				if (tree.getSelectionCount() == 0) return;
				TreeItem ti = tree.getSelection()[0];
				DiagnoselisteItem di = (DiagnoselisteItem) ti.getData();
				(new DiagnoseDialog(getSite().getShell(), di, showDate)).open();
				setupTI(ti, di);
			}
		};


		actAdd = new Action("Neue Kategorie") { {
				setImageDescriptor(Desk.getImageDescriptor(Desk.IMG_NEW));
			}
			@Override
			public void run() {
				Patient p = ElexisEventDispatcher.getSelectedPatient();
				DiagnoselisteItem root = DiagnoselisteItem.getRoot(p, typ);
				DiagnoselisteItem di = root.createChild();
				(new DiagnoseDialog(getSite().getShell(), di, showDate)).open();
				createTI(di, null, di.getPosition());
			}
		};

		actAddChild = new Action("Neue Unterdiagnose") { {
				setImageDescriptor(Desk.getImageDescriptor(Desk.IMG_ADDITEM));
			}
			@Override
			public void run() {
				TreeItem[] tis = tree.getSelection();
				if (tis.length > 0) {
					DiagnoselisteItem di = (DiagnoselisteItem) tis[0].getData();
					DiagnoselisteItem ndi = di.createChild();

					DiagnoseDialog dd =  new DiagnoseDialog(
							getSite().getShell(), ndi, showDate);
					if (dd.open() == DiagnoseDialog.OK) {
						createTI(ndi, tis[0], ndi.getPosition());
						// Parent expanden
						tis[0].setExpanded(true);
					} else {
						ndi.delete();
					}
				}
			}
		};
		actDel = new Action("Löschen") { {
				setImageDescriptor(Desk.getImageDescriptor(Desk.IMG_DELETE));
			}
			@Override
			public void run() {
				TreeItem[] tis = tree.getSelection();
				if (tis.length > 0) {
					DiagnoselisteItem di = (DiagnoselisteItem) tis[0].getData();
					if (!di.getChildren().isEmpty()) {
						SWTHelper.alert("Es existieren noch Unterdiagnosen",
							"Bitte zuerst alle Unterdiagnosen der zu löschenden"
							+ " Diagnose entfernen.");
						return;
					}
					di.delete();
					tis[0].dispose();
				}
			}
		};
		actClear = new Action("Alle Löschen") {
			@Override
			public void run() {
				if (SWTHelper.askYesNo("Alle Eintraege loeschen",
						"Sollen alle Eintraege unweiderruflich gelöscht werden?"))
				{
					Patient pat = ElexisEventDispatcher.getSelectedPatient();
					DiagnoselisteItem.getRoot(pat, typ).deleteChildren();
					updateTree();
				}
			}
		};
		actMoveUp = new Action("Hoch") { {
				setImageDescriptor(Desk.getImageDescriptor(Desk.IMG_ARROWUP));
			}
			@Override
			public void run() {
				TreeItem[] tis = tree.getSelection();
				if (tis.length > 0) {
					DiagnoselisteItem di = (DiagnoselisteItem) tis[0].getData();
					di.moveUp();

					TreeItem parent = tis[0].getParentItem();
					tis[0].dispose();

					// Item neu erstellen an neuer Position
					TreeItem ti = createTI(di, parent, di.getPosition());
					insertSubtree(di, ti);
					tree.select(ti);
				}
			}
		};
		actMoveDown = new Action("Runter") { {
				setImageDescriptor(Desk.getImageDescriptor(Desk.IMG_ARROWDOWN));
			}
			@Override
			public void run() {
				TreeItem[] tis = tree.getSelection();
				if (tis.length > 0) {
					DiagnoselisteItem di = (DiagnoselisteItem) tis[0].getData();
					di.moveDown();

					TreeItem parent = tis[0].getParentItem();
					tis[0].dispose();

					// Item neu erstellen an neuer Position
					TreeItem ti = createTI(di, parent, di.getPosition());
					insertSubtree(di, ti);
					tree.select(ti);
				}
			}
		};

		actImportPA = new ImportAction(DiagnoselisteItem.TYP_PERSANAMNESE);
		actImportSA = new ImportAction(DiagnoselisteItem.TYP_SYSANAMNESE);
		actImportDL = new ImportAction(DiagnoselisteItem.TYP_DIAGNOSELISTE);
		
		actImportCB = new Action("Import Zwischenablage") {
			{
				setImageDescriptor(Desk.getImageDescriptor(Desk.IMG_IMPORT));
				setToolTipText("Aus Zwischenablage importieren");
				setDescription("Sollen die Diagnosen wie unten angegeben "+
						"importiert werden?");
			}

			@Override
			public void run() {
				// Daten aus Zwischenablage holen
				Clipboard cb = new Clipboard(Desk.getDisplay());
				String s = (String) cb.getContents(HTMLTransfer.getInstance());
				if (s == null) return;

				// Wurzel des Diagnosebaums holen
				Patient p = ElexisEventDispatcher.getSelectedPatient();
				DiagnoselisteItem root = DiagnoselisteItem.getRoot(p, typ);

				// Daten parsen
				DNode dn = new DLParser().parse(s);
				if (dn == null) return;

				DLDialog di = new DLDialog(Desk.getTopShell(), dn);
				if (di.open() == DLDialog.OK) {
					dn.storeChildren(root);
					updateTree(p);
				}
			}
		};

	}

	@Override
	public void setFocus() {
	}

	@Override
	public void dispose() {
		ElexisEventDispatcher.getInstance().removeListeners(this);
	}

	public void catchElexisEvent(ElexisEvent ev) {
		if (ev.getType() == ElexisEvent.EVENT_SELECTED) {
			updateTree((Patient) ev.getObject());
		}
	}

	private final ElexisEvent eetmpl = new ElexisEvent(null, Patient.class,
			ElexisEvent.EVENT_SELECTED);
	public ElexisEvent getElexisEventFilter() {
		return eetmpl;
	}

}