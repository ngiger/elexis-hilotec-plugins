package com.hilotec.elexis.kgview.diagnoseliste;

import java.util.List;

import ch.elexis.data.Patient;
import ch.elexis.data.PersistentObject;
import ch.elexis.data.Query;
import ch.rgw.tools.StringTool;

public class DiagnoselisteItem extends PersistentObject {
	public static final String VERSION = "1";
	public static final String PLUGIN_ID = "com.hilotec.elexis.kgview";
	
	private static final String TABLENAME = "COM_HILOTEC_ELEXIS_KGVIEW_DIAGNOSE";
	public static final String FLD_PATIENT = "Patient";
	public static final String FLD_PARENT = "Parent";
	public static final String FLD_POSITION = "Position";
	public static final String FLD_TEXT = "Text";
	
	static {
		addMapping(TABLENAME,
				FLD_PATIENT,
				FLD_PARENT,
				FLD_POSITION,
				FLD_TEXT);
		checkTable();
	}
	
	private static final String create =
		"CREATE TABLE " + TABLENAME + " ("
			+ "  ID				VARCHAR(25) PRIMARY KEY, "
			+ "  lastupdate 	BIGINT, "
			+ "  deleted		CHAR(1) DEFAULT '0', "
			+ "  Patient		VARCHAR(25), "
			+ "  Parent	 		VARCHAR(25), "
			+ "  Position		INT DEFAULT 0, "
			+ "  Text			TEXT "
			+ ");"
			+ "INSERT INTO " + TABLENAME + " (ID, Position) VALUES "
			+ "	('VERSION', '" + VERSION + "');";
	
	private static void checkTable() {	
		String fm = null;
		try { fm = getConnection().queryString(
				"SELECT Position FROM " + TABLENAME + " WHERE ID='VERSION';");
		} catch (Exception e) {}
		if (fm == null) {
			createOrModifyTable(create);
		}
	}

	protected DiagnoselisteItem(Patient pat) {
		create(null);
		setPatient(pat);
		setPosition(0);
	}
	
	protected DiagnoselisteItem(DiagnoselisteItem parent, int pos) {
		create(null);
		setPatient(parent.getPatient());
		setParent(parent);
		setPosition(pos);
	}
	
	protected DiagnoselisteItem() {}
	
	protected DiagnoselisteItem(String id) {
		super(id);
	}
	
	public static DiagnoselisteItem load(String id) {
		DiagnoselisteItem di = new DiagnoselisteItem(id);
		if (!di.exists()) return null;
		return di;
	}
	
	
	public Patient getPatient() {
		return Patient.load(get(FLD_PATIENT));
	}
	
	public void setPatient(Patient pat) {
		if (pat == null)
			set(FLD_PATIENT, null);
		else
			set(FLD_PATIENT, pat.getId());
	}
	
	public DiagnoselisteItem getParent() {
		String id = get(FLD_PARENT);
		if (id == null || id.isEmpty()) return null;
		return load(id);
	}
	
	public void setParent(DiagnoselisteItem parent) {
		set(FLD_PARENT, parent.getId());
	}
	
	public int getPosition() {
		return Integer.parseInt(get(FLD_POSITION));
	}
	
	public void setPosition(int pos) {
		set(FLD_POSITION, Integer.toString(pos));
	}
	
	public String getText() {
		return StringTool.unNull(get(FLD_TEXT));
	}
	
	public void setText(String text) {
		set(FLD_TEXT, text);
	}
	
	
	private int nextChildPos() {
		String sql = "SELECT (MAX(Position) + 1) FROM " +
		TABLENAME + " WHERE Patient = '" + getPatient().getId() + "' AND " +
		"Parent = '" + getId() + "' AND deleted = '0'";
		return getConnection().queryInt(sql);
	}
	
	public List<DiagnoselisteItem> getChildren() {
		Query<DiagnoselisteItem> q =
			new Query<DiagnoselisteItem>(DiagnoselisteItem.class);
		q.add(FLD_PATIENT, Query.EQUALS, get(FLD_PATIENT));
		q.and();
		q.add(FLD_PARENT, Query.EQUALS, getId());
		q.orderBy(false, FLD_POSITION);
		return q.execute();
	}
	
	public DiagnoselisteItem createChild() {
		return new DiagnoselisteItem(this, nextChildPos());
	}
	
	public static DiagnoselisteItem getRoot(Patient pat) {
		Query<DiagnoselisteItem> q =
			new Query<DiagnoselisteItem>(DiagnoselisteItem.class);
		q.add(FLD_PATIENT, Query.EQUALS, pat.getId());
		q.and();
		q.add(FLD_PARENT, Query.EQUALS, null);
		List<DiagnoselisteItem> dis = q.execute();
		
		// Wenn noch kein root-Element existiert, eins anlegen
		if (dis.isEmpty()) return new DiagnoselisteItem(pat);
		
		return dis.get(0);
	}
	
	/* FIXME: Generalisieren in ein remove() und insert() oder so */
	public void moveUp() {
		int pos = getPosition();
		if (pos == 0) return;
		
		String par = (getParent() == null ? "IS NULL" : "= '" + getParent().getId() + "'");
		getConnection().exec("UPDATE " + TABLENAME + " SET Position = Position + 1 "
				+ "WHERE Parent " + par + " AND Position = " + (pos - 1) + " AND "
				+ "deleted = '0'");
		setPosition(pos - 1);
	}
	
	public void moveDown() {
		int pos = getPosition();
		if (getParent().getChildren().size() <= pos + 1) return;
		
		String par = (getParent() == null ? "IS NULL" : "= '" + getParent().getId() + "'");
		getConnection().exec("UPDATE " + TABLENAME + " SET Position = Position - 1 "
				+ "WHERE Parent " + par + " AND Position = " + (pos + 1) + " AND "
				+ "deleted = '0'");
		setPosition(pos + 1);
	}
	
	public boolean delete() {
		String par = (getParent() == null ? "IS NULL" : "= '" + getParent().getId() + "'");
		getConnection().exec("UPDATE " + TABLENAME + " SET Position = Position - 1 "
			+ "WHERE Parent " + par + " AND Position > " + getPosition() + " AND "
			+ "deleted = '0'");
		return super.delete();
	}
	
	@Override
	public String getLabel() {
		return getText();
	}

	@Override
	protected String getTableName() {
		return TABLENAME;
	}

}
