package com.hilotec.elexis.kgview;

import org.eclipse.swt.SWT;
import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

public class KGViewPerspektive implements IPerspectiveFactory {
	public static final String ID = "com.hilotec.elexis.kgview.KGViewPerspektive";
	
	public void createInitialLayout(IPageLayout layout) {
		String editorArea = layout.getEditorArea();
		layout.setEditorAreaVisible(false);
		layout.setFixed(false);
		
		IFolderLayout ifr = layout.createFolder("right", SWT.RIGHT, 1.0f, editorArea);
		ifr.addView(FamAnamneseView.ID);
		ifr.addView(PersAnamneseView.ID);
		ifr.addView(SozAnamneseView.ID);
		ifr.addView(SysAnamneseView.ID);
		ifr.addView(RisikoFView.ID);
		ifr.addView(AllgemeinStView.ID);
		ifr.addView(LokalStView.ID);
		ifr.addView(ProzedereView.ID);
		ifr.addView(DiagnoseView.ID);
		ifr.addView(JetzLeidenView.ID);
	}
}
