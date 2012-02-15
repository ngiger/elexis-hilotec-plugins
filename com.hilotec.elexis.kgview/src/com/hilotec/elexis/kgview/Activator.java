/*******************************************************************************
 * Copyright (c) 2007-2009, A. Kaufmann and Elexis
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    A. Kaufmann - initial implementation 
 *    
 * $Id: Activator.java 5386 2009-06-23 11:34:17Z rgw_ch $
 *******************************************************************************/

package com.hilotec.elexis.kgview;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import ch.elexis.actions.ElexisEvent;
import ch.elexis.actions.ElexisEventDispatcher;
import ch.elexis.data.Konsultation;
import ch.elexis.data.Patient;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {
	
	// The plug-in ID
	public static final String PLUGIN_ID = "com.hilotec.elexis.kgview";
	
	// The shared instance
	private static Activator plugin;
	
	// Workaround (see below)
	private static MyPatSelListener psl = null;
	
	/**
	 * The constructor
	 */
	public Activator(){}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception{
		super.start(context);
		plugin = this;
		
		// Workaround fuer Selektionsevents bei Konsultationen
		psl = new MyPatSelListener();
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception{
		plugin = null;
		psl.destroy();
		psl = null;
		super.stop(context);
	}
	
	/**
	 * Returns the shared instance
	 * 
	 * @return the shared instance
	 */
	public static Activator getDefault(){
		return plugin;
	}
	
	
	/**
	 * Sorgt dafuer dass ein Deselection-Event gefired wird fuer
	 * Konsultationen, wenn ein Patient deselektiert wird. Passiert 
	 * Seiten Elexis nur manchmal. 
	 */
	private class MyPatSelListener extends POSelectionListener<Patient> {
		public MyPatSelListener() {
			init();
		}
		
		@SuppressWarnings("static-access")
		@Override
		protected void deselected(Patient pat) {
			ElexisEventDispatcher eed = ElexisEventDispatcher.getInstance(); 
			Konsultation kons = (Konsultation)
				eed.getSelected(Konsultation.class);
			if (kons != null) {
				eed.fire(new ElexisEvent(null, Konsultation.class,
									ElexisEvent.EVENT_DESELECTED));
			}
		}
		
		@Override
		protected void selected(Patient pat) {}
	}
}
