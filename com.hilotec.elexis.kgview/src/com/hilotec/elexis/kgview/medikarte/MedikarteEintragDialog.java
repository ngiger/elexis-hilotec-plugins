package com.hilotec.elexis.kgview.medikarte;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.hilotec.elexis.kgview.data.FavMedikament;

import ch.elexis.data.Patient;
import ch.elexis.data.Prescription;
import ch.elexis.util.SWTHelper;
import ch.rgw.tools.TimeTool;

/**
 * Dialog um einen Eintrag in der Liste der favorisierten Medikamente
 * anzupassen oder neu zu erstellen.
 * 
 * @author Antoine Kaufmann
 */
public class MedikarteEintragDialog extends TitleAreaDialog {
	private Patient pat;
	private FavMedikament fm;
	private Prescription presc;
	
	private Text tDoMorgen;
	private Text tDoMittag;
	private Text tDoAbend;
	private Text tDoNacht;
	private Text tVon;
	private Text tBis;
	private Combo cEV;
	

	public MedikarteEintragDialog(Shell parentShell, Patient patient, FavMedikament med) {
		super(parentShell);
		fm = med;
		pat = patient;
		presc = null;
	}
	
	public MedikarteEintragDialog(Shell parentShell, Patient patient, Prescription prescription) {
		super(parentShell);
		fm = FavMedikament.load(prescription.getArtikel());
		pat = patient;
		presc = prescription;
	}
	
	@Override
	protected Control createDialogArea(Composite parent){
		Composite comp = new Composite(parent, 0);
		comp.setLayout(new GridLayout(2, false));
		
		// Patient, Medikament beides rein informativ
		setTitle("Neues Medikament fuer " +
				pat.getName() + ", " + pat.getGeburtsdatum());
		Label lLMed = new Label(comp, 0);
		lLMed.setText("Medikament");
		Label lMed = new Label(comp, SWT.BORDER);
		lMed.setText(fm.getBezeichnung());
		
		// Felder zum ausfuellen, Datum von bis, Dosis
		Label lVon = new Label(comp, 0);
		lVon.setText("Von");
		tVon = SWTHelper.createText(comp, 1, 0);
		Label lBis = new Label(comp, 0);
		lBis.setText("Bis");
		tBis = SWTHelper.createText(comp, 1, 0);

		
		Label lDosierung = new Label(comp, 0);
		lDosierung.setText("Dosierung");
		
		Composite cDos = new Composite(comp, 0);
		cDos.setLayout(new RowLayout());
		tDoMorgen = new Text(cDos, SWT.BORDER);
		tDoMorgen.setLayoutData(new RowData(30, SWT.DEFAULT));
		tDoMittag = new Text(cDos, SWT.BORDER);
		tDoMittag.setLayoutData(new RowData(30, SWT.DEFAULT));
		tDoAbend = new Text(cDos, SWT.BORDER);
		tDoAbend.setLayoutData(new RowData(30, SWT.DEFAULT));
		tDoNacht = new Text(cDos, SWT.BORDER);
		tDoNacht.setLayoutData(new RowData(30, SWT.DEFAULT));
		
		Label lEV = new Label(comp, 0);
		lEV.setText("Einnahmevorschrift");
		cEV = new Combo(comp, SWT.DROP_DOWN | SWT.BORDER);
		cEV.add("");
		cEV.add("Bei Bedarf");
		cEV.select(0);

		// Zweck, Einheit, rein informativ
		Label lZweck = new Label(comp, 0);
		lZweck.setText("Zweck");
		Label lZweckText = new Label(comp, SWT.BORDER);
		lZweckText.setText(fm.getZweck());
		
		Label lEinheit = new Label(comp, 0);
		lEinheit.setText("Einheit");
		Label lEinheitText = new Label(comp, SWT.BORDER);
		lEinheitText.setText(fm.getEinheit());
		
		
		if (presc != null) {
			tVon.setText(presc.getBeginDate());
			tBis.setText(presc.getEndDate());
			String[] dos = presc.getDosis().split("-");
			tDoMorgen.setText(dos[0]);
			tDoMittag.setText(dos[1]);
			tDoAbend.setText(dos[2]);
			tDoNacht.setText(dos[3]);
			
			// XXX: Nicht wirklich elegant so
			if (presc.getBemerkung().equals("Bei Bedarf"))
				cEV.select(1);
		} else {
			tVon.setText(new TimeTool().toString(TimeTool.DATE_GER));
			tDoMorgen.setText("0");
			tDoMittag.setText("0");
			tDoAbend.setText("0");
			tDoNacht.setText("0");
		}
		
		return comp;
	}

	private boolean validateDate(String s, boolean allowempty) {
		TimeTool tt = new TimeTool();
		return (s.isEmpty() && allowempty) || tt.setDate(s);
	}
	
	private boolean validateInput() {
		setMessage("");
		if (!validateDate(tVon.getText(), false) ||
			!validateDate(tBis.getText(), true))
		{
			setMessage("Fehler: Ungültiges Datum. Erwarte Format " +
					"dd.mm.jjjj, oder leer (nur Bis).");
			return false;
		}
		return true;
	}
	
	@Override
	public void okPressed() {
		if (!validateInput()) return;
		String dosierung =
			tDoMorgen.getText() + "-" + tDoMittag.getText() + "-" +
			tDoAbend.getText() + "-" + tDoNacht.getText();
		String bemerkung =
			(cEV.getSelectionIndex() == 1 ? "Bei Bedarf" : "");
		
		if (presc == null) {
			presc = new Prescription(fm.getArtikel(), pat,
					dosierung, bemerkung);
		} else {
			presc.setDosis(dosierung);
			presc.setBemerkung(bemerkung);
		}
		presc.setBeginDate(tVon.getText());
		presc.setEndDate(tBis.getText());
		close();
	}
}
