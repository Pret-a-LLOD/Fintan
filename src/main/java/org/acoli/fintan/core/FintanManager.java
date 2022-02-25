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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;

import org.acoli.fintan.core.util.IOUtils;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * The FintanManager class is designed to build and execute Fintan pipelines.
 * It has a main method for working with command line arguments.
 * Instantiation of Fintan components is done on a generic basis, so it can host 
 * and run any class implementing the respective interfaces.
 * 
 * @author Christian Faeth {@literal faeth@em.uni-frankfurt.de}
 *
 */
public class FintanManager {
	
	protected static final Logger LOG = LogManager.getLogger(FintanManager.class.getName());
	
	public static final String[] DEFAULT_PACKAGES = {
			"org.acoli.fintan.core",
			"org.acoli.fintan.genericIO",
			"org.acoli.fintan.load",
			"org.acoli.fintan.rdf",
			"org.acoli.fintan.write",
			"org.acoli.conll.rdf"
	};
	
	public static final String DEFAULT_TDB_PATH = "tdb/";
	public static final String DEFAULT_CACHE_PATH = "fileCache/";

	
	private ObjectNode config;
	
	private HashMap<String, FintanStreamComponent> componentStack;
	
	
	/**
	 * Starts a Fintan pipeline with command line arguments. Wildcards in config
	 * are replaced by params -p ...:   <$param0> --> arg0
	 * @param args
	 * 		-c 	path/to/config.json [-p someValue [someOtherValue]*]
	 * @throws Exception
	 * 		if anything bad happens.
	 */
	public static void main(String[] args) throws Exception {
		Options options = new Options();
		options.addRequiredOption("c", "config", true, "Specify JSON config file");
		options.addOption("p", "params", true, "Specify optional parameters for JSON config file.");
		options.getOption("p").setOptionalArg(true);
		options.getOption("p").setArgs(100);
		
		CommandLineParser parser = new DefaultParser();
		CommandLine cmd = parser.parse(options, args);
		
		FintanManager man = new FintanManager();
		
		String[] params = new String[] {};
		if(cmd.hasOption("p")) {
			params = cmd.getOptionValues("p");
		}
		
		if(cmd.hasOption("c")) {
			try {
				man.readConfig(cmd.getOptionValue("c"), params);
			} catch (IOException e) {
				throw new Exception("Error when reading config file "+new File(cmd.getOptionValue("c")).getAbsolutePath(), e);
			}
		}
		else {
		    throw new ParseException("No config file specified.");
		}
		
		man.buildComponentStack();
		man.start();
	}

	/**
	 * Reads the config file and replaces wildcards with command line parameters.
	 * <$param0> --> arg0
	 * @throws IOException
	 */
	public void readConfig(String path, String[] params) throws IOException {
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.configure(Feature.ALLOW_COMMENTS, true);

		String jsonConf = IOUtils.readSourceAsString(path);
		
		for (int i = 0; i < params.length; i++) {
			jsonConf = jsonConf.replace("<$param" + i + ">", params[i]);
		}
		
		JsonNode node = objectMapper.readTree(jsonConf);
		if (!node.getNodeType().equals(JsonNodeType.OBJECT)) {
			throw new IOException("File is no valid JSON config.");
		}
		config = (ObjectNode) node;
	}

	
	/**
	 * Validates config and reads all parameters to construct a Fintan pipeline.
	 * @throws IOException
	 */
	public void buildComponentStack() throws IOException {
		//validate
		if (!validateConfig()) {
			throw new IOException("File is no valid JSON config.");
		}
		
		
		//BUILD COMPONENT STACK
		if (componentStack == null) 
			componentStack = new HashMap<String, FintanStreamComponent>();
		else
			componentStack.clear();
		

		//BUILD DEFAULT "PIPELINE" including default I/O
		if (config.hasNonNull("pipeline"))
			buildDefaultPipeline();
		
		//BUILD ALL OTHER COMPONENTS
		if (config.hasNonNull("components"))
			buildOtherComponents();
		
		//INTERLINK COMPONENTS as defined in "streams"
		if (config.hasNonNull("streams"))
			buildStreams();
		
		validateLinkState();
	}
	

	/**
	 * Only does basic validation of optional elements.
	 * @return true if valid
	 */
	private boolean validateConfig() {
		boolean valid = false;
		boolean validPipeline = false;
		boolean validStreams = false;
		boolean validOtherComponents = false;
		boolean validDefaultInput = false;
		boolean validDefaultOutput = false;
		if (config.hasNonNull("pipeline") && config.get("pipeline").isArray()) {
			validPipeline = true;
		}
		if (config.hasNonNull("streams") && config.get("streams").isArray()) {
			validStreams = true;
		}
		if (config.hasNonNull("components") && config.get("components").isArray()) {
			validOtherComponents = true;
		}
		if (config.hasNonNull("input")) {
			validDefaultInput = true;
		}
		if (config.hasNonNull("output")) {
			validDefaultOutput = true;
		}
		
		if (validPipeline && validDefaultInput && validDefaultOutput) {
			//regular old CoNLL-RDF pipeline
			valid = true;
		}
		
		if (validStreams && (validOtherComponents || validPipeline)) {
			//new version: in case other streams are defined, it is sufficient 
			//if a valid pipeline or other components exist
			valid = true;
		}
		
		if (validOtherComponents && validDefaultInput && validDefaultOutput) {
			//can only be valid, if there is only one component.
			valid = true;
		}
		
		return valid;
	}

	/**
	 * Builds componentStack which is interlinked by default streams.
	 * Also uses the default "input" and "output" streams as defined in the config.
	 * @throws IOException if streams cannot be parsed
	 */
	private void buildDefaultPipeline() throws IOException {
		//read default input parameter, can be null in case it is defined in "streams"
		InputStream defaultInput = null;
		if (config.hasNonNull("input"))
			defaultInput = IOUtils.parseConfEntryAsInputStream(config.get("input").asText());

		//read default output parameter, can be null in case it is defined in "streams"
		OutputStream defaultOutput = null;
		if (config.hasNonNull("output"))
			defaultOutput = IOUtils.parseConfEntryAsOutputStream(config.get("output").asText());

		// First inputStream is always main input
		Object nextInput = defaultInput;
		// Traverse pipeline array	
		for (JsonNode pipelineElement:config.withArray("pipeline")) {
			if (!pipelineElement.getNodeType().equals(JsonNodeType.OBJECT)) {
				throw new IOException("Elements in 'pipeline' must be object nodes.");
			} 

			
			// Create FintanStreamComponents (StreamExtractor, Updater, Formatter ...)
			FintanStreamComponent component = buildComponent((ObjectNode) pipelineElement);
			
			String identifier = Integer.toString(component.hashCode()); //default, if no componentInstance specified
			if (pipelineElement.hasNonNull("componentInstance")) {
				identifier = pipelineElement.get("componentInstance").asText();
				if (componentStack.containsKey(identifier))
					throw new IOException("'componentInstance' : '"+identifier+"' is not unique!");
			}
			component.setInstanceName(identifier);
			
			componentStack.put(identifier, component);

			
			// Define Pipeline I/O
			// always use previously defined input... first main input, later piped input
			// currently late binding. Will terminate if streams are incompatible.
			if (nextInput != null)
				component.setInputStream(nextInput);
			
			if (componentStack.size() == config.withArray("pipeline").size()) {
				// last component, final output
				if (defaultOutput != null)
					component.setOutputStream(defaultOutput);
			} else {
				nextInput = connectComponents(component, null, null, null);
			}
		}
	}

	/**
	 * same as buildDefaultPipeline, but these components are not connected by default streams.
	 * @throws IOException
	 */
	private void buildOtherComponents() throws IOException {
		for (JsonNode node:config.withArray("components")) {
			if (!node.getNodeType().equals(JsonNodeType.OBJECT)) {
				throw new IOException("Elements in 'components' must be object nodes.");
			} 

			String identifier = null;
			if (node.hasNonNull("componentInstance")) {
				identifier = node.get("componentInstance").asText();
				if (componentStack.containsKey(identifier))
					throw new IOException("'componentInstance' : '"+identifier+"' is not unique!");
			} else {
				throw new IOException("UNIQUE 'componentInstance' identifier must be specified for all 'components' outside default 'pipeline'");
			}

			// Create FintanStreamComponents (StreamExtractor, Updater, Formatter ...)
			FintanStreamComponent component = buildComponent((ObjectNode) node);
			component.setInstanceName(identifier);
			componentStack.put(identifier, component);
		}
	}
	
	/**
	 * connects Instances to Streams:
	 * 1. sourceOutputStream to destinationInputStream
	 * 2. sourceOutputStream to destinationFile
	 * 3. sourceFile to destinationInputStream
	 * @throws IOException
	 */
	private void buildStreams() throws IOException {
		for (JsonNode node:config.withArray("streams")) {
			
			//basic validation
			if (!node.getNodeType().equals(JsonNodeType.OBJECT)) {
				throw new IOException("Elements in 'streams' must be object nodes.");
			} 
			if (node.hasNonNull("readsFromSource") && node.hasNonNull("readsFromInstance")) {
				throw new IOException("Single stream cannot read from both an instance AND a file/url source.");
			}
			if (node.hasNonNull("writesToDestination") && node.hasNonNull("writesToInstance")) {
				throw new IOException("Single stream cannot write to both an instance AND a file destination.");
			}
			if (node.hasNonNull("readsFromSource") && node.hasNonNull("writesToDestination")) {
				throw new IOException("Fintan cannot write from a file/url source to a file destination. It does not support file copying.");
			}
			

			FintanStreamComponent sourceComp = null;
			String sourceGraph = null;
			FintanStreamComponent destComp = null;
			String destGraph = null;
			InputStream inputStream = null;
			OutputStream outputStream = null;
			
			if (node.hasNonNull("readsFromSource")) {
				inputStream = IOUtils.parseConfEntryAsInputStream(node.get("readsFromSource").asText());
			}
			
			if (node.hasNonNull("readsFromInstance")) {
				sourceComp = componentStack.get(node.get("readsFromInstance").asText());
				if (sourceComp == null) 
					throw new IOException("Stream made reference to unspecified component '"
							+node.get("readsFromInstance").asText()+"'");

				if (node.hasNonNull("readsFromInstanceGraph")) {
				    sourceGraph = node.get("readsFromInstanceGraph").asText();
				}
			}
			
			if (node.hasNonNull("writesToDestination")) {
				outputStream = IOUtils.parseConfEntryAsOutputStream(node.get("writesToDestination").asText());
			}
			if (node.hasNonNull("writesToInstance")) {
				destComp = componentStack.get(node.get("writesToInstance").asText());
				if (destComp == null) 
					throw new IOException("Stream made reference to unspecified component '"
							+node.get("writesToInstance").asText()+"'");
				
				if (node.hasNonNull("writesToInstanceGraph")) {
					destGraph = node.get("writesToInstanceGraph").asText();
				}
			}
			
			if (inputStream != null) {
				destComp.setInputStream(inputStream, destGraph);
			} else if (outputStream != null) {
				sourceComp.setOutputStream(outputStream, sourceGraph);
			} else {
				connectComponents(sourceComp, sourceGraph, destComp, destGraph);
			}
		}
		
	}
	

	/**
	 * Checks whether all components are linked to at least one Input and one Output stream.
	 * @throws IOException
	 */
	private void validateLinkState() throws IOException {
		// TODO possibly check for deadlocks using Fintan Ontology.
		for (String key:componentStack.keySet()) {
			FintanStreamComponent component = componentStack.get(key);
			if (component.listInputStreamNames().length <=0)
				throw new IOException("Component has no valid InputStream: "+key);

			if (component.listOutputStreamNames().length <=0)
				throw new IOException("Component has no valid OutputStream: "+key);
		}
	}

	/**
	 * Generic instantiation of FintanStreamComponent objects. 
	 * 
	 * The conf Object node needs to denote a class which implements 
	 * the FintanStreamComponentFactory interface. 
	 * (In some cases, this may be the same class as the resulting StreamComponent.)
	 * @param conf
	 * @return
	 */
	private FintanStreamComponent buildComponent(ObjectNode conf) {
		Class targetClass = null;
		try {
			targetClass = Class.forName(conf.get("class").asText());
		} catch (ClassNotFoundException e) {
			LOG.trace("Class not found: "+conf.get("class")+". Trying default packages.");
			for (String pkg:DEFAULT_PACKAGES) {
//			for (Package pkg:Package.getPackages()) {
				try {
					targetClass = Class.forName(pkg+"."+conf.get("class").asText());
					LOG.trace("Class loaded successfully: " + targetClass.getName());
					break;
				} catch (ClassNotFoundException e1) {
					LOG.trace("Class "+conf.get("class")+" not in package "+pkg);
				}
			}
		}
		if (targetClass==null) {
			LOG.error("Class not found in any default package: "+conf.get("class")+". Please provide the fully qualified name.");
			System.exit(1);
		}
		FintanStreamComponent component = null;
		try {
			try {
				component = ((FintanStreamComponentFactory) targetClass.getDeclaredConstructor().newInstance()).buildFromJsonConf(conf);
			} catch (ClassCastException e) {
				targetClass = Class.forName(targetClass.getCanonicalName()+"Factory");
				component = ((FintanStreamComponentFactory) targetClass.getDeclaredConstructor().newInstance()).buildFromJsonConf(conf);
			}
			component.setConfig(conf);
			return component;
		} catch (IllegalArgumentException e) {
			LOG.error(e, e);
			System.exit(1);
		} catch (IOException e) {
			LOG.error(e, e);
			System.exit(1);
		} catch (InvocationTargetException e) {
			LOG.error(e, e);
			System.exit(1);
		} catch (NoSuchMethodException e) {
			LOG.error(e, e);
			System.exit(1);
		} catch (SecurityException e) {
			LOG.error(e, e);
			System.exit(1);
		} catch (InstantiationException | IllegalAccessException e) {
			LOG.error(e, e);
			System.exit(1);
		} catch (ParseException e) {
			LOG.error(e, e);
			System.exit(1);
		} catch (ClassNotFoundException e) {
			LOG.error(e, e);
			System.exit(1);
		}
		return null;
	}
	
	/**
	 * attempts to connect the provided stream slots of two FintanStreamComponents
	 * @param sourceComp
	 * 	the source component for which the OutputStream is to be defined.
	 * @param sourceGraph
	 * 	the stream/graph slot of the source Component for which the OutputStream is to be defined. 
	 *  If null, then DefaultGraph
	 * @param destComp
	 * 	the destination component for which the InputStream is to be defined. 
	 *  If null, then stream says unconnected.
	 * @param destGraph
	 *  the stream/graph slot of the destination Component for which the InputStream is to be defined. 
	 *  If null, then DefaultGraph
	 * @return the InputStream which can be connected to the next component. If it has already been connected successfully: null
	 * @throws IOException
	 * 	for various reasons. Esp. if slots of the Components are already occupied.
	 */
	private Object connectComponents(FintanStreamComponent sourceComp, String sourceGraph, FintanStreamComponent destComp, String destGraph) throws IOException {
		if (sourceGraph == null) sourceGraph = FintanStreamComponent.FINTAN_DEFAULT_STREAM_NAME;
		if (destGraph == null) destGraph = FintanStreamComponent.FINTAN_DEFAULT_STREAM_NAME;
		if (sourceComp.getOutputStream(sourceGraph) != null) {
			throw new IOException("OutputStream slot is already occupied for component '"+sourceComp.getInstanceName()+"', graph '"+sourceGraph+ "'");
		}
		
		if (destComp != null) {
			if (destComp.getInputStream(destGraph) != null) {
				throw new IOException("InputStream slot is already occupied for component '"+destComp.getInstanceName()+"', graph '"+destGraph+ "'");
			}
		}
		
		Object nextInput = null;
		try {
		if (sourceComp instanceof StreamLoader) {
			// Loader uses FintanStream as Output
			FintanStreamHandler compOutput = new FintanStreamHandler();
			sourceComp.setOutputStream(compOutput, sourceGraph);
			nextInput = compOutput;
		} else if (sourceComp instanceof StreamRdfUpdater) {
			// Updater uses FintanStream as Output
			FintanStreamHandler compOutput = new FintanStreamHandler();
			sourceComp.setOutputStream(compOutput, sourceGraph);
			nextInput = compOutput;
		} else if (sourceComp instanceof StreamTransformerGenericIO) {
			// GenericIO uses java OutputStreams
			PipedOutputStream compOutput = new PipedOutputStream();
			sourceComp.setOutputStream(compOutput, sourceGraph);
			nextInput = new PipedInputStream(compOutput);
		} else if (sourceComp instanceof StreamWriter) {
			// GenericIO uses java OutputStreams
			PipedOutputStream compOutput = new PipedOutputStream();
			sourceComp.setOutputStream(compOutput, sourceGraph);
			nextInput = new PipedInputStream(compOutput);
		}
		
		if (destComp != null && nextInput != null) {
			destComp.setInputStream(nextInput, destGraph);
			return null;
		}
		} catch (ClassCastException e) {
			throw new IOException("Error when trying to connect srcComp: "+sourceComp.getInstanceName()+" srcGraph: "+sourceGraph+" tgtComp: "+destComp.getInstanceName()+"tgtGraph: "+destGraph, e);
		}
		
		return nextInput;
	}

	/**
	 * Start pipeline execution. Each component is run in a separate thread.
	 * ComponentStack must be built beforehand.
	 * @throws InterruptedException 
	 */
	public void start() throws InterruptedException {
		for (FintanStreamComponent component:componentStack.values()) {
			Thread t = new Thread(component);
	        t.start();
	        t.join();
		}
	}


}

