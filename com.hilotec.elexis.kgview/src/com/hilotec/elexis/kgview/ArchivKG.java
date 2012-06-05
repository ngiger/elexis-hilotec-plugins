package com.hilotec.elexis.kgview;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

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
import ch.elexis.Hub;
import ch.elexis.actions.ElexisEvent;
import ch.elexis.actions.ElexisEventDispatcher;
import ch.elexis.actions.ElexisEventListener;
import ch.elexis.actions.Messages;
import ch.elexis.actions.Heartbeat.HeartListener;
import ch.elexis.data.Fall;
import ch.elexis.data.Konsultation;
import ch.elexis.data.Patient;
import ch.elexis.util.ViewMenus;
import ch.rgw.tools.TimeTool;

public class ArchivKG extends ViewPart implements ElexisEventListener,
		HeartListener
{
	public static final String ID = "com.hilotec.elexis.kgview.ArchivKG";
	
	ScrolledFormText text;
	private Action actNeueKons;
	private Action actKonsAendern;
	private Action actAutoAkt;
	private Action actSortierungUmk;
	private boolean sortRev;

	
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
		
		sortRev = false;
		
		// TODO: Fonts fuer text laden
		//text.setFont("konstitel", Desk.getFont(cfgName));
		
		createActions();
		ViewMenus menus = new ViewMenus(getViewSite());
		menus.createToolbar(actNeueKons, actKonsAendern, actAutoAkt,
				actSortierungUmk);
		
		// Aktuell ausgewaehlten Patienten laden
		Patient pat = (Patient) ElexisEventDispatcher.getSelected(Patient.class);
		loadPatient(pat);
		
		ElexisEventDispatcher.getInstance().addListeners(this);
	}

	/**
	 * @return Sortierte Liste aller Konsultation dieses Patienten
	 */
	private ArrayList<Konsultation> getKonsultationen(Patient pat,
			final boolean reversed)
	{
		// Konsultationen sammeln
		ArrayList<Konsultation> konsliste = new ArrayList<Konsultation>();
		for (Fall f: pat.getFaelle()) {
			for (Konsultation k: f.getBehandlungen(true)) {
				konsliste.add(k);
			}
		}
		
		// Konsultationen sortieren
		Comparator<Konsultation> comp = new Comparator<Konsultation>() {
			public int compare(Konsultation k0, Konsultation k1) {
				KonsData kd0 = KonsData.load(k0);
				TimeTool tt0 = new TimeTool(k0.getDatum());
				tt0.setTime(new TimeTool(kd0.getKonsBeginn()));
				
				KonsData kd1 = KonsData.load(k1);
				TimeTool tt1 = new TimeTool(k1.getDatum());
				tt1.setTime(new TimeTool(kd1.getKonsBeginn()));
				
				if (reversed) {
					return tt0.compareTo(tt1);
				} else {
					return tt1.compareTo(tt0);
				}
			}
		};
		Collections.sort(konsliste, comp);
		
		return konsliste;
	}
	
	/**
	 * ArchivKG zum angegebenen Patienten laden.
	 */
	private void loadPatient(Patient pat) {
		if (pat == null) {
			text.setText("Kein Patient ausgewählt!");
			return;
		}
		
		// Inhalt fuer Textfeld generieren
		StringBuilder sb = new StringBuilder();
		sb.append("<form>");
		for (Konsultation k: getKonsultationen(pat, sortRev)) {
			processKonsultation(k, sb);
		}
		sb.append("</form>");
		text.setText(sb.toString());
	}
	
	/**
	 * Neu laden
	 */
	private void refresh() {
		loadPatient(ElexisEventDispatcher.getSelectedPatient());
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
		sb.append(" " + k.getFall().getAbrechnungsSystem());
		sb.append(" (" + k.getAuthor() + ")");
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
		final ArchivKG akg = this;
		actAutoAkt = new Action("Automatisch aktualisieren", Action.AS_CHECK_BOX) {
			{
				setImageDescriptor(Desk.getImageDescriptor(Desk.IMG_REFRESH));
				setToolTipText("Automatisch aktualisieren");
			}
			
			@Override
			public void run() {
				boolean ch = isChecked();
				if (ch) Hub.heart.addListener(akg);
				else Hub.heart.removeListener(akg);
			}
		};
		
		actSortierungUmk = new Action("Sortierung umkehren", Action.AS_CHECK_BOX) {
			{
				setImageDescriptor(Desk.getImageDescriptor(Desk.IMG_ARROWUP));
				setToolTipText("Sortierung umkehren, älteste zuoberst");
			}
			
			@Override
			public void run() {
				sortRev = isChecked();
				refresh();
			}
		};
	}

	@Override
	public void dispose() {
		if (actAutoAkt.isChecked()) Hub.heart.removeListener(this);
		super.dispose();
	}
	
	@Override
	public void heartbeat() {
		refresh();
	}
}
