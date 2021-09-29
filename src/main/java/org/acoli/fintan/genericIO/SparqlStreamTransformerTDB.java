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
package org.acoli.fintan.genericIO;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import org.acoli.fintan.core.FintanManager;
import org.acoli.fintan.core.FintanStreamComponentFactory;
import org.acoli.fintan.core.StreamTransformerGenericIO;
import org.acoli.fintan.core.util.CustomCSVFormat;
import org.acoli.fintan.core.util.IOUtils;
import org.acoli.fintan.core.util.JenaUtils;
import org.apache.commons.io.FileUtils;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.sparql.resultset.ResultsFormat;
import org.apache.jena.tdb.TDBFactory;
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
public class SparqlStreamTransformerTDB extends StreamTransformerGenericIO implements FintanStreamComponentFactory {

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
	public SparqlStreamTransformerTDB buildFromJsonConf(ObjectNode conf) throws IOException, IllegalArgumentException {
		SparqlStreamTransformerTDB writer = new SparqlStreamTransformerTDB();
		writer.setConfig(conf);

		if (conf.hasNonNull("lang")) {
			writer.setLang(conf.get("lang").asText());
		}
		if (conf.hasNonNull("query")) {
			writer.setQuery(JenaUtils.parseSelectQuery(IOUtils.readSourceAsString(conf.get("query").asText())));
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
		
		if (conf.hasNonNull("tdbPath")) {
			writer.initTDB(conf.get("tdbPath").asText());
		} else {
			writer.initTDB(null);
		}
		
		return writer;
	}

	@Override
	public SparqlStreamTransformerTDB buildFromCLI(String[] args) throws IOException, IllegalArgumentException {
		// TODO Auto-generated method stub
		return null;
	}
	
	/**
	 * Overrides default method. Only accepts default stream.
	 * 
	 * @throws IOException if named stream is set.
	 */
	@Override
	public void setOutputStream(OutputStream outputStream, String name) throws IOException {
		if (name == null || FINTAN_DEFAULT_STREAM_NAME.equals(name)) {
			setOutputStream(outputStream);
		} else {
			throw new IOException("Only default OutputStream is supported for "+SparqlStreamTransformerTDB.class.getName());
		}
	}
	
	protected static final Logger LOG = LogManager.getLogger(SparqlStreamTransformerTDB.class.getName());

	

	private Dataset tdbDataset; 
	private String lang = "TTL";
	private Query query;
	private CustomCSVFormat customFormat;
	private ResultsFormat jenaFormat;
	

	public String getLang() {
		return lang;
	}

	public void setLang(String lang) {
		this.lang = lang;
	}
	
	public Query getQuery() {
		return query;
	}

	public void setQuery(Query query) {
		this.query = query;
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

	public void initTDB(String path) {
		if (path == null) path = FintanManager.DEFAULT_TDB_PATH;
		if (!path.endsWith("/")) path+="/";
		File f = new File(path+this.getClass().getName()+this.hashCode()+"/");
		if (f.exists() && f.isDirectory()) {
			try {
				FileUtils.deleteDirectory(f);
			} catch (IOException e) {
				LOG.error("Could not delete directory <"+f.getAbsolutePath()+">. "
						+ "Preexisting data may corrupt the current stream! "
						+ "Error message:"+e);
			}
		}
		f.mkdirs();
		Runtime.getRuntime().addShutdownHook(new Thread(() -> FileUtils.deleteQuietly(f)));
		tdbDataset = TDBFactory.createDataset(f.getAbsolutePath());
	}

	private void processStream() {

		PrintStream out = new PrintStream(getOutputStream());
		try {
			//load streamed data into graphs
			for (String name:listInputStreamNames()) {
				tdbDataset.begin(ReadWrite.WRITE);
				if (name.equals(FINTAN_DEFAULT_STREAM_NAME)) {
					tdbDataset.getDefaultModel().read(getInputStream(name), null, lang);
				} else {
					tdbDataset.getNamedModel(name).read(getInputStream(name), null, lang);
					tdbDataset.getDefaultModel().setNsPrefixes(tdbDataset.getNamedModel(name).getNsPrefixMap());
				}
				tdbDataset.commit();
				tdbDataset.end();
			}

			tdbDataset.begin(ReadWrite.READ);
			ResultSet rs = QueryExecutionFactory.create(query, tdbDataset).execSelect();
			if(jenaFormat != null) {
				ResultSetFormatter.output(out, rs, jenaFormat);
			} else {
				JenaUtils.outputCustomCSV(out, rs, customFormat);
			}
			tdbDataset.end();

		} finally {
			tdbDataset.close();
			out.flush();
			out.close();
		}
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
