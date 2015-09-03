package org.iti.sqlschemacomparerplugin.utils;

import java.util.Map.Entry;
import java.util.Observable;
import java.util.Observer;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.iti.sqlSchemaComparison.SchemaModification;
import org.iti.sqlSchemaComparison.SqlSchemaComparisonResult;
import org.iti.sqlSchemaComparison.vertex.ISqlElement;

public class SchemaChangeListener implements Listener, Observer {

	private SqlSchemaManager schemaManager = null;
	private Tree schemaChangesList = null;

	public SchemaChangeListener(Tree schemaChangesList) {
		this.schemaChangesList = schemaChangesList;
		schemaManager = sqlschemacomparerplugin.Activator.getDefault().getSchemaManager();

		schemaManager.addObserver(this);
	}

	@Override
	public void handleEvent(Event event) {
		TreeItem item = (TreeItem) event.item;

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
		TreeItem item = (TreeItem) event.item;
		TreeItem parent = item.getParentItem();
		SqlSchemaComparisonResult result = (SqlSchemaComparisonResult) parent.getData();

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
		TreeItem item = (TreeItem) event.item;
		SqlSchemaComparisonResult result = schemaManager.getModifications().get(modificationIndex);

		item.setText(String.format("%s. Modification", modificationIndex + 1));
		item.setData(result);
		item.setItemCount(result.getModifications().size());
	}

	@Override
	public void update(Observable o, Object arg) {
		Display.getDefault().syncExec(new Runnable() {
			@Override
			public void run() {
				schemaChangesList.clearAll(true);
				schemaChangesList.setItemCount(getModificationsCount());
				schemaChangesList.update();
			}
		});
	}

	private int getModificationsCount() {
		return schemaManager.getModifications().size();
	}
}
