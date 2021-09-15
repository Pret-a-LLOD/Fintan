package de.unifrankfurt.informatik.acoli.fintan.write;

import java.io.IOException;
import java.io.PrintStream;
import java.io.StringWriter;

import org.apache.jena.rdf.model.Model;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.node.ObjectNode;

import de.unifrankfurt.informatik.acoli.fintan.core.FintanStreamComponentFactory;
import de.unifrankfurt.informatik.acoli.fintan.core.StreamWriter;

/**
 * Writes FintanStreams to any provided RDF serialization.
 * For convenience, Fintan's treatment of named streams works in default configuration:
 * 	- for each matching pair of named streams, a separate Thread is spawned 
 * 		which converts the stream independently
 *  - input streams without matching output streams are dropped.
 * @author CF
 *
 */
public class RDFStreamWriter extends StreamWriter implements FintanStreamComponentFactory {
	
	@Override
	public RDFStreamWriter buildFromJsonConf(ObjectNode conf) throws IOException, IllegalArgumentException {
		RDFStreamWriter writer = new RDFStreamWriter();
		writer.setConfig(conf);
		if (conf.hasNonNull("lang")) {
			writer.setLang(conf.get("lang").asText());
		}
		if (conf.hasNonNull("delimiter")) {
			writer.setSegmentDelimiter(conf.get("delimiter").asText());
		}
		if (conf.hasNonNull("prefixDeduplication")) {
			writer.setPrefixDeduplication(conf.get("prefixDeduplication").asBoolean());
		}
		return writer;
	}

	@Override
	public RDFStreamWriter buildFromCLI(String[] args) throws IOException, IllegalArgumentException {
		// TODO Auto-generated method stub
		return null;
	}

	
	protected static final Logger LOG = LogManager.getLogger(RDFStreamWriter.class.getName());

	private String lang = "TTL";
	private String segmentDelimiter = null;
	private boolean prefixDeduplication = false;
	
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
	
	
	public boolean isPrefixDeduplication() {
		return prefixDeduplication;
	}

	public void setPrefixDeduplication(boolean prefixDeduplication) {
		this.prefixDeduplication = prefixDeduplication;
	}

	private void processStream() {
		// Spawn writers for parallel processing, in case there are multiple streams.
		for (String name:listInputStreamNames()) {
			if (name == FINTAN_DEFAULT_STREAM_NAME) 
				continue;
			if (getOutputStream(name) == null) {
				LOG.info("Input stream '"+name+"' does not have a corresponding output stream and is thus dropped.");
				continue;
			}

			RDFStreamWriter writer = new RDFStreamWriter();
			writer.setSegmentDelimiter(segmentDelimiter);
			writer.setLang(lang);
			writer.setPrefixDeduplication(prefixDeduplication);
			try {
				writer.setInputStream(getInputStream(name));
				writer.setOutputStream(getOutputStream(name));
			} catch (IOException e) {
				LOG.error(e, e);
				System.exit(1);
			}
			new Thread(writer).start();
		}
		
		//terminate in case there is no default stream. 
		//named streams are handled in subthreads.
		if (getOutputStream()==null) return;
		
		PrintStream out = new PrintStream(getOutputStream());
		String prefixCacheOut = new String();
		
		while (getInputStream().canRead()) {
			try {
				Model m = getInputStream().read();
				//read may return null in case the queue has been emptied and terminated since asking for canRead()
				if (m == null) continue;
				
				if (prefixDeduplication) {
					String outString = new String();
					StringWriter buffer = new StringWriter();
					m.write(buffer, lang);
					String prefixCacheTMP = new String();
					for (String buffLine:buffer.toString().split("\n")) {
						if (buffLine.trim().startsWith("@prefix")) {
							prefixCacheTMP += buffLine+"\n";
						} else if (!buffLine.trim().isEmpty()) {
								outString += buffLine+"\n";
						}
					}
					if (!prefixCacheTMP.equals(prefixCacheOut)) {
						prefixCacheOut = prefixCacheTMP;
						outString = prefixCacheTMP + outString;
					}
					out.print(outString);
				} else {
					m.write(out, lang);
				}
				
				if (segmentDelimiter != null) {
					out.println(segmentDelimiter);
				}
				
			} catch (InterruptedException e) {
				LOG.error("Error when reading from Stream: " +e);
			}
		}
		out.close();

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