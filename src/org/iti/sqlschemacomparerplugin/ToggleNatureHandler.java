/*******************************************************************************
 * Copyright (c) 10.09.2013 Hagen Schink.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Hagen Schink - initial API and implementation and/or initial documentation
 *******************************************************************************/

package org.iti.sqlschemacomparerplugin;

import java.util.Map;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.menus.UIElement;
import org.iti.sqlschemacomparerplugin.builder.SqlSchemaComparerNature;

public class ToggleNatureHandler extends AbstractHandler implements IElementUpdater {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection currentSelection = HandlerUtil.getCurrentSelection(event);
		IProject selectedProject = PluginUtils.getSelectedProject(currentSelection);
		
		toggleNature(selectedProject);
		
		return null;
	}

	private void toggleNature(IProject project) {
		try {
			IProjectDescription description = project.getDescription();
			String[] natures = description.getNatureIds();
			String[] newNatures = null;
			int natureId = PluginUtils.getNatureId(project);
			
			if (natureId >= 0) {
				// Remove the nature
				newNatures = new String[natures.length - 1];
				System.arraycopy(natures, 0, newNatures, 0, natureId);
				System.arraycopy(natures, natureId + 1, newNatures, natureId,
						natures.length - natureId - 1);
				description.setNatureIds(newNatures);
				project.setDescription(description, null);
			} else {
				// Add the nature
				newNatures = new String[natures.length + 1];
				System.arraycopy(natures, 0, newNatures, 0, natures.length);
				newNatures[natures.length] = SqlSchemaComparerNature.NATURE_ID;
				description.setNatureIds(newNatures);
				project.setDescription(description, null);
			}
		} catch (CoreException e) {
			System.err.println(e.getStackTrace());
		}
	}
	
	@Override
	public void updateElement(UIElement element, @SuppressWarnings("rawtypes") Map parameters) {
		element.setChecked(PluginUtils.isNatureActive(PluginUtils.getSelectedProject()));
	}
}
