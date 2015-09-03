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

package org.iti.sqlschemacomparerplugin.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.MemberValuePair;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.iti.sqlSchemaComparison.edge.ForeignKeyRelationEdge;
import org.iti.sqlSchemaComparison.edge.TableHasColumnEdge;
import org.iti.sqlSchemaComparison.frontends.technologies.IJPASchemaFrontend;
import org.iti.sqlSchemaComparison.vertex.ISqlElement;
import org.iti.sqlSchemaComparison.vertex.SqlColumnVertex;
import org.iti.sqlSchemaComparison.vertex.SqlElementFactory;
import org.iti.sqlSchemaComparison.vertex.SqlElementType;
import org.iti.sqlSchemaComparison.vertex.sqlColumn.IColumnConstraint;
import org.iti.sqlSchemaComparison.vertex.sqlColumn.PrimaryKeyColumnConstraint;
import org.iti.sqlschemacomparerplugin.utils.databaseformatter.IDatabaseIdentifierFormatter;
import org.iti.sqlschemacomparerplugin.utils.databaseformatter.NullFormatter;
import org.iti.structureGraph.nodes.IStructureElement;
import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleDirectedGraph;

public class EclipseJPASchemaFrontend implements IJPASchemaFrontend {

	private final static String OVERRIDE = "Override";
	private final static String VERSION = "Version";
	private final static String JOIN_COLUMN = "JoinColumn";

	private IFile file = null;
	private IDatabaseIdentifierFormatter formatter = new NullFormatter();
	
	private static class JPAAnnotationVisitor extends ASTVisitor {

		public Map<String, String> classToTable = new HashMap<>();
		public Map<String, TypeDeclaration> classDeclarations = new HashMap<>();

		private final static String TRANSIENT = "Transient";
		private final static String JOIN_TABLE = "JoinTable";
		private final static String ID = "Id";
		private static final String SETTER_PREFIX = "set";

		private static final List<String> SUPPORTED_RETURN_TYPES = new ArrayList<String>() {
			/**
			 *
			 */
			private static final long serialVersionUID = 1L;

			{
					add("String");
					add("Date");
			}
		};

		private DirectedGraph<IStructureElement, DefaultEdge> schema;

		private ISqlElement lastVisitedClass;

		private IDatabaseIdentifierFormatter formatter;
		
		private Map<String, MethodDeclaration> possibleColumnMethods = new HashMap<>();

		public JPAAnnotationVisitor(DirectedGraph<IStructureElement, DefaultEdge> schema,
				IDatabaseIdentifierFormatter formatter) {
			this.schema = schema;
			this.formatter = formatter;
		}

		@Override
		public boolean visit(TypeDeclaration node) {
			if (hasAnnotationOfType(ENTITY, node.modifiers())) {
				processClass(node);
				return true;
			}
			
			return false;
		}

		private void processClass(TypeDeclaration n) {
			String tableName = formatter.formatTable(getTableName(n));
			ISqlElement table = SqlElementFactory.createSqlElement(SqlElementType.Table, tableName);
			
			table.setSourceElement(n);
			
			schema.addVertex(table);
			
			lastVisitedClass = table;
			
			classToTable.put(n.getName().toString(), tableName);
			classDeclarations.put(n.getName().toString(), n);
		}

		@Override
		public boolean visit(MethodDeclaration node) {
			if (isGetter(node)
					&& isSupportedGetterMethod(node)
					&& isColumnRepresentingMethod(node)) {
				String setterName = node.getName().toString().substring(GETTER_PREFIX.length());
				
				possibleColumnMethods.put("set" + setterName, node);
			} else if (isSetter(node) && possibleColumnMethods.get(node.getName().toString()) != null) {
				processMethod(possibleColumnMethods.remove(node.getName().toString()));
			}

			return false;
		}

		private boolean isSupportedGetterMethod(MethodDeclaration node) {
			List<?> modifiers = node.modifiers();

			return representsSingleValue(node)
					&& node.parameters().isEmpty()
					&& !hasAnnotationOfType(JOIN_TABLE, modifiers)
					&& !hasAnnotationOfType(OVERRIDE, modifiers)
					&& returnsSupportedType(node);
		}

		private boolean returnsSupportedType(MethodDeclaration node) {
			Type returnType = node.getReturnType2();

			if (returnType instanceof PrimitiveType) {
				return true;
			} else if (returnType instanceof SimpleType) {
				SimpleType simpleType = (SimpleType)returnType;

				return SUPPORTED_RETURN_TYPES.contains(simpleType.getName().toString());
			}

			return false;
		}

		private boolean isColumnRepresentingMethod(MethodDeclaration node) {
			List<?> modifiers = node.modifiers();

			return !hasAnnotationOfType(TRANSIENT, modifiers);
		}

		private boolean isGetter(MethodDeclaration n) {
			return n.getName().toString().startsWith(GETTER_PREFIX);
		}

		private boolean isSetter(MethodDeclaration n) {
			return n.getName().toString().startsWith(SETTER_PREFIX);
		}

		private boolean representsSingleValue(MethodDeclaration n) {
			return !n.getName().toString().endsWith("s");
		}

		private void processMethod(MethodDeclaration n) {
			String id = formatter.formatColumn(getColumnName(n));
			String type = "?";
			List<IColumnConstraint> constraints = new ArrayList<>();
			
			ISqlElement column = new SqlColumnVertex(id, type, lastVisitedClass.getSqlElementId());
			
			column.setSourceElement(n);
			
			((SqlColumnVertex) column).setConstraints(constraints);
			
			if (hasAnnotationOfType(ID, n.modifiers())) {
				PrimaryKeyColumnConstraint constraint = new PrimaryKeyColumnConstraint("", column);
				
				constraints.add(constraint);
			}
			
			schema.addVertex(column);
			schema.addEdge(lastVisitedClass, column, new TableHasColumnEdge(lastVisitedClass, column));
		}
	}
	
	private static class PrimaryKeyVisitor extends ASTVisitor {

		private DirectedGraph<IStructureElement, DefaultEdge> schema;
		
		private Map<String, String> classToTable;
		
		private Map<String, TypeDeclaration> classDeclarations = new HashMap<>();
		
		public PrimaryKeyVisitor(DirectedGraph<IStructureElement, DefaultEdge> schema,
				Map<String, String> classToTable,
				Map<String, TypeDeclaration> classDeclarations) {
			this.schema = schema;
			this.classToTable = classToTable;
			this.classDeclarations = classDeclarations;
		}

		@Override
		public boolean visit(TypeDeclaration node) {
			if (hasAnnotationOfType(ENTITY, node.modifiers())) {
				processClass(node);
				return true;
			}
			
			return false;
		}

		private void processClass(TypeDeclaration n) {
			ISqlElement primaryKeyColumn = getPrimaryKeyOfType(n);
			
			if (primaryKeyColumn == null) {
				primaryKeyColumn = getPrimaryKeyOfSupertypes(n.getSuperclassType());
				
				setPrimaryKeyOfTable(n, primaryKeyColumn);
			}
		}


		private void setPrimaryKeyOfTable(TypeDeclaration n, ISqlElement primaryKeyColumn) {
			if (primaryKeyColumn != null && primaryKeyColumn instanceof SqlColumnVertex) {
				String tableId = classToTable.get(n.getName().toString());
				ISqlElement table = SqlElementFactory.getMatchingSqlElement(SqlElementType.Table, tableId, schema.vertexSet());
				SqlColumnVertex foreignKeyColumn = (SqlColumnVertex)primaryKeyColumn;
				ISqlElement foreignKeyTable = SqlElementFactory.getMatchingSqlElement(SqlElementType.Table, foreignKeyColumn.getTable(), schema.vertexSet());
				String id = foreignKeyColumn.getSqlElementId();
				String type = foreignKeyColumn.getType();
				List<IColumnConstraint> constraints = new ArrayList<>();
				
				ISqlElement column = new SqlColumnVertex(id, type, table.getSqlElementId());
				
				column.setSourceElement(n);
				
				((SqlColumnVertex) column).setConstraints(constraints);
				
				PrimaryKeyColumnConstraint constraint = new PrimaryKeyColumnConstraint("", column);
				
				constraints.add(constraint);
				
				schema.addVertex(column);
				schema.addEdge(table, column, new TableHasColumnEdge(table, column));
				schema.addEdge(table, column, new ForeignKeyRelationEdge(column, foreignKeyTable, foreignKeyColumn));
			}			
		}

		private ISqlElement getPrimaryKeyOfType(TypeDeclaration type) {
			String tableId = classToTable.get(type.getName().toString());
			ISqlElement table = SqlElementFactory.getMatchingSqlElement(SqlElementType.Table, tableId, schema.vertexSet());
			
			return SqlElementFactory.getPrimaryKey(table, schema);
		}
		
		private ISqlElement getPrimaryKeyOfSupertypes(Type superclassType) {
			ISqlElement primaryKeyColumn = null;
			
			if (superclassType instanceof SimpleType) {
				SimpleType simpleType = (SimpleType)superclassType;
				TypeDeclaration superclass = classDeclarations.get(simpleType.getName());
				
				if (superclass != null) {
					primaryKeyColumn = getPrimaryKeyOfType(superclass);
				
					if (primaryKeyColumn == null) {
						primaryKeyColumn = getPrimaryKeyOfSupertypes(superclass.getSuperclassType());
					}
				}
			}
			
			return primaryKeyColumn;
		}
	}

	private static class ForeignKeyVisitor extends ASTVisitor {

		private final static String[] RELATIONSHIP_ANNOTATIONS = new String[]
		{
			"@ManyToMany",
			"ManyToOne",
			"OneToMany",
			"OneToOne"
		};
		
		private DirectedGraph<IStructureElement, DefaultEdge> schema;
		
		private Map<String, String> classToTable = new HashMap<>();

		private ISqlElement lastVisitedClass;

		private IDatabaseIdentifierFormatter formatter;

		public ForeignKeyVisitor(DirectedGraph<IStructureElement, DefaultEdge> schema,
				Map<String, String> classToTable,
				IDatabaseIdentifierFormatter formatter) {
			this.schema = schema;
			this.classToTable = classToTable;
			this.formatter = formatter;
		}

		@Override
		public boolean visit(TypeDeclaration node) {
			if (hasAnnotationOfType(ENTITY, node.modifiers())) {
				processClass(node);
				return true;
			}
			
			return false;
		}

		private void processClass(TypeDeclaration n) {
			String id = formatter.formatTable(getTableName(n));
			
			lastVisitedClass = SqlElementFactory.getMatchingSqlElement(SqlElementType.Table, id, schema.vertexSet());
		}

		@Override
		public boolean visit(MethodDeclaration node) {
			if (isGetter(node) && hasAnnotationsOfType(RELATIONSHIP_ANNOTATIONS, node.modifiers())) {
				processMethod(node);
			}
			
			return super.visit(node);
		}

		private boolean hasAnnotationsOfType(String[] relationshipAnnotations,
				List<?> modifiers) {
			for (String annotationType : relationshipAnnotations) {
				if (hasAnnotationOfType(annotationType, modifiers)) {
					return true;
				}
			}
			
			return false;
		}

		private boolean isGetter(MethodDeclaration n) {
			return n.getName().toString().startsWith(GETTER_PREFIX);
		}

		private void processMethod(MethodDeclaration n) {
			String columnId = lastVisitedClass.getSqlElementId() + "." + formatter.formatColumn(getColumnName(n));
			String foreignTableId = classToTable.get(n.getName().toString());
			ISqlElement foreignKeyTable = SqlElementFactory.getMatchingSqlElement(SqlElementType.Table, foreignTableId, schema.vertexSet());

			if (foreignKeyTable != null) {
				ISqlElement referencingColumn = SqlElementFactory.getMatchingSqlColumns(columnId, schema.vertexSet(), true).get(0);
				ISqlElement foreignKeyColumn = SqlElementFactory.getPrimaryKey(foreignKeyTable, schema);

				if (referencingColumn != null && foreignKeyColumn != null) {
					schema.addEdge(referencingColumn, foreignKeyColumn, new ForeignKeyRelationEdge(referencingColumn, foreignKeyTable, foreignKeyColumn));
				}
			}
		}
		
	}

	private static String getTableName(TypeDeclaration n) {
		String value = getAnnotationMemberValue(n.modifiers(), TABLE, TABLE_NAME);
		
		return (value == null) ? n.getName().toString() : value;
	}
	
	private static boolean hasAnnotationOfType(String type, List<?> modifiers) {
		for (Object object : modifiers) {
			IExtendedModifier modifier = (IExtendedModifier)object;
			
			if (modifier.isAnnotation()) {
				Annotation annotation = (Annotation)modifier;
				
				if (annotation.getTypeName().toString().equals(type))
					return true;
				
			}
		}
		
		return false;
	}
	
	private static String getAnnotationMemberValue(List<?> modifiers,
			String annotationName,
			String attributeName) {
		for (Object object : modifiers) {
			IExtendedModifier modifier = (IExtendedModifier)object;
			
			if (modifier.isAnnotation()) {
				Annotation annotation = (Annotation)modifier;
				
				if (annotation.getTypeName().toString().equals(annotationName)) {
					NormalAnnotation a = (NormalAnnotation)annotation;
					
					for (Object value : a.values()) {
						MemberValuePair p = (MemberValuePair)value;
						
						if (p.getName().toString().equals(attributeName))
							return p.getValue().toString().replaceAll("\"", "");
					}
				}
				
			}
		}
		
		return null;
	}

	private static String getColumnName(MethodDeclaration n) {
		String value = null;

		if (hasAnnotationOfType(VERSION, n.modifiers())) {
			value = "Versions";
		} else if (hasAnnotationOfType(COLUMN, n.modifiers())) {
			value = getAnnotationMemberValue(n.modifiers(), COLUMN, TABLE_NAME);
		} else if (hasAnnotationOfType(JOIN_COLUMN, n.modifiers())) {
			value = getAnnotationMemberValue(n.modifiers(), JOIN_COLUMN, TABLE_NAME);
		}
		
		return (value == null) ? n.getName().toString().substring(GETTER_PREFIX.length(), n.getName().toString().length()).toLowerCase()
							   : value;
	}
	
	@Override
	public DirectedGraph<IStructureElement, DefaultEdge> createSqlSchema() {
		DirectedGraph<IStructureElement, DefaultEdge> schema = new SimpleDirectedGraph<IStructureElement, DefaultEdge>(DefaultEdge.class);
		List<CompilationUnit> cus = new ArrayList<>();
		Map<String, String> classToTable = new HashMap<>();
		Map<String, TypeDeclaration> classDeclarations = new HashMap<>();
		
		ICompilationUnit unit = JavaCore.createCompilationUnitFrom(file);
		
		if (unit != null)
			cus.add((CompilationUnit)ParseUtils.parse(unit));
		
		for (CompilationUnit c : cus) {
			parseJavaCompilationUnit(c, schema, classToTable, classDeclarations);
		}
		
		for (CompilationUnit c : cus) {
			createForeignKeyPrimaryRelationships(c, schema, classToTable, classDeclarations);
		}
		
		for (CompilationUnit c : cus) {
			createForeignKeyRelationships(c, schema, classToTable);
		}

        return schema;
	}
	
	private void parseJavaCompilationUnit(CompilationUnit cu, 
			DirectedGraph<IStructureElement, DefaultEdge> schema,
			Map<String, String> classToTable, 
			Map<String, TypeDeclaration> classDeclarations) {
		JPAAnnotationVisitor visitor = new JPAAnnotationVisitor(schema, formatter);
		cu.accept(visitor);
		
		classToTable.putAll(visitor.classToTable);
		classDeclarations.putAll(visitor.classDeclarations);
	}
	
	private void createForeignKeyPrimaryRelationships(CompilationUnit cu,
			DirectedGraph<IStructureElement, DefaultEdge> schema,
			Map<String, String> classToTable,
			Map<String, TypeDeclaration> classDeclarations) {
		PrimaryKeyVisitor visitor = new PrimaryKeyVisitor(schema, classToTable, classDeclarations);
		cu.accept(visitor);
	}
	
	private void createForeignKeyRelationships(CompilationUnit cu, 
			DirectedGraph<IStructureElement, DefaultEdge> schema,
			Map<String, String> classToTable) {
		
		ForeignKeyVisitor visitor = new ForeignKeyVisitor(schema, classToTable, formatter);
		cu.accept(visitor);
	}

	public EclipseJPASchemaFrontend(IFile file) {
		if (file == null)
			throw new NullPointerException("Path to JPA file(s) must not be null or empty!");
		
		this.file = file;
	}

	public EclipseJPASchemaFrontend(IFile file, IDatabaseIdentifierFormatter formatter) {
		this(file);
		this.formatter = formatter;
	}
}
