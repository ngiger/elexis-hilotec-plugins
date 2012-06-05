package com.hilotec.elexis.kgview;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import ch.elexis.Hub;
import ch.elexis.preferences.SettingsPreferenceStore;
import ch.elexis.preferences.inputs.MultilineFieldEditor;

/**
 * Einstelllungsseite fuer kgview-Plugin.
 * 
 * @author Antoine Kaufmann
 */
public class Preferences extends FieldEditorPreferencePage implements
		IWorkbenchPreferencePage {
	public static final String CFG_EVLISTE = "hilotec/kgview/einnahmevorschriften";
	public static final String CFG_FLORDZ = "hilotec/kgview/ordnungszahlfavliste";
	public static final String CFG_AKG_HEARTBEAT = "hilotec/kgview/archivkgheartbaeat";

	public Preferences() {
		super(GRID);
		setPreferenceStore(new SettingsPreferenceStore(Hub.mandantCfg));
	}
	
	@Override
	public void init(IWorkbench workbench) {
	}

	@Override
	protected void createFieldEditors() {
		addField(new MultilineFieldEditor(CFG_EVLISTE, "Einnahmevorschriften",
				5, SWT.V_SCROLL, true, getFieldEditorParent()));
		addField(new BooleanFieldEditor(CFG_FLORDZ,
			    "Ordnungszahl in FML anzeigen", getFieldEditorParent()));
		addField(new IntegerFieldEditor(CFG_AKG_HEARTBEAT,
				"Archiv KG Heartbeat", getFieldEditorParent()));
	}

	/**
	 * @return Konfigurierte Einnahmevorschriften im aktuellen Mandant.
	 */
	public static String[] getEinnahmevorschriften() {
		String s = Hub.mandantCfg.get(CFG_EVLISTE, "");
		return s.split(",");
	}
	
	/**
	 * @return
	 */
	public static boolean getOrdnungszahlInFML() {
		boolean oz = Hub.mandantCfg.get(CFG_FLORDZ, false);
		return oz;
	}
	
	/**
	 * @return Heartbeat abstand in Sekunden, fuer die Aktualisierung der
	 *         ArchivKG-Ansicht.
	 */
	public int getArchivKGHeartbeat() {
		int n = Integer.parseInt(Hub.mandantCfg.get(CFG_AKG_HEARTBEAT, "10"));
		if (n < 1) n = 1;
		return n;
	}
}
