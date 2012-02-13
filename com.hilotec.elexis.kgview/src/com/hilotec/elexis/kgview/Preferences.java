package com.hilotec.elexis.kgview;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
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
	}

	/**
	 * @return Konfigurierte Einnahmevorschriften im aktuellen Mandant.
	 */
	public static String[] getEinnahmevorschriften() {
		String s = Hub.mandantCfg.get(CFG_EVLISTE, "");
		return s.split(",");
	}
}
