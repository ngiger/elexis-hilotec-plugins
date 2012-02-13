package com.hilotec.elexis.kgview.diagnoseliste;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import ch.elexis.util.SWTHelper;

public class DiagnoseDialog extends TitleAreaDialog {
	private DiagnoselisteItem di;
	private Text text;
	
	public DiagnoseDialog(Shell parentShell, DiagnoselisteItem di) {
		super(parentShell);
		this.di = di;
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		text = SWTHelper.createText(parent, 5, SWT.MULTI);
		text.setText(di.getText());
		return text;
	}

	@Override
	public void create(){
		super.create();
		getShell().setText("Diagnose bearbeiten");
		
	}
	
	@Override
	protected void okPressed() {
		di.setText(text.getText());
		close();
	}
}
