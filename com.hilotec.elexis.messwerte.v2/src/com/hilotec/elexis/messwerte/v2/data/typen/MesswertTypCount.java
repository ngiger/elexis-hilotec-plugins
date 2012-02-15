/*******************************************************************************
 * Copyright (c) 2011, Elexis
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

import java.math.RoundingMode;
import java.text.DecimalFormat;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Widget;

import ch.elexis.Hub;
import ch.elexis.selectors.ActiveControl;
import ch.elexis.util.SWTHelper;

import com.hilotec.elexis.messwerte.v2.data.Messwert;
import com.hilotec.elexis.messwerte.v2.data.MesswertBase;

/**
 * @author Patrick Chaubert
 */
public class MesswertTypCount extends MesswertBase implements IMesswertTyp {
	
	private static final String CONFIG_BASE_NAME = "com/hilotec/messwerte/v2/"; //$NON-NLS-1$
	private String counterMode = "global_counter";
	private DecimalFormat df = new DecimalFormat("#,000");
	private int startValue = 0;
	
	public MesswertTypCount(String n, String t, String u){
		super(n, t, u);
		df.setRoundingMode(RoundingMode.HALF_UP);
	}
	
	public String getDefault(){
		return "";
	}
	
	public void setCounterMode(String cn){
		this.counterMode = cn;
	}
	
	public void setDefault(String def){}
	
	public void setStartValue(String sv){
		this.startValue = Integer.parseInt(sv);
	}
	
	public Widget createWidget(Composite parent, Messwert messwert){
		widget = SWTHelper.createText(parent, 1, SWT.NONE);
		((Text) widget).setText(messwert.getWert());
		((Text) widget).setEditable(false);
		setShown(true);
		return widget;
	}
	
	public ActiveControl createControl(Composite parent, Messwert messwert, boolean bEditable){
		// TODO Auto-generated method stub
		return null;
	}
	
	public void saveInput(Messwert messwert){
		String s = messwert.getWert();
		if (s.equals("")) {
			int value = Hub.globalCfg.get(CONFIG_BASE_NAME + counterMode, startValue);
			if (value < startValue) {
				value = startValue;
			} else {
				value++;
			}
			Hub.globalCfg.set(CONFIG_BASE_NAME + counterMode, value);
			Hub.globalCfg.flush();
			messwert.setWert(df.format(value));
		}
	}
	
	public String erstelleDarstellungswert(Messwert messwert){
		return messwert.getWert();
	}
	
}
