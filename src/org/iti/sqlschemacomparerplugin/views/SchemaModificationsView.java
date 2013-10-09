/*******************************************************************************
 * Copyright (c) 10.09.2013 Hagen Schink.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Hagen Schink - initial API and implementation and/or initial documentation
 *******************************************************************************/

package org.iti.sqlschemacomparerplugin.views;

import java.util.Map.Entry;
import java.util.Observable;
import java.util.Observer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.part.ViewPart;
import org.iti.sqlSchemaComparison.SchemaModification;
import org.iti.sqlSchemaComparison.SqlSchemaComparisonResult;
import org.iti.sqlSchemaComparison.vertex.ISqlElement;
import org.iti.sqlschemacomparerplugin.utils.SqlSchemaManager;

public class SchemaModificationsView extends ViewPart {

	private static Tree schemaModificationsList;
	
	private static class SchemaChangeDataListener implements Listener, Observer {

		private SqlSchemaManager schemaManager = null;
		
		public SchemaChangeDataListener() {
			schemaManager = sqlschemacomparerplugin.Activator.getDefault().getSchemaManager();
			
			schemaManager.addObserver(this);
		}
		
		@Override
		public void handleEvent(Event event) {
			TreeItem item = (TreeItem)event.item;
            
            if (getModificationsCount() == 0) {
            	item.setText("No schema changes available...");
            } else {
                if (item.getParentItem() == null) {
                	setModificationRoot(event);
                } else {
                	setModificationEntry(event);
                }
            }
		}

		private void setModificationEntry(Event event) {
			int index = event.index;
			TreeItem item = (TreeItem)event.item;
			TreeItem parent = item.getParentItem();
			SqlSchemaComparisonResult result = (SqlSchemaComparisonResult)parent.getData();

			for (Entry<ISqlElement, SchemaModification> entry : result.getModifications().entrySet()) {
				if (index == 0) {
					item.setText(String.format("%s %s", entry.getValue(), entry.getKey()));
					break;
				}

				index--;
			}
 		}

		private void setModificationRoot(Event event) {
			int modificationIndex = getModificationsCount() - (event.index + 1);
			TreeItem item = (TreeItem)event.item;
			SqlSchemaComparisonResult result =  schemaManager.getModifications().get(modificationIndex);
			
			item.setText(String.format("%s. Modification", modificationIndex + 1));
			item.setData(result);
			item.setItemCount(result.getModifications().size());
		}

		@Override
		public void update(Observable o, Object arg) {
			Display.getDefault().syncExec(new Runnable() {
				@Override
				public void run() {
					schemaModificationsList.clearAll(true);
					
					schemaModificationsList.setItemCount(getModificationsCount());
					
					schemaModificationsList.update();
				}
			});
		}

		private int getModificationsCount() {
			return schemaManager.getModifications().size();
		}
	}
	
	@Override
	public void createPartControl(Composite parent) {
		schemaModificationsList = new Tree(parent, SWT.VIRTUAL | SWT.BORDER);
		
		schemaModificationsList.addListener(SWT.SetData, new SchemaChangeDataListener());

		schemaModificationsList.setItemCount(1);
	}

	@Override
	public void setFocus() {
		schemaModificationsList.setFocus();
	}
}
