package com.hilotec.elexis.kgview.medikarte;

import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.part.ViewPart;

import com.hilotec.elexis.kgview.data.FavMedikament;

import ch.elexis.actions.ElexisEvent;
import ch.elexis.actions.ElexisEventDispatcher;
import ch.elexis.actions.ElexisEventListener;
import ch.elexis.data.Artikel;
import ch.elexis.data.Patient;
import ch.elexis.data.PersistentObject;
import ch.elexis.data.Prescription;
import ch.elexis.util.PersistentObjectDropTarget;
import ch.elexis.util.SWTHelper;
import ch.elexis.util.ViewMenus;

public class MedikarteView extends ViewPart implements ElexisEventListener {
	public static final String ID = "com.hilotec.elexis.kgview.MedikarteView";
	
	private Table table;
	// Alle Verschreibungen anzeigen? Oder nur die aktiven.
	private boolean alle = false;
	private Patient patient;
	
	private Action actEdit;
	private Action actStop;
	private Action actDelete;
	private Action actFilter;
	
	@Override
	public void createPartControl(Composite parent) {
		table = new Table(parent,
				SWT.V_SCROLL | SWT.SINGLE | SWT.FULL_SELECTION);
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		
		// Ein haufen Tabellenueberschriften
		String[] ueberschriften = { "Medikament", "Ordnungszahl",
				"Mo", "Mi", "Ab", "Na",
				"Einh", "Zweck", "Einnahmevorschrift", "Von", "Bis" };
		int[] breiten = { 140, 30, 30, 30, 30, 30, 40, 200, 70, 70, 70 };
		for (int i = 0; i < breiten.length; i++) {
			TableColumn tc = new TableColumn(table, SWT.NONE);
			tc.setText(ueberschriften[i]);
			tc.setWidth(breiten[i]);
		}
		
		makeActions();
		
		// Drop target um neue Medikamente auf die Liste zu nehmen
		new PersistentObjectDropTarget(table,
				new PersistentObjectDropTarget.IReceiver() {
					public void dropped(PersistentObject o, DropTargetEvent e) {
						if (patient == null) return;
						FavMedikament fm = FavMedikament.load((Artikel) o);
						new MedikarteEintragDialog(getSite().getShell(), patient, fm).open();
						refresh();
					}
					
					@Override
					public boolean accept(PersistentObject o) {
						if (!(o instanceof Artikel) || patient == null) return false;
						return (FavMedikament.load((Artikel) o) != null);
					}
		});
		
		// Listener fuer Doppelklick um eintraege zu editieren
		table.addMouseListener(new MouseListener() {
			public void mouseUp(MouseEvent e) { }
			public void mouseDown(MouseEvent e) { }
			public void mouseDoubleClick(MouseEvent e) {
				actEdit.run();
			}
		});
		
		// Key-Listener zum stoppen von Eintraegen
		table.addKeyListener(new KeyListener() {
			public void keyReleased(KeyEvent e) {}
			public void keyPressed(KeyEvent e) {
				if (e.keyCode != SWT.DEL) return;
				actStop.run();
			}
		});
		
		// Menus oben rechts in der View
		ViewMenus menus = new ViewMenus(getViewSite());
		menus.createToolbar(actFilter);
		
		// Contextmenu fuer Tabelle
		menus.createControlContextMenu(table, actEdit, actStop, actDelete);
		
		ElexisEventDispatcher.getInstance().addListeners(this);
		patient = (Patient)
			ElexisEventDispatcher.getSelected(Patient.class);
		refresh();
	}
	
	private void makeActions() {
		// Aktion zum Bearbeiten einer Verschreibung
		actEdit = new Action("Bearbeiten", Action.AS_PUSH_BUTTON) {
			@Override
			public void run() {
				TableItem[] sel = table.getSelection();
				if (sel == null || sel.length != 1) return;
				new MedikarteEintragDialog(getSite().getShell(), patient,
						(Prescription) sel[0].getData()).open();
				refresh();
			}
		};
		
		// Aktion zum Stoppen einer Verschreibung
		actStop = new Action("Stoppen", Action.AS_PUSH_BUTTON) {
			@Override
			public void run() {
				TableItem[] tis = table.getSelection();
				if (tis == null || tis.length != 1) return;
				Prescription presc = (Prescription) tis[0].getData();
				presc.setEndDate(null);
				refresh();
			}
		};
		
		// Aktion zum Loeschen von Verschreibungen
		actDelete = new Action("Löschen", Action.AS_PUSH_BUTTON) {
			@Override
			public void run() {
				TableItem[] tis = table.getSelection();
				if (tis == null || tis.length != 1) return;
				if (!SWTHelper.askYesNo("Verschreibung loeschen", "Soll der " +
						"markierte Eintrag wirklich permanent gelöscht werden?"))
					return;
					
				Prescription presc = (Prescription) tis[0].getData();
				presc.delete();
				refresh();
			}
		};
		
		// Aktion fuer den Filter-Button
		actFilter = new Action("Alle anzeigen", Action.AS_CHECK_BOX) {
			@Override
			public void run() {
				if (isChecked()) {
					alle = true;
					// TODO: Async?
					refresh();
				} else {
					alle = false;
					refresh();
				}
			}
		};
	}
	

	private void refresh() {
		table.removeAll();
		if (patient == null) return;
			
		// Medikation zu Patient zusammensuchen.
		List<Prescription> l = MedikarteHelpers.
			medikarteMedikation(patient, alle);
		
		// Tabelle neu befuellen
		for (Prescription p: l) {
			FavMedikament fm = FavMedikament.load(p.getArtikel());
			String[] dosierung = p.getDosis().split("-");
			if (dosierung.length != 4 || fm == null) continue;
			
			TableItem ti = new TableItem(table, 0);
			ti.setData(p);
			
			int i = 0;
			ti.setText(i++, fm.getBezeichnung());
			int ord = MedikarteHelpers.getOrdnungszahl(p);
			ti.setText(i++, Integer.toString(ord));
			ti.setText(i++, dosierung[0]);
			ti.setText(i++, dosierung[1]);
			ti.setText(i++, dosierung[2]);
			ti.setText(i++, dosierung[3]);
			
			ti.setText(i++, fm.getEinheit());
			ti.setText(i++, fm.getZweck());
			ti.setText(i++, p.getBemerkung());
			
			ti.setText(i++, p.getBeginDate());
			ti.setText(i++, p.getEndDate());
		}
	}
	
	@Override
	public void setFocus() {}

	@Override
	public void dispose() {
		ElexisEventDispatcher.getInstance().removeListeners(this);
	}
	
	public void catchElexisEvent(ElexisEvent ev) {
		if (ev.getObjectClass().equals(Patient.class)) {
			Patient p = (Patient) ev.getObject();
			if (ev.getType() == ElexisEvent.EVENT_SELECTED)
				patient = p;
			else if (ev.getType() == ElexisEvent.EVENT_DESELECTED)
				patient = null;
			refresh();
		} else if (ev.getObjectClass().equals(Prescription.class)) {
			refresh();
		}
		
	}
	
	private final ElexisEvent eetmpl =
		new ElexisEvent(null, null, ElexisEvent.EVENT_SELECTED
			| ElexisEvent.EVENT_DESELECTED
			| ElexisEvent.EVENT_CREATE
			| ElexisEvent.EVENT_DELETE);

	public ElexisEvent getElexisEventFilter() {
		return eetmpl;
	}
}
