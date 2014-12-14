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
import org.eclipse.jdt.core.dom.StringLiteral;

public class ParseUtils {

	private static class JavaStringFinder extends ASTVisitor {

		public List<StringLiteral> javaStringLiterals = new ArrayList<>();
		
		@Override
		public boolean visit(StringLiteral node) {
			javaStringLiterals.add(node);
			
			return false;
		}		
	}
	
	public static int getLineNumber(IFile file, int startPosition) {
		ICompilationUnit unit = JavaCore.createCompilationUnitFrom(file);
		
		CompilationUnit node = (CompilationUnit)parse(unit);
		
		return node.getLineNumber(startPosition);
	}
	
	public static Map<String, Integer> getAllJavaStrings(IFile file) {
		Map<String, Integer> javaStringsAndLineNumbers = new HashMap<>();
		JavaStringFinder visitor = new JavaStringFinder();
		ICompilationUnit unit = JavaCore.createCompilationUnitFrom(file);
		
		CompilationUnit node = (CompilationUnit)parse(unit);
		
		node.accept(visitor);
		
		for (StringLiteral literal : visitor.javaStringLiterals) {
			javaStringsAndLineNumbers.put(literal.getLiteralValue(), node.getLineNumber(literal.getStartPosition()));
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
