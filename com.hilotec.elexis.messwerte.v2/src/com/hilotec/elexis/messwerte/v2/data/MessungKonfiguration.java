/*******************************************************************************
 * Copyright (c) 2007-2010, A. Kaufmann and Elexis
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    A. Kaufmann - initial implementation 
 *    G. Weirich - added layout option
 *    P. Chaubert - adapted to Messwerte V2
 *    
 * $Id$
 *******************************************************************************/

package com.hilotec.elexis.messwerte.v2.data;

import java.io.File;
import java.io.FileInputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.LinkedList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import com.hilotec.elexis.messwerte.v2.data.typen.IMesswertTyp;
import com.hilotec.elexis.messwerte.v2.data.typen.MesswertTypBool;
import com.hilotec.elexis.messwerte.v2.data.typen.MesswertTypCalc;
import com.hilotec.elexis.messwerte.v2.data.typen.MesswertTypCount;
import com.hilotec.elexis.messwerte.v2.data.typen.MesswertTypData;
import com.hilotec.elexis.messwerte.v2.data.typen.MesswertTypDate;
import com.hilotec.elexis.messwerte.v2.data.typen.MesswertTypEnum;
import com.hilotec.elexis.messwerte.v2.data.typen.MesswertTypNum;
import com.hilotec.elexis.messwerte.v2.data.typen.MesswertTypScale;
import com.hilotec.elexis.messwerte.v2.data.typen.MesswertTypStr;
import com.hilotec.elexis.messwerte.v2.views.Preferences;

import ch.elexis.Hub;
import ch.elexis.util.Log;
import ch.elexis.util.PlatformHelper;
import ch.elexis.util.SWTHelper;
import ch.rgw.tools.ExHandler;

public class MessungKonfiguration {
	public static final String ATTR_TYPE = "type";
	public static final String NAME_DATAFIELD = "datafield";
	public static final String NAME_LAYOUTFIELD = "design";
	public static final String ATTR_SOURCE = "source";
	public static final String ELEMENT_VAR = "var";
	public static final String ATTR_INTERPRETER = "interpreter";
	public static final String ELEMENT_FORMULA = "formula";
	public static final String NAME_CALCFIELD = "calcfield";
	public static final String ATTR_VALUE = "value";
	public static final String NAME_ENUMFIELD = "enumfield";
	public static final String NAME_STRINGFIELD = "strfield";
	public static final String NAME_BOOLFIELD = "boolfield";
	public static final String NAME_NUMFIELD = "numfield";
	public static final String NAME_SCALEFIELD = "scalefield";
	public static final String ATTR_DEFAULT = "default";
	public static final String ATTR_UNIT = "unit";
	public static final String ATTR_TITLE = "title";
	public static final String ATTR_NAME = "name";
	public static final String ATTR_MAX = "max";
	public static final String ATTR_MIN = "min";
	public static final String ATTR_LINES = "lines";
	public static final String ELEMENT_DATATYPE = "datatype";
	public static final String CONFIG_FILENAME = "messwerte_v2.xml";
	public static final String ATTR_FORMATPATTERN = "formatpattern";
	public static final String ATTR_ROUNDMODE = "roundingmode";
	public static final String NAME_DATEFIELD = "datefield";
	public static final String NAME_COUNTERFIELD = "counterfield";
	public static final String ATTR_COUNTERMODE = "countermode";
	public static final String ATTR_STARTVALUE = "startvalue";
	
	private static MessungKonfiguration the_one_and_only_instance = null;
	ArrayList<MessungTyp> types;
	private final Log log = Log.get("DataConfiguration");
	private String defaultFile;
	
	public static MessungKonfiguration getInstance(){
		if (the_one_and_only_instance == null) {
			the_one_and_only_instance = new MessungKonfiguration();
		}
		return the_one_and_only_instance;
	}
	
	private MessungKonfiguration(){
		types = new ArrayList<MessungTyp>();
		defaultFile =
			Hub.localCfg.get(Preferences.CONFIG_FILE, Hub.getWritableUserDir() + File.separator
				+ CONFIG_FILENAME);
		
		readFromXML(null);
	}
	
	private Panel createPanelFromNode(Element n){
		String type = n.getAttribute("type");
		Panel ret = new Panel(type);
		LinkedList<String> attributeList = new LinkedList<String>();
		LinkedList<Panel> panelsList = new LinkedList<Panel>();
		Node node = n.getFirstChild();
		while (node != null) {
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				String nodename = node.getNodeName();
				if (nodename.equals("attribute")) {
					NamedNodeMap na = node.getAttributes();
					String nx =
						na.getNamedItem("name").getNodeValue() + "="
							+ na.getNamedItem("value").getNodeValue();
					attributeList.add(nx);
				} else if (nodename.equals("panel")) {
					panelsList.add(createPanelFromNode((Element) node));
				}
			}
			node = node.getNextSibling();
		}
		ret.setAttributes(attributeList.toArray(new String[0]));
		ret.setPanels(panelsList.toArray(new Panel[0]));
		return ret;
		
	}
	
	public void readFromXML(String path){
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder;
		Document doc;
		SchemaFactory sfac = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
		
		if (path == null) {
			path = defaultFile;
		}
		try {
			String schemapath =
				PlatformHelper.getBasePath("com.hilotec.elexis.messwerte.v2") + File.separator
					+ "rsc" + File.separator + "messwerte.xsd";
			Schema s = sfac.newSchema(new File(schemapath));
			factory.setSchema(s);
			
			builder = factory.newDocumentBuilder();
			builder.setErrorHandler(new ErrorHandler() {
				public void error(SAXParseException exception) throws SAXException{
					throw exception;
				}
				
				public void fatalError(SAXParseException exception) throws SAXException{
					throw exception;
				}
				
				public void warning(SAXParseException exception) throws SAXException{
					throw exception;
				}
			});
			doc = builder.parse(new FileInputStream(path));
			
			Element rootel = doc.getDocumentElement();
			
			// datatype-Deklarationen durchgehen und einlesen
			NodeList nl = rootel.getElementsByTagName(ELEMENT_DATATYPE);
			for (int i = 0; i < nl.getLength(); i++) {
				Element edt = (Element) nl.item(i);
				String name = edt.getAttribute(ATTR_NAME);
				String title = edt.getAttribute(ATTR_TITLE);
				if (title.length() == 0) {
					title = name;
				}
				NodeList nll = edt.getElementsByTagName(NAME_LAYOUTFIELD);
				Element layout = (Element) nll.item(0);
				MessungTyp dt = null;
				if (layout != null) {
					NodeList nl2 = edt.getElementsByTagName("panel");
					Panel panel = createPanelFromNode((Element) nl2.item(0));
					dt = new MessungTyp(name, title, panel);
				} else {
					dt = new MessungTyp(name, title);
					
				}
				
				// Einzlene Felddeklarationen durchgehen
				NodeList dtf = edt.getChildNodes();
				for (int j = 0; j < dtf.getLength(); j++) {
					Node ndtf = dtf.item(j);
					if (ndtf.getNodeType() != Node.ELEMENT_NODE) {
						continue;
					}
					
					Element edtf = (Element) ndtf;
					String fn = edtf.getAttribute(ATTR_NAME);
					String ft = edtf.getAttribute(ATTR_TITLE);
					if (ft.equals("")) {
						ft = fn;
					}
					
					// OldMesswertTyp dft;
					IMesswertTyp typ;
					if (edtf.getNodeName().equals(NAME_NUMFIELD)) {
						typ = new MesswertTypNum(fn, ft, edtf.getAttribute(ATTR_UNIT));
						if (edtf.hasAttribute(ATTR_DEFAULT)) {
							typ.setDefault(edtf.getAttribute(ATTR_DEFAULT));
						}
						if (edtf.hasAttribute(ATTR_FORMATPATTERN)) {
							((MesswertTypNum) typ).setFormatPattern(edtf
								.getAttribute(ATTR_FORMATPATTERN));
						}
						if (edtf.hasAttribute(ATTR_DEFAULT)) {
							((MesswertTypNum) typ).setRoundingMode(edtf
								.getAttribute(ATTR_ROUNDMODE));
						}
					} else if (edtf.getNodeName().equals(NAME_BOOLFIELD)) {
						typ = new MesswertTypBool(fn, ft, edtf.getAttribute(ATTR_UNIT));
						if (edtf.hasAttribute(ATTR_DEFAULT)) {
							typ.setDefault(edtf.getAttribute(ATTR_DEFAULT));
						}
					} else if (edtf.getNodeName().equals(NAME_STRINGFIELD)) {
						MesswertTypStr str =
							new MesswertTypStr(fn, ft, edtf.getAttribute(ATTR_UNIT));
						typ = str;
						
						if (edtf.hasAttribute(ATTR_DEFAULT)) {
							typ.setDefault(edtf.getAttribute(ATTR_DEFAULT));
						}
						if (edtf.hasAttribute(ATTR_LINES)) {
							str.setLines(Integer.parseInt(edtf.getAttribute("lines")));
						}
					} else if (edtf.getNodeName().equals(NAME_ENUMFIELD)) {
						MesswertTypEnum en =
							new MesswertTypEnum(fn, ft, edtf.getAttribute(ATTR_UNIT));
						typ = en;
						
						if (edtf.hasAttribute(ATTR_DEFAULT)) {
							typ.setDefault(edtf.getAttribute(ATTR_DEFAULT));
						}
						
						NodeList children = edtf.getChildNodes();
						for (int k = 0; k < children.getLength(); k++) {
							if (children.item(k).getNodeType() != Node.ELEMENT_NODE) {
								continue;
							}
							
							Element choice = (Element) children.item(k);
							en.addChoice(choice.getAttribute(ATTR_TITLE),
								Integer.parseInt(choice.getAttribute(ATTR_VALUE)));
						}
						
						// Wenn kein vernuenftiger Standardwert angegeben wurde
						// nehmen wir die erste Auswahlmoeglichkeit
						if (typ.getDefault().equals("")) {
							for (int k = 0; k < children.getLength(); k++) {
								if (children.item(k).getNodeType() == Node.ELEMENT_NODE) {
									Element choice = (Element) children.item(k);
									typ.setDefault(choice.getAttribute(ATTR_VALUE));
									break;
								}
							}
						}
					} else if (edtf.getNodeName().equals(NAME_CALCFIELD)) {
						MesswertTypCalc calc =
							new MesswertTypCalc(fn, ft, edtf.getAttribute(ATTR_UNIT));
						typ = calc;
						
						Element formula =
							(Element) edtf.getElementsByTagName(ELEMENT_FORMULA).item(0);
						calc.setFormula(formula.getTextContent(),
							formula.getAttribute(ATTR_INTERPRETER));
						
						NodeList children = edtf.getElementsByTagName(ELEMENT_VAR);
						for (int k = 0; k < children.getLength(); k++) {
							Node n = children.item(k);
							if (n.getNodeType() != Node.ELEMENT_NODE) {
								continue;
							}
							Element var = (Element) n;
							calc.addVariable(var.getAttribute(ATTR_NAME),
								var.getAttribute(ATTR_SOURCE));
						}
					} else if (edtf.getNodeName().equals(NAME_DATAFIELD)) {
						MesswertTypData data =
							new MesswertTypData(fn, ft, edtf.getAttribute(ATTR_UNIT));
						typ = data;
						
						data.setRefType(edtf.getAttribute(ATTR_TYPE));
					} else if (edtf.getNodeName().equals(NAME_SCALEFIELD)) {
						MesswertTypScale scale =
							new MesswertTypScale(fn, ft, edtf.getAttribute(ATTR_UNIT));
						typ = scale;
						if (edtf.hasAttribute(ATTR_DEFAULT)) {
							scale.setDefault(edtf.getAttribute(ATTR_DEFAULT));
						}
						if (edtf.hasAttribute(ATTR_MIN)) {
							scale.setMin(Integer.parseInt(edtf.getAttribute(ATTR_MIN)));
						}
						if (edtf.hasAttribute(ATTR_MAX)) {
							scale.setMax(Integer.parseInt(edtf.getAttribute(ATTR_MAX)));
						}
					} else if (edtf.getNodeName().equals(NAME_DATEFIELD)) {
						MesswertTypDate date =
							new MesswertTypDate(fn, ft, edtf.getAttribute(ATTR_UNIT));
						typ = date;
					} else if (edtf.getNodeName().equals(NAME_COUNTERFIELD)) {
						MesswertTypCount counter =
							new MesswertTypCount(fn, ft, edtf.getAttribute(ATTR_UNIT));
						counter.setCounterMode(edtf.getAttribute(ATTR_COUNTERMODE));
						if (edtf.hasAttribute(ATTR_STARTVALUE)) {
							counter.setStartValue(edtf.getAttribute(ATTR_STARTVALUE));
						}
						typ = counter;
					} else {
						log.log("Unbekannter Feldtyp: '" + edtf.getNodeName() + "'", Log.ERRORS);
						continue;
					}
					dt.addField(typ);
				}
				types.add(dt);
			}
			
		} catch (Error e) {
			log.log("Einlesen der XML-Datei felgeschlagen: " + e.getMessage(), Log.ERRORS);
		} catch (SAXParseException e) {
			ExHandler.handle(e);
			SWTHelper.showError("Hilotec-Messwerte: Fehler in XML Struktur", MessageFormat.format(
				"Die Datei {0} enthielt einen Fehler in Zeile {1}: {2}", path, e.getLineNumber(),
				e.getMessage()));
			log.log(
				"Einlesen der XML-Datei felgeschlagen: " + e.getMessage() + ", Zeile: "
					+ e.getLineNumber(), Log.ERRORS);
		} catch (Exception e) {
			ExHandler.handle(e);
			log.log("Einlesen der XML-Datei felgeschlagen: " + e.getMessage(), Log.ERRORS);
			
		}
		
	}
	
	public ArrayList<MessungTyp> getTypes(){
		return types;
	}
	
	public MessungTyp getTypeByName(String name){
		for (MessungTyp t : types) {
			if (t.getName().compareTo(name) == 0) {
				return t;
			}
		}
		return null;
	}
}
