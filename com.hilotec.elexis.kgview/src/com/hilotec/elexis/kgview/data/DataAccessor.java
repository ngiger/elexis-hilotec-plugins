package com.hilotec.elexis.kgview.data;

import java.util.ArrayList;
import java.util.List;

import ch.elexis.data.Konsultation;
import ch.elexis.data.PersistentObject;
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

	public Result<Object> getObject(String descriptor,
			PersistentObject dependentObject, String dates, String[] params)
	{	
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
