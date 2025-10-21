/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.hcwathome.openmrs;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

public class DbUtils {
	
	/**
	 * Executes the specified query with a prepared statement and returns a list of mappings between
	 * column names and their values.
	 * 
	 * @param query the query to execute
	 * @param dataSource the datasource to use to execute the query
	 * @param values the values to set for the prepared statement
	 * @return List
	 * @throws SQLException
	 */
	public static List<Map<String, Object>> executeQuery(String query, DataSource dataSource, List<Object> values)
	    throws SQLException {
		List<Map<String, Object>> rows = new ArrayList<>();
		try (Connection c = dataSource.getConnection(); PreparedStatement s = c.prepareStatement(query)) {
			for (int i = 0; i < values.size(); i++) {
				s.setObject(i + 1, values.get(i));
			}
			
			try (ResultSet r = s.executeQuery()) {
				ResultSetMetaData rmd = r.getMetaData();
				int columnCount = rmd.getColumnCount();
				while (r.next()) {
					Map<String, Object> row = new HashMap<>();
					for (int i = 1; i <= columnCount; i++) {
						row.put(rmd.getColumnName(i), r.getObject(i));
					}
					
					rows.add(row);
				}
				
			}
		}
		
		return rows;
	}
	
}
