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

package org.iti.sqlschemacomparerplugin.utils.databaseformatter;

public class UpperCaseFormatter implements IDatabaseIdentifierFormatter {

	@Override
	public String formatTable(String tableName) {
		return tableName.toUpperCase();
	}

	@Override
	public String formatColumn(String columnName) {
		return columnName.toUpperCase();
	}
}
