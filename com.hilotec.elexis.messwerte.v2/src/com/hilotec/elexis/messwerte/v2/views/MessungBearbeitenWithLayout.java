/*******************************************************************************
 * Copyright (c) 2010, G. Weirich and Elexis
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    G. Weirich - initial implementation
 *    P. Chaubert - adapted to Messwerte V2
 *
 * $Id$
 *******************************************************************************/

package com.hilotec.elexis.messwerte.v2.views;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import ch.elexis.selectors.ActiveControl;
import ch.elexis.selectors.ActiveControlListener;
import ch.elexis.util.SWTHelper;
import ch.rgw.tools.TimeTool;

import com.hilotec.elexis.messwerte.v2.data.Messung;
import com.hilotec.elexis.messwerte.v2.data.MessungTyp;
import com.hilotec.elexis.messwerte.v2.data.Messwert;
import com.hilotec.elexis.messwerte.v2.data.Panel;
import com.hilotec.elexis.messwerte.v2.data.typen.IMesswertTyp;
import com.hilotec.elexis.messwerte.v2.data.typen.MesswertTypCalc;
import com.hilotec.elexis.messwerte.v2.data.typen.MesswertTypEnum;
import com.tiff.common.ui.datepicker.DatePickerCombo;

/**
 * An edit dialog that honors layout hints in the measurement description XML
 * 
 * @author gerry
 * 
 */
public class MessungBearbeitenWithLayout extends TitleAreaDialog {
	private Messung messung;
	private DatePickerCombo dateWidget;
	private List<Messwert> messwerte;
	private List<ActiveControl> calcFields = new ArrayList<ActiveControl>();
	private List<ActiveControl> enumFields = new ArrayList<ActiveControl>();
	private HashMap<Messwert, ActiveControl> acMap;
	
	public MessungBearbeitenWithLayout(Shell shell, Messung m){
		super(shell);
		messung = m;
		acMap = new HashMap<Messwert, ActiveControl>();
	}
	
	@Override
	protected void createButtonsForButtonBar(Composite parent){
		createButton(parent, IDialogConstants.OK_ID, "Schliessen", true);
	}
	
	@Override
	protected Control createDialogArea(Composite parent){
		MessungTyp typ = messung.getTyp();
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
		messwerte = messung.getMesswerte();
		createComposite(typ.getPanel(), comp);
		comp.setSize(comp.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		return scroll;
	}
	
	public Composite createComposite(Panel p, Composite parent){
		Composite ret = new Composite(parent, SWT.NONE);
		if (p.getType().equals("plain")) {
			ret.setLayout(new FillLayout());
		} else if (p.getType().equals("display")) {
			ret.setLayout(new GridLayout());
			Browser browser = new Browser(ret, SWT.NONE);
			String url = p.getAttribute("url");
			browser.setUrl(url == null ? "" : url);
			GridData gd = SWTHelper.getFillGridData(1, true, 1, true);
			String bounds = p.getAttribute("size");
			if (bounds != null) {
				String[] coord = bounds.trim().split("\\s*,\\s*");
				if (coord.length == 2) {
					gd = new GridData(Integer.parseInt(coord[0]), Integer.parseInt(coord[1]));
					gd.grabExcessHorizontalSpace = true;
					gd.grabExcessVerticalSpace = true;
				}
			}
			browser.setLayoutData(gd);
		} else if (p.getType().equals("label")) {
			ret.setLayout(new FillLayout());
			Label l = new Label(ret, SWT.WRAP);
			l.setText(p.getAttribute("text"));
			
		} else if (p.getType().equals("grid")) {
			String cols = p.getAttribute("columns");
			if (cols == null) {
				ret.setLayout(new GridLayout());
			} else {
				ret.setLayout(new GridLayout(Integer.parseInt(cols), false));
			}
		} else if (p.getType().equals("field")) {
			String fieldref = p.getAttribute("ref");
			Messwert mw = getMesswert(fieldref);
			if (mw != null) {
				IMesswertTyp dft = mw.getTyp();
				
				boolean bEditable = true;
				String iattr = p.getAttribute("editable");
				if (iattr != null && iattr.equals("false")) {
					bEditable = false;
				}
				
				ActiveControl ac = dft.createControl(ret, mw, bEditable);
				ac.setData("messwert", mw);
				if (dft instanceof MesswertTypCalc) {
					calcFields.add(ac);
				}
				if (dft instanceof MesswertTypEnum) {
					enumFields.add(ac);
				}
				String validPattern = p.getAttribute("validpattern");
				if (validPattern != null) {
					String invalidMsg = p.getAttribute("invalidmessage");
					ac.setValidPattern(validPattern, invalidMsg == null ? "ungültige Eingabe"
							: invalidMsg);
				}
				String bounds = p.getAttribute("size");
				if (bounds != null) {
					String[] coord = bounds.trim().split("\\s*,\\s*");
					if (coord.length == 2) {
						ac.setBounds(0, 0, Integer.parseInt(coord[0]), Integer.parseInt(coord[1]));
					}
				}
				ac.addListener(new ActiveControlListener() {
					
					public void titleClicked(ActiveControl field){
						
					}
					
					public void invalidContents(ActiveControl field){
						
					}
					
					public void contentsChanged(ActiveControl ac){
						if (ac.isValid() && !ac.isReadonly()) {
							Messwert messwert = (Messwert) ac.getData("messwert");
							messwert.setWert(ac.getText());
							setErrorMessage(null);
							for (ActiveControl cc : calcFields) {
								Messwert mc = (Messwert) cc.getData("messwert");
								cc.setText(((MesswertTypCalc) mc.getTyp())
									.erstelleDarstellungswert(mc));
							}
							getButton(OK).setEnabled(true);
						} else {
							setErrorMessage(ac.getErrMsg());
							getButton(OK).setEnabled(false);
						}
						
					}
				});
				acMap.put(mw, ac);
				setLayoutData(ac);
			}
		}
		for (Panel panel : p.getPanels()) {
			setLayoutData(createComposite(panel, ret));
		}
		return ret;
	}
	
	public Messwert getMesswert(String name){
		for (Messwert m : messwerte) {
			if (m.getName().equals(name)) {
				return m;
			}
		}
		return null;
	}
	
	private void setLayoutData(Control c){
		if (c.getParent().getLayout() instanceof GridLayout) {
			c.setLayoutData(SWTHelper.getFillGridData(1, true, 1, true));
		}
		c.pack();
	}
	
	@Override
	protected void okPressed(){
		// TODO Speichern der Auswahl im EnumField, da dies mit dem Actionlistener
		// nicht funktioniert
		close();
	}
	
	@Override
	public void create(){
		super.create();
		getShell().setText("Messung bearbeiten");
		setTitle(messung.getTyp().getTitle());
	}
	
}
