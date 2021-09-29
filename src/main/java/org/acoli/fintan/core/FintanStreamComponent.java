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
package org.acoli.fintan.core;

import java.io.IOException;
import java.util.HashMap;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Generic abstract class for a component to be run in Fintan pipelines. 
 * Implements Runnable, to be executed in a Thread.
 * 
 * Handles input and output streams. Both differentiate between:
 * 		default input stream (adhering to a default RDF graph)
 * 		default output stream (adhering to a default RDF graph)
 * 		named input stream (adhering to a named RDF graph)
 * 		named output stream (adhering to a named RDF graph)
 * 
 * @author Christian Faeth {@literal faeth@em.uni-frankfurt.de}
 *
 * @param <In> The type of input stream it accepts.
 * @param <Out> The type of output stream it accepts.
 */
public abstract class FintanStreamComponent<In, Out> implements Runnable {

	public static final String FINTAN_DEFAULT_SEGMENT_DELIMITER_TTL = "###FINTAN#end#segment###";
	public static final String FINTAN_DEFAULT_SEGMENT_DELIMITER_CoNLL = "###FINTAN#end#segment###";
	public static final String FINTAN_DEFAULT_SEGMENT_DELIMITER_TSV = "###FINTAN#end#segment###";
	public static final String FINTAN_DEFAULT_STREAM_NAME = "";

	private ObjectNode config;
	private HashMap<String,In> inputStreams = new HashMap<String,In>();
	private HashMap<String,Out> outputStreams = new HashMap<String,Out>();

	//TODO: add segmentation delimiter handling
	//TODO: -TEXT-	StreamComponents: 	add data FORMAT handling
	//									add segmentation DELIMITER handling
	//		-RDF-	StreamComponents: 	add data MODEL / ONTOLOGY handling
	//		-BOTH- 	possibly to be moved to manager classes, or abstraction layer in Fintan Service.
	//				should be compatible with OpenAPI, elexis and teanga.
	/**
	 * 
	 * @return the JSON configuration of this component.
	 */
	public ObjectNode getConfig() {
		return config;
	}

	/**
	 * Provide a JSON configuration to this class.
	 * 
	 * @param config The configuration for this component.
	 */
	public void setConfig(ObjectNode config) {
		this.config = config;
	}

	/**
	 * Get the input stream attached to the default input stream slot.
	 * 
	 * @return stream
	 */
	public In getInputStream() {
		return inputStreams.get(FINTAN_DEFAULT_STREAM_NAME);
	}
	
	/**
	 * Get the input stream attached to a named input stream slot.
	 * 
	 * @param name
	 * @return stream
	 */
	public In getInputStream(String name) {
		return inputStreams.get(name);
	}

	/**
	 * Set an input stream as the default input stream.
	 * 
	 * @param inputStream
	 * @throws IOException if slot is not available (for overriding methods)
	 */
	public void setInputStream(In inputStream) throws IOException {
		this.inputStreams.put(FINTAN_DEFAULT_STREAM_NAME, inputStream);
	}
	
	/**
	 * Set an input stream as a named input stream.
	 * 
	 * @param inputStream
	 * @param name
	 * @throws IOException if slot is not available (for overriding methods)
	 */
	public void setInputStream(In inputStream, String name) throws IOException {
		this.inputStreams.put(name, inputStream);
	}

	/**
	 * Get the output stream attached to the default output stream slot.
	 * 
	 * @return stream
	 */
	public Out getOutputStream() {
		return outputStreams.get(FINTAN_DEFAULT_STREAM_NAME);
	}

	/**
	 * Get the output stream attached to a named output stream slot.
	 * 
	 * @param name
	 * @return stream
	 */
	public Out getOutputStream(String name) {
		return outputStreams.get(name);
	}

	/**
	 * Set an output stream as the default output stream.
	 * 
	 * @param outputStream
	 * @throws IOException if slot is not available (for overriding methods)
	 */
	public void setOutputStream(Out outputStream) throws IOException {
		this.outputStreams.put(FINTAN_DEFAULT_STREAM_NAME, outputStream);
	}
	
	/**
	 * Set an output stream as a named output stream.
	 * 
	 * @param outputStream
	 * @param name
	 * @throws IOException if slot is not available (for overriding methods)
	 */
	public void setOutputStream(Out outputStream, String name) throws IOException {
		this.outputStreams.put(name, outputStream);
	}
	
	/**
	 * 
	 * @return an array containing all input stream names (incl. default)
	 */
	public String[] listInputStreamNames() {
		return inputStreams.keySet().toArray(new String[] {});
	}
	
	/**
	 * 
	 * @return an array containing all output stream names (incl. default)
	 */
	public String[] listOutputStreamNames() {
		return outputStreams.keySet().toArray(new String[] {});
	}


	/**
	 * Start this component.
	 */
	public void start() {
		run();
	}
	
	
	//@fintan-core: Factory Pattern
	//@fintan-submodules: implement 1xFactory, 1xComponent
	
	
}
