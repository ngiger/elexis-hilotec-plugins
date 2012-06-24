package com.hilotec.elexis.kgview.diagnoseliste;

import org.eclipse.jface.action.Action;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TreeEditor;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.part.ViewPart;

import ch.elexis.actions.ElexisEvent;
import ch.elexis.actions.ElexisEventDispatcher;
import ch.elexis.actions.ElexisEventListener;
import ch.elexis.data.Patient;
import ch.elexis.util.SWTHelper;
import ch.elexis.util.ViewMenus;

/*
 * Helper class to draw multiline tree items, kind of ugly, but according
 * to the eclipse forum there is no other way at the moment.
 *
 * Derived From: http://git.eclipse.org/c/platform/
 *  + eclipse.platform.swt.git/tree/examples/org.eclipse.swt.snippets/
 *  + src/org/eclipse/swt/snippets/Snippet227.java
 * Copyright (c) 2000, 2006 IBM Corporation and others (EPL)
 */
class MultilinePaintListener implements Listener {
	public void handleEvent(Event event) {
		switch(event.type) {
			case SWT.MeasureItem: {
				TreeItem item = (TreeItem)event.item;
				String text = getText(item, event.index);
				Point size = event.gc.textExtent(text);
				event.width = size.x;
				event.height = Math.max(event.height, size.y);
				break;
			}
			case SWT.PaintItem: {
				TreeItem item = (TreeItem)event.item;
				String text = getText(item, event.index);
				Point size = event.gc.textExtent(text);
				int offset2 = event.index == 0 ? Math.max(0, (event.height - size.y) / 2) : 0;
				event.gc.drawText(text, event.x, event.y + offset2, true);
				break;
			}
			case SWT.EraseItem: {
				event.detail &= ~SWT.FOREGROUND;
				break;
			}
		}
	}
	String getText(TreeItem item, int column) {
		String text = item.getText(column);
		/*if (column != 0) {
			TreeItem parent = item.getParentItem();
			int index = parent == null ? tree.indexOf(item) : parent.indexOf(item);
			if ((index+column) % 3 == 1){
				text +="\nnew line";
			}
			if ((index+column) % 3 == 2) {
				text +="\nnew line\nnew line";
			}
		}*/
		return text;
	}
}


public abstract class DiagnoselisteBaseView extends ViewPart
		implements ElexisEventListener
{
	/** Typ der anzuzeigenden items */
	protected final int typ;

	/** Sollen neue eintraege erstellt werden koenenn? */
	protected boolean canAdd = true;

	/** Soll das Datum bei Eintraegen angezeigt werden? */
	protected boolean showDate = true;

	private Tree tree;
	Action actAdd;
	Action actEdit;
	Action actAddChild;
	Action actDel;
	Action actMoveUp;
	Action actMoveDown;

	/**
	 * Diagnoseliste Anzeige fuer Items eines bestimmten Typs initialisieren.
	 *
	 * @param typ Typ der anzuzeigenden Elemente
	 */
	public DiagnoselisteBaseView(int typ) {
		super();
		this.typ = typ;
	}


	private void setupTI(TreeItem ti, DiagnoselisteItem di) {
		if (showDate) {
			ti.setText(di.getDatum() + ":" + di.getText());
		} else {
			ti.setText(di.getText());
		}
		ti.setData(di);
	}

	/**
	 * Neues TreeItem für die übergebene Diagnose am angegebenen Index
	 * erstellen.
	 */
	private TreeItem createTI(DiagnoselisteItem di, TreeItem tip, int index) {
		TreeItem ti;
		if (tip == null)
			ti = new TreeItem(tree, SWT.NONE, index);
		else
			ti = new TreeItem(tip, SWT.NONE, index);
		setupTI(ti, di);
		return ti;
	}

	private void insertSubtree(DiagnoselisteItem dip, TreeItem tip) {
		TreeItem ti;
		for (DiagnoselisteItem di: dip.getChildren()) {
			ti = createTI(di, tip, di.getPosition());
			insertSubtree(di, ti);
		}
	}

	private void updateTree(Patient pat) {
		tree.removeAll();
		boolean en = (pat != null);

		actAdd.setEnabled(en && canAdd);
		actAddChild.setEnabled(en && canAdd);

		actEdit.setEnabled(en);
		actDel.setEnabled(en);
		actMoveUp.setEnabled(en);
		actMoveDown.setEnabled(en);

		if (pat == null) return;

		insertSubtree(DiagnoselisteItem.getRoot(pat, typ), null);
	}

	@Override
	public void createPartControl(Composite parent) {
		tree = new Tree(parent, SWT.NONE);

		final TreeEditor editor = new TreeEditor(tree);
		editor.horizontalAlignment = SWT.LEFT;
		editor.grabHorizontal = true;

		// SelectionListener um Eintraege inline bearbeiten zu koennen.
		/*tree.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) { }
			public void widgetSelected(SelectionEvent e) {
				TreeItem diagItem = (TreeItem) e.item;
				if (diagItem == null) return;

	            Control old = editor.getEditor();
	            if (old != null) old.dispose();

	            Text diagEditor = new Text(tree, SWT.MULTI | SWT.WRAP);
	            diagEditor.setData(diagItem);
	            diagEditor.setText(diagItem.getText());

	            // ModifyListener um Aenderungen zu speichern
	            diagEditor.addModifyListener(new ModifyListener() {
					public void modifyText(ModifyEvent e) {
						Text te = (Text) e.widget;
						TreeItem ti = (TreeItem) te.getData();
						DiagnoselisteItem di = (DiagnoselisteItem) ti.getData();
						ti.setText(te.getText());
						di.setText(te.getText());
					}
				});
	            //diagEditor.selectAll();
	            diagEditor.setFocus();
	            editor.setEditor(diagEditor, diagItem);
			}
		});*/
		MultilinePaintListener mlListener = new MultilinePaintListener();
		tree.addListener(SWT.MeasureItem, mlListener);
		tree.addListener(SWT.PaintItem, mlListener);
		tree.addListener(SWT.EraseItem, mlListener);


		actEdit = new Action("Bearbeiten") {
			@Override
			public void run() {
				if (tree.getSelectionCount() == 0) return;
				TreeItem ti = tree.getSelection()[0];
				DiagnoselisteItem di = (DiagnoselisteItem) ti.getData();
				(new DiagnoseDialog(getSite().getShell(), di, showDate)).open();
				setupTI(ti, di);
			}
		};

		tree.addMouseListener(new MouseListener() {
			public void mouseUp(MouseEvent e) {}
			public void mouseDown(MouseEvent e) {}
			public void mouseDoubleClick(MouseEvent e) {
				actEdit.run();
			}
		});

		actAdd = new Action("Neue Kategorie") {
			@Override
			public void run() {
				Patient p = ElexisEventDispatcher.getSelectedPatient();
				DiagnoselisteItem root = DiagnoselisteItem.getRoot(p, typ);
				DiagnoselisteItem di = root.createChild();
				(new DiagnoseDialog(getSite().getShell(), di, showDate)).open();
				createTI(di, null, di.getPosition());
			}
		};

		actAddChild = new Action("Neue Unterdiagnose") {
			@Override
			public void run() {
				TreeItem[] tis = tree.getSelection();
				if (tis.length > 0) {
					DiagnoselisteItem di = (DiagnoselisteItem) tis[0].getData();
					DiagnoselisteItem ndi = di.createChild();

					DiagnoseDialog dd =  new DiagnoseDialog(
							getSite().getShell(), ndi, showDate);
					if (dd.open() == DiagnoseDialog.OK) {
						createTI(ndi, tis[0], ndi.getPosition());
						// Parent expanden
						tis[0].setExpanded(true);
					} else {
						ndi.delete();
					}
				}
			}
		};
		actDel = new Action("Löschen") {
			@Override
			public void run() {
				TreeItem[] tis = tree.getSelection();
				if (tis.length > 0) {
					DiagnoselisteItem di = (DiagnoselisteItem) tis[0].getData();
					if (!di.getChildren().isEmpty()) {
						SWTHelper.alert("Es existieren noch Unterdiagnosen",
							"Bitte zuerst alle Unterdiagnosen der zu löschenden"
							+ " Diagnose entfernen.");
						return;
					}
					di.delete();
					tis[0].dispose();
				}
			}
		};

		actMoveUp = new Action("Hoch") {
			@Override
			public void run() {
				TreeItem[] tis = tree.getSelection();
				if (tis.length > 0) {
					DiagnoselisteItem di = (DiagnoselisteItem) tis[0].getData();
					di.moveUp();

					TreeItem parent = tis[0].getParentItem();
					tis[0].dispose();

					// Item neu erstellen an neuer Position
					TreeItem ti = createTI(di, parent, di.getPosition());
					insertSubtree(di, ti);
					tree.select(ti);
				}
			}
		};
		actMoveDown = new Action("Runter") {
			@Override
			public void run() {
				TreeItem[] tis = tree.getSelection();
				if (tis.length > 0) {
					DiagnoselisteItem di = (DiagnoselisteItem) tis[0].getData();
					di.moveDown();

					TreeItem parent = tis[0].getParentItem();
					tis[0].dispose();

					// Item neu erstellen an neuer Position
					TreeItem ti = createTI(di, parent, di.getPosition());
					insertSubtree(di, ti);
					tree.select(ti);
				}
			}
		};

		// Menus oben rechts in der View
		ViewMenus menus = new ViewMenus(getViewSite());
		menus.createToolbar(actAdd, actMoveUp, actMoveDown);

		menus.createControlContextMenu(tree, actAddChild, actDel);

		ElexisEventDispatcher.getInstance().addListeners(this);
		updateTree(ElexisEventDispatcher.getSelectedPatient());
	}

	@Override
	public void setFocus() {
	}

	@Override
	public void dispose() {
		ElexisEventDispatcher.getInstance().removeListeners(this);
	}

	public void catchElexisEvent(ElexisEvent ev) {
		if (ev.getType() == ElexisEvent.EVENT_SELECTED) {
			updateTree((Patient) ev.getObject());
		}
	}

	private final ElexisEvent eetmpl = new ElexisEvent(null, Patient.class,
			ElexisEvent.EVENT_SELECTED);
	public ElexisEvent getElexisEventFilter() {
		return eetmpl;
	}

}