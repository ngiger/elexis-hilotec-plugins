/*******************************************************************************
 * Copyright (c) 2009, A. Kaufmann and Elexis
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

package com.hilotec.elexis.messwerte.v2.data;

import org.eclipse.swt.widgets.Widget;

/**
 * Abstrakte Basisklasse fuer die einzelnen Messwerttypen
 * 
 * @author Antoine Kaufmann
 */
public abstract class MesswertBase {
	private String name;
	private String title;
	private String unit;
	protected boolean editable = true;
	private String validpattern;
	private String invalidmessage;
	private String size;
	private boolean isShown = false;
	protected Widget widget;
	
	public MesswertBase(String n, String t, String u){
		name = n;
		title = t;
		unit = u;
	}
	
	public String getName(){
		return name;
	}
	
	public String getTitle(){
		return title;
	}
	
	public String getUnit(){
		return unit;
	}
	
	public boolean isEditable(){
		return editable;
	}
	
	public void setEditable(boolean editable){
		this.editable = editable;
	}
	
	public String getValidpattern(){
		return validpattern;
	}
	
	public void setValidpattern(String validpattern){
		this.validpattern = validpattern;
	}
	
	public String getInvalidmessage(){
		return invalidmessage;
	}
	
	public void setInvalidmessage(String invalidmessage){
		this.invalidmessage = invalidmessage;
	}
	
	public String getSize(){
		return size;
	}
	
	public void setSize(String size){
		this.size = size;
	}
	
	public void saveInput(Messwert messwert){}
	
	public boolean checkInput(Messwert messwert, String pattern){
		return true;
	}
	
	public String getActualValue(){
		return "";
	}
	
	public boolean isShown(){
		return isShown;
	}
	
	public void setShown(boolean isShown){
		this.isShown = isShown;
	}
	
}
