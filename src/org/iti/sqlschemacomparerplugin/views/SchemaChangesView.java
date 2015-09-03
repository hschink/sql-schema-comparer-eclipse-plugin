package org.iti.sqlschemacomparerplugin.views;

import javax.annotation.PostConstruct;

import org.eclipse.e4.ui.di.Focus;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;
import org.iti.sqlschemacomparerplugin.utils.SchemaChangeListener;

public class SchemaChangesView {

	private static Tree schemaChangesList;

	@PostConstruct
	public void createPartControl(Composite parent) {
		schemaChangesList = new Tree(parent, SWT.VIRTUAL | SWT.BORDER);

		schemaChangesList.addListener(SWT.SetData, new SchemaChangeListener(schemaChangesList));
		schemaChangesList.setItemCount(1);
	}

	@Focus
	public void setFocus() {
		schemaChangesList.setFocus();
	}
}
