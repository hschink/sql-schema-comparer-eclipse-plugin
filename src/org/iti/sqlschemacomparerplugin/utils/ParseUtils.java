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

}
