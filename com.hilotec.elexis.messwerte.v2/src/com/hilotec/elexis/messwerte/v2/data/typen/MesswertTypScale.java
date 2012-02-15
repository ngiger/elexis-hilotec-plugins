/*******************************************************************************
 * Copyright (c) 2009-2010, A. Kaufmann and Elexis
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

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Widget;

import ch.elexis.selectors.ActiveControl;
import ch.elexis.selectors.SpinnerField;

import com.hilotec.elexis.messwerte.v2.data.Messwert;
import com.hilotec.elexis.messwerte.v2.data.MesswertBase;

/**
 * @author Antoine Kaufmann
 */
public class MesswertTypScale extends MesswertBase implements IMesswertTyp {
	int defVal = 0;
	
	/**
	 * Kleinster auswaehlbarer Wert
	 */
	int min = 0;
	
	/**
	 * Groesster auswaehlbarer Wert
	 */
	int max = 0;
	
	public MesswertTypScale(String n, String t, String u){
		super(n, t, u);
	}
	
	public String erstelleDarstellungswert(Messwert messwert){
		return messwert.getWert();
	}
	
	public String getDefault(){
		return Integer.toString(defVal);
	}
	
	public void setDefault(String str){
		defVal = Integer.parseInt(str);
	}
	
	/**
	 * Groesster auswaehlbarer Wert setzen
	 */
	public void setMax(int m){
		max = m;
	}
	
	/**
	 * Kleinster auswaehlbarer Wert setzen
	 */
	public void setMin(int m){
		min = m;
	}
	
	public Widget createWidget(Composite parent, Messwert messwert){
		widget = new Spinner(parent, SWT.NONE);
		((Spinner) widget).setMinimum(min);
		((Spinner) widget).setMaximum(max);
		((Spinner) widget).setSelection(Integer.parseInt(messwert.getWert()));
		setShown(true);
		return widget;
	}
	
	public void saveInput(Messwert messwert){
		messwert.setWert(Integer.toString(((Spinner) widget).getSelection()));
	}
	
	public ActiveControl createControl(Composite parent, Messwert messwert, boolean bEditable){
		IMesswertTyp dft = messwert.getTyp();
		String labelText = dft.getTitle();
		if (!dft.getUnit().equals("")) {
			labelText += " [" + dft.getUnit() + "]";
		}
		SpinnerField sf = new SpinnerField(parent, 0, labelText, min, max);
		sf.setText(messwert.getDarstellungswert());
		return sf;
	}
	
	public String getActualValue(){
		return ((Spinner) widget).getText();
	}
}
