/*
 * Copyright [2021] [ACoLi Lab, Prof. Dr. Chiarcos, Christian Faeth, Goethe University Frankfurt]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.acoli.fintan.core.util;

import java.io.PrintStream;
import java.util.List;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryException;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Utilities for working with Jena objects (like Model, Dataset or ResultSet). 
 * 
 * @author Christian Faeth {@literal faeth@em.uni-frankfurt.de}
 *
 */
public class JenaUtils {

	protected static final Logger LOG = LogManager.getLogger(JenaUtils.class.getName());
	/**
	 * Parse a select query. 
	 * 
	 * @param query_string 
	 * 			Must be a Select query with at least one project variable.
	 * @return	
	 * 			The parsed query.
	 * @throws QueryException	
	 * 			if parsing fails.
	 */
	public static Query parseSelectQuery(String query_string) throws QueryException {
		LOG.debug("Attempting to read query string: \n" + query_string);
		Query query = QueryFactory.create(query_string);
		if (query.isSelectType() && query.getProjectVars().size() > 0) {
			return query;
		} else {
			throw new QueryException("Query must be a SELECT query.");
		}
	}
	
	/**
	 * Consumes a Jena ResultSet and creates a custom serialization in the specified
	 * CustomCSVFormat.
	 * 
	 * @param out PrintStream to write to
	 * @param rs ResultSet to consume
	 * @param format CustomCSVFormat for serializing the ResultSet
	 */
	public static void outputCustomCSV(PrintStream out, ResultSet rs, CustomCSVFormat format) {
		List<String> cols = rs.getResultVars();
		while(rs.hasNext()) {
			QuerySolution sol = rs.next();
			
			boolean firstCol = true; //no delimiter before first column
			
			for(String col : cols) {
				if (!firstCol) {
					out.print(format.delimiterCSV);
				} else {
					firstCol = false;
				}
				
				if(sol.get(col)==null) {
					out.print(format.emptyChar);		
				} else {
					out.print(format.writeColumnContent(sol.get(col).toString()));
				}
			}
			
			out.println();
			out.flush();
		}
	}
}
