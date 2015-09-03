package org.iti.sqlschemacomparerplugin.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Observable;

import org.eclipse.core.runtime.CoreException;
import org.iti.sqlSchemaComparison.SqlSchemaComparer;
import org.iti.sqlSchemaComparison.SqlSchemaComparisonResult;
import org.iti.sqlSchemaComparison.frontends.ISqlSchemaFrontend;
import org.iti.sqlSchemaComparison.frontends.database.H2SchemaFrontend;
import org.iti.sqlSchemaComparison.frontends.database.SqliteSchemaFrontend;
import org.iti.sqlschemacomparerplugin.utils.DatabaseManager.DatabaseFile;
import org.iti.structureGraph.comparison.StructureGraphComparisonException;
import org.iti.structureGraph.nodes.IStructureElement;
import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.DefaultEdge;

public class SqlSchemaManager extends Observable {
	long modificationStamp;

	DirectedGraph<IStructureElement, DefaultEdge> currentSchema = null;

	List<SqlSchemaComparisonResult> modifications = new ArrayList<>();

	public DirectedGraph<IStructureElement, DefaultEdge> getCurrentSchema() {
		return currentSchema;
	}

	public List<SqlSchemaComparisonResult> getModifications() {
		return modifications;
	}

	public void save() {

	}

	private SqlSchemaComparisonResult result = null;

	public boolean updateSchema(DatabaseFile databaseFile) throws CoreException, StructureGraphComparisonException {
		DirectedGraph<IStructureElement, DefaultEdge> schema = generateSqlDatabaseSchema(databaseFile);

		if (currentSchema == null || hasSchemaChanged(schema)) {
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

	private boolean hasSchemaChanged(DirectedGraph<IStructureElement, DefaultEdge> schema)
			throws StructureGraphComparisonException {
		SqlSchemaComparer comparer = new SqlSchemaComparer(currentSchema, schema);

		result = comparer.comparisonResult;

		return !comparer.isIsomorphic();
	}

	private DirectedGraph<IStructureElement, DefaultEdge> generateSqlDatabaseSchema(DatabaseFile databaseFile) {
		String databasePath = databaseFile.databaseFile.getAbsolutePath();
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

}
