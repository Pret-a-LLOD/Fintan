package de.unifrankfurt.informatik.acoli.fintan.rdf.update;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.apache.jena.query.*;
import org.apache.jena.update.*;

import de.unifrankfurt.informatik.acoli.fintan.rdf.update.DefaultStreamRdfUpdater.*;

import org.apache.jena.rdf.listeners.ChangedListener;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;

/**
 * Only gains access to his specific dataset.
 * Either forwards request to DBMS or executes on in-memory dataset.
 * 
 * @author Christian Faeth
 *
 */
public class DefaultStreamRdfUpdaterThread extends Thread {
	
	protected DefaultStreamRdfUpdater updater;
	protected int threadID;
	protected Dataset dataset;
	
	/**
	 * Each UpdateThread receives its own ID and a back-reference to the calling Updater.
	 * 
	 * In the current implementation, each thread manages its own in-memory Dataset.
	 * This is the fastest approach since no concurring access on a single Datasets occurs.
	 * However: lots of RAM may be needed.
	 * 
	 * @param updater
	 * 				The calling Updater (= ThreadHandler)
	 * @param id
	 * 				The id of this Thread.
	 */
	public DefaultStreamRdfUpdaterThread(DefaultStreamRdfUpdater updater, int id) {
		this.updater = updater;
		threadID = id;
		dataset = DatasetFactory.create();
		Iterator<String> iter = updater.dataset.listNames();
		while(iter.hasNext()) {
			String graph = iter.next();
			dataset.addNamedModel(graph, updater.dataset.getNamedModel(graph));
		}
		dataset.addNamedModel("http://lookback", ModelFactory.createDefaultModel());
		dataset.addNamedModel("http://lookahead", ModelFactory.createDefaultModel());
	}
	
	/**
	 * Run the update thread.
	 * Load the buffer, execute the updates with all iterations and graphsout, unload the buffer.
	 */
	public void run() {
		while (updater.running) {
			//Execute Thread

			DefaultStreamRdfUpdater.LOG.trace("NOW Processing on thread "+threadID+": outputbuffersize "+updater.outputBuffer.size());
			Triple<List<Model>, Model, List<Model>> sentBufferThread = updater.threadBuffer.get(threadID);
			StringWriter out = new StringWriter();
			try {
				loadBuffer(sentBufferThread);
				
				List<Pair<Integer,Long> > ret = executeUpdates(updater.updates);
				if (updater.dRTs.get(threadID).isEmpty())
					updater.dRTs.get(threadID).addAll(ret);
				else
					for (int x = 0; x < ret.size(); ++x)
						updater.dRTs.get(threadID).set(x, new Pair<Integer, Long>(
								updater.dRTs.get(threadID).get(x).key + ret.get(x).key, 
								updater.dRTs.get(threadID).get(x).value + ret.get(x).value));
				
				
				Model m = unloadBuffer();

				// synchronized write access to sentBuffer in order to avoid corruption
				synchronized(updater) {
					DefaultStreamRdfUpdater.LOG.trace("NOW PRINTING on thread "+threadID+": outputbuffersize "+updater.outputBuffer.size());
					for (int i = 0; i < updater.outputBuffer.size(); i++) {
						if (updater.outputBuffer.get(i).key.intValue()==threadID) {
							updater.outputBuffer.get(i).value=m;
							break;
						}
					}	
					
					//go to sleep and let Updater take control
					DefaultStreamRdfUpdater.LOG.trace("Updater notified by "+threadID);
					updater.notify();
				}
			
			} catch (Exception e) {
//				memDataset.begin(ReadWrite.WRITE);
				dataset.getDefaultModel().removeAll();
				dataset.getNamedModel("http://lookback").removeAll();
				dataset.getNamedModel("http://lookahead").removeAll();
//				memDataset.commit();
//				memDataset.end();
				DefaultStreamRdfUpdater.LOG.error(e, e);
//				continue;
			}
			try {
				synchronized (this) {
					DefaultStreamRdfUpdater.LOG.trace("Waiting: "+threadID);
					wait();
				}
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				DefaultStreamRdfUpdater.LOG.error(e, e);
			}
		}
	}
	
	/**
	 * Loads Data to this thread's working model.
	 * @param buffer 
	 * 			the model to be read.
	 * @throws Exception
	 */
	private void loadBuffer(Triple<List<Model>, Model, List<Model>> sentBufferThread) throws Exception { //TODO: adjust for TXN-Models
		//load ALL
		try {
//			memDataset.begin(ReadWrite.WRITE);
			
			// for lookback
			for (Model m:sentBufferThread.first) {
				dataset.getNamedModel("http://lookback").add(m);
			}
			
			// for current sentence
			dataset.getDefaultModel().add(sentBufferThread.second);
			
			// for lookahead
			for (Model m:sentBufferThread.third) {
				dataset.getNamedModel("http://lookahead").add(m);
			}
			
//			memDataset.commit();
//			Model m = ModelFactory.createDefaultModel().read(new StringReader(buffer),null, "TTL");
//			memAccessor.add(m);
//			memDataset.getDefaultModel().setNsPrefixes(m.getNsPrefixMap());
		} catch (Exception ex) {
			DefaultStreamRdfUpdater.LOG.error("Exception while reading: " + sentBufferThread.second);
			throw ex;
		} finally {
//			memDataset.end();
		}
		
	}

	/**
	 * Unloads Data from this thread's working model.
	 * Includes comments from original data.
	 * @param buffer
	 * 			Original data for extracting comments.
	 * @param out
	 * 			Output Writer.
	 * @throws Exception
	 */
	private Model unloadBuffer() throws Exception { //TODO: adjust for TXN-Models
		Model m = ModelFactory.createDefaultModel();
		try {
			m.add(dataset.getDefaultModel().removeAll());
		} catch (Exception e) {
//			memDataset.abort();
			DefaultStreamRdfUpdater.LOG.error("Exception while unloading Thread: " + threadID);
			DefaultStreamRdfUpdater.LOG.error(e, e);
		} finally {
//			memDataset.begin(ReadWrite.WRITE);
			dataset.getDefaultModel().removeAll();
			dataset.getNamedModel("https://github.com/acoli-repo/conll-rdf/lookback").removeAll();
			dataset.getNamedModel("https://github.com/acoli-repo/conll-rdf/lookahead").removeAll();
//			memDataset.commit();
//			memDataset.end();
		}
		return m;
	}
	
	/**
	 * Executes updates on this thread. Data must be preloaded first.
	 * 
	 * @param updates
	 * 			The updates as a List of Triples containing
	 * 			- update filename
	 * 			- update script
	 * 			- number of iterations
	 * @return
	 * 			List of pairs containing Execution info on each update:
	 * 			- total no. of iterations
	 * 			- total time
	 */
	private List<Pair<Integer, Long>> executeUpdates(List<Triple<String, String, String>> updates) { //TODO: NOW:check consistency and check graphsout stepping

		String sent = new String();
		boolean graphsout = false;
		boolean triplesout = false;
		
		List<Pair<Integer,Long> > result = new ArrayList<Pair<Integer,Long> >();
		int upd_id = 1;
		int iter_id = 1;
		for(Triple<String, String, String> update : updates) {
			iter_id = 1;
			Long startTime = System.currentTimeMillis();
			Model defaultModel = dataset.getDefaultModel();
			ChangedListener cL = new ChangedListener();
			defaultModel.register(cL);
			String oldModel = "";
			int frq = DefaultStreamRdfUpdater.MAXITERATE, v = 0;
			boolean change = true;
			try {
				frq = Integer.parseInt(update.third);
			} catch (NumberFormatException e) {
				if (!"*".equals(update.third))
					throw e;
			}
			while(v < frq && change) {
				try {
					UpdateRequest updateRequest;
					updateRequest = UpdateFactory.create(update.second);
					if (graphsout || triplesout) { //execute Update-block step by step and output intermediate results
						int step = 1;
						Model dM = dataset.getDefaultModel();
						String dMS = dM.toString();
						ChangedListener cLdM = new ChangedListener();
						dM.register(cLdM);
						for(Update operation : updateRequest.getOperations()) {
							//							memDataset.begin(ReadWrite.WRITE);
							UpdateAction.execute(operation, dataset);
							//							memDataset.commit();
							//							memDataset.end();
							if (cLdM.hasChanged() && (!dMS.equals(dataset.getDefaultModel().toString()))) {
								//TODO: insert onChange operations.
							}
							step++;
						}
					} else { //execute updates en bloc
						//						memDataset.begin(ReadWrite.WRITE);
						UpdateAction.execute(updateRequest, dataset); //REMOVE THE PARAMETERS sent_id, upd_id, iter_id to use deshoe's original file names
						//						memDataset.commit();
						//						memDataset.end();
					}
				} catch (Exception e) {
					DefaultStreamRdfUpdater.LOG.error("Error while processing update No. "+upd_id+": "+update.first);
					DefaultStreamRdfUpdater.LOG.error(e, e);
				}
				
				
				if (oldModel.isEmpty()) {
					change = cL.hasChanged();
					DefaultStreamRdfUpdater.LOG.trace("cl.hasChanged(): "+change);
				} else {
					change = !defaultModel.toString().equals(oldModel);
					oldModel = "";
				}
				if (DefaultStreamRdfUpdater.CHECKINTERVAL.contains(v))
					oldModel = defaultModel.toString();
				v++;
				iter_id++;
			}
			if (v == DefaultStreamRdfUpdater.MAXITERATE)
				DefaultStreamRdfUpdater.LOG.warn("Warning: MAXITERATE reached for " + update.first + ".");
			result.add(new Pair<Integer, Long>(v, System.currentTimeMillis() - startTime));
			defaultModel.unregister(cL);
			upd_id++;
		}			
		return result;
	}
	
	
}
