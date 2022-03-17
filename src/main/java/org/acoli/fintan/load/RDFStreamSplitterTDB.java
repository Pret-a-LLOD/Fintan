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
package org.acoli.fintan.load;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.acoli.fintan.core.FintanManager;
import org.acoli.fintan.core.FintanStreamComponentFactory;
import org.acoli.fintan.core.StreamLoader;
import org.acoli.fintan.core.util.IOUtils;
import org.acoli.fintan.core.util.JenaUtils;
import org.apache.commons.io.FileUtils;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryException;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.tdb.TDBFactory;
import org.apache.jena.update.Update;
import org.apache.jena.update.UpdateAction;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Load unsegmented RDF streams in the given serialization format. Default: TTL
 * 
 * For splitting data into segments, the Splitter employs two distinct modes:
 * * `ITERATE_CONSTRUCT` mode uses a select query as an iterator (e.g. for all 
 * 		LexicalEntries in a graph) and a construct query to create isolated models 
 * 		for each of the seed elements (i.e. instances of LexicalEntry). Each 
 * 		resulting graph is then streamed to its respective output stream. The 
 * 		result variables of the iterator query must be referenced in the construct 
 * 		query by wildcards:
 * 
 *     * `?var1` in the iterator query corresponds to `<?var1>` in the construct 
 *     		query
 *     * the `<?var1>` wildcard is then dynamically replaced by the actual values 
 *     		of the query results for each iteration.
 *     
 * * `RECURSIVE_UPDATE` mode instead uses SPARQL updates to construct the result 
 * 		models within distinct result graphs. The Update must be designed in a 
 * 		way that it “consumes” the source graphs, i.e it should delete the data 
 * 		it writes to the result graphs from the source graphs. After each 
 * 		processing step, all target graphs are emptied after supplying their 
 * 		content to the output streams. The update is repeated until it produces 
 * 		only empty target graphs. 
 * 
 * Given the write penalties associated with SPARQL updates, `RECURSIVE_UPDATE` 
 * can be slower, but is more powerful in constructing multiple graphs at the 
 * same time, traversing complex property paths and being able to easily export 
 * unused deltas, which have never been addressed in any segment. Both modes are 
 * mutually exclusive and cannot be combined.
 * 
 * Output: Stream(s) of Models.
 * 
 * @author Christian Faeth {@literal faeth@em.uni-frankfurt.de}
 *
 */
public class RDFStreamSplitterTDB extends StreamLoader implements FintanStreamComponentFactory {
	
		protected static final Logger LOG = LogManager.getLogger(RDFStreamSplitterTDB.class.getName());
		
		/**
		 * The Splitter has the following parameters which can be set in the JSON config:
		 * 
		 * `lang` to specify the RDF syntax. Supported languages follow the 
		 * 		naming convention of Apache Jena (ttl, TURTLE, RDF/XML, N3, …)
		 * `tdbPath` to specify a custom directory to create the temporary TDB 
		 * 		database
		 * `iteratorQuery` for `ITERATE_CONSTRUCT` mode. Must be a select query. 
		 * `constructQuery` for `ITERATE_CONSTRUCT` mode. Must be a construct or 
		 * 		describe query. 
		 * `initUpdate` for any mode. Optional update which is 
		 * 		executed a single time at startup to initialize the recursion
		 * 		or prepare the data. 
		 * 		(e.g. to insert a “next” marker.)
		 * `recursiveUpdate` for `RECURSIVE_UPDATE` mode. Repeated until it 
		 * 		produces empty target graphs.
		 * `segmentStreams` for `RECURSIVE_UPDATE` mode specify which of the 
		 * 		target graphs host the target segments. Only these graphs are 
		 * 		streamed after each recursion.
		 * `deltaStreams` for `RECURSIVE_UPDATE` mode specifies a list of graphs 
		 * 		whose content should be streamed after the last recursion.
		 */
		@Override
		public RDFStreamSplitterTDB buildFromJsonConf(ObjectNode conf) throws IOException, IllegalArgumentException {
			RDFStreamSplitterTDB splitter = new RDFStreamSplitterTDB();
			splitter.setConfig(conf);
			
			if (conf.hasNonNull("lang")) {
				splitter.setLang(conf.get("lang").asText());
			}
			if (conf.hasNonNull("iteratorQuery")) {
				splitter.setIteratorQuery(IOUtils.readSourceAsString(conf.get("iteratorQuery").asText()));
			}
			if (conf.hasNonNull("constructQuery")) {
				splitter.setConstructQuery(IOUtils.readSourceAsString(conf.get("constructQuery").asText()));
			}
			if (conf.hasNonNull("initUpdate")) {
				splitter.setInitUpdate(IOUtils.readSourceAsString(conf.get("initUpdate").asText()));
			}
			if (conf.hasNonNull("recursiveUpdate")) {
				splitter.setRecursiveUpdate(IOUtils.readSourceAsString(conf.get("recursiveUpdate").asText()));
			}
			if (conf.hasNonNull("segmentStreams")) {
				ArrayList<String> segmentStreams = new ArrayList<String>();
				for (JsonNode node:conf.withArray("segmentStreams")) {
					segmentStreams.add(node.asText());
				}
				if (!segmentStreams.isEmpty())
					splitter.setSegmentStreams(segmentStreams.toArray(new String[] {}));
			}
			if (conf.hasNonNull("deltaStreams")) {
				ArrayList<String> deltaStreams = new ArrayList<String>();
				for (JsonNode node:conf.withArray("deltaStreams")) {
					deltaStreams.add(node.asText());
				}
				if (!deltaStreams.isEmpty())
					splitter.setDeltaStreams(deltaStreams.toArray(new String[] {}));
			}
			if (conf.hasNonNull("tdbPath")) {
				splitter.initTDB(conf.get("tdbPath").asText());
			} else {
				splitter.initTDB(null);
			}
			
			if (splitter.validateSplitterMode() == SplitterMode.INVALID) {
				throw new IllegalArgumentException("Splitter is configured inconsistently. "
						+ "Please supply either an UPDATE or a set of ITERATE / CONSTRUCT queries");
			}
			
			return splitter;
		}

		@Override
		public RDFStreamSplitterTDB buildFromCLI(String[] args) throws IOException, IllegalArgumentException {
			// TODO Auto-generated method stub
			return null;
		}

		
		public enum SplitterMode {ITERATE_CONSTRUCT, RECURSIVE_UPDATE, INVALID}
		private SplitterMode mode = SplitterMode.INVALID;
		
		private Dataset tdbDataset; 
		private String lang = "TTL";
		private String constructQuery;
		private String iteratorQuery;
		private String recursiveUpdate;
		private String initUpdate;
		private String[] deltaStreams;
		private String[] segmentStreams;
		
		
		public SplitterMode validateSplitterMode() {
			if (recursiveUpdate == null && iteratorQuery != null && constructQuery != null) {
				mode = SplitterMode.ITERATE_CONSTRUCT;
			} else if (recursiveUpdate != null && iteratorQuery == null && constructQuery == null) {
				//initUpdate is optional
				mode = SplitterMode.RECURSIVE_UPDATE;
			} else {
				mode = SplitterMode.INVALID;
			}
			return mode;
		}
		
		public String getLang() {
			return lang;
		}

		public void setLang(String lang) {
			this.lang = lang;
		}
		
		public String getConstructQuery() {
			return constructQuery;
		}
		
		public void setConstructQuery(String constructQuery) {
			this.constructQuery = constructQuery;
			validateSplitterMode();
		}

		public String getIteratorQuery() {
			return iteratorQuery;
		}
		
		public void setIteratorQuery(String iteratorQuery) {
			this.iteratorQuery = iteratorQuery;
			validateSplitterMode();
		}

		public String getRecursiveUpdate() {
			return recursiveUpdate;
		}

		public void setRecursiveUpdate(String updateQuery) {
			this.recursiveUpdate = updateQuery;
			validateSplitterMode();
		}
		
		public String getInitUpdate() {
			return initUpdate;
		}

		public void setInitUpdate(String updateQuery) {
			this.initUpdate = updateQuery;
			validateSplitterMode();
		}

		public String[] getDeltaStreams() {
			if (deltaStreams == null) return null;
			if (deltaStreams.length <= 0) return null;
			return deltaStreams;
		}

		public void setDeltaStreams(String[] deltaStreams) {
			this.deltaStreams = deltaStreams;
		}

		public String[] getSegmentStreams() {
			if (segmentStreams == null) return listOutputStreamNames();
			if (segmentStreams.length <= 0) return listOutputStreamNames();
			return segmentStreams;
		}

		public void setSegmentStreams(String[] segmentStreams) {
			this.segmentStreams = segmentStreams;
		}

		/**
		 * Parse iterator query. Project variables must correspond to wildcards 
		 * in construct query.
		 * 
		 * @param query_string 
		 * 			Must be a Select query with at least one project variable.
		 * @return	
		 * 			The parsed query.
		 * @throws QueryException	
		 * 			if parsing fails.
		 */
		public Query parseIteratorQuery(String query_string) throws QueryException {
			try {
				return JenaUtils.parseSelectQuery(query_string);
			} catch (QueryException e) {
				throw new QueryException("Iterator query must be a SELECT query."
						+ "with explicit columns corresponding to the construct query wildcards.",e);
			}
		}
		
		/**
		 * Parse the construct query and replace all wildcards by their respective seed elements
		 * If no seedElements HashMap is supplied (=null), test parsing the query with blank nodes instead.
		 * @param query_string
		 * @param seedElements
		 * @return
		 * 			The parsed query.
		 * @throws QueryException
		 * 			if parsing fails.
		 */
		public Query parseConstructQuery(String query_string, HashMap<String, String> seedElements) throws QueryException {
			LOG.debug("Attempting to read query string: \n" + query_string);
			
			if (seedElements==null) {
				// replace all wildcards by blank nodes (Test parsing)
				query_string = query_string.replaceAll("<\\?.+?>", "<http://test>");
			} else {
				// replace all wildcards with their respective seedElements
				for (String var:seedElements.keySet()) {
					query_string = query_string.replace("<?"+var+">", "<"+seedElements.get(var)+">");
				}
			}
			
			Query query = QueryFactory.create(query_string);
			if (query.isConstructType() || query.isDescribeType()) {
				return query;
			} else {
				throw new QueryException("Construct query must be either CONSTRUCT or DESCRIBE type.");
			}
		}
		
		/**
		 * Parse update query. 
		 * 
		 * @param query_string 
		 * 			Must be a valid SPARQL Update (INSERT / DELETE)
		 * @return	
		 * 			The parsed update.
		 * @throws QueryException	
		 * 			if parsing fails.
		 */
		public UpdateRequest parseUpdate(String query_string) {
			LOG.debug("Attempting to read update string: \n" + query_string);
			UpdateRequest update = UpdateFactory.create(query_string);
			return update;
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
			try {
				//parse / test all queries on init
				Query iteratorQuery = null;
				UpdateRequest recursiveUpdate = null;
				UpdateRequest initUpdate = null;
				if (mode == SplitterMode.ITERATE_CONSTRUCT) {
					iteratorQuery = parseIteratorQuery(this.iteratorQuery);
					parseConstructQuery(this.constructQuery, null);
				} else if (mode == SplitterMode.RECURSIVE_UPDATE) {
					recursiveUpdate = parseUpdate(this.recursiveUpdate);
				}
				if (this.initUpdate != null)
					initUpdate = parseUpdate(this.initUpdate);

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

				if (initUpdate != null) {
					tdbDataset.begin(ReadWrite.WRITE);
					for(Update operation : initUpdate.getOperations()) {
						UpdateAction.execute(operation, tdbDataset);
					}
					tdbDataset.commit();
					tdbDataset.end();
				}
				
				if (mode == SplitterMode.ITERATE_CONSTRUCT) {
					executeIterateConstruct(iteratorQuery);
				} else if (mode == SplitterMode.RECURSIVE_UPDATE) {
					executeRecursiveUpdates(recursiveUpdate);
				}

			} finally {
				tdbDataset.close();
				for (String name:listOutputStreamNames()) {
					getOutputStream(name).terminate();
				}
			}
			
			
		}

		//constructQuery has to be reparsed for each iteration, and is not supplied
		private void executeIterateConstruct(Query iteratorQuery) {
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
				Query constructQuery = parseConstructQuery(this.constructQuery, seedElements);
				
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
							if (getOutputStream(name) == null) {
								LOG.info("Input stream '"+name+"' does not have a corresponding output stream and is thus dropped.");
								continue;
							}
							getOutputStream(name).write(resultDataset.getNamedModel(name));
						} catch (InterruptedException e) {
							LOG.error("Error when processing stream "+name+ ": " +e);
						}
					}
				}

				//for CONSTRUCT query the default graph has been written in the resultModel
				//for DESCRIBE query only default graph is needed and directly supplied as resultModel
				if (resultModel != null) {
					try {
						getOutputStream().write(resultModel);
					} catch (InterruptedException e) {
						LOG.error("Error when processing default output stream: " +e);
					}
				}
			}
			tdbDataset.end();
		}
		
		
		private void executeRecursiveUpdates(UpdateRequest recursiveUpdate) {
			
			//first: recursive Updates
			while (doRecursiveUpdate(recursiveUpdate)) {
				//do nothing else
			}
			//then: terminate (after optionally supplying delta)
			if (getDeltaStreams() == null) 
				return;

			
			for (String name:getDeltaStreams()) {

				//supplies a new in-memory model with the data from the TDB's model.
				Model m = popStreamableModel(name);

				if (m != null) {
					try {
						getOutputStream(name).write(m);
					} catch (InterruptedException e) {
						LOG.error("Error when writing graph <"+name+"> to delta stream: " +e);
					}
				} else {
					LOG.info("Delta for stream <"+name+"> was empty.");
				}
			}

			
		}
		
		//executes one Update recursion
		private boolean doRecursiveUpdate(UpdateRequest update) {
			tdbDataset.begin(ReadWrite.WRITE);
			for(Update operation : update.getOperations()) {
				UpdateAction.execute(operation, tdbDataset);
			}
			tdbDataset.commit();
			tdbDataset.end();
			
			
			boolean changed = false;
			
			for (String name:getSegmentStreams()) {

				//supplies a new in-memory model with the data from the TDB's model.
				Model m = popStreamableModel(name);
				
				if (m != null) {
					try {
						getOutputStream(name).write(m);
					} catch (InterruptedException e) {
						LOG.error("Error when processing stream "+name+ ": " +e);
					}
					changed = true;
				}
			}

			return changed;
		}

		//supplies a new in-memory model with the data from the TDB's model.
		//deletes the Model from the Dataset.
		private Model popStreamableModel(String name) {
			if (getOutputStream(name) == null) {
				LOG.error("Output is dropped, since the specified "
						+ "output stream <"+name
						+ "> has not been connected to the Splitter.");
				return null;
			} 

			tdbDataset.begin(ReadWrite.WRITE);
			
			Model tdbModel;
			if (name == FINTAN_DEFAULT_STREAM_NAME) {
				tdbModel = tdbDataset.getDefaultModel();
			} else {
				tdbModel = tdbDataset.getNamedModel(name);
			}
			
			Model m;
			if (tdbModel.isEmpty())
				m = null;
			else {
				m = ModelFactory.createDefaultModel();
				//full prefixmap is stored in default graph.
				m.setNsPrefixes(tdbDataset.getDefaultModel().getNsPrefixMap());
				m.add(tdbModel);
				tdbModel.removeAll();
			}
			tdbDataset.commit();
			tdbDataset.end();
			
			return m;
			
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
				tdbDataset.close();
				System.exit(1);
			}
		}




}
