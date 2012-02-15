/*******************************************************************************
 * Copyright (c)2009, A. Kaufmann and Elexis
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    A. Kaufmann - initial implementation 
 *    P. Chaubert - adapted to Messwerte V2
 *    
 * $Id$
 *******************************************************************************/

package com.hilotec.elexis.messwerte.v2.data.typen;

import java.util.ArrayList;
import java.util.HashMap;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Widget;

import ch.elexis.selectors.ActiveControl;
import ch.elexis.selectors.ComboField;
import ch.elexis.selectors.TextField;
import ch.rgw.tools.StringTool;

import com.hilotec.elexis.messwerte.v2.data.Messwert;
import com.hilotec.elexis.messwerte.v2.data.MesswertBase;

/**
 * @author Antoine Kaufmann
 */
public class MesswertTypEnum extends MesswertBase implements IMesswertTyp {
	int defVal = 0;
	private HashMap<Messwert, Widget> widgetMap;
	
	/**
	 * Bezeichnungen fuer die einzelnen Auswahlmoeglichkeiten
	 */
	public ArrayList<String> choices = new ArrayList<String>();
	
	/**
	 * Werte fuer die Auswahlmoeglichkeiten. (notwendig, da die Combo nur fortlaufende Werte nimmt.
	 */
	ArrayList<Integer> values = new ArrayList<Integer>();
	
	public MesswertTypEnum(String n, String t, String u){
		super(n, t, u);
		widgetMap = new HashMap<Messwert, Widget>();
	}
	
	public String erstelleDarstellungswert(Messwert messwert){
		int wert = StringTool.parseSafeInt(messwert.getWert());
		for (int i = 0; i < values.size(); i++) {
			if (values.get(i) == wert) {
				return choices.get(i);
			}
		}
		return "";
	}
	
	public String getDefault(){
		return Integer.toString(defVal);
	}
	
	public void setDefault(String str){
		defVal = StringTool.parseSafeInt(str);
	}
	
	/**
	 * Neue Auswahlmoeglichkeit fuer dieses Enum-Feld anfuegen
	 * 
	 * @param c
	 *            Beschriftung dieser Auswahlmoeglichkeit
	 * @param v
	 *            Wert fuer diese Auswahlmoeglichkeit
	 */
	public void addChoice(String c, int v){
		choices.add(c);
		values.add(v);
	}
	
	public Widget createWidget(Composite parent, Messwert messwert){
		widget = new Combo(parent, SWT.DROP_DOWN);
		for (int i = 0; i < choices.size(); i++) {
			((Combo) widget).add(choices.get(i), i);
		}
		try {
			int wert = Integer.parseInt(messwert.getWert());
			for (int i = 0; i < values.size(); i++) {
				if (values.get(i).compareTo(wert) == 0) {
					((Combo) widget).select(i);
					break;
				}
			}
		} catch (NumberFormatException e) {
			e.printStackTrace();
		}
		setShown(true);
		return widget;
	}
	
	public ActiveControl createControl(Composite parent, Messwert messwert, boolean bEditable){
		int flags = 0;
		if (!bEditable) {
			flags |= TextField.READONLY;
		}
		IMesswertTyp dft = messwert.getTyp();
		String labelText = dft.getTitle();
		if (!dft.getUnit().equals("")) {
			labelText += " [" + dft.getUnit() + "]";
		}
		Composite c = new Composite(parent, SWT.NONE);
		GridLayout gl = new GridLayout(1, false);
		gl.marginHeight = 0;
		gl.verticalSpacing = 0;
		c.setLayout(gl);
		ComboField cf = new ComboField(c, flags, labelText, choices.toArray(new String[0]));
		cf.setText(messwert.getDarstellungswert());
		c.pack();
		widgetMap.put(messwert, cf);
		return cf;
	}
	
	public void saveInput(Messwert messwert){
		messwert.setWert(Integer.toString(values.get(((Combo) widget).getSelectionIndex())));
	}
	
	public String getActualValue(){
		return ((Combo) widget).getText();
	}
	
}
