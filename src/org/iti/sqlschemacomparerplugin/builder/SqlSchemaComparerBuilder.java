/*******************************************************************************
 * Copyright (c) 10.09.2013 Hagen Schink.
 * 
 * Disclaimer: The following copyright notice does not include the method
 * checkSqlStatement(IFile, Entry<String, Integer>) because it access code
 * licensed under the terms of the GPLv3. The aforementioned method is made
 * available under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Hagen Schink - initial API and implementation and/or initial documentation
 *******************************************************************************/

package org.iti.sqlschemacomparerplugin.builder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.dom.ASTNode;
import org.iti.sqlSchemaComparison.SqlStatementExpectationValidationResult;
import org.iti.sqlSchemaComparison.SqlStatementExpectationValidator;
import org.iti.sqlSchemaComparison.frontends.ISqlSchemaFrontend;
import org.iti.sqlSchemaComparison.frontends.SqlStatementFrontend;
import org.iti.sqlSchemaComparison.frontends.database.H2SchemaFrontend;
import org.iti.sqlSchemaComparison.frontends.database.SqliteSchemaFrontend;
import org.iti.sqlSchemaComparison.frontends.technologies.IJPASchemaFrontend;
import org.iti.sqlSchemaComparison.vertex.ISqlElement;
import org.iti.sqlSchemaComparison.vertex.SqlTableVertex;
import org.iti.sqlschemacomparerplugin.utils.EclipseJPASchemaFrontend;
import org.iti.sqlschemacomparerplugin.utils.ParseUtils;
import org.iti.sqlschemacomparerplugin.utils.databaseformatter.IDatabaseIdentifierFormatter;
import org.iti.sqlschemacomparerplugin.utils.databaseformatter.NullFormatter;
import org.iti.sqlschemacomparerplugin.utils.databaseformatter.UpperCaseFormatter;
import org.iti.sqlschemacomparerplugin.visitors.FileByEndingFinder;
import org.iti.structureGraph.nodes.IStructureElement;
import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.DefaultEdge;

public class SqlSchemaComparerBuilder extends IncrementalProjectBuilder {

	class SampleDeltaVisitor implements IResourceDeltaVisitor {
		/*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.core.resources.IResourceDeltaVisitor#visit(org.eclipse.core.resources.IResourceDelta)
		 */
		public boolean visit(IResourceDelta delta) throws CoreException {
			IResource resource = delta.getResource();
			switch (delta.getKind()) {
			case IResourceDelta.ADDED:
				// handle added resource
				checkSqlStatements(resource);
				checkEntityDefinition(resource);
				break;
			case IResourceDelta.REMOVED:
				// handle removed resource
				break;
			case IResourceDelta.CHANGED:
				// handle changed resource
				checkSqlStatements(resource);
				checkEntityDefinition(resource);
				break;
			}
			//return true to continue visiting children.
			return true;
		}
	}

	class SampleResourceVisitor implements IResourceVisitor {
		public boolean visit(IResource resource) {
			checkSqlStatements(resource);
			checkEntityDefinition(resource);
			//return true to continue visiting children.
			return true;
		}
	}

	private enum DatabaseType {
		SQLITE, H2
	}

	private class DatabaseFile {
		public IFile databaseFile;
		public DatabaseType databaseType;

		public DatabaseFile(IFile databaseFile, DatabaseType databaseType) {
			this.databaseFile = databaseFile;
			this.databaseType = databaseType;
		}
	}

	public static final String BUILDER_ID = "org.iti.sqlSchemaComparerPlugin.sqlSchemaComparerBuilder";

	private static final String SQL_STATEMENT_MARKER_TYPE = "org.iti.sqlSchemaComparerPlugin.sqlStatementProblem";
	
	private static final String JPA_ENTITY_MARKER_TYPE = "org.iti.sqlSchemaComparerPlugin.jpaEntityDefinitionProblem";

	private void addMarker(String markerType, IFile file, String message,
			int lineNumber, int severity) {
		try {
			IMarker marker = file.createMarker(markerType);
			marker.setAttribute(IMarker.MESSAGE, message);
			marker.setAttribute(IMarker.SEVERITY, severity);
			if (lineNumber == -1) {
				lineNumber = 1;
			}
			marker.setAttribute(IMarker.LINE_NUMBER, lineNumber);
		} catch (CoreException e) {
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.internal.events.InternalBuilder#build(int,
	 *      java.util.Map, org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected IProject[] build(int kind, @SuppressWarnings("rawtypes") Map args, IProgressMonitor monitor)
			throws CoreException {
		if (kind == FULL_BUILD) {
			fullBuild(monitor);
		} else {
			IResourceDelta delta = getDelta(getProject());
			if (delta == null) {
				fullBuild(monitor);
			} else {
				incrementalBuild(delta, monitor);
			}
		}
		return null;
	}
	
	void checkSqlStatements(IResource resource) {
		if (resource instanceof IFile && resource.getName().endsWith(".java")) {
			IFile file = (IFile) resource;
			
			if (statementValidator != null) {
				Map<String, Integer> javaStringsAndLineNumbers = ParseUtils.getAllJavaStrings(file);
	
				deleteMarkers(SQL_STATEMENT_MARKER_TYPE, file);
				
				for (Entry<String, Integer> entry : javaStringsAndLineNumbers.entrySet()) {
					checkSqlStatement(file, entry);
				}
			}
		}
	}

	private void checkEntityDefinition(IResource resource) {
		if (resource instanceof IFile && resource.getName().endsWith(".java")) {
			IFile file = (IFile) resource;
			
			if (statementValidator != null) {
				IJPASchemaFrontend frontend = new EclipseJPASchemaFrontend(file, getFormatter());
				DirectedGraph<IStructureElement, DefaultEdge> statementSchema = frontend.createSqlSchema();
				
				if (statementSchema != null) {
					SqlStatementExpectationValidationResult result = statementValidator.computeGraphMatching(statementSchema);

					deleteMarkers(JPA_ENTITY_MARKER_TYPE, file);
					
					if (resultsAvaiable(result)) {
						for (ISqlElement table : result.getMissingTables()) {
							int lineNumber = ParseUtils.getLineNumber(file, ((ASTNode)table.getSourceElement()).getStartPosition());
							addMarker(JPA_ENTITY_MARKER_TYPE, file, "Missing Table: " + table.getSqlElementId(), lineNumber, IMarker.SEVERITY_ERROR);
						}
						
						for (ISqlElement column : result.getMissingColumns()) {
							int lineNumber = ParseUtils.getLineNumber(file, ((ASTNode)column.getSourceElement()).getStartPosition());
							addMarker(JPA_ENTITY_MARKER_TYPE, file, "Missing Column: " + column.getSqlElementId(), lineNumber, IMarker.SEVERITY_ERROR);
						}
					}
				}
			}
		}
	}

	private IDatabaseIdentifierFormatter getFormatter() {
		switch(databaseFile.databaseType) {
		case H2:
			return new UpperCaseFormatter();
		default:
			return new NullFormatter();
		}
	}

	private boolean resultsAvaiable(
			SqlStatementExpectationValidationResult result) {
		return !result.isStatementValid();
	}

	private void deleteMarkers(String markerType, IFile file) {
		try {
			file.deleteMarkers(markerType, false, IResource.DEPTH_ZERO);
		} catch (CoreException ce) {
		}
	}

	private void checkSqlStatement(IFile file, Entry<String, Integer> entry) {
		ISqlSchemaFrontend frontend = new SqlStatementFrontend(entry.getKey(), null);
		
		try {
			DirectedGraph<IStructureElement, DefaultEdge> statementSchema = frontend.createSqlSchema();
		
			if (statementSchema != null) {
				SqlStatementExpectationValidationResult result = statementValidator.computeGraphMatching(statementSchema);
				
				if (resultsAvaiable(result)) {
					String message = getErrorMessage(result.getMissingTables(), "Missing Tables");
					
					if (message != "" && result.getMissingColumns().size() > 0) {
						message += " ";
					}
					
					message += getErrorMessage(result.getMissingColumns(), "Missing Columns");
					
					message += getErrorMessage(result.getMissingButReachableColumns(), "Missing but reachable Columns");
	
					addMarker(SQL_STATEMENT_MARKER_TYPE, file, message, entry.getValue(), IMarker.SEVERITY_ERROR);
				}
			}
		} catch (Exception ex) {
			addMarker(SQL_STATEMENT_MARKER_TYPE, file, ex.getMessage(), entry.getValue(), IMarker.SEVERITY_ERROR);
		}
	}

	private String getErrorMessage(List<ISqlElement> list, String title) {
		if (list.size() > 0) {
			StringBuilder message = new StringBuilder();
			message.append(title + ": [");
			
			for (ISqlElement element : list) {
				message.append(element.getSqlElementId() + ", ");
			}
			
			message.setLength(message.length() - 2);
			
			return message.toString() + "]";
		}

		return "";
	}

	private String getErrorMessage(Map<ISqlElement, List<List<ISqlElement>>> list, String title) {
		if (list.size() > 0) {
			StringBuilder message = new StringBuilder();

			message.append(title + ": [");

			for (ISqlElement element : list.keySet()) {
				message.append(element.getSqlElementId());

				setReachablePathString(message, list.get(element).get(0));

				message.append(", ");
			}

			message.setLength(message.length() - 2);

			return message.toString() + "]";
		}
		
		return "";
	}

	private void setReachablePathString(StringBuilder message, List<ISqlElement> list) {
		List<ISqlElement> tables = new ArrayList<>();

		message.append(" (");

		for (ISqlElement element : list) {
			if (element instanceof SqlTableVertex) {
				tables.add(element);
			}
		}

		for (int x = 0; x < tables.size(); x++) {
			ISqlElement element = tables.get(x);

			message.append(element.getSqlElementId().toString());

			if (x < tables.size() - 1) {
				message.append(" > ");
			}
		}

		message.append(")");
	}

	private SqlStatementExpectationValidator statementValidator;
	private long modificationStamp;
	private DatabaseFile databaseFile;

	protected void fullBuild(final IProgressMonitor monitor)
			throws CoreException {
		try {
			databaseFile = findDatabaseFile();
			
			statementValidator = null;
			
			initializeSqlSchemaComparison(databaseFile);
			
			checkDatabaseAccess();
		} catch (CoreException e) {
		}
	}

	protected void incrementalBuild(IResourceDelta delta,
			IProgressMonitor monitor) throws CoreException {
		DatabaseFile databaseFile = findDatabaseFile();
		
		if (statementValidator == null || databaseChanged(databaseFile)) {
			initializeSqlSchemaComparison(databaseFile);
		}
		
		checkDatabaseAccess(delta);
	}

	private DatabaseFile findDatabaseFile() throws CoreException {
		DatabaseFile databaseFile = findSqliteDatabaseFile();

		if (databaseFile == null) {
			databaseFile = findH2DatabaseFile();
		}

		return databaseFile;
	}

	private boolean databaseChanged(DatabaseFile databaseFile) throws CoreException {
		databaseFile.databaseFile.refreshLocal(IResource.DEPTH_ZERO, null);
		
		return databaseFile.databaseFile.getModificationStamp() != modificationStamp;
	}

	private DatabaseFile findSqliteDatabaseFile() throws CoreException {
		return findDatabaseFile(DatabaseType.SQLITE);
	}

	private DatabaseFile findH2DatabaseFile() throws CoreException {
		return findDatabaseFile(DatabaseType.H2);
	}

	private DatabaseFile findDatabaseFile(DatabaseType databaseType) throws CoreException {
		DatabaseFile database = null;
		FileByEndingFinder visitor = new FileByEndingFinder(getFileEnding(databaseType));
		
		getProject().accept(visitor);

		if (visitor.fileFound()) {
			database = new DatabaseFile(visitor.file, databaseType);
		}
		
		return database;
	}

	private String getFileEnding(DatabaseType databaseType) {
		switch (databaseType) {
		case SQLITE:
			return "sqlite";
		case H2:
			return "db";
		default:
			throw new IllegalArgumentException(databaseType.toString()
					+ "is not supported!");
		}
	}

	private void initializeSqlSchemaComparison(DatabaseFile databaseFile) {
		DirectedGraph<IStructureElement, DefaultEdge> schema = null;
		
		if (databaseFile != null) {
			schema = generateSqlDatabaseSchema(databaseFile);
			modificationStamp = databaseFile.databaseFile.getModificationStamp();
			
			statementValidator = new SqlStatementExpectationValidator(schema);
		}
	}

	private DirectedGraph<IStructureElement, DefaultEdge> generateSqlDatabaseSchema(DatabaseFile databaseFile) {
		String databasePath = databaseFile.databaseFile.getLocation().toString();
		ISqlSchemaFrontend frontend = null;

		switch (databaseFile.databaseType) {
		case SQLITE:
			frontend = new SqliteSchemaFrontend(databasePath);
			break;
		case H2:
			frontend = new H2SchemaFrontend(databasePath.replaceAll("\\.mv\\.db$", ""));
			break;
		}
		
		return frontend.createSqlSchema();
	}

	private void checkDatabaseAccess() throws CoreException {
		getProject().accept(new SampleResourceVisitor());
	}

	private void checkDatabaseAccess(IResourceDelta delta) throws CoreException {
		delta.accept(new SampleDeltaVisitor());
	}
}
