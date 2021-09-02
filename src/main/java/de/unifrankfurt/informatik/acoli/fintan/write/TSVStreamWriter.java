package de.unifrankfurt.informatik.acoli.fintan.write;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryException;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.node.ObjectNode;

import de.unifrankfurt.informatik.acoli.fintan.core.FintanManager;
import de.unifrankfurt.informatik.acoli.fintan.core.FintanStreamComponentFactory;
import de.unifrankfurt.informatik.acoli.fintan.core.StreamWriter;

public class TSVStreamWriter extends StreamWriter implements FintanStreamComponentFactory{


	@Override
	public TSVStreamWriter buildFromJsonConf(ObjectNode conf) throws IOException, IllegalArgumentException {
		TSVStreamWriter writer = new TSVStreamWriter();
		writer.setConfig(conf);

		if (conf.hasNonNull("query")) {
			writer.setQuery(parseSelectQuery(FintanManager.readSourceAsString(conf.get("query").asText())));
		}
		if (conf.hasNonNull("delimiter")) {
			writer.setSegmentDelimiter(conf.get("delimiter").asText());
		}
		if (conf.hasNonNull("delimiterCSV")) {
			writer.setDelimiterCSV(conf.get("delimiterCSV").asText());
		}
		return writer;
	}

	@Override
	public TSVStreamWriter buildFromCLI(String[] args) throws IOException, IllegalArgumentException {
		// TODO Auto-generated method stub
		return null;
	}
	
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
	
	protected static final Logger LOG = LogManager.getLogger(TSVStreamWriter.class.getName());

	

	private Query query;
	private String segmentDelimiter = null;
	private String escapeChar = null;
	private String delimiterCSV = "\t";
	private String quoteChar = "";
	private String emptyChar = "";
	

	public Query getQuery() {
		return query;
	}

	public void setQuery(Query query) {
		this.query = query;
	}

	public String getSegmentDelimiter() {
		return segmentDelimiter;
	}

	public void setSegmentDelimiter(String segmentDelimiter) {
		this.segmentDelimiter = segmentDelimiter;
	}

	public String getEscapeChar() {
		return escapeChar;
	}

	public void setEscapeChar(String escapeChar) {
		this.escapeChar = escapeChar;
	}
	
	public String getDelimiterCSV() {
		return delimiterCSV;
	}

	public void setDelimiterCSV(String delimiterCSV) {
		if (delimiterCSV == null)
			this.delimiterCSV = "";
		else
			this.delimiterCSV = delimiterCSV;
	}
	
	public String getQuoteChar() {
		return quoteChar;
	}

	public void setQuoteChar(String quoteChar) {
		if (quoteChar == null)
			this.quoteChar = "";
		else 
			this.quoteChar = quoteChar;
	}

	public String getEmptyChar() {
		return emptyChar;
	}

	public void setEmptyChar(String emptyChar) {
		if (emptyChar == null)
			this.emptyChar = "";
		else
			this.emptyChar = emptyChar;
	}

	private void processStream() {
		
		// Spawn writers for parallel processing, in case there are multiple streams.
		for (String name:listInputStreamNames()) {
			if (name == FINTAN_DEFAULT_STREAM_NAME) 
				continue;
			if (getOutputStream(name) == null) {
				LOG.info("Input stream '"+name+"' does not have a corresponding output stream and is thus dropped.");
				continue;
			}

			TSVStreamWriter writer = new TSVStreamWriter();
			writer.setSegmentDelimiter(segmentDelimiter);
			writer.setQuery(query);
			writer.setDelimiterCSV(delimiterCSV);
			try {
				writer.setInputStream(getInputStream(name));
				writer.setOutputStream(getOutputStream(name));
			} catch (IOException e) {
				LOG.error(e, e);
				System.exit(1);
			}
			new Thread(writer).start();
		}
		
		PrintStream out = new PrintStream(getOutputStream());
		
		while (getInputStream().canRead()) {
			try {
				Model m = getInputStream().read();
				//read may return null in case the queue has been emptied and terminated since asking for canRead()
				if (m == null) continue;
				
				ResultSet rs = QueryExecutionFactory.create(query, m).execSelect();
				List<String> cols = rs.getResultVars();
				
				while(rs.hasNext()) {
					QuerySolution sol = rs.next();
					for(String col : cols) {
						if(sol.get(col)==null) {
							out.print(emptyChar+delimiterCSV);		
						} else {
							String outString = sol.get(col).toString();
							if(escapeChar != null) {
								if (!quoteChar.equals("")) {
									outString = outString.replace(quoteChar, escapeChar+quoteChar);
								} else if (!delimiterCSV.equals("")){
									outString = outString.replace(delimiterCSV, escapeChar+delimiterCSV);
								}
							}
							out.print(quoteChar+outString+quoteChar+delimiterCSV);
						}
					}
					out.println();
					out.flush();
				}
				
				if (segmentDelimiter != null) {
					out.println(segmentDelimiter);
				}

				out.flush();
				
			} catch (InterruptedException e) {
				LOG.error("Error when reading from Stream: " +e);
			}
		}
		out.close();
	}
	
	@Override
	public void start() {
		run();
	}

	@Override
	public void run() {
		try {
			processStream();
		} catch (Exception e) {
			LOG.error(e, e);
			System.exit(1);
		}
	}




	
}