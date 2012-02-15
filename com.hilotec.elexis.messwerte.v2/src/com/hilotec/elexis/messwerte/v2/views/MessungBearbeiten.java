/*******************************************************************************
 * Copyright (c) 2009-2010, A. Kaufmann and Elexis
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    G. Weirich - initial implementation
 *    A. Kaufmann - copied from befunde-Plugin and adapted to new data structure
 *    M. Descher - Override createContents to include Tab title
 *    G. Weirich - added layout options
 *    P. Chaubert - adapted to Messwerte V2
 *    
 * $Id$
 *******************************************************************************/

package com.hilotec.elexis.messwerte.v2.views;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import ch.elexis.util.SWTHelper;
import ch.rgw.tools.TimeTool;

import com.hilotec.elexis.messwerte.v2.data.Messung;
import com.hilotec.elexis.messwerte.v2.data.Messwert;
import com.hilotec.elexis.messwerte.v2.data.typen.IMesswertTyp;
import com.tiff.common.ui.datepicker.DatePickerCombo;

/**
 * Dialog um eine Messung zu bearbeiten oder neu zu erstellen
 * 
 * @author Antoine Kaufmann
 */
public class MessungBearbeiten extends TitleAreaDialog {
	private Messung messung;
	private DatePickerCombo dateWidget;
	private String tabtitle;
	
	public MessungBearbeiten(final Shell parent, Messung m){
		super(parent);
		messung = m;
	}
	
	public MessungBearbeiten(Shell shell, Messung m, String text){
		this(shell, m);
		tabtitle = text;
	}
	
	@Override
	protected Control createDialogArea(Composite parent){
		ScrolledComposite scroll =
			new ScrolledComposite(parent, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		scroll.setLayoutData(SWTHelper.getFillGridData(1, true, 1, true));
		Composite comp = new Composite(scroll, SWT.NONE);
		scroll.setContent(comp);
		
		comp.setLayout(new GridLayout());
		comp.setLayoutData(SWTHelper.getFillGridData(1, true, 1, true));
		
		dateWidget = new DatePickerCombo(comp, SWT.NONE);
		dateWidget.setDate(new TimeTool(messung.getDatum()).getTime());
		dateWidget.setLayoutData(SWTHelper.getFillGridData(1, true, 1, false));
		
		for (Messwert messwert : messung.getMesswerte()) {
			Label l = new Label(comp, SWT.NONE);
			IMesswertTyp dft = messwert.getTyp();
			String labelText = dft.getTitle();
			if (!dft.getUnit().equals("")) {
				labelText += " [" + dft.getUnit() + "]";
			}
			l.setText(labelText);
			dft.createWidget(comp, messwert);
		}
		comp.setSize(comp.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		return scroll;
		
	}
	
	@Override
	public void create(){
		super.create();
		getShell().setText("Messung bearbeiten");
		
	}
	
	@Override
	protected Control createContents(Composite parent){
		Control contents = super.createContents(parent);
		setTitle(tabtitle);
		return contents;
	}
	
	@Override
	public void okPressed(){
		TimeTool tt = new TimeTool(dateWidget.getDate().getTime());
		messung.setDatum(tt.toString(TimeTool.DATE_GER));
		for (Messwert mw : messung.getMesswerte()) {
			mw.getTyp().saveInput(mw);
		}
		close();
	}
	
	@Override
	public boolean close(){
		for (Messwert mw : messung.getMesswerte()) {
			mw.getTyp().setShown(false);
		}
		return super.close();
	}
}
