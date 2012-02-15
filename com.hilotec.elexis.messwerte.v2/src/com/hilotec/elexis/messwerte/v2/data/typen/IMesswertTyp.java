/*******************************************************************************
 * Copyright (c) 2007-2009, A. Kaufmann and Elexis
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

package com.hilotec.elexis.messwerte.v2.data.typen;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Widget;

import ch.elexis.selectors.ActiveControl;

import com.hilotec.elexis.messwerte.v2.data.Messwert;

public interface IMesswertTyp {
	/**
	 * @return Feldname des Messwertes (interne Verwendung zum Referenzieren von Feldern)
	 */
	public abstract String getName();
	
	/**
	 * @return Beschriftung des Messwertes (zum Anzeigen fuer den User)
	 */
	public abstract String getTitle();
	
	/**
	 * @return Einheit, die zum Messwert angezeigt werden soll, kann auch leer sein.
	 */
	public abstract String getUnit();
	
	/**
	 * @return Standardwert des Messwerts, wenn er neu angelegt wird
	 */
	public abstract String getDefault();
	
	/**
	 * Standardwert aendern
	 * 
	 * @param def
	 *            Neuer Standardwert
	 */
	public abstract void setDefault(String def);
	
	/**
	 * Widget fuer die Darstellung des Messwertes im Editieren-Dialog erstellen und auch glech mit
	 * aktuellem Wert befuellen.
	 * 
	 * @see saveInput
	 * 
	 * @param parent
	 *            Eltern-Element in dem das Widget escheinen soll
	 * @param messwert
	 *            Messwert, der dargestellt werden soll
	 * 
	 * @return erzeugtes Widget
	 */
	public abstract Widget createWidget(Composite parent, Messwert messwert);
	
	/**
	 * Erzeugt ein AciveControl und befüllt es mit dem Messwert
	 * 
	 * @param parent
	 * @param messwert
	 * @param bEditable
	 *            true wenn das Feld editierbar sein soll
	 * 
	 * @return erzeugtes ActiveControl
	 */
	public abstract ActiveControl createControl(Composite parent, Messwert messwert,
		boolean bEditable);
	
	/**
	 * Eingaben im Widget, welches in createWidget erstellt wurde, in den angegebenen Messwert
	 * einfuellen.
	 * 
	 * @see createWidget
	 * 
	 * @param widget
	 * @param messwert
	 */
	public abstract void saveInput(Messwert messwert);
	
	/**
	 * Eingaben im Widget, welches in createWidget erstellt wurde, überprüfen.
	 * 
	 * @see createWidget
	 * 
	 * @param messwert
	 * @param pattern
	 * 
	 * @return gibt true zurück wenn die Eingabe dem Pattern enspricht, ansonsten false
	 */
	public abstract boolean checkInput(Messwert messwert, String pattern);
	
	/**
	 * Von einem Messwert eine fuer den Benutzer lesbare Form generieren
	 * 
	 * @param messwert
	 *            Darzustellender Messwert
	 * 
	 * @return String wie er dem Benutzer praesentiert werden kann
	 */
	public abstract String erstelleDarstellungswert(Messwert messwert);
	
	/**
	 * @return ob das Feld editierbar ist
	 */
	public abstract boolean isEditable();
	
	/**
	 * Setzen ob das Feld editierbar ist oder nicht
	 * 
	 * @param editable
	 */
	public abstract void setEditable(boolean editable);
	
	/**
	 * @return Pattern zum Prüfen der Eingabe im Widget
	 */
	public abstract String getValidpattern();
	
	/**
	 * Pattern für die Prüfung der Eingabe im Widget setzen
	 * 
	 * @param validpattern
	 */
	public abstract void setValidpattern(String validpattern);
	
	/**
	 * @return Text zum Anzeigen bei einber fehlerhaften Eingabe im Widget
	 */
	public abstract String getInvalidmessage();
	
	/**
	 * Text, der bei einer fehlerhaften Eingabe im Widget angezeigt wird, setzen
	 * 
	 * @param invalidmessage
	 */
	public abstract void setInvalidmessage(String invalidmessage);
	
	/**
	 * @return den aktuelle eingegebenen Wert im Widget
	 */
	public abstract String getActualValue();
	
	/**
	 * @return den gesetzten Wert von isShown. True wenn das Widget angzeigt wird, ansonsten false
	 */
	public abstract boolean isShown();
	
	/**
	 * Setz isShown auf den übergebenen Wert
	 * 
	 * @param isShown
	 */
	public abstract void setShown(boolean isShown);
}