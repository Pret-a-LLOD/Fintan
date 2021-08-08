package de.unifrankfurt.informatik.acoli.fintan.load;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.StringReader;

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
 * Output: Stream of Models.
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
		//TODO: read from config: which streams to pass to which output?.
		for (String name:listInputStreamNames()) {
			if (getOutputStream(name) == null) {
				LOG.info("Input stream '"+name+"' does not have a corresponding output stream and is thus dropped.");
				continue;
			}
			BufferedReader in = new BufferedReader(new InputStreamReader(getInputStream(name)));
			String ttlsegment = "";
			try {
				for(String line = ""; line !=null; line=in.readLine()) {
					if (line.equals(segmentDelimiter)) {
						outputSegment(ttlsegment, name);
						
						ttlsegment = "";
					} else {
						ttlsegment+=line+"\n";
					}
				}
				//final segment in case there is no segmentDelimiter in last row
				outputSegment(ttlsegment, name);
			} catch (IOException e) {
				LOG.error("Error when reading from Stream "+name+ ": " +e);
			}
		}
	}
	
	private void outputSegment(String ttlsegment, String outputStreamName) {
		Model m = ModelFactory.createDefaultModel();
		m.read(new StringReader(ttlsegment), null, lang);
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
