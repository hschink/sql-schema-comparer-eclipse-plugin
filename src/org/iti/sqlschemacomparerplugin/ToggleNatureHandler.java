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

import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.menus.UIElement;
import org.iti.sqlschemacomparerplugin.builder.SqlSchemaComparerNature;

public class ToggleNatureHandler extends AbstractHandler implements IElementUpdater {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection currentSelection = HandlerUtil.getCurrentSelection(event);
		IProject selectedProject = getSelectedProject(currentSelection);
		
		toggleNature(selectedProject);
		
		return null;
	}

	private IProject getSelectedProject(ISelection currentSelection) {
		IProject project = null;
		
		if (currentSelection instanceof IStructuredSelection) {
			for (Iterator<?> it = ((IStructuredSelection) currentSelection).iterator(); it
					.hasNext();) {
				Object element = it.next();
				
				if (element instanceof IProject) {
					project = (IProject) element;
				} else if (element instanceof IAdaptable) {
					project = (IProject) ((IAdaptable) element)
							.getAdapter(IProject.class);
				}
				
				if (project != null) {
					break;
				}
			}
		}
		
		return project;
	}

	private void toggleNature(IProject project) {
		try {
			IProjectDescription description = project.getDescription();
			String[] natures = description.getNatureIds();
			String[] newNatures = null;
			int natureId = getNatureId(project);
			
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
		}
	}
	
	private int getNatureId(IProject project) {
		try {
			IProjectDescription description = project.getDescription();
			String[] natures = description.getNatureIds();
			
			for (int i = 0; i < natures.length; ++i) {
				if (SqlSchemaComparerNature.NATURE_ID.equals(natures[i])) {
					return i;
				}
			}
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return -1;
	}
	
	private boolean isNatureActive(IProject project) {
		return getNatureId(project) >= 0;
	}

	@Override
	public void updateElement(UIElement element, @SuppressWarnings("rawtypes") Map parameters) {
		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		ISelection currentSelection = window.getSelectionService().getSelection("org.eclipse.jdt.ui.PackageExplorer");
		IProject selectedProject = getSelectedProject(currentSelection);
		
		element.setChecked(isNatureActive(selectedProject));
	}
}
