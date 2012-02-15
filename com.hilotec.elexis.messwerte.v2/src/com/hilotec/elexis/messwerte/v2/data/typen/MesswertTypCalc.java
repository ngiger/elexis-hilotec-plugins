/*******************************************************************************
 * Copyright (c) 2009-2010, A. Kaufmann and Elexis
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    A. Kaufmann - initial implementation
 *    G. Weirich - adapted to new Elexis scripting system by Ver. 2.1
 *    P. Chaubert - adapted to Messwerte V2
 * 
 * $Id$
 *******************************************************************************/

package com.hilotec.elexis.messwerte.v2.data.typen;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Widget;

import ch.elexis.ElexisException;
import ch.elexis.Hub;
import ch.elexis.actions.ElexisEventDispatcher;
import ch.elexis.data.Fall;
import ch.elexis.data.Konsultation;
import ch.elexis.data.Script;
import ch.elexis.scripting.Interpreter;
import ch.elexis.selectors.ActiveControl;
import ch.elexis.selectors.TextField;
import ch.elexis.util.SWTHelper;
import ch.rgw.tools.Log;
import ch.rgw.tools.TimeTool;

import com.hilotec.elexis.messwerte.v2.data.Messung;
import com.hilotec.elexis.messwerte.v2.data.Messwert;
import com.hilotec.elexis.messwerte.v2.data.MesswertBase;

/**
 * @author Antoine Kaufmann
 */
public class MesswertTypCalc extends MesswertBase implements IMesswertTyp {
	private final Log log = Log.get("Messwerte");
	
	/**
	 * Decimal Format für die Anzeige
	 */
	DecimalFormat df = new DecimalFormat("#0.#");
	
	/**
	 * Eigentlicher Code der Formel
	 */
	private String formula;
	
	/**
	 * Interpreter, der benutzt werden soll, um die
	 */
	private String interpreter;
	
	/**
	 * Liste mit den Variablen die fuer die Formel gesetzt werden sollen
	 */
	private final ArrayList<CalcVar> variables = new ArrayList<CalcVar>();
	
	public MesswertTypCalc(String n, String t, String u){
		super(n, t, u);
	}
	
	/**
	 * Kontext des Interpreters vorbereiten um die Formel auswerten zu koennen. Dabei werden die
	 * Variablen importiert.
	 * 
	 * TODO: Ist noch Beanshell-spezifisch
	 * 
	 * @param interpreter
	 *            Interpreter
	 * @param messung
	 *            Messung in der die Formel ausgewertet werden soll
	 * @throws EvalError
	 */
	private void interpreterSetzeKontext(Interpreter interpreter, Messung messung)
		throws ElexisException{
		for (CalcVar cv : variables) {
			Object wert = holeVariable(messung, cv.getName(), cv.getSource());
			if (wert != null) {
				interpreter.setValue(cv.getName(), wert);
			}
		}
		interpreter.setValue("actPatient", ElexisEventDispatcher.getSelectedPatient());
		interpreter.setValue("actFall", ElexisEventDispatcher.getSelected(Fall.class));
		interpreter.setValue("actKons", ElexisEventDispatcher.getSelected(Konsultation.class));
		interpreter.setValue("actMandant", Hub.actMandant);
		interpreter.setValue("actUser", Hub.actUser);
		interpreter.setValue("Elexis", Hub.plugin);
	}
	
	/**
	 * Wert einer Variable fuer die Formel bestimmen
	 * 
	 * @param messung
	 *            Messung in der die Formel ausgewertet werten soll
	 * @param name
	 *            Name der Variable. Kann mit . getrennt sein, wenn sich links vom Punkt jeweils ein
	 *            Data-Feld befindet, dabei bezieht sich der Teil rechts vom Punkt auf das Feld in
	 *            dem referenzierten Objekt.
	 * @param source
	 *            Quelle der Variable
	 * 
	 * @return Wert der dem Interpreter uebergeben werden soll. Haengt vom typ der Variable ab.
	 */
	private Object holeVariable(Messung messung, String name, String source){
		if (messung == null) {
			return "messung?";
		}
		if (source == null) {
			return "source?";
		}
		String[] parts = source.split("\\.");
		Messwert messwert = messung.getMesswert(parts[0]);
		IMesswertTyp typ = messwert.getTyp();
		
		if (parts.length == 1) {
			if (typ instanceof MesswertTypNum) {
				if (typ.isShown()) {
					return Double.parseDouble(typ.getActualValue());
				} else {
					return Double.parseDouble(messwert.getWert());
				}
			} else if (typ instanceof MesswertTypBool) {
				if (typ.isShown()) {
					return Boolean.parseBoolean(typ.getActualValue());
				} else {
					return Boolean.parseBoolean(messwert.getWert());
				}
			} else if (typ instanceof MesswertTypStr) {
				if (typ.isShown()) {
					return typ.getActualValue();
				} else {
					return messwert.getWert();
				}
			} else if (typ instanceof MesswertTypCalc) {
				if (typ.isShown()) {
					return Double.parseDouble(typ.getActualValue());
				} else {
					return Double.parseDouble(messwert.getDarstellungswert());
				}
			} else if (typ instanceof MesswertTypEnum) {
				if (typ.isShown()) {
					return Integer.parseInt(typ.getActualValue());
				} else {
					return Integer.parseInt(messwert.getWert());
				}
			} else if(typ instanceof MesswertTypDate){
				if (typ.isShown()) {
					TimeTool tt = new TimeTool(typ.getActualValue());
					return tt.get(TimeTool.YEAR) * 365 + tt.get(TimeTool.DAY_OF_YEAR);
				} else {
					TimeTool tt = new TimeTool(messwert.getWert());
					return tt.get(TimeTool.YEAR) * 365 + tt.get(TimeTool.DAY_OF_YEAR);
				}
			} else if (typ instanceof MesswertTypData) {
				log.log("Fehler beim Auswerten einer Variable(" + name + "): "
					+ "wertet auf ein Data-Feld aus.", Log.ERRORS);
				return null;
			}
		}

		if (!(typ instanceof MesswertTypData)) {
			log.log("Fehler beim Auswerten einer Variable(" + name + "): "
				+ "Dereferenziertes Feld ist nicht vom Typ DATA", Log.ERRORS);
			return null;
		}
		MesswertTypData t = (MesswertTypData) typ;
		Messung dm = t.getMessung(messwert);
		return holeVariable(dm, name + "." + parts[0], source.substring(source.indexOf(".") + 1));
	}
	
	/**
	 * Interne Klasse die eine Variable fuer die Formel darstellt(nur deklaration).
	 * 
	 * @author Antoine Kaufmann
	 */
	private class CalcVar {
		/**
		 * Name der Variable
		 */
		private final String name;
		
		/**
		 * Quelle der Variable(meist Feldname in der Messung)
		 */
		private final String source;
		
		CalcVar(String n, String s){
			name = n;
			source = s;
		}
		
		String getName(){
			return name;
		}
		
		String getSource(){
			return source;
		}
	}
	
	/**
	 * Neue Variable hinzufuegen
	 * 
	 * @param name
	 *            Name der Variable
	 * @param source
	 *            Quelle fuer den Variableninhalt
	 */
	public void addVariable(String name, String source){
		variables.add(new CalcVar(name, source));
	}
	
	/**
	 * Formel, die berechnet werden soll, setzen.
	 * 
	 * @param f
	 *            Formel
	 * @param i
	 *            Interpreter fuer die Formel
	 */
	public void setFormula(String f, String i){
		formula = f;
		interpreter = i;
	}
	
	public String getFormatPattern(){
		return df.toPattern();
	}
	
	public void setFormatPattern(String pattern){
		df.applyPattern(pattern);
	}
	
	public String getRoundingMode(){
		return df.getRoundingMode().toString();
	}
	
	public void setRoundingMode(String roundingMode){
		df.setRoundingMode(RoundingMode.valueOf(roundingMode));
	}
	
	public String erstelleDarstellungswert(Messwert messwert){
		
		try {
			Interpreter interpreter = Script.getInterpreterFor(formula);
			interpreterSetzeKontext(interpreter, messwert.getMessung());
			Object wert = interpreter.run(formula, false);
			
			try{
				return df.format(wert);
			} catch(Exception e){
				// Wenn formatieren von 'wert' fehlschlägt (bei String oder Datum), den Wert unformatiert zurückgeben
				return wert.toString();
			}
		} catch (ElexisException e) {
			e.printStackTrace();
			log.log("Fehler beim Berechnen eines Wertes: " + e.getMessage(), Log.ERRORS);
		} catch (Throwable e) {
			e.printStackTrace();
		}
		return "";
	}
	
	public String getDefault(){
		return "";
	}
	
	public void setDefault(String str){}
	
	public Widget createWidget(Composite parent, Messwert messwert){
		widget = SWTHelper.createText(parent, 1, SWT.NONE);
		((Text) widget).setText(messwert.getDarstellungswert());
		((Text) widget).setEditable(false);
		setShown(true);
		return widget;
	}
	
	public ActiveControl createControl(Composite parent, Messwert messwert, boolean bEditable){
		IMesswertTyp dft = messwert.getTyp();
		String labelText = dft.getTitle();
		if (!dft.getUnit().equals("")) {
			labelText += " [" + dft.getUnit() + "]";
		}
		TextField tf = new TextField(parent, ActiveControl.READONLY, labelText);
		tf.setText(messwert.getDarstellungswert());
		return tf;
		
	}
	
	public void calcNewValue(Messwert messwert){
		((Text) widget).setText(messwert.getDarstellungswert());
	}
	
}
