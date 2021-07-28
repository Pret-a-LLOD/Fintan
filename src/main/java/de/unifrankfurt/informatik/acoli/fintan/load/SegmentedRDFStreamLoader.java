package de.unifrankfurt.informatik.acoli.fintan.load;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.unifrankfurt.informatik.acoli.fintan.core.FintanStreamComponent;
import de.unifrankfurt.informatik.acoli.fintan.core.StreamLoader;

/**
 * Load pre-segmented RDF streams in the given serialization format. Default: TTL
 * Output: Stream of Models.
 * @author CF
 *
 */
public class SegmentedRDFStreamLoader extends StreamLoader {
	
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
							Model m = ModelFactory.createDefaultModel();
							m.read(new StringReader(ttlsegment), null, lang);
							try {
								getOutputStream(name).writeObject(m);
								getOutputStream(name).flush();
							} catch (IOException e) {
								LOG.error("Error when writing to Stream "+name+ ": " +e);
							}
							ttlsegment = "";
						} else {
							ttlsegment+=line+"\n";
						}
					}
				} catch (IOException e) {
					LOG.error("Error when reading from Stream "+name+ ": " +e);
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
