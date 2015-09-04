package org.iti.sqlschemacomparerplugin.utils;

import java.io.File;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

public class DatabaseManager {

	public enum DatabaseType {
		SQLITE, H2
	}

	public class DatabaseFile {
		public File databaseFile;
		public DatabaseType databaseType;

		public DatabaseFile(File databaseFile, DatabaseType databaseType) {
			this.databaseFile = databaseFile;
			this.databaseType = databaseType;
		}
	}

	private DatabaseFile databaseFile;
	private long modificationStamp;
	private boolean databaseChanged;

	public DatabaseManager() {

	}

	public void update(IProject project, IProgressMonitor monitor) throws CoreException {
		databaseFile = findDatabaseFile(project, monitor);

		if (databaseFile != null) {
			databaseChanged = databaseFile.databaseFile.lastModified() != modificationStamp;
			modificationStamp = databaseFile.databaseFile.lastModified();
		} else {
			databaseChanged = false;
		}
	}

	public boolean databaseChanged() throws CoreException {
		return databaseChanged;
	}

	public DatabaseFile getDatabaseFile() {
		return databaseFile;
	}

	private DatabaseFile findDatabaseFile(IProject project, IProgressMonitor monitor) throws CoreException {
		DatabaseFile databaseFile = null;

		monitor.subTask("Find SQLite database");

		databaseFile = findDatabaseFileByType(project, DatabaseType.SQLITE);

		if (databaseFile == null) {
			monitor.subTask("Find H2 database");

			databaseFile = findDatabaseFileByType(project, DatabaseType.H2);
		}

		monitor.worked(1);

		return databaseFile;
	}

	private DatabaseFile findDatabaseFileByType(IProject project, DatabaseType databaseType) throws CoreException {
		DatabaseFile database = null;
		List<File> files = ParseUtils.findFilesByEnding(project, getFileEnding(databaseType));

		if (!files.isEmpty()) {
			database = new DatabaseFile(files.get(0), databaseType);
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
			throw new IllegalArgumentException(databaseType.toString() + "is not supported!");
		}
	}
}
