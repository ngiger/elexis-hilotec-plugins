package com.hilotec.elexis.kgview.medikarte;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.hilotec.elexis.kgview.data.FavMedikament;

import ch.elexis.data.Artikel;
import ch.elexis.util.SWTHelper;

/**
 * Dialog um einen Eintrag in der Liste der favorisierten Medikamente
 * anzupassen oder neu zu erstellen.
 * 
 * @author Antoine Kaufmann
 */
public class FavMedikamentDialog extends TitleAreaDialog {
	private Artikel artikel;
	private FavMedikament fm;
	
	private Text tBezeichnung;
	private Text tZweck;
	private Text tEinheit;

	public FavMedikamentDialog(Shell parentShell, Artikel artikel) {
		super(parentShell);
		this.artikel = artikel;
		fm = FavMedikament.load(artikel);
	}
	
	public FavMedikamentDialog(Shell parentShell, FavMedikament med) {
		super(parentShell);
		fm = med;
		artikel = fm.getArtikel();
	}
	
	@Override
	protected Control createDialogArea(Composite parent){
		Composite comp = new Composite(parent, 0);
		comp.setLayout(new GridLayout(2, false));
		
		
		Label lLMed = new Label(comp, 0);
		lLMed.setText("Medikament");
		Label lMed = new Label(comp, 0);
		lMed.setText(artikel.getName());
		
		Label lBez = new Label(comp, 0);
		lBez.setText("Bezeichnung");
		tBezeichnung = SWTHelper.createText(comp, 1, 0);

		Label lZweck = new Label(comp, 0);
		lZweck.setText("Zweck");
		tZweck = SWTHelper.createText(comp, 2, 0);
		tZweck.addTraverseListener(new TraverseListener() {
			public void keyTraversed(TraverseEvent e) {
				if (e.detail == SWT.TRAVERSE_TAB_NEXT ||
						e.detail == SWT.TRAVERSE_TAB_PREVIOUS)
					e.doit = true;
			}
		});
		
		Label lEinheit = new Label(comp, 0);
		lEinheit.setText("Einheit");
		tEinheit = SWTHelper.createText(comp, 1, 0);
		
		if (fm != null) {
			tBezeichnung.setText(fm.getBezeichnung());
			tZweck.setText(fm.getZweck());
			tEinheit.setText(fm.getEinheit());
		} else {
			tBezeichnung.setText(artikel.getName());
		}
		
		return comp;
	}

	@Override
	public void okPressed() {
		if (fm == null) {
			fm = new FavMedikament(artikel, tBezeichnung.getText(),
					tZweck.getText(), tEinheit.getText());
			System.out.println("Created: ");
			System.out.println(fm);
		} else {
			fm.setBezeichnung(tBezeichnung.getText());
			fm.setZweck(tZweck.getText());
			fm.setEinheit(tEinheit.getText());
		}
		close();
	}
}
