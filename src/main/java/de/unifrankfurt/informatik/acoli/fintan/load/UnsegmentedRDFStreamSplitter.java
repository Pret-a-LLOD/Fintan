package de.unifrankfurt.informatik.acoli.fintan.load;

import static org.acoli.conll.rdf.CoNLLRDFCommandLine.readString;
import static org.acoli.conll.rdf.CoNLLRDFCommandLine.readUrl;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.HashMap;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.tdb.TDB;
import org.apache.jena.tdb.TDBFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.unifrankfurt.informatik.acoli.fintan.core.FintanCLIManager;
import de.unifrankfurt.informatik.acoli.fintan.core.FintanStreamComponent;
import de.unifrankfurt.informatik.acoli.fintan.core.StreamLoader;

/**
 * Load unsegmented RDF streams in the given serialization format. Default: TTL
 * Utilize SPARQL query to split data into segments.
 * Output: Stream of Models.
 * @author CF
 *
 */
public class UnsegmentedRDFStreamSplitter extends StreamLoader {
	
		protected static final Logger LOG = LogManager.getLogger(UnsegmentedRDFStreamSplitter.class.getName());
		
		public static final String DEFAULT_TDB_PATH = "tdb/";

		private Dataset tdbDataset; 
		private String lang = "TTL";
		private Query query;
		
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
		
		public void parseQuery(String query) {
			if (new File(query).exists()) {
				LOG.debug("Attempting to read query from file");
				this.query = QueryFactory.create(FintanCLIManager.readString(Paths.get(query)));
			}

			try {
				URL url = new URL(query);
				LOG.debug("Attempting to read query from URL");
				this.query = QueryFactory.create(FintanCLIManager.readUrl(url));
			} catch (MalformedURLException e) {
				LOG.debug(e);
			}
		}
		
		public void initTDB(String path) {
			if (path == null) path = DEFAULT_TDB_PATH;
			if (!path.endsWith("/")) path+="/";
			tdbDataset = TDBFactory.createDataset(path+this.hashCode()+"/");
		}

		private void processStream() {
			for (String name:listInputStreamNames()) {
				tdbDataset.begin(ReadWrite.WRITE);
				if (name.equals("")) {
					tdbDataset.getDefaultModel().read(getInputStream(name),lang);
				} else {
					tdbDataset.getNamedModel(name).read(getInputStream(name),lang);
				}
			}
			
			
			try {
				Model m = ModelFactory.createDefaultModel();
				m.read(getInputStream(name), null, lang);
				getOutputStream(name).writeObject(m);
				getOutputStream(name).flush();
			} catch (IOException e) {
				LOG.error("Error when processing Stream "+name+ ": " +e);
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
