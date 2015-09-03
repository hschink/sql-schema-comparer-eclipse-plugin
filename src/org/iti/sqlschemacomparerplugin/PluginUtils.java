package org.iti.sqlschemacomparerplugin;

import java.util.Iterator;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.iti.sqlschemacomparerplugin.builder.SqlSchemaComparerNature;

public class PluginUtils {

	public static int getNatureId(IProject project) {
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
	
	public static boolean isNatureActive(IProject project) {
		return getNatureId(project) >= 0;
	}


	public static IProject getSelectedProject() {
		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		ISelection currentSelection = window.getSelectionService().getSelection(JavaUI.ID_PACKAGES);

		return getSelectedProject(currentSelection);
	}

	public static IProject getSelectedProject(ISelection currentSelection) {
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

}
