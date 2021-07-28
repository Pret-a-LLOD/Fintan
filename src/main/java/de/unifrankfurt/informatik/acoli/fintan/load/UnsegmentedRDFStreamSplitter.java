package de.unifrankfurt.informatik.acoli.fintan.load;

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
import java.util.Iterator;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryException;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.query.ResultSet;
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
		private String constructQuery;
		private String iteratorQuery;
		
		public String getLang() {
			return lang;
		}

		public void setLang(String lang) {
			this.lang = lang;
		}
		
		public String getConstructQuery() {
			return constructQuery;
		}

		public void setConstructQuery(String query) {
			this.constructQuery = query;
		}
		
		public String getIteratorQuery() {
			return iteratorQuery;
		}

		public void setIteratorQuery(String query) {
			this.iteratorQuery = query;
		}
		
		

		//TODO: consider reassembling the query using an iterator over e.g. LexEntries, Sentences etc.
		public Query parseIteratorQuery(String query_string) throws QueryException {
			LOG.debug("Attempting to read query as String " + query_string);
			Query query = QueryFactory.create(query_string);
			if (query.isSelectType() && query.getProjectVars().size() > 0) {
				return query;
			} else {
				throw new QueryException("Iterator query must be a SELECT query."
						+ "with explicit columns corresponding to the construct query wildcards.");
			}
		}
		
		/**
		 * Parse the construct query and replace all wildcards by their respective seed elements
		 * If no seedElements HashMap is supplied (=null), test parsing the query with blank nodes instead.
		 * @param query_string
		 * @param seedElements
		 * @return
		 * @throws QueryException
		 */
		public Query parseConstructQuery(String query_string, HashMap<String, String> seedElements) throws QueryException {
			LOG.debug("Attempting to read query as String " + query_string);
			
			if (seedElements==null) {
				// replace all wildcards by blank nodes (Test parsing)
				query_string = query_string.replaceAll("<\\?.+?>", "[]");
			} else {
				// replace all wildcards with their respective seedElements
				for (String var:seedElements.keySet()) {
					query_string = query_string.replace("<"+var+">", seedElements.get(var));
				}
			}
			
			Query query = QueryFactory.create(query_string);
			if (query.isConstructType() || query.isDescribeType()) {
				return query;
			} else {
				throw new QueryException("Construct query must be either CONSTRUCT or DESCRIBE type.");
			}
		}
		
		public void initTDB(String path) {
			if (path == null) path = DEFAULT_TDB_PATH;
			if (!path.endsWith("/")) path+="/";
			tdbDataset = TDBFactory.createDataset(path+this.getClass().getName()+this.hashCode()+"/");
		}

		private void processStream() {
			//parse / test all queries on init
			Query iteratorQuery = parseIteratorQuery(this.iteratorQuery);
			Query constructQuery = parseConstructQuery(this.constructQuery, null);
			
			//load streamed data into graphs
			for (String name:listInputStreamNames()) {
				tdbDataset.begin(ReadWrite.WRITE);
				if (name.equals("")) {
					tdbDataset.getDefaultModel().read(getInputStream(name),lang);
				} else {
					tdbDataset.getNamedModel(name).read(getInputStream(name),lang);
				}
				tdbDataset.commit();
				tdbDataset.end();
			}

			//execute iteratorQuery
			tdbDataset.begin(ReadWrite.READ);
			ResultSet rs = QueryExecutionFactory.create(iteratorQuery, tdbDataset).execSelect();
			
			//for each resulting row, parse and execute construct query -> pipe as modelstream
			while (rs.hasNext()) {
				QuerySolution sol = rs.next();
				
				//read seedElements and parse Query
				HashMap <String, String> seedElements = new HashMap<String, String>();
				for (String var:rs.getResultVars()) {
					seedElements.put(var, sol.get(var).toString());
				}
				constructQuery = parseConstructQuery(this.constructQuery, seedElements);
				
				//execute CONSTRUCT or DESCRIBE query
				Model resultModel = null;
				Dataset resultDataset = null;
				if (constructQuery.isDescribeType()) {
					resultModel = QueryExecutionFactory.create(constructQuery, tdbDataset).execDescribe();
				} else {
					resultDataset = QueryExecutionFactory.create(constructQuery, tdbDataset).execConstructDataset();
				}
				
				//for CONSTRUCT queries write all resulting subgraphs into their respective OutputStreams
				if (resultDataset != null) {
					//write the default graph into the resultModel (is empty, since no DESCRIBE query) 
					resultModel = resultDataset.getDefaultModel();
					//write named models to their respective streams
					Iterator<String> iter = resultDataset.listNames();
					while(iter.hasNext()) {
						String name = iter.next();
						try {
							getOutputStream(name).writeObject(resultDataset.getNamedModel(name));
							getOutputStream(name).flush();
						} catch (IOException e) {
							LOG.error("Error when processing stream "+name+ ": " +e);
						}
					}
				}

				//for CONSTRUCT query the default graph has been written in the resultModel
				//for DESCRIBE query only default graph is needed and directly supplied as resultModel
				if (resultModel == null) {
					try {
						getOutputStream().writeObject(resultModel);
						getOutputStream().flush();
					} catch (IOException e) {
						LOG.error("Error when processing default output stream: " +e);
					}
				}
			}
			
			tdbDataset.end();
			tdbDataset.close();
			
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
