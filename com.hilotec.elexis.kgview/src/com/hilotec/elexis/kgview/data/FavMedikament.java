package com.hilotec.elexis.kgview.data;

import ch.elexis.data.Artikel;
import ch.elexis.data.PersistentObject;

public class FavMedikament extends PersistentObject {
	public static final String VERSION = "1";
	public static final String PLUGIN_ID = "com.hilotec.elexis.kgview";
	
	private static final String TABLENAME = "COM_HILOTEC_ELEXIS_KGVIEW_FAVMEDIKAMENT";
	
	public static final String FLD_BEZEICHNUNG	= "Bezeichnung";
	public static final String FLD_ZWECK		= "Zweck";
	public static final String FLD_EINHEIT		= "Einheit";
	
	static {
		addMapping(TABLENAME,
			"Bezeichnung",
			"Zweck",
			"Einheit");
		checkTable();
	}

	private static final String create =
		"CREATE TABLE " + TABLENAME + " ("
			+ "  ID				VARCHAR(25) PRIMARY KEY, "
			+ "  lastupdate 	BIGINT, "
			+ "  deleted		CHAR(1) DEFAULT '0', "
			+ "  Artikel	 	VARCHAR(25), "
			+ "  Bezeichnung	TEXT, "
			+ "  Zweck			TEXT, "
			+ "  Einheit		TEXT  "
			+ ");"
			+ "INSERT INTO " + TABLENAME + " (ID, Bezeichnung) VALUES "
			+ "	('VERSION', '" + VERSION + "');";
	
	private static void checkTable() {
		
		String fm = null;
		try { fm = getConnection().queryString(
				"SELECT Bezeichnung FROM " + TABLENAME + " WHERE ID='VERSION';");
		} catch (Exception e) {}
		
		if (fm == null) {
			createOrModifyTable(create);
		}
	}

	public FavMedikament(Artikel a, String b, String z, String e) {
		super(a.getId());
		if (!exists()) {
			create(a.getId());
		}
		
		setBezeichnung(b);
		setZweck(z);
		setEinheit(e);
	}
	
	public static FavMedikament load(final String id) {
		// FIXME: Dirty hack um zu pruefen ob ein Medikament schon
		//        existiert, damit nicht versehentlich leere angelegt
		//        werden.
		String fm =	getConnection().queryString(
				"SELECT Bezeichnung FROM " + TABLENAME +
				" WHERE ID='"+id+"';");
		
		if (fm == null) return null;
		return new FavMedikament(id);
	}
	
	public static FavMedikament load(Artikel art) {
		return load(art.getId());
	}
	
	protected FavMedikament() {}
	
	protected FavMedikament(final String id) {
		super(id);
	}

	@Override
	protected String getTableName() {
		return TABLENAME;
	}

	@Override
	public String getLabel() {
		return getBezeichnung();
	}
	
	public Artikel getArtikel() {
		return Artikel.load(getId());
	}
	

	public String getBezeichnung() {
		return get(FLD_BEZEICHNUNG);
	}
	
	public String getZweck() {
		return get(FLD_ZWECK);
	}
	
	public String getEinheit() {
		return get(FLD_EINHEIT);
	}
	
	
	public void setBezeichnung(String txt) {
		set(FLD_BEZEICHNUNG, txt);
	}

	public void setZweck(String txt) {
		set(FLD_ZWECK, txt);
	}
	
	public void setEinheit(String txt) {
		set(FLD_EINHEIT, txt);
	}
}
