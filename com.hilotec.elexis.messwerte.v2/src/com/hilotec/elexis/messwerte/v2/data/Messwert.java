/*******************************************************************************
 * Copyright (c) 2009-2010, A. Kaufmann and Elexis
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    A. Kaufmann - initial implementation 
 *    P. Chaubert - adapted to Messwerte V2
 *    
 * $Id$
 *******************************************************************************/

package com.hilotec.elexis.messwerte.v2.data;

import com.hilotec.elexis.messwerte.v2.Activator;
import com.hilotec.elexis.messwerte.v2.data.typen.IMesswertTyp;

import ch.elexis.data.PersistentObject;

/**
 * @author Antoine Kaufmann
 */
public class Messwert extends PersistentObject {
	public static final String VERSION = "2";
	public static final String PLUGIN_ID = Activator.PLUGIN_ID;
	private static final String TABLENAME = "COM_HILOTEC_ELEXIS_MESSWERTE_MESSWERTE";
	
	static {
		addMapping(TABLENAME, "MessungID", "Name", "Wert");
		checkTable();
	}
	
	private static final String create = "CREATE TABLE " + TABLENAME + " ("
		+ "  ID			VARCHAR(25) PRIMARY KEY, " + "  lastupdate 	BIGINT, "
		+ "  deleted		CHAR(1) DEFAULT '0', " + "  MessungID	VARCHAR(25), "
		+ "  Name			VARCHAR(25), " + "  Wert			VARCHAR(25) " + ");" + "INSERT INTO " + TABLENAME
		+ " (ID, Name) VALUES " + "	('VERSION', '" + VERSION + "');";
	
	/**
	 * Pruefen ob die Tabelle existiert, und wenn nein, anlegen
	 */
	private static void checkTable(){
		Messwert check = load("VERSION");
		if (!check.exists()) {
			createOrModifyTable(create);
		}
	}
	
	@Override
	public String getLabel(){
		return get("Name");
	}
	
	@Override
	public String getTableName(){
		return TABLENAME;
	}
	
	/**
	 * Dieser Konstruktor darf nicht von aussen erreichbar sein
	 */
	protected Messwert(){}
	
	/**
	 * Bereits existierenden Messwert anhand seiner ID erstellen
	 * 
	 * @param id
	 *            ID
	 */
	protected Messwert(String id){
		super(id);
	}
	
	/**
	 * Neuen Messwert anglegen
	 * 
	 * @param messung
	 *            Messung zu der dieser Messwert gehoeren soll
	 * @param name
	 *            Name des Messwertes
	 * @param wert
	 *            Zu speichernder Wert
	 */
	public Messwert(Messung messung, String name, String wert){
		create(null);
		set("MessungID", messung.getId());
		set("Name", name);
		set("Wert", wert);
	}
	
	/**
	 * Neuen Messwert anlegen
	 * 
	 * @param messung
	 *            Messung zu der dieser Messwert gehoeren soll
	 * @param name
	 *            Name dieses Messwertes
	 */
	public Messwert(Messung messung, String name){
		create(null);
		set("MessungID", messung.getId());
		set("Name", name);
		set("Wert", getTyp().getDefault());
	}
	
	/**
	 * Messwert anhand seiner ID aus der Datenbank laden
	 * 
	 * @param id
	 *            ID des gewuenschten Messwerts
	 * 
	 * @return Messwert
	 */
	public static Messwert load(final String id){
		return new Messwert(id);
	}
	
	/**
	 * @return Name dieses Messwertes
	 */
	public String getName(){
		return get("Name");
	}
	
	/**
	 * @return Eigentlicher Messwert
	 */
	public String getWert(){
		return get("Wert");
	}
	
	/**
	 * @return Dem Benutzer anzeigbare Form dieses Messwertes
	 */
	public String getDarstellungswert(){
		return getTyp().erstelleDarstellungswert(this);
	}
	
	/**
	 * Messwert aendern
	 * 
	 * @param wert
	 *            Neuer Wert
	 */
	public void setWert(String wert){
		set("Wert", wert);
	}
	
	/**
	 * @return Die Messung zu der diese Messung gehoert
	 */
	public Messung getMessung(){
		return new Messung(get("MessungID"));
	}
	
	/**
	 * @return Typ des Messwertes
	 */
	public IMesswertTyp getTyp(){
		return getMessung().getTyp().getMesswertTyp(getName());
	}
}
