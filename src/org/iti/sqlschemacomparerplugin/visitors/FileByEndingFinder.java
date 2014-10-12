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

package org.iti.sqlschemacomparerplugin.visitors;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;

public class FileByEndingFinder implements IResourceVisitor,
		IResourceDeltaVisitor {

	private String fileEnding = "";

	public boolean fileFound() {
		return file != null;
	}
	
	public IFile file = null;

	public FileByEndingFinder(String fileEnding) {
		this.fileEnding = fileEnding;
	}
	
	@Override
	public boolean visit(IResourceDelta delta) throws CoreException {
		IResource resource = delta.getResource();
		switch (delta.getKind()) {
		case IResourceDelta.ADDED:
			extractFile(resource);
			break;
		case IResourceDelta.CHANGED:
			extractFile(resource);
			break;
		}

		return !fileFound();
	}

	@Override
	public boolean visit(IResource resource) throws CoreException {
		extractFile(resource);
		
		return !fileFound();
	}

	private void extractFile(IResource resource) {
		if (resource instanceof IFile && resource.getName().endsWith("." + fileEnding)) {
			file = (IFile)resource;
		}
	}
}
