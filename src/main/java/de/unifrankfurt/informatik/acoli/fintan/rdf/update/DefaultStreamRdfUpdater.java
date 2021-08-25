package de.unifrankfurt.informatik.acoli.fintan.rdf.update;


import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.zip.GZIPInputStream;

import org.apache.jena.query.*;
import org.apache.jena.update.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.rdf.listeners.ChangedListener;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import de.unifrankfurt.informatik.acoli.fintan.core.FintanStreamComponent;
import de.unifrankfurt.informatik.acoli.fintan.core.FintanStreamComponentFactory;
import de.unifrankfurt.informatik.acoli.fintan.core.StreamRdfUpdater;

public class DefaultStreamRdfUpdater extends StreamRdfUpdater implements FintanStreamComponentFactory {



	@Override
	public FintanStreamComponent buildFromJsonConf(ObjectNode conf) {
		// READ THREAD PARAMETERS
				int threads = 0;
				if (conf.get("threads") != null)
					threads = conf.get("threads").asInt(0);
				DefaultStreamRdfUpdater updater = new DefaultStreamRdfUpdater("","",threads);

				// READ LOOKAHEAD PARAMETERS
				if (conf.get("lookahead") != null) {
					int lookahead_snts = conf.get("lookahead").asInt(0);
					if (lookahead_snts > 0)
						updater.activateLookahead(lookahead_snts);
				}

				// READ LOOKBACK PARAMETERS
				if (conf.get("lookback") != null) {
					int lookback_snts = conf.get("lookback").asInt(0);
					if (lookback_snts > 0)
						updater.activateLookback(lookback_snts);
				}

				// READ ALL UPDATES
				// should be <#UPDATEFILENAMEORSTRING, #UPDATESTRING, #UPDATEITER>
				List<Triple<String, String, String>> updates = new ArrayList<Triple<String, String, String>>();
				for (JsonNode update:conf.withArray("updates")) {
					String freq = update.get("iter").asText("1");
					if (freq.equals("u"))
						freq = "*";
					try {
						Integer.parseInt(freq);
					} catch (NumberFormatException e) {
						if (!"*".equals(freq))
							throw e;
					}
					String path = update.get("path").asText();
					updates.add(new Triple<String, String, String>(path, path, freq));
				}
				try {
					updater.parseUpdates(updates);
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					LOG.error("Error when parsing Updates", e1);
				}

				// READ ALL MODELS
				for (JsonNode model:conf.withArray("models")) {
					List<String> models = new ArrayList<String>();
					String uri = model.get("source").asText();
					if (!uri.equals("")) models.add(uri);
					uri = model.get("graph").asText();
					if (!uri.equals("")) models.add(uri);
					if (models.size()==1) {
						try {
							updater.loadGraph(new URI(models.get(0)), new URI(models.get(0)));
						} catch (Exception e) {
							LOG.error("Error when reading Model at " + models.get(0), e);
							System.exit(1);
						}
					} else if (models.size()==2){
						try {
							updater.loadGraph(new URI(models.get(0)), new URI(models.get(1)));
						} catch (Exception e) {
							LOG.error("Error when reading Model at " + models.get(0), e);
							System.exit(1);
						}
					} else if (models.size()>2){
						LOG.error("Error while loading model: Please specify model source URI and graph destination.");
						System.exit(1);
					}
					models.removeAll(models);
				}

				return updater;
	}

	
	@Override
	public FintanStreamComponent buildFromCLI(String[] args) throws IOException, IllegalArgumentException {
		// TODO Auto-generated method stub
		return null;
	}
	
	protected static final List<Integer> CHECKINTERVAL = new ArrayList<Integer>() {{add(3); add(10); add(25); add(50); add(100); add(200); add(500);}};
	protected static final Logger LOG = LogManager.getLogger(DefaultStreamRdfUpdater.class.getName());
	protected static final int MAXITERATE = 999; // maximum update iterations allowed until the update loop is cancelled and an error message is thrown - to prevent faulty update scripts running in an endless loop
	protected static final String DEFAULTUPDATENAME = "DIRECTUPDATE";
	
	public static class Pair<F, S> {
		public F key;
		public S value;
		public Pair (F key, S value) {
			this.key = key;
			this.value = value;
		}
	}
	
	public static class Triple<F, S, M> {
		public F first;
		public S second;
		public M third;
		public Triple (F first, S second, M third) {
			this.first = first;
			this.second = second;
			this.third = third;
		}
	}


	protected boolean running;
	
	protected final Dataset dataset;
	
	//for updates
	protected final List<Triple<String, String, String>> updates;
	
	//for thread handling
	protected final List<DefaultStreamRdfUpdaterThread> updaterThreads;
	
	//Buffer for outputting Models in original order
	///KEY == Thread ID... IF <0, then execution finished!   /TODO::: CHECKKKK
	protected final List<Pair<Integer, Model>> outputBuffer; 
	
	// Buffer providing each thread with its respective sentence(s) to process
	// <List:lookbackBuffer>, <String:currentSentence>, <List:lookaheadBuffer>
	protected final List<Triple<List<Model>, Model, List<Model>>> threadBuffer; 


	//For lookahead
	protected final List<Model> lookaheadBuffer; 
	private int lookahead_chunks = 0;
	
	//For lookback
	private final List<Model> lookbackBuffer; 
	protected int lookback_chunks = 0;
	
	//for statistics
	protected final List<List<Pair<Integer,Long>>> dRTs; // iterations and execution time of each update in seconds
	//private int parsedSentences = 0; // no longer used since graphsout default is set at sentence readin

	/**
	 * Default Constructor providing empty data to the standard constructor.
	 */
	public DefaultStreamRdfUpdater() {
		this("", "", 0);
	}
	
	/**
	 * Standard Constructor for Updater. Creates Threads and Buffers for Thread handling.
	 * Also creates the database modules for the respective execution modes.
	 * @param type: The type of database to be used:
	 * 				MEM: fully independent in-memory datasets per thread 
	 * 						(fastest, no transactions, high RAM usage, no HDD)
	 * 				TXN: single transactional in-memory dataset for all threads
	 * 						(in development, medium speed and RAM, no HDD)
	 * 				TDB2: single transactional TDB2-database for all threads
	 * 						(in development, slow-medium speed, low RAM usage, high HDD usage)
	 * 				default: MEM
	 * @param path: 
	 * 				path to database (only for TDB2 or other DB-backed modes)
	 * @param threads
	 * 				Maximum amount of threads for execution.
	 * 				default: threads = number of logical cores available to runtime
	 */
	public DefaultStreamRdfUpdater(String type, String path, int threads) {
		if (type == "TDB2") {
			//TODO
			dataset = DatasetFactory.create();//TDB
		} else if (type == "TXN") {
			dataset = DatasetFactory.createTxnMem();
		} else {
			dataset = DatasetFactory.createTxnMem();
		}
//		memAccessor = DatasetAccessorFactory.create(memDataset);
		
		//updates
		updates = Collections.synchronizedList(new ArrayList<Triple<String, String, String>>());
		
		//threads
		// Use the processor cores available to runtime (but at least 1) as thread count, if an invalid thread count is provided.
		if (threads <= 0) {
			threads = (Runtime.getRuntime().availableProcessors()>0)?(Runtime.getRuntime().availableProcessors()):(1);
			LOG.info("Falling back to default thread maximum.");
		}
		LOG.info("Executing on "+threads+" processor cores, max.");
		updaterThreads = Collections.synchronizedList(new ArrayList<DefaultStreamRdfUpdaterThread>());
		threadBuffer = Collections.synchronizedList(new ArrayList<Triple<List<Model>, Model, List<Model>>>());
		dRTs = Collections.synchronizedList(new ArrayList<List<Pair<Integer,Long>>>());
		for (int i = 0; i < threads; i++) {
			updaterThreads.add(null);
			dataset.addNamedModel("http://thread"+i, ModelFactory.createDefaultModel());
			threadBuffer.add(new Triple<List<Model>, Model, List<Model>>(
					new ArrayList<Model>(), ModelFactory.createDefaultModel(), new ArrayList<Model>()));
			dRTs.add(Collections.synchronizedList(new ArrayList<Pair<Integer,Long> >()));
		}
		outputBuffer = Collections.synchronizedList(new ArrayList<Pair<Integer,Model>>());
		
		//lookahead+lookback
		lookaheadBuffer = Collections.synchronizedList(new ArrayList<Model>());
		lookbackBuffer = Collections.synchronizedList(new ArrayList<Model>());
		
		//runtime
		//parsedSentences = 0;
		running = false;
	}

	/**
	 * Activates the lookahead mode for caching a fixed number of additional sentences per thread.
	 * @param lookahead_chunks
	 * 			the number of additional sentences to be cached
	 */
	public void activateLookahead(int lookahead_chunks) {
		if (lookahead_chunks < 0) lookahead_chunks = 0;
		this.lookahead_chunks = lookahead_chunks;
	}
	
	/**
	 * Activates the lookback mode for caching a fixed number of preceding sentences per thread.
	 * @param lookback_chunks
	 * 			the number of preceding sentences to be cached
	 */
	public void activateLookback(int lookback_chunks) {
		if (lookback_chunks < 0) lookback_chunks = 0;
		this.lookback_chunks = lookback_chunks;
	}
	

	/**
	 * Load external RDF file into a named graph of the local dataset. 
	 * This graph is permanent for the runtime and is accessed read-only by all threads.
	 * The default graph of the local dataset is reserved for updating nif:Sentences and 
	 * can not be defined here.
	 * @param url
	 * 			location of the RDF file to be loaded
	 * @param graph (optional)
	 * 			the named graph to load the data into.
	 * 			default: graph = url
	 * @throws IOException
	 */
	public void loadGraph(URI url, URI graph) throws IOException {
		LOG.info("loading...");
		LOG.info(url +" into "+ graph);
		if (!url.isAbsolute()) {
			url = (new File(url.toString())).toURI();
		}
		if (graph == null) {
			graph = url;
		}
		Model m = ModelFactory.createDefaultModel();
		try {
			m.read(readInURI(url));
			dataset.addNamedModel(graph.toString(), m);
		} catch (IOException ex) {
			LOG.error("Exception while reading " + url + " into " + graph);
			throw ex;
		}
		LOG.info("done...");
	}
	
	/**
	 * Define a set of updates to be executed for each sentence processed by this CoNLLRDFUpdater.
	 * Existing updates will be overwritten by calling this function.
	 * @param updatesRaw
	 * 			The new set of updates as a List of String Triples. Each Triple has the following form:
	 * 			<Name of Update>, <update script>OR<path to script>, <iterations>
	 * @throws IOException
	 */
	public void parseUpdates(List<Triple<String, String, String>> updatesRaw) throws IOException { 
		this.updates.clear();
		
		List<Triple<String, String, String>> updatesTemp = new ArrayList<Triple<String, String, String>>();
		updatesTemp.addAll(updatesRaw);
		
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i<updatesTemp.size(); i++) {
			Reader sparqlreader = new StringReader(updatesTemp.get(i).second);
			File f = new File(updatesTemp.get(i).second);
			URL u = null;
			try {
				u = new URL(updatesTemp.get(i).second);
			} catch (MalformedURLException e) {}
			if(f.exists()) {			// can be read from a file 
				try {
					sparqlreader = new FileReader(f);
					sb.append("f");
				} catch (Exception e) {}
			} else if(u!=null) {
				try {
					sparqlreader = new InputStreamReader(u.openStream());
					sb.append("u");
				} catch (Exception e) {
				}
			}
			
			String updateName = updatesTemp.get(i).first;
			// check for String as Update and set update name to default
			if (!(sparqlreader instanceof FileReader) && !(sparqlreader instanceof InputStreamReader)) {
				updateName = DEFAULTUPDATENAME;
			}
			
			updatesTemp.set(i,new Triple<String, String, String>(updateName, "", updatesTemp.get(i).third)); // TODO: Needed? just removes the second string, but second is overwritten later anyway....
			BufferedReader in = new BufferedReader(sparqlreader);
			String updateBuff = "";
			for(String line = in.readLine(); line!=null; line=in.readLine())
				updateBuff = updateBuff + line + "\n";
			isValidUTF8(updateBuff, "SPARQL update String is not UTF-8 encoded for " + updatesTemp.get(i).first);
			try {
				@SuppressWarnings("unused")
				UpdateRequest qexec = UpdateFactory.create(updateBuff);
				updatesTemp.set(i,new Triple<String, String, String>(updatesTemp.get(i).first, updateBuff, updatesTemp.get(i).third));
			} catch (QueryParseException e) {
				LOG.error("SPARQL parse exception for Update No. "+i+": "+updateName+"\n" + e + "\n" + updateBuff); // this is SPARQL code with broken SPARQL syntax
				try {
					@SuppressWarnings("unused")
					Path ha = Paths.get(updateBuff.trim());
				} catch (InvalidPathException d) {
					LOG.error("SPARQL parse exception for:\n" + updateBuff); // this is SPARQL code with broken SPARQL syntax
					System.exit(1);
				}
				LOG.error("File not found exception for (Please note - if update is passed on as a String is has to be in `-quotes!): " + updatesTemp.get(i).first); // this is a faulty SPARQL script file path - if you have written a valid path into your SPARQL script file, it is your own fault
				System.exit(1);
			}				
			updatesTemp.set(i,new Triple<String, String, String>(updatesTemp.get(i).first, updateBuff, updatesTemp.get(i).third));
			sb.append(".");
		}
		
		this.updates.addAll(Collections.synchronizedList(updatesTemp));
		LOG.debug(sb.toString());

	}
	
	/**
	 * Tries to read from a specific URI.
	 * Tries to read content directly or from GZIP
	 * Validates content against UTF-8.
	 * @param uri
	 * 		the URI to be read
	 * @return
	 * 		the text content
	 * @throws MalformedURLException
	 * @throws IOException
	 */
	protected static String readInURI(URI uri) throws MalformedURLException, IOException {
		String result = null;
		try {
			result = uri.toString();
			if (result != null && result.endsWith(".gz")) {
				StringBuilder sb = new StringBuilder();
				BufferedReader br = new BufferedReader(new InputStreamReader(new GZIPInputStream(uri.toURL().openStream())));
				for (String line; (line = br.readLine()) != null; sb.append(line));
				result = sb.toString();
				isValidUTF8(result, "Given URI input (" + uri.getPath() + ") is not UTF-8 encoded");
			}
		} catch (Exception ex) {
			LOG.error("Excpetion while reading " + uri.getPath());
			throw ex;
		}
		return result;
	}
	
	protected static void isValidUTF8(String s, String message) {
		try 
		{
			s.getBytes("UTF-8");
		} 
		catch (UnsupportedEncodingException e)
		{
		    LOG.error(message + " - Encoding error: " + e.getMessage());
		    System.exit(-1);
		}		
	}

	/**
	 * Processes CoNLL-RDF on the local dataset using the predfined updates and threads.
	 * Streams data from a buffered reader to a buffered writer. Distributes the processing 
	 * across available threads. Each thread handles one sentence at a time.
	 * Caches and outputs the resulting sentences in-order.
	 * @param in
	 * 			the input stream wrapped in a BufferedReader
	 * @param out
	 * 			the output stream wrapped in a PrintStream
	 * @throws IOException
	 * 			if impossible to read from Stream
	 * @throws ClassNotFoundException 
	 * 			if Stream contains Objects of a class which is not in ClassPath
	 */
	public void processStream() throws IOException, ClassNotFoundException {
		running = true;
		
		Model model = ModelFactory.createDefaultModel();
		
		while ((model = (Model) getInputStream().readObject())!=null) {
				
			//lookahead
			//add ALL sentences to sentBufferLookahead
			lookaheadBuffer.add(model);
			if (lookaheadBuffer.size() > lookahead_chunks) {
				//READY TO PROCESS 
				// remove first sentence from buffer and process it.
				// !!if lookahead = 0 then only current buffer is in sentBufferLookahead!!
				executeThread(lookaheadBuffer.remove(0));
			}		
			
			//lookback
			//needs to consider lookahead buffer. The full buffer size needs to be lookahead + lookback.
			if (lookback_chunks > 0) {
				while (lookbackBuffer.size() >= lookback_chunks + lookaheadBuffer.size()) lookbackBuffer.remove(0);
				lookbackBuffer.add(model);
			}
			
			flushOutputBuffer(getOutputStream());

		}
		
		//lookahead
		//work down remaining buffer
		while (lookaheadBuffer.size()>0) {
			executeThread(lookaheadBuffer.remove(0));
			if (lookback_chunks > 0) {
				while (lookbackBuffer.size() >= lookback_chunks + lookaheadBuffer.size()) lookbackBuffer.remove(0);
			}
			flushOutputBuffer(getOutputStream());
		}
			
		
		//wait for threads to finish work
		boolean threadsRunning = true;
		while(threadsRunning) {
			threadsRunning = false;
			for (DefaultStreamRdfUpdaterThread t:updaterThreads) {
				if (t != null)
				if (t.getState() == Thread.State.RUNNABLE || t.getState() == Thread.State.BLOCKED) {
					threadsRunning = true;
				}
			}
		}
		//terminate all threads
		running = false;
		for (DefaultStreamRdfUpdaterThread t:updaterThreads) {
			if (t != null)
			if(t.getState() == Thread.State.NEW) {
				t.start(); //in case of spontaneous resurrection, new threads should not have any work to do at this point
			} else if (!(t.getState() == Thread.State.TERMINATED)) {
				synchronized(t) {
					t.notify();
				}
			}
		}
		
		//sum up statistics
		List<Pair<Integer,Long>> dRTs_sum = new ArrayList<Pair<Integer,Long> >();
		for (List<Pair<Integer,Long>> dRT_thread:dRTs) {
			if (dRTs_sum.isEmpty())
				dRTs_sum.addAll(dRT_thread);
			else
				for (int x = 0; x < dRT_thread.size(); ++x)
					dRTs_sum.set(x, new Pair<Integer, Long>(
							dRTs_sum.get(x).key + dRT_thread.get(x).key, 
							dRTs_sum.get(x).value + dRT_thread.get(x).value));
			
		}
		if (!dRTs_sum.isEmpty())
			LOG.debug("Done - List of iterations and execution times for the updates done (in given order):\n\t\t" + dRTs_sum.toString());

		//final flush
		flushOutputBuffer(getOutputStream());
		getOutputStream().close();
		
	}

	private synchronized void flushOutputBuffer(ObjectOutputStream out) throws IOException {
		LOG.trace("OutBufferSIze: "+outputBuffer.size());
		while (!outputBuffer.isEmpty()) {
			if (outputBuffer.get(0).key>=0) break;   ///KEY == Thread ID... IF <0, then finished!
			out.writeObject(outputBuffer.remove(0).value);
		}
	}

	private void executeThread(Model processedModel) {
		Triple<List<Model>, Model, List<Model>>modelsForThread = 
				new Triple<List<Model>, Model, List<Model>>(
				new ArrayList<Model>(), processedModel, new ArrayList<Model>());
		
		//sentBufferLookback only needs to be filled up to the current sentence. 
		//All other sentences are for further lookahead iterations 
//		sentBufferThread.first.addAll(sentBufferLookback);
		for (int i = 0; i < lookbackBuffer.size() - lookaheadBuffer.size(); i++) {
			modelsForThread.first.add(lookbackBuffer.get(i));
		}
		modelsForThread.second = processedModel;
		modelsForThread.third.addAll(lookaheadBuffer);
		int i = 0;
		while(i < updaterThreads.size()) {
			LOG.trace("ThreadState " + i + ": "+((updaterThreads.get(i)!=null)?updaterThreads.get(i).getState():"null"));
			if (updaterThreads.get(i) == null) {
				threadBuffer.set(i, modelsForThread);
				outputBuffer.add(new Pair<Integer, Model>(i,ModelFactory.createDefaultModel())); //add current thread to the end of the output queue.
				updaterThreads.set(i, new DefaultStreamRdfUpdaterThread(this, i));
				updaterThreads.get(i).start();
				LOG.trace("restart "+i);
				LOG.trace("OutBufferSize: "+outputBuffer.size());
				break;
			} else 
				if (updaterThreads.get(i).getState() == Thread.State.WAITING) {
				synchronized(updaterThreads.get(i)) {
				threadBuffer.set(i, modelsForThread);
				outputBuffer.add(new Pair<Integer, Model>(i,ModelFactory.createDefaultModel())); //add current thread to the end of the output queue.
				updaterThreads.get(i).notify();
				}
				LOG.trace("wake up "+i);
				break;
			} else 
				if (updaterThreads.get(i).getState() == Thread.State.NEW) {
				threadBuffer.set(i, modelsForThread);
				outputBuffer.add(new Pair<Integer, Model>(i,ModelFactory.createDefaultModel())); //add current thread to the end of the output queue.
				updaterThreads.get(i).start();
				LOG.trace("start "+i);
				LOG.trace("OutBufferSize: "+outputBuffer.size());
				break;
			} else 
				if (updaterThreads.get(i).getState() == Thread.State.TERMINATED) {
				threadBuffer.set(i, modelsForThread);
				outputBuffer.add(new Pair<Integer, Model>(i,ModelFactory.createDefaultModel())); //add current thread to the end of the output queue.
				updaterThreads.set(i, new DefaultStreamRdfUpdaterThread(this, i));
				updaterThreads.get(i).start();
				LOG.trace("restart "+i);
				LOG.trace("OutBufferSize: "+outputBuffer.size());
				break;
			}
			
			i++;
			if (i >= updaterThreads.size()) {
				try {
					synchronized (this) {
						LOG.trace("Updater waiting");
						wait(20);
					}
				} catch (InterruptedException e) {
					LOG.error(e, e);
				} finally {
					i = 0;
				}
			}
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
