package de.unifrankfurt.informatik.acoli.fintan.genericIO;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

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

import de.unifrankfurt.informatik.acoli.fintan.core.FintanManager;
import de.unifrankfurt.informatik.acoli.fintan.core.FintanStreamComponentFactory;
import de.unifrankfurt.informatik.acoli.fintan.core.StreamTransformerGenericIO;
import de.unifrankfurt.informatik.acoli.fintan.core.util.CustomCSVFormat;
import de.unifrankfurt.informatik.acoli.fintan.core.util.IOUtils;
import de.unifrankfurt.informatik.acoli.fintan.core.util.JenaUtils;

public class SparqlStreamTransformerTDB extends StreamTransformerGenericIO implements FintanStreamComponentFactory {

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
