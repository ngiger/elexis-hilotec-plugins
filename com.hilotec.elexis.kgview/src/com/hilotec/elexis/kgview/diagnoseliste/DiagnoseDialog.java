package com.hilotec.elexis.kgview.diagnoseliste;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.tiff.common.ui.datepicker.DatePickerCombo;

import ch.elexis.util.SWTHelper;
import ch.rgw.tools.TimeTool;

public class DiagnoseDialog extends TitleAreaDialog {
	private DiagnoselisteItem di;
	private DatePickerCombo date;
	private Text text;
	private boolean showDate;
	
	public DiagnoseDialog(Shell parentShell, DiagnoselisteItem di,
			boolean showDate)
	{
		super(parentShell);
		this.di = di;
		this.showDate = showDate;
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		setTitle("Eintrag bearbeiten");

		Composite comp = new Composite(parent, 0);
		comp.setLayout(new GridLayout(2, false));
		comp.setLayoutData(SWTHelper.getFillGridData());

		Label lblText = new Label(comp, 0);
		lblText.setText("Text");
		text = SWTHelper.createText(comp, 5, SWT.MULTI);
		text.setText(di.getText());
		text.setLayoutData(SWTHelper.getFillGridData());

		if (showDate) {
			Label lblDate = new Label(comp, 0);
			lblDate.setText("Datum");
			date = new DatePickerCombo(comp, 0);
			TimeTool tt = new TimeTool(di.getDatum());
			date.setDate(tt.getTime());
		}

		return comp;
	}
	
	@Override
	protected void okPressed() {
		di.setText(text.getText());
		if (showDate) {
			di.setDatum(new TimeTool(date.getDate().getTime()).
					toString(TimeTool.DATE_GER));
		}
		close();
	}
}
