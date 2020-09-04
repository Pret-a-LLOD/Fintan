package de.unifrankfurt.informatik.acoli.fintan.rdf.split;

import org.apache.jena.query.*;
import org.apache.jena.tdb.TDBFactory;

import de.unifrankfurt.informatik.acoli.fintan.core.StreamRdfSplitter;


/**
 * TODO: UNIMPLEMENTED YET
 * 
 * Reads input from file
 * loads to TDB
 * constructs subgraphs
 * writes subgraphs to outputstream
 * @author ranis
 *
 */
public class RdfSplitterTDB extends StreamRdfSplitter {
	
	protected final Dataset dataset;
	
	public RdfSplitterTDB(String dir) {
		this.dataset = TDBFactory.createDataset(dir);
	}

	
	

	/**
	 * Create in-memory Fuseki TDB dataset
	 * @param dir TDB directory 
	 */
	public RdfSplitterTDB() {
		super();
		this.dataset = TDBFactory.createDataset();
	}
	
	
	public ResultSet queryCopiedResults(String queryString) {
		
		 /*gr
		 Location location = ... ;
		 */
		
		 this.dataset.begin(ReadWrite.READ);
		 
		 try(QueryExecution qExec = QueryExecutionFactory.create(queryString, this.dataset)) {
		     ResultSet rs = qExec.execSelect();
		     return ResultSetFactory.copyResults(rs);
		 }
		 finally {
		 dataset.end();
		 }
	}
}
