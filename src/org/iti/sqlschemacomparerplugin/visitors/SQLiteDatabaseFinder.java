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

public class SQLiteDatabaseFinder implements IResourceVisitor,
		IResourceDeltaVisitor {

	public boolean sqliteDatabaseFound() {
		return sqliteDatabase != null;
	}
	
	public IFile sqliteDatabase = null;
	
	@Override
	public boolean visit(IResourceDelta delta) throws CoreException {
		IResource resource = delta.getResource();
		switch (delta.getKind()) {
		case IResourceDelta.ADDED:
			extractSqliteDatabaseFile(resource);
			break;
		case IResourceDelta.CHANGED:
			extractSqliteDatabaseFile(resource);
			break;
		}

		return !sqliteDatabaseFound();
	}

	@Override
	public boolean visit(IResource resource) throws CoreException {
		extractSqliteDatabaseFile(resource);
		
		return !sqliteDatabaseFound();
	}

	private void extractSqliteDatabaseFile(IResource resource) {
		if (resource instanceof IFile && resource.getName().endsWith(".sqlite")) {
			sqliteDatabase = (IFile)resource;
		}
	}
}
