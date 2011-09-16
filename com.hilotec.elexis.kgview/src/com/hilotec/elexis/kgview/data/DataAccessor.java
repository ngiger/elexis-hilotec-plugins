package com.hilotec.elexis.kgview.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import com.hilotec.elexis.kgview.medikarte.MedikarteHelpers;

import ch.elexis.data.Konsultation;
import ch.elexis.data.Patient;
import ch.elexis.data.PersistentObject;
import ch.elexis.data.Prescription;
import ch.elexis.util.IDataAccess;
import ch.rgw.tools.Result;
import ch.rgw.tools.StringTool;

public class DataAccessor implements IDataAccess {
	private static final String[] fields = {
		"JetzigesLeiden", "JetzigesLeidenICPC",
		"AllgStatus", "LokStatus",
		"Prozedere", "ProzedereICPC",
		"Diagnose", "DiagnoseICPC"};

	public String getName() {
		return "Hilotec-KG";
	}

	public String getDescription() {
		return "Daten aus dem Hilotec-KG-Plugin";
	}

	public List<Element> getList() {
		ArrayList<Element> res = new ArrayList<Element>(fields.length);
		for (String s: fields) {
			res.add(new Element(IDataAccess.TYPE.STRING, s, s,
				Konsultation.class, 0));
		}
		return res;
	}

	private String[] emptyRow(int count) {
		String[] row = new String[count];
		for (int i = 0; i < row.length; i++)
			row[i] = "";
		return row;
	}
	
	private Result<Object> generiereMedikarte(Patient pat) {
		List<Prescription> l = MedikarteHelpers.
			medikarteMedikation(pat, false);
		
		// Liste aller Einnahmevorschriften und Medis gruppiert nach EV
		SortedSet<String> evs = new TreeSet<String>();
		HashMap<String, List<Prescription>> prescs =
			new HashMap<String, List<Prescription>>();
		for (Prescription p: l) {
			String ev = p.getBemerkung();
			evs.add(ev);
			if (!prescs.containsKey(ev))
				prescs.put(ev, new ArrayList<Prescription>());
			prescs.get(ev).add(p);
		}
		
		// Zeilen fuer Spaltenueberschriften zaehlen
		int extra = 2 * evs.size() - (evs.contains("") ? 2 : 1);
		String[][] res = new String[l.size() + extra][];
		
		// Tabelle Generieren
		int i = 0;
		for (String ev: evs) {
			// Ueberschrift
			if (i != 0) res[i++] = emptyRow(7);
			if (!ev.equals("")) {
				String[] row = emptyRow(7);
				row[0] = ev;
				res[i++] = row;
			}
			
			// Medikamente
			for (Prescription p: prescs.get(ev)){
				FavMedikament fm = FavMedikament.load(p.getArtikel());
				String[] dosierung = p.getDosis().split("-");
				if (dosierung.length != 4 || fm == null) continue;
				
				String[] row = new String[7];
				row[0] = fm.getBezeichnung();
				row[5] = fm.getEinheit();
				row[6] = fm.getZweck();
				row[1] = dosierung[0];
				row[2] = dosierung[1];
				row[3] = dosierung[2];
				row[4] = dosierung[3];
				
				res[i++] = row;
			}
		}
		return new Result<Object>(res);
	}
	
	public Result<Object> getObject(String descriptor,
			PersistentObject dependentObject, String dates, String[] params)
	{
		if (dependentObject instanceof Patient &&
				descriptor.equals("Medikarte"))
		{
			return generiereMedikarte((Patient) dependentObject);
		} else if (dependentObject instanceof Patient &&
				descriptor.equals("Medikarte.Datum"))
		{
			return new Result<Object>(
				MedikarteHelpers.medikarteDatum((Patient) dependentObject));
		}
		
		if (!(dependentObject instanceof Konsultation)) {
			return new Result<Object>(Result.SEVERITY.ERROR,
				IDataAccess.INVALID_PARAMETERS, "Ungültiger Parameter",
				dependentObject, true);
		}
		Konsultation kons = (Konsultation) dependentObject;
		KonsData data = new KonsData(kons);
		
		for (String f: fields) {
			if (f.equals(descriptor)) {
				return new Result<Object>(StringTool.unNull(data.get(f)));
			}
		}
		return new Result<Object>(Result.SEVERITY.ERROR, IDataAccess.INVALID_PARAMETERS,
				"Ungültiges Feld", dependentObject, true);
	}

}
