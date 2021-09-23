package org.acoli.fintan.core.util;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.List;

import org.acoli.fintan.core.FintanManager;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryException;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.sparql.resultset.ResultsFormat;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
	
	public static void outputCustomCSV(PrintStream out, ResultSet rs, CustomCSVFormat format) {
		List<String> cols = rs.getResultVars();
		while(rs.hasNext()) {
			QuerySolution sol = rs.next();
			for(String col : cols) {
				if(sol.get(col)==null) {
					out.print(format.emptyChar+format.delimiterCSV);		
				} else {
					String outString = sol.get(col).toString();
					if(format.escapeChar != null) {
						if (!format.quoteChar.equals("")) {
							outString = outString.replace(format.quoteChar, format.escapeChar+format.quoteChar);
						} else if (!format.delimiterCSV.equals("")){
							outString = outString.replace(format.delimiterCSV, format.escapeChar+format.delimiterCSV);
						}
					}
					out.print(format.quoteChar+outString+format.quoteChar+format.delimiterCSV);
				}
			}
			out.println();
			out.flush();
		}
	}
}
