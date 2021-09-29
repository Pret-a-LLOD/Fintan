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
package org.acoli.fintan.write;

import java.io.IOException;
import java.io.PrintStream;

import org.acoli.fintan.core.FintanStreamComponentFactory;
import org.acoli.fintan.core.StreamWriter;
import org.acoli.fintan.core.util.CustomCSVFormat;
import org.acoli.fintan.core.util.IOUtils;
import org.acoli.fintan.core.util.JenaUtils;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.sparql.resultset.ResultsFormat;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Consumes serialized RDF streams and loads them into graphs.  It can consume 
 * multiple streams of serialized RDF data (in a given lang), store them into a 
 * temporary TDB (at a given tdbPath) and query across all graphs at once. 
 * Named input streams are stored into the graph of the same name. 
 * For outputting the query results only the default stream is supported.
 * 
 * @author Christian Faeth {@literal faeth@em.uni-frankfurt.de}
 *
 */
public class SparqlStreamWriter extends StreamWriter implements FintanStreamComponentFactory{


	/**
	 * All possible serializations of query results in the Apache Jena library are supported. On top of that, it is possible to define custom CSV exports. The following JSON parameters are common for both of them:
	 * * `query` to define the path to the SPARQL query
	 * * `outFormat` can be used to denote a preconfigured output format. It can be either a Jena format (such as `TSV`) or `CoNLL` (which is a preconfigured custom Format). If no `outFormat` is specified, a custom format can be applied with the following parameters.
	 * * `escapeChar` for escaping functional characters 
	 * * `delimiterCSV` for the column delimiter. `\t` for CoNLL
	 * * `quoteChar` optional for wrapping cell content
	 * * `emptyChar` optional to denote an empty cell. `_` for CoNLL
	 */
	@Override
	public SparqlStreamWriter buildFromJsonConf(ObjectNode conf) throws IOException, IllegalArgumentException {
		SparqlStreamWriter writer = new SparqlStreamWriter();
		writer.setConfig(conf);

		if (conf.hasNonNull("query")) {
			writer.setQuery(JenaUtils.parseSelectQuery(IOUtils.readSourceAsString(conf.get("query").asText())));
		}
		if (conf.hasNonNull("delimiter")) {
			writer.setSegmentDelimiter(conf.get("delimiter").asText());
		}
		if (conf.hasNonNull("outFormat")) {
			writer.setJenaFormat(ResultsFormat.lookup(conf.get("outFormat").asText().toLowerCase()));
			if (writer.getJenaFormat()==null) {
				writer.setCustomFormat(CustomCSVFormat.lookup(conf.get("outFormat").asText()));
				if (writer.getCustomFormat()==null) {
					throw new IllegalArgumentException("'"+conf.get("outFormat").asText()+"' is no valid OutputFormat.");
				}
			}
		} else {
			String escapeChar=null;
			String delimiterCSV=null; 
			String quoteChar=null; 
			String emptyChar=null;
			if (conf.hasNonNull("escapeChar")) {
				escapeChar = conf.get("escapeChar").asText();
			}
			if (conf.hasNonNull("delimiterCSV")) {
				delimiterCSV = conf.get("delimiterCSV").asText();
			}
			if (conf.hasNonNull("quoteChar")) {
				quoteChar = conf.get("quoteChar").asText();
			}
			if (conf.hasNonNull("emptyChar")) {
				emptyChar = conf.get("emptyChar").asText();
			}
			writer.setCustomFormat(new CustomCSVFormat(escapeChar, delimiterCSV, quoteChar, emptyChar));
		}
		
		return writer;
	}

	@Override
	public SparqlStreamWriter buildFromCLI(String[] args) throws IOException, IllegalArgumentException {
		// TODO Auto-generated method stub
		return null;
	}
	
	protected static final Logger LOG = LogManager.getLogger(SparqlStreamWriter.class.getName());

	

	private Query query;
	private String segmentDelimiter = null;
	private CustomCSVFormat customFormat;
	private ResultsFormat jenaFormat;
	

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

	public CustomCSVFormat getCustomFormat() {
		return customFormat;
	}

	public void setCustomFormat(CustomCSVFormat customFormat) {
		this.customFormat = customFormat;
	}

	public ResultsFormat getJenaFormat() {
		return jenaFormat;
	}

	public void setJenaFormat(ResultsFormat jenaFormat) {
		this.jenaFormat = jenaFormat;
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

			SparqlStreamWriter writer = new SparqlStreamWriter();
			writer.setConfig(getConfig());
			writer.setQuery(query);
			writer.setSegmentDelimiter(segmentDelimiter);
			writer.setCustomFormat(customFormat);
			writer.setJenaFormat(jenaFormat);
			try {
				writer.setInputStream(getInputStream(name));
				writer.setOutputStream(getOutputStream(name));
			} catch (IOException e) {
				LOG.error(e, e);
				System.exit(1);
			}
			new Thread(writer).start();
		}
		
		//terminate in case there is no default stream. 
		//named streams are handled in subthreads.
		if (getOutputStream()==null) return;
		
		PrintStream out = new PrintStream(getOutputStream());
		
		while (getInputStream().canRead()) {
			try {
				Model m = getInputStream().read();
				//read may return null in case the queue has been emptied and terminated since asking for canRead()
				if (m == null) continue;
				
				ResultSet rs = QueryExecutionFactory.create(query, m).execSelect();
				if(jenaFormat != null) {
					ResultSetFormatter.output(out, rs, jenaFormat);
				} else {
					JenaUtils.outputCustomCSV(out, rs, customFormat);
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