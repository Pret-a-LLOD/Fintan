package de.unifrankfurt.informatik.acoli.fintan.load;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Map;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.node.ObjectNode;

import de.unifrankfurt.informatik.acoli.fintan.core.FintanStreamComponent;
import de.unifrankfurt.informatik.acoli.fintan.core.FintanStreamComponentFactory;
import de.unifrankfurt.informatik.acoli.fintan.core.StreamLoader;

/**
 * Load pre-segmented RDF streams in the given serialization format. Default: TTL
 * For convenience, Fintan's treatment of named streams works in default configuration:
 * 	- for each matching pair of named streams, a separate Thread is spawned 
 * 		which converts the stream independently
 *  - input streams without matching outputstreams are dropped.
 * @author CF
 *
 */
public class SegmentedRDFStreamLoader extends StreamLoader implements FintanStreamComponentFactory{

	//Factory methods
	
	@Override
	public SegmentedRDFStreamLoader buildFromJsonConf(ObjectNode conf) throws IOException, IllegalArgumentException {
		SegmentedRDFStreamLoader loader = new SegmentedRDFStreamLoader();
		loader.setConfig(conf);
		if (conf.hasNonNull("lang")) {
			loader.setLang(conf.get("lang").asText());
		}
		if (conf.hasNonNull("delimiter")) {
			loader.setSegmentDelimiter(conf.get("delimiter").asText());
		}
		return loader;
	}

	@Override
	public SegmentedRDFStreamLoader buildFromCLI(String[] args) throws IOException, IllegalArgumentException {
		// TODO Auto-generated method stub
		return null;
	}

	protected static final Logger LOG = LogManager.getLogger(SegmentedRDFStreamLoader.class.getName());


	private String lang = "TTL";
	private String segmentDelimiter = FINTAN_DEFAULT_SEGMENT_DELIMITER_TTL;
	private Map<String, String> prefixMap;

	public String getLang() {
		return lang;
	}

	public void setLang(String lang) {
		this.lang = lang;
	}

	public String getSegmentDelimiter() {
		return segmentDelimiter;
	}

	public void setSegmentDelimiter(String segmentDelimiter) {
		this.segmentDelimiter = segmentDelimiter;
	}


	private void processStream() {
		
		// Spawn loaders for parallel processing, in case there are multiple streams.
		for (String name:listInputStreamNames()) {
			if (name == FINTAN_DEFAULT_STREAM_NAME) 
				continue;
			if (getOutputStream(name) == null) {
				LOG.info("Input stream '"+name+"' does not have a corresponding output stream and is thus dropped.");
				continue;
			}
			
			SegmentedRDFStreamLoader loader = new SegmentedRDFStreamLoader();
			loader.setSegmentDelimiter(segmentDelimiter);
			loader.setLang(lang);
			loader.setInputStream(getInputStream(name));
			loader.setOutputStream(getOutputStream(name));
			new Thread(loader).start();
		}
		
		// process default stream
		BufferedReader in = new BufferedReader(new InputStreamReader(getInputStream()));
		String rdfsegment = "";
		try {
			for(String line = in.readLine(); line !=null; line=in.readLine()) {
				if (line.equals(segmentDelimiter)) {
					outputSegment(rdfsegment, "");

					rdfsegment = "";
				} else {
					rdfsegment+=line+"\n";
				}
			}
			//final segment in case there is no segmentDelimiter in last row
		} catch (IOException e) {
			LOG.error("Error when reading from Stream: " +e);
		}
		if (!rdfsegment.isBlank())
			outputSegment(rdfsegment, "");
		getOutputStream().terminate();
	}
	
	private void outputSegment(String rdfsegment, String outputStreamName) {
		Model m = ModelFactory.createDefaultModel();
		
		//TODO: find a better way to assess existence of prefixes. 
		//Exception handling may be slow.
		try {
			m.read(new StringReader(rdfsegment), null, lang);
		} catch (org.apache.jena.riot.RiotException e) {
			//probably missing prefixes.
			if (prefixMap != null) {
				m.setNsPrefixes(prefixMap);
				StringWriter prefixWright = new StringWriter();
				m.write(prefixWright, lang);
				rdfsegment = prefixWright + rdfsegment;
			}
			m.read(new StringReader(rdfsegment), null, lang);
		}
		prefixMap = m.getNsPrefixMap();
		try {
			getOutputStream(outputStreamName).write(m);
		} catch (InterruptedException e) {
			LOG.error("Error when writing to Stream "+outputStreamName+": "+e);
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
