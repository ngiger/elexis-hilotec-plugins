package com.hilotec.elexis.kgview.medikarte;

import java.util.ArrayList;
import java.util.List;

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
import ch.elexis.data.PersistentObject;
import ch.elexis.data.Query;
import ch.elexis.util.PersistentObjectDragSource;
import ch.elexis.util.PersistentObjectDropTarget;
import ch.elexis.util.SWTHelper;

/**
 * Liste der Favorisierten Medikamente anzeigen. Bietet die Möglichkeit zu
 * suchen, neue Eintraege zu erstellen indem ein neues Medikament per
 * Drag&Drop auf die Tabelle gezogen wird, Eintraege zu veraendern und zu
 * loeschen.
 * 
 * @author Antoine Kaufmann
 */
public class FavMedikamentListe extends ViewPart
		implements ElexisEventListener
{
	public static final String ID = "com.hilotec.elexis.kgview.medikarte.FavMedikamentListe";
	
	private Table table;
	
	@Override
	public void createPartControl(Composite parent) {
		table = new Table(parent, SWT.V_SCROLL | SWT.SINGLE | SWT.FULL_SELECTION);
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		
		TableColumn tc;
		
		tc = new TableColumn(table, 0);
		tc.setText("Bezeichnung");
		tc.setWidth(180);
		
		tc = new TableColumn(table, 0);
		tc.setText("Zweck");
		tc.setWidth(180);
		
		tc = new TableColumn(table, 0);
		tc.setText("Einheit");
		tc.setWidth(40);

		
		// Drop Target um neue Eintraege zu erstellen
		new PersistentObjectDropTarget(table,
				new PersistentObjectDropTarget.IReceiver()
		{			
					@Override
					public void dropped(PersistentObject o, DropTargetEvent e) {
						Artikel a = (Artikel) o;
						new FavMedikamentDialog(getSite().getShell(), a).open();
					}
					
					@Override
					public boolean accept(PersistentObject o) {
						if (o instanceof Artikel) {
							Artikel a = (Artikel) o;
							// Droppen nur erlauben wenn Medikament noch nicht in Liste
							FavMedikament fm = FavMedikament.load(a.getId());
							return (fm == null);
						}
						return false;
					}
		});
		
		// Drag Source um die Eintraege zu benutzen
		// XXX: Dabei wird kein FavMedikament mitgeben sondern direkt der
		//      Artikel
		new PersistentObjectDragSource(table,
				new PersistentObjectDragSource.ISelectionRenderer()
		{
			public List<PersistentObject> getSelection() {
				TableItem[] tis = table.getSelection();
				if (table.getSelection() == null) return null;
				
				ArrayList<PersistentObject> res =
					new ArrayList<PersistentObject>(tis.length);
				for (TableItem ti: tis) {
					FavMedikament fm = (FavMedikament) ti.getData();
					res.add(fm.getArtikel());
				}
				return res;
			}
		});
		
		// Mouse Listener zum aendern von Eintraegen
		table.addMouseListener(new MouseListener() {
			public void mouseUp(MouseEvent e) {}
			public void mouseDown(MouseEvent e) {}
			public void mouseDoubleClick(MouseEvent e) {
				TableItem[] tis = table.getSelection();
				if (tis == null || tis.length != 1) return;
				new FavMedikamentDialog(getSite().getShell(),
						(FavMedikament) tis[0].getData()).open();
				refresh();
			}
		});
		
		// Key Listener zum Loeschen von Eintraegen
		table.addKeyListener(new KeyListener() {
			public void keyReleased(KeyEvent e) {}
			public void keyPressed(KeyEvent e) {
				if (e.keyCode != SWT.DEL) return;
				TableItem[] tis = table.getSelection();
				if (tis == null || tis.length == 0) return;
				
				// Nachfragen
				if (!SWTHelper.askYesNo("Medikament(e) aus Liste entfernen",
						"Sollen das/die ausgewählte(n) Medikament(e) aus der" +
						" Liste entfernt werden?"))
					return;
				
				for (TableItem ti: tis) {
					FavMedikament fm = (FavMedikament) ti.getData();
					fm.delete();
				}
				refresh();
			}
		});
		
		
		
		refresh();
		ElexisEventDispatcher.getInstance().addListeners(this);
	}
	
	/**
	 * Liste neu laden.
	 */
	private void refresh() {
		Query<FavMedikament> qMeds = new Query<FavMedikament>(FavMedikament.class);
		// XXX: Irgendwie unschoen
		qMeds.add("ID", Query.NOT_EQUAL, "VERSION");
		qMeds.orderBy(false, FavMedikament.FLD_BEZEICHNUNG);
		
		List<FavMedikament> meds = qMeds.execute();
		table.removeAll();
		for (FavMedikament med: meds) {
			TableItem ti = new TableItem(table, 0);
			ti.setData(med);
			ti.setText(0, med.getBezeichnung());
			ti.setText(1, med.getZweck());
			ti.setText(2, med.getEinheit());
		}
	}

	@Override
	public void setFocus() {}

	@Override
	public void dispose() {
		ElexisEventDispatcher.getInstance().removeListeners(this);
	}
	
	@Override
	public void catchElexisEvent(ElexisEvent ev) {
		refresh();
	}

	private final ElexisEvent eetmpl =
		new ElexisEvent(null, FavMedikament.class,
			ElexisEvent.EVENT_CREATE
			| ElexisEvent.EVENT_DELETE
			| ElexisEvent.EVENT_UPDATE
			| ElexisEvent.EVENT_RELOAD);
	
	@Override
	public ElexisEvent getElexisEventFilter() {
		return eetmpl;
	}

}
