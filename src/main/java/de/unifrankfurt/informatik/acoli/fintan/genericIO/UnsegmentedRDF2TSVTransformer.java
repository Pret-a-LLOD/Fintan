package de.unifrankfurt.informatik.acoli.fintan.genericIO;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.query.ResultSet;
import org.apache.jena.tdb.TDBFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.node.ObjectNode;

import de.unifrankfurt.informatik.acoli.fintan.core.FintanManager;
import de.unifrankfurt.informatik.acoli.fintan.core.FintanStreamComponentFactory;
import de.unifrankfurt.informatik.acoli.fintan.core.StreamTransformerGenericIO;
import de.unifrankfurt.informatik.acoli.fintan.write.TSVStreamWriter;

public class UnsegmentedRDF2TSVTransformer extends StreamTransformerGenericIO implements FintanStreamComponentFactory {

	@Override
	public UnsegmentedRDF2TSVTransformer buildFromJsonConf(ObjectNode conf) throws IOException, IllegalArgumentException {
		UnsegmentedRDF2TSVTransformer writer = new UnsegmentedRDF2TSVTransformer();
		writer.setConfig(conf);

		if (conf.hasNonNull("lang")) {
			writer.setLang(conf.get("lang").asText());
		}
		if (conf.hasNonNull("query")) {
			writer.setQuery(FintanManager.parseSelectQuery(FintanManager.readSourceAsString(conf.get("query").asText())));
		}
		if (conf.hasNonNull("escapeChar")) {
			writer.setEscapeChar(conf.get("escapeChar").asText());
		}
		if (conf.hasNonNull("delimiterCSV")) {
			writer.setDelimiterCSV(conf.get("delimiterCSV").asText());
		}
		if (conf.hasNonNull("quoteChar")) {
			writer.setQuoteChar(conf.get("quoteChar").asText());
		}
		if (conf.hasNonNull("emptyChar")) {
			writer.setEmptyChar(conf.get("emptyChar").asText());
		}
		if (conf.hasNonNull("tdbPath")) {
			writer.initTDB(conf.get("tdbPath").asText());
		} else {
			writer.initTDB(null);
		}
		
		return writer;
	}

	@Override
	public UnsegmentedRDF2TSVTransformer buildFromCLI(String[] args) throws IOException, IllegalArgumentException {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public void setOutputStream(OutputStream outputStream, String name) throws IOException {
		if (name == null || FINTAN_DEFAULT_STREAM_NAME.equals(name)) {
			setOutputStream(outputStream);
		} else {
			throw new IOException("Only default OutputStream is supported for "+UnsegmentedRDF2TSVTransformer.class.getName());
		}
	}
	
	protected static final Logger LOG = LogManager.getLogger(TSVStreamWriter.class.getName());

	

	private Dataset tdbDataset; 
	private String lang = "TTL";
	private Query query;
	private String escapeChar = null;
	private String delimiterCSV = "\t";
	private String quoteChar = "";
	private String emptyChar = "";
	

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
