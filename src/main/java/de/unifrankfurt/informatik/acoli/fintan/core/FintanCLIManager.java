package de.unifrankfurt.informatik.acoli.fintan.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

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

public class FintanCLIManager {
	
	//TODO: adapt to multiple streams + JSON
	
	protected static final Logger LOG = LogManager.getLogger(FintanCLIManager.class.getName());
	
	public static final String[] DEFAULT_PACKAGES = {
			"de.unifrankfurt.informatik.acoli.fintan.core",
			"de.unifrankfurt.informatik.acoli.fintan.genericIO",
			"de.unifrankfurt.informatik.acoli.fintan.load",
			"de.unifrankfurt.informatik.acoli.fintan.split",
			"de.unifrankfurt.informatik.acoli.fintan.transform",
			"de.unifrankfurt.informatik.acoli.fintan.write",
			"org.acoli.conll.rdf"
	};
	
	private ObjectNode config;
	
	private List<FintanStreamComponent> componentStack;
	
	OutputStream output;
	InputStream input;
	
	public static void main(String[] args) throws Exception {
		Options options = new Options();
		options.addRequiredOption("c", "config", true, "Specify JSON config file");
		
		
		CommandLineParser parser = new DefaultParser();
		CommandLine cmd = parser.parse(options, args);
		
		FintanCLIManager man = new FintanCLIManager();
		
		if(cmd.hasOption("c")) {
			try {
				man.readConfig(cmd.getOptionValue("c"));
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
	 * Reads the content of a given path or URL as String. Can be used for reading queries etc.
	 * @param pathOrURL
	 * @return file content as String
	 * @throws IOException if unreadable.
	 */
	public static String readSourceAsString (String pathOrURL) throws IOException {
		InputStream inputStream;
		File f = new File(pathOrURL);
		if (f.canRead()) {
			inputStream = new FileInputStream(f);
		} else {
			URL url = new URL(pathOrURL);
			inputStream = url.openStream();
		}
		BufferedReader reader;
		if (pathOrURL.endsWith(".gz")) {
			reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(inputStream), StandardCharsets.UTF_8));
		} else {
			reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
		}
		
		StringBuilder out = new StringBuilder();
		for (String line = reader.readLine(); line != null; line = reader.readLine())
			out.append(line + "\n");
		return out.toString();
	}


	public void readConfig(String path) throws IOException {
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.configure(Feature.ALLOW_COMMENTS, true);

		File file = new File(path);
		if (!file.canRead()) {
			throw new IOException("File cannot be read.");
		}
		JsonNode node = objectMapper.readTree(file);
		if (!node.getNodeType().equals(JsonNodeType.OBJECT)) {
			throw new IOException("File is no valid JSON config.");
		}
		config = (ObjectNode) node;
	}

	private InputStream parseConfAsInputStream(String confEntry) throws IOException {
		InputStream input;
		if (confEntry.equals("System.in")) {
			input = System.in;
		} else if (new File(confEntry).canRead()) {
			if (confEntry.endsWith(".gz")) 
				input = new GZIPInputStream(new FileInputStream(confEntry));
			else
				input = new FileInputStream(confEntry);
		} else {
			throw new IOException("Could not read from " + confEntry);
		}
		return input;
	}
	
	private OutputStream parseConfAsOutputStream(String confEntry) throws IOException {
		PrintStream output;
		if (confEntry.equals("System.out")) {
			output = System.out;
		} else if (new File(confEntry).canWrite()) {
			output = new PrintStream(confEntry);
		} else if (new File(confEntry).createNewFile()) {
			output = new PrintStream(confEntry);
		} else {
			throw new IOException("Could not write to " + confEntry);
		}
		return output;
	}

	public void buildComponentStack() throws IOException {
		//READ INPUT PARAMETER
		input = parseConfAsInputStream(config.get("input").asText());
		
		//READ OUTPUT PARAMETER
		output = parseConfAsOutputStream(config.get("output").asText());
		
		//READ PIPELINE PARAMETER
		if (!config.get("pipeline").isArray()) {
			throw new IOException("File is no valid JSON config.");
		}
		
		
		
		//BUILD COMPONENT STACK
		if (componentStack == null) 
			componentStack = new ArrayList<FintanStreamComponent>();
		else
			componentStack.clear();
		
		// First inputStream is always main input
		Object nextInput = input;
		// Traverse pipeline array	
		for (JsonNode pipelineElement:config.withArray("pipeline")) {
			if (!pipelineElement.getNodeType().equals(JsonNodeType.OBJECT)) {
				throw new IOException("File is no valid JSON config.");
			} 
			
			// Create FintanStreamComponents (StreamExtractor, Updater, Formatter ...)
			FintanStreamComponent component = buildComponent((ObjectNode) pipelineElement);
			componentStack.add(component);
			
			// Define Pipeline I/O
			// always use previously defined input... first main input, later piped input
			// currently late binding. Will terminate if streams are incompatible.
			component.setInputStream(nextInput);
			if (componentStack.size() == config.withArray("pipeline").size()) {
				// last component, final output
				component.setOutputStream(output);
			} else if (component instanceof StreamLoader) {
				// Loader uses FintanStream as Output
				FintanStreamHandler compOutput = new FintanStreamHandler();
				component.setOutputStream(compOutput);
				nextInput = compOutput;
			} else if (component instanceof StreamRdfUpdater) {
				// Updater uses FintanStream as Output
				FintanStreamHandler compOutput = new FintanStreamHandler();
				component.setOutputStream(compOutput);
				nextInput = compOutput;
			} else if (component instanceof StreamTransformerGenericIO) {
				// GenericIO uses java OutputStreams
				PipedOutputStream compOutput = new PipedOutputStream();
				component.setOutputStream(compOutput);
				nextInput = new PipedInputStream(compOutput);
			} else if (component instanceof StreamWriter) {
				// GenericIO uses java OutputStreams
				PipedOutputStream compOutput = new PipedOutputStream();
				component.setOutputStream(compOutput);
				nextInput = new PipedInputStream(compOutput);
			}
			
			
			// Define Pipeline I/O
			// always use previously defined input... first main input, later piped input
			
			
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
			LOG.info("Class not found: "+conf.get("class")+". Trying default packages.");
			for (String pkg:DEFAULT_PACKAGES) {
//			for (Package pkg:Package.getPackages()) {
				try {
					targetClass = Class.forName(pkg+"."+conf.get("class").asText());
					LOG.info("Class loaded successfully: " + targetClass.getName());
					break;
				} catch (ClassNotFoundException e1) {
					LOG.info("Class "+conf.get("class")+" not in package "+pkg);
				}
			}
		}
		if (targetClass==null) System.exit(1);
		try {
			FintanStreamComponent component;
			try {
				component = ((FintanStreamComponentFactory) targetClass.getDeclaredConstructor().newInstance()).buildFromJsonConf(conf);
				return component;
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
				System.exit(1);
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			} catch (InvocationTargetException e) {
				e.printStackTrace();
				System.exit(1);
			} catch (NoSuchMethodException e) {
				e.printStackTrace();
				System.exit(1);
			} catch (SecurityException e) {
				e.printStackTrace();
				System.exit(1);
			}
			
		} catch (InstantiationException | IllegalAccessException e) {
			LOG.error(e, e);
			System.exit(1);
		}
		return null;
	}


	public void start() {
		for (FintanStreamComponent component:componentStack) {
			Thread t = new Thread(component);
	        t.start();
		}
	}


}

