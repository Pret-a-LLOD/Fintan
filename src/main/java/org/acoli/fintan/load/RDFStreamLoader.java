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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Map;

import org.acoli.fintan.core.FintanStreamComponentFactory;
import org.acoli.fintan.core.StreamLoader;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Load pre-segmented RDF streams in the given serialization format. Default: TTL
 * For convenience, Fintan's treatment of named streams works in default configuration:
 * 	- for each matching pair of named streams, a separate Thread is spawned 
 * 		which converts the stream independently
 *  - input streams without matching outputstreams are dropped.
 * 
 * @author Christian Faeth {@literal faeth@em.uni-frankfurt.de}
 *
 */
public class RDFStreamLoader extends StreamLoader implements FintanStreamComponentFactory{

	//Factory methods
	/**
	 * Three parameters can be set in the JSON config:
	 * 
	 * `lang` to specify the RDF syntax. Supported languages follow the naming 
	 * 		convention of Apache Jena (ttl, TURTLE, RDF/XML, N3, …)
	 * 
	 * `delimiter` to specify the textual delimiter indicating the end of a segment. 
	 * 		The specified delimiter is always expected to be the full content of 
	 * 		a delimiting line of text. "" corresponds to an empty line.
	 * 
	 * `globalPrefixes` (`true`/`false`) is specifically designed for Turtle syntax. 
	 * 		In a Turtle file, usually the prefixes are defined globally in the 
	 * 		beginning of the File. However, they can be overridden in between. 
	 * 		Without the `globalPrefixes` setting Fintan expects the prefixes to 
	 * 		be repeated for every segment of data. If it fails to load, it will 
	 * 		still retry with the last successful set of prefixes, but this will 
	 * 		increase processing overhead. In this case the `globalPrefixes` flag
	 * 		should be set to `true`.
	 */
	@Override
	public RDFStreamLoader buildFromJsonConf(ObjectNode conf) throws IOException, IllegalArgumentException {
		RDFStreamLoader loader = new RDFStreamLoader();
		loader.setConfig(conf);
		if (conf.hasNonNull("lang")) {
			loader.setLang(conf.get("lang").asText());
		}
		if (conf.hasNonNull("delimiter")) {
			loader.setSegmentDelimiter(conf.get("delimiter").asText());
			loader.setSplit(true);
		}
		if (conf.hasNonNull("split")) {
			loader.setSplit(conf.get("split").asBoolean());
		}
		if (conf.hasNonNull("globalPrefixes")) {
			loader.setGlobalPrefixes(conf.get("globalPrefixes").asBoolean());
		}
		return loader;
	}

	@Override
	public RDFStreamLoader buildFromCLI(String[] args) throws IOException, IllegalArgumentException {
		// TODO Auto-generated method stub
		return null;
	}

	protected static final Logger LOG = LogManager.getLogger(RDFStreamLoader.class.getName());


	private String lang = "TTL";
	private boolean split = false;
	private String segmentDelimiter = null;
	private boolean globalPrefixes = false;
	private String prefixCache = "";

	public String getLang() {
		return lang;
	}

	public void setLang(String lang) {
		this.lang = lang;
	}

	public boolean isSplit() {
		return split;
	}

	public void setSplit(boolean split) {
		this.split = split;
	}

	public String getSegmentDelimiter() {
		return segmentDelimiter;
	}

	public void setSegmentDelimiter(String segmentDelimiter) {
		this.segmentDelimiter = segmentDelimiter;
	}

	public boolean hasGlobalPrefixes() {
		return globalPrefixes;
	}

	public void setGlobalPrefixes(boolean globalPrefixes) {
		this.globalPrefixes = globalPrefixes;
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
			
			RDFStreamLoader loader = new RDFStreamLoader();
			loader.setSegmentDelimiter(segmentDelimiter);
			loader.setLang(lang);
			loader.setSplit(split);
			loader.setGlobalPrefixes(globalPrefixes);
			try {
				loader.setInputStream(getInputStream(name));
				loader.setOutputStream(getOutputStream(name));
			} catch (IOException e) {
				LOG.error(e, e);
				System.exit(1);
			}
			new Thread(loader).start();
		}
		
		//terminate in case there is no default stream. 
		//named streams are handled in subthreads.
		if (getOutputStream()==null) return;
		
		// process default stream
		BufferedReader in = new BufferedReader(new InputStreamReader(getInputStream()));
		String rdfsegment = "";
		try {
			for(String line = in.readLine(); line !=null; line=in.readLine()) {
				if (split && segmentDelimiter == null) {
					outputSegment(line+"\n", "");
				} else if (split && line.equals(segmentDelimiter)) {
					outputSegment(rdfsegment, "");

					rdfsegment = "";
				} else {
					rdfsegment+=line+"\n";
				}
			}
			//final segment in case there is no segmentDelimiter in last row
		} catch (IOException e) {
			LOG.trace("Error when reading from Stream: " +e);
		}
		if (!rdfsegment.trim().isEmpty())
			outputSegment(rdfsegment, "");
		getOutputStream().terminate();
	}
	
	private void outputSegment(String rdfsegment, String outputStreamName) {
		Model m = ModelFactory.createDefaultModel();
		
		if (globalPrefixes) 
			rdfsegment = prefixCache + rdfsegment;
		//TODO: find a better way to assess existence of prefixes. 
		//Exception handling may be slow.
		try {
			m.read(new StringReader(rdfsegment), null, lang);
		} catch (org.apache.jena.riot.RiotException e) {
			//probably missing prefixes.
			rdfsegment = prefixCache + rdfsegment;
			m.read(new StringReader(rdfsegment), null, lang);
		}
		
		if (!globalPrefixes || prefixCache.length()==0) 
			cachePrefixes(m.getNsPrefixMap());
		try {
			getOutputStream(outputStreamName).write(m);
		} catch (InterruptedException e) {
			LOG.error("Error when writing to Stream "+outputStreamName+": "+e);
		}
	}
	
	private void cachePrefixes(Map<String,String> prefixMap) {
		if (prefixMap == null) return;
		//assemble prefixes:
		// - add PrefixMap to empty model 
		// - write empty (prefix-only) serialization 
		// - concatenate empty (prefix-only) serialization with rdfsegment
		// - attempt to read again
		Model m = ModelFactory.createDefaultModel();
		m.setNsPrefixes(prefixMap);
		StringWriter prefixWright = new StringWriter();
		m.write(prefixWright, lang);
		prefixCache = prefixWright.toString();
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
