package de.unifrankfurt.informatik.acoli.fintan.write;

import java.io.IOException;
import java.io.PrintStream;

import org.apache.jena.rdf.model.Model;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.node.ObjectNode;

import de.unifrankfurt.informatik.acoli.fintan.core.FintanStreamComponentFactory;
import de.unifrankfurt.informatik.acoli.fintan.core.StreamWriter;

public class RDFStreamWriter extends StreamWriter implements FintanStreamComponentFactory {
	
	@Override
	public RDFStreamWriter buildFromJsonConf(ObjectNode conf) throws IOException, IllegalArgumentException {
		RDFStreamWriter writer = new RDFStreamWriter();
		writer.setConfig(conf);
		if (conf.has("lang")) {
			writer.setLang(conf.get("lang").asText());
		}
		if (conf.has("delimiter")) {
			writer.setSegmentDelimiter(conf.get("delimiter").asText());
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
		for (String name:listInputStreamNames()) {
			if (getOutputStream(name) == null) {
				LOG.info("Input stream '"+name+"' does not have a corresponding output stream and is thus dropped.");
				continue;
			}
			PrintStream out = new PrintStream(getOutputStream(name));
			while (getInputStream(name).canRead()) {
				try {
					Model m = getInputStream(name).read();
					m.write(out, lang);
				} catch (InterruptedException e) {
					LOG.error("Error when reading from Stream "+name+ ": " +e);
				}
				if (segmentDelimiter != null) {
					out.println(segmentDelimiter);
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