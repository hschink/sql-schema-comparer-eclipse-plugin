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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

public class ParseUtils {

	private static class QueryStringFinder extends ASTVisitor {

		public List<StringLiteral> queryStrings = new ArrayList<>();

		private CompilationUnit compilationUnit;

		@Override
		public boolean visit(CompilationUnit compilationUnit) {
			this.compilationUnit = compilationUnit;

			return true;
		}

		@Override
		public boolean visit(MethodInvocation methodInvocation) {
			if (isQueryExecutionCall(methodInvocation)) {
				queryStrings.add(getQueryString(methodInvocation));

				return false;
			}

			return true;
		}

		private boolean isQueryExecutionCall(MethodInvocation methodInvocation) {
			return isJDBCStatementExecution(methodInvocation) || isJDBCStatementPreparation(methodInvocation);
		}

		private boolean isJDBCStatementExecution(MethodInvocation methodInvocation) {
			return isMethodInvocationOnClass(methodInvocation, "java.sql.Statement", "executeQuery");
		}

		private boolean isJDBCStatementPreparation(MethodInvocation methodInvocation) {
			return isMethodInvocationOnClass(methodInvocation, "java.sql.Connection", "prepareStatement");
		}

		private boolean isMethodInvocationOnClass(MethodInvocation methodInvocation,
				String expectedFullQualifiedClassName, String expectedMethodName) {
			String methodName = methodInvocation.getName().getIdentifier();
			IMethodBinding methodBinding = methodInvocation.resolveMethodBinding();
			ITypeBinding typeBinding = methodBinding.getDeclaringClass();
			String className = typeBinding.getQualifiedName();

			return className.equals(expectedFullQualifiedClassName) && methodName.equals(expectedMethodName);
		}

		private StringLiteral getQueryString(MethodInvocation methodInvocation) {
			Object firstArgument = methodInvocation.arguments().get(0); // TODO: It may not always be the first argument...

			if (firstArgument instanceof StringLiteral) {
				return (StringLiteral)firstArgument;
			} else if (firstArgument instanceof SimpleName) {
				return resolveQueryExecutionArgument((SimpleName)firstArgument);
			}

			return null;
		}

		private StringLiteral resolveQueryExecutionArgument(SimpleName argument) {
			IBinding binding = argument.resolveBinding();

			if (binding.getKind() == IBinding.VARIABLE) {
				IVariableBinding variableBinding = (IVariableBinding) binding;
				IVariableBinding declaration = variableBinding.getVariableDeclaration();
				ASTNode node = compilationUnit.findDeclaringNode(declaration);

				if (node instanceof VariableDeclarationFragment) {
					VariableDeclarationFragment fragment = (VariableDeclarationFragment)node;
					Expression initializer = fragment.getInitializer();

					if (initializer instanceof StringLiteral) {
						return (StringLiteral)initializer;
					}
				}
			}

			return null;
		}
	}

	public static int getLineNumber(IFile file, int startPosition) {
		ICompilationUnit unit = JavaCore.createCompilationUnitFrom(file);

		CompilationUnit node = (CompilationUnit)parse(unit);

		return node.getLineNumber(startPosition);
	}

	public static Map<String, Integer> getAllJavaStrings(IFile file) {
		Map<String, Integer> javaStringsAndLineNumbers = new HashMap<>();
		ICompilationUnit unit = JavaCore.createCompilationUnitFrom(file);
		CompilationUnit node = (CompilationUnit)parse(unit);
		QueryStringFinder visitor = new QueryStringFinder();

		node.accept(visitor);

		for (StringLiteral literal : visitor.queryStrings) {
			if (literal != null) {
				javaStringsAndLineNumbers.put(literal.getLiteralValue(),
						node.getLineNumber(literal.getStartPosition()));
			}
		}

		return javaStringsAndLineNumbers;
	}

	public static ASTNode parse(ICompilationUnit unit) {
		ASTParser parser = ASTParser.newParser(AST.JLS8);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setSource(unit);
		parser.setResolveBindings(true);
		return parser.createAST(null);
	}

	public static List<File> findFilesByEnding(IResource resource, final String suffix) {
		FilenameFilter filter = new FilenameFilter() {

			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith(suffix);
			}
		};

		return findFiles(resource, filter);
	}

	public static List<String> getIgnoredFileNames(IResource resource) {
		List<File> files = findFilesByName(resource, ".ssc.ignore");
		List<String> ignored = new ArrayList<>();

		if (!files.isEmpty()) {
			BufferedReader reader = null;

			try {
				reader = new BufferedReader(new FileReader(files.get(0)));

				while (reader.ready()) {
					ignored.add(reader.readLine().trim());
				}
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
				try {
					reader.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

		return ignored;
	}

	public static List<File> findFilesByName(IResource resource, final String fileName) {
		FilenameFilter filter = new FilenameFilter() {

			@Override
			public boolean accept(File dir, String name) {
				return name.equals(fileName);
			}
		};

		return findFiles(resource, filter);
	}

	private static List<File> findFiles(IResource resource,
			FilenameFilter filter) {
		List<File> files = new ArrayList<>();
		File root = resource.getLocation().toFile();

		if (root.isDirectory()) {
			File[] fileList = root.listFiles(filter);

			files = Arrays.asList(fileList);
		}

		return files;
	}
}
