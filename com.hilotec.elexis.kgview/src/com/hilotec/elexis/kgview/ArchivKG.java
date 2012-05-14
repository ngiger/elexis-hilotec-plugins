package com.hilotec.elexis.kgview;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.events.IHyperlinkListener;
import org.eclipse.ui.forms.widgets.ScrolledFormText;
import org.eclipse.ui.part.ViewPart;

import com.hilotec.elexis.kgview.data.KonsData;

import ch.elexis.Desk;
import ch.elexis.actions.ElexisEvent;
import ch.elexis.actions.ElexisEventDispatcher;
import ch.elexis.actions.ElexisEventListener;
import ch.elexis.actions.Messages;
import ch.elexis.data.Fall;
import ch.elexis.data.Konsultation;
import ch.elexis.data.Patient;
import ch.elexis.util.ViewMenus;

public class ArchivKG extends ViewPart implements ElexisEventListener {
	public static final String ID = "com.hilotec.elexis.kgview.ArchivKG";
	
	ScrolledFormText text;
	private Action actNeueKons;
	private Action actKonsAendern;
	
	@Override
	public void createPartControl(Composite parent) {
		parent.setLayout(new FillLayout());

		text = new ScrolledFormText(parent, true);
		text.getFormText().addHyperlinkListener(new IHyperlinkListener() {
			public void linkExited(HyperlinkEvent e) {}
			public void linkEntered(HyperlinkEvent e) {}
			public void linkActivated(HyperlinkEvent e) {
				if (!(e.getHref() instanceof String)) return;
				String href = (String) e.getHref();
				if (href.startsWith("kons:")) {
					Konsultation kons = Konsultation.load(href.substring(5));
					ElexisEventDispatcher.fireSelectionEvent(kons);
				}
			}
		});
		
		// TODO: Fonts fuer text laden
		//text.setFont("konstitel", Desk.getFont(cfgName));
		
		createActions();
		ViewMenus menus = new ViewMenus(getViewSite());
		menus.createToolbar(actNeueKons, actKonsAendern);
		
		// Aktuell ausgewaehlten Patienten laden
		Patient pat = (Patient) ElexisEventDispatcher.getSelected(Patient.class);
		loadPatient(pat);
		
		ElexisEventDispatcher.getInstance().addListeners(this);
	}

	/**
	 * ArchivKG zum angegebenen Patienten laden.
	 */
	private void loadPatient(Patient pat) {
		if (pat == null) {
			text.setText("Kein Patient ausgewählt!");
			return;
		}
		
		StringBuilder sb = new StringBuilder();
		sb.append("<form>");
		
		Fall[] faelle = pat.getFaelle();
		for (Fall f: faelle) {
			Konsultation[] konsultationen = f.getBehandlungen(true);
			for (Konsultation k: konsultationen) {
				processKonsultation(k, sb);
			}
		}
		
		sb.append("</form>");
		//browser.setText(sb.toString());
		text.setText(sb.toString());
	}
	
	/**
	 * sb um die die Konsultation k erweitern
	 */
	private void processKonsultation(Konsultation k, StringBuilder sb) {
		KonsData kd = KonsData.load(k);
		
		sb.append("<p>");
		sb.append("<b>Konsultation</b> ");
		sb.append("<a href=\"kons:" + k.getId() + "\">");
		sb.append(k.getDatum() + " " + kd.getKonsBeginn() + "</a>");
		sb.append("<br/>");
		
		addParagraph("Jetziges Leiden", kd.getJetzigesLeiden(),
			kd.getJetzigesLeidenICPC(), sb);
		addParagraph("Lokalstatus", kd.getLokalstatus(), sb);
		addParagraph("Röntgen", kd.getRoentgen(), sb);
		addParagraph("EKG", kd.getEKG(), sb);
		addParagraph("Diagnose", kd.getDiagnose(),
			kd.getDiagnoseICPC(), sb);
		addParagraph("Therapie", kd.getTherapie(), sb);
		addParagraph("Verlauf", kd.getVerlauf(), sb);
		addParagraph("Procedere", kd.getProzedere(),
			kd.getProzedereICPC(), sb);
		sb.append("</p>");
	}
	
	private void addParagraph(String titel, String text, StringBuilder sb) {
		addParagraph(titel, text, null, sb);
	}
	
	private void addParagraph(String titel, String text, String icpc,
			StringBuilder sb)
	{
		if ((text == null || text.isEmpty()) &&
				(icpc == null || icpc.isEmpty()))
			return;

		sb.append("<b>" + titel + "</b><br/>");
		if (icpc != null && !icpc.isEmpty())
			sb.append("ICPC: " + icpc.replace(",", ", ") + "<br/>");
		sb.append(cleanUp(text));
		sb.append("<br/><br/>");
	}
	
	private String cleanUp(String text) {
		return text.replace(">", "&gt;").
			replace("<", "&lt;").
			replace("\n", "<br/>");
	}



	public void catchElexisEvent(ElexisEvent ev) {
		Patient p = (Patient) ev.getObject();
		if (ev.getType() == ElexisEvent.EVENT_SELECTED) {
			loadPatient(p);
		} else if (ev.getType() == ElexisEvent.EVENT_DESELECTED) {
			loadPatient(null);
		}
	}
	
	private final ElexisEvent eetmpl =
		new ElexisEvent(null, Patient.class, ElexisEvent.EVENT_SELECTED
			| ElexisEvent.EVENT_DESELECTED);

	public ElexisEvent getElexisEventFilter() {
		return eetmpl;
	}

	public void setFocus() {}
	
	private void createActions() {
		actNeueKons = new Action(Messages.getString("GlobalActions.NewKons")) { //$NON-NLS-1$
			{
				setImageDescriptor(Desk.getImageDescriptor(Desk.IMG_NEW));
				setToolTipText(Messages.getString("GlobalActions.NewKonsToolTip")); //$NON-NLS-1$
			}
			
			@Override
			public void run(){
				Fall fall = (Fall)
					ElexisEventDispatcher.getSelected(Fall.class);
				if (fall == null || !fall.isOpen()) {
					MessageDialog.openError(null, "Kein offener Fall ausgewählt",
						"Um eine neue Konsultation erstellen zu können, muss "+
						"ein offener Fall ausgewählt werden");
					return;
				}
				new NeueKonsDialog(getSite().getShell(), fall).open(); 
			}
		};
		actKonsAendern = new Action("Konsultations Datum/Zeit ändern") {
			{
				setImageDescriptor(Desk.getImageDescriptor(Desk.IMG_EDIT));
				setToolTipText("Konsultations Datum/Zeit ändern");
			}
			
			@Override
			public void run(){
				Konsultation kons = (Konsultation)
					ElexisEventDispatcher.getSelected(Konsultation.class);
				if (kons == null || !kons.isEditable(false)) {
					MessageDialog.openError(null, "Keine/Ungültige " +
							"Konsultation ausgewählt",
							"Es muss eine veränderbare Konsultation " +
							"ausgewählt sein.");
						return;
				}
				new NeueKonsDialog(getSite().getShell(), kons).open();
			}
		};
	}
}
