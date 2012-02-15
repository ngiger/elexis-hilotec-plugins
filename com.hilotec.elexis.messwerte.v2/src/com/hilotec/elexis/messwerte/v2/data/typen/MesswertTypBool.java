/*******************************************************************************
 * Copyright (c) 2010, A. Kaufmann and Elexis
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
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Widget;

import ch.elexis.selectors.ActiveControl;
import ch.elexis.selectors.BooleanField;
import ch.elexis.selectors.TextField;

import com.hilotec.elexis.messwerte.v2.data.Messwert;
import com.hilotec.elexis.messwerte.v2.data.MesswertBase;

/**
 * @author Antoine Kaufmann
 */
public class MesswertTypBool extends MesswertBase implements IMesswertTyp {
	boolean defVal;
	
	public MesswertTypBool(String n, String t, String u){
		super(n, t, u);
		defVal = false;
	}
	
	public String erstelleDarstellungswert(Messwert messwert){
		if (messwert.getWert().equals("1")) {
			return "Ja";
		}
		return (Boolean.parseBoolean(messwert.getWert()) ? "Ja" : "Nein");
	}
	
	public String getDefault(){
		return Boolean.toString(defVal);
	}
	
	public void setDefault(String def){
		defVal = Boolean.parseBoolean(def);
	}
	
	public Widget createWidget(Composite parent, Messwert messwert){
		widget = new Button(parent, SWT.CHECK);
		((Button) widget).setSelection(Boolean.parseBoolean(messwert.getWert()));
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
		BooleanField bf = new BooleanField(parent, flags, labelText);
		if (messwert.getWert().equals("1")) {
			messwert.setWert("true");
		} else if (messwert.getWert().equals("0")) {
			messwert.setWert("false");
		}
		bf.setText(Boolean.toString(Boolean.parseBoolean(messwert.getWert())));
		
		return bf;
	}
	
	public void saveInput(Messwert messwert){
		messwert.setWert(Boolean.toString(((Button) widget).getSelection()));
	}
	
	public String getActualValue(){
		return (Boolean.toString(((Button) widget).getSelection()));
	}
	
}
