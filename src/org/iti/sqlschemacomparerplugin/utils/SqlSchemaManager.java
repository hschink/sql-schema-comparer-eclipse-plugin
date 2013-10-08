package org.iti.sqlschemacomparerplugin.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Observable;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.iti.sqlSchemaComparison.SqlSchemaComparer;
import org.iti.sqlSchemaComparison.SqlSchemaComparisonResult;
import org.iti.sqlSchemaComparison.frontends.ISqlSchemaFrontend;
import org.iti.sqlSchemaComparison.frontends.database.SqliteSchemaFrontend;
import org.iti.sqlSchemaComparison.vertex.ISqlElement;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;

public class SqlSchemaManager extends Observable {

	long modificationStamp;

	Graph<ISqlElement, DefaultEdge> currentSchema = null;

	List<SqlSchemaComparisonResult> modifications = new ArrayList<>();

	public Graph<ISqlElement, DefaultEdge> getCurrentSchema() {
		return currentSchema;
	}

	public List<SqlSchemaComparisonResult> getModifications() {
		return modifications;
	}
	
	public void save() {

	}

	private SqlSchemaComparisonResult result = null;

	public boolean updateSchema(IFile file) throws CoreException {
		ISqlSchemaFrontend frontend = new SqliteSchemaFrontend(file.getLocation().toString());
		Graph<ISqlElement, DefaultEdge> schema = frontend.createSqlSchema();

		if (currentSchema == null || hasDatabaseChanged(file, schema)) {
			if (result != null) {
				modifications.add(result);
				setChanged();

				result = null;
			}

			currentSchema = schema;

			notifyObservers();

			return true;
		}

		return false;
	}

	private boolean hasDatabaseChanged(IFile file,
			Graph<ISqlElement, DefaultEdge> schema) {

		return databaseChanged(file) && hasSchemaChanged(schema);
	}

	private boolean databaseChanged(IFile file) {
		try {
			file.refreshLocal(IResource.DEPTH_ZERO, null);
		} catch (CoreException e) {
			e.printStackTrace();
		}
		
		return file.getModificationStamp() != modificationStamp;
	}

	private boolean hasSchemaChanged(Graph<ISqlElement, DefaultEdge> schema) {
		SqlSchemaComparer comparer = new SqlSchemaComparer(currentSchema, schema);
		
		result = comparer.comparisonResult;

		return !comparer.isIsomorphic();
	}
}
