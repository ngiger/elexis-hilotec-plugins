package com.hilotec.elexis.kgview;

import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.part.ViewPart;

import com.hilotec.elexis.kgview.data.KonsData;

import ch.elexis.actions.ElexisEvent;
import ch.elexis.actions.ElexisEventDispatcher;
import ch.elexis.actions.ElexisEventListener;
import ch.elexis.data.Konsultation;
import ch.elexis.data.Patient;

public class KonsTimeView extends ViewPart
	implements ElexisEventListener
{
	public static final String ID = "com.hilotec.elexis.kgview.KonsTimeView";
	
	Label timerLbl;

	KonsData konsData;
	long time;
	
	@Override
	public void createPartControl(Composite parent) {
		parent.setLayout(new FillLayout());
		
		Composite comp = new Composite(parent, 0);
		comp.setLayout(new GridLayout());
		
		timerLbl = new Label(comp, 0);
		createButtonControl(comp);

		setEnabled(false);	
		ElexisEventDispatcher.getInstance().addListeners(this);
		
		Konsultation k = (Konsultation)
			ElexisEventDispatcher.getSelected(Konsultation.class);
		if (k != null) {
			changeToKons(k);
		}
	}

	@Override
	public void setFocus() { }

	protected void createButtonControl(Composite parent) { }
	protected void setEnabled(boolean en) { }
	protected void stopTimer() { }

	private void changeToKons(Konsultation k) {
		stopTimer();
		if (konsData != null)
			konsData.setKonsZeit(time);
		setEnabled(false);
		
		if (k != null) {
			konsData = new KonsData(k);
			time = konsData.getKonsZeit();
			setEnabled(true);
		} else {
			time = 0;
			konsData = null;
		}
		updateLabel();
	}
	
	protected void updateLabel() {
		if (timerLbl.isDisposed()) {
			return;
		}
		
		long secs = time / 1000;
		String text = String.format("%02d:%02d:%02d",
			(secs / 3600),
			((secs % 3600) / 60),
			(secs % 60));
		timerLbl.setText(text);
		timerLbl.pack();
		timerLbl.update();
	}
	
	@Override
	public void catchElexisEvent(ElexisEvent ev) {
		Object obj = ev.getObject();
		if (obj instanceof Konsultation) {
			Konsultation k = (Konsultation) obj;
			if (ev.getType() == ElexisEvent.EVENT_SELECTED)
				changeToKons(k);
			else if (ev.getType() == ElexisEvent.EVENT_DESELECTED)
				changeToKons(null);
		} else if (obj instanceof Patient)
			changeToKons(null);
	}
	
	private final ElexisEvent eetmpl =
		new ElexisEvent(null, null, ElexisEvent.EVENT_SELECTED
			| ElexisEvent.EVENT_DESELECTED);
	

	@Override
	public ElexisEvent getElexisEventFilter() {
		return eetmpl;
	}
}
