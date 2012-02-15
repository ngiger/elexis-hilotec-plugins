/*******************************************************************************
 * Copyright (c) 2009-2010, Elexis
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    P. Chaubert - initial implementation 
 *    
 * $Id$
 *******************************************************************************/

package com.hilotec.elexis.messwerte.v2.data.typen;

import java.text.SimpleDateFormat;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Widget;

import ch.elexis.selectors.ActiveControl;
import ch.elexis.util.SWTHelper;
import ch.rgw.tools.TimeTool;

import com.hilotec.elexis.messwerte.v2.data.Messwert;
import com.hilotec.elexis.messwerte.v2.data.MesswertBase;
import com.tiff.common.ui.datepicker.DatePickerCombo;

/**
 * @author Patrick Chaubert
 */
public class MesswertTypDate extends MesswertBase implements IMesswertTyp {
	
	public MesswertTypDate(String n, String t, String u){
		super(n, t, u);
	}
	
	public String erstelleDarstellungswert(Messwert messwert){
		return messwert.getWert();
	}
	
	public String getDefault(){
		return "";
	}
	
	public void setDefault(String str){}
	
	public Widget createWidget(Composite parent, Messwert messwert){
		widget = new DatePickerCombo(parent, SWT.NONE);
		((DatePickerCombo) widget).setFormat(new SimpleDateFormat("dd.MM.yyyy"));
		((DatePickerCombo) widget).setDate(new TimeTool(messwert.getWert()).getTime());
		((DatePickerCombo) widget).setLayoutData(SWTHelper.getFillGridData(1, true, 1, false));
		setShown(true);
		return widget;
	}
	
	public void saveInput(Messwert messwert){
		messwert.setWert(((DatePickerCombo) widget).getText());
	}
	
	public String getActualValue(){
		return ((DatePickerCombo) widget).getText();
	}
	
	public ActiveControl createControl(Composite parent, Messwert messwert, boolean bEditable){
		// TODO Auto-generated method stub
		return null;
	}
}
