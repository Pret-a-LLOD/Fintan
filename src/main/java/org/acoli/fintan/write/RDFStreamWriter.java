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
package org.acoli.fintan.write;

import java.io.IOException;
import java.io.PrintStream;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Iterator;

import org.acoli.fintan.core.FintanStreamComponentFactory;
import org.acoli.fintan.core.StreamWriter;
import org.apache.jena.rdf.model.Model;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Writes FintanStreams to any provided RDF serialization.
 * For convenience, Fintan's treatment of named streams works in default configuration:
 * 	- for each matching pair of named streams, a separate Thread is spawned 
 * 		which converts the stream independently
 *  - input streams without matching output streams are dropped.
 * 
 * @author Christian Faeth {@literal faeth@em.uni-frankfurt.de}
 *
 */
public class RDFStreamWriter extends StreamWriter implements FintanStreamComponentFactory {
	
	/**
	 * The following parameters can be set in the JSON config:
	 * 
	 * `lang` to specify the target RDF syntax. Supported languages follow the 
	 * 		naming convention of Apache Jena (ttl, TURTLE, RDF/XML, N3, …)
	 * `delimiter` to specify the textual delimiter indicating the end of a 
	 * 		segment. The specified delimiter is always expected to be the full 
	 * 		content of a delimiting line of text. "" corresponds to an empty line.
	 * `prefixDeduplication` (`true`/`false`) can be set to remove duplicate 
	 * 		prefix declarations in Turtle syntax. Since in Fintan, each segment 
	 * 		is contained in its own model, by default, the Jena API repeatedly 
	 * 		outputs all prefixes for each segment. With this flag, the duplicates 
	 * 		are removed from the resulting text stream.
	 * `customPrefixes` optional object to override existing prefix assignments 
	 * 		for optimized output. Syntax: "prefix1" : "uri1", "prefix2" : "uri2"
	 */
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
		if (conf.hasNonNull("customPrefixes")) {
			Iterator<String> iter = conf.get("customPrefixes").fieldNames();
			while (iter.hasNext()) {
				String entry = iter.next();
				writer.getCustomPrefixes().put(entry, conf.get("customPrefixes").get(entry).asText());
			}
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
	private HashMap<String,String> customPrefixes = new HashMap<String,String>();
	
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

	public HashMap<String,String> getCustomPrefixes() {
		return customPrefixes;
	}

	public void setCustomPrefixes(HashMap<String,String> customPrefixes) {
		this.customPrefixes = customPrefixes;
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
			writer.setCustomPrefixes(customPrefixes);
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
				
				for(String prefix:customPrefixes.keySet()) {
					m.setNsPrefix(prefix, customPrefixes.get(prefix));
				}
				
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