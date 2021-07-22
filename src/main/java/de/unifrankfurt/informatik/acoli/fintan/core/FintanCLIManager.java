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
	
	protected static final Logger LOG = LogManager.getLogger(FintanCLIManager.class.getName());
	
	public static final String[] DEFAULT_PACKAGES = {
			"de.unifrankfurt.informatik.acoli.fintan.core",
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
	
	
	/**
	 * Read an entire file to String, encoded as UTF-8. Intended to load small files
	 * like sparql queries into memory. Implementation of
	 * {@Code Files.readString(Path path)} available in Java 11
	 *
	 * @param path the path to the file
	 * @return a String containing the content read from the file
	 * @throws IOException              if the read fails
	 * @throws IllegalArgumentException if the file is reported as larger than 2GB
	 */
	public static String readString(Path path) throws IOException {
		// Files.readString(path, StandardCharsets.UTF_8); // requires Java 11
		if (path.toFile().length() > 2000000000) {
			throw new IllegalArgumentException(
					"The file " + path + " is too large. " + path.toFile().length() + " bytes");
		}
		byte[] buffer = new byte[(int) path.toFile().length()];
		buffer = Files.readAllBytes(path);
		return new String(buffer, StandardCharsets.UTF_8);
	}

	public static String readUrl(URL url) throws IOException {
		BufferedReader reader;
		if (url.toString().endsWith(".gz")) {
			reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(url.openStream()), StandardCharsets.UTF_8));
		} else {
			reader = new BufferedReader(new InputStreamReader(url.openStream(), StandardCharsets.UTF_8));
		}
		StringBuilder out = new StringBuilder();
		for (String line = reader.readLine(); line != null; line = reader.readLine())
			out.append(line + "\n");
		return out.toString();
	}
	
	
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
		InputStream nextInput = input;
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
			component.setInputStream(nextInput);
			if (componentStack.size() == config.withArray("pipeline").size()) {
				// last component, final output
				component.setOutputStream(output);
			} else {
				// intermediate pipeline to next component (using PipedOutputStream->PipedInputStream)
				PipedOutputStream compOutput = new PipedOutputStream();
				componentStack.get(componentStack.size()-1).setOutputStream(compOutput);
				nextInput = new PipedInputStream(compOutput);
			}
			
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
					LOG.error("Class "+conf.get("class")+" not in package "+pkg);
				}
			}
		}
		if (targetClass==null) System.exit(1);
		try {
			FintanStreamComponent component;
			try {
				component = ((FintanStreamComponentFactory) targetClass.newInstance()).buildFromJsonConf(conf);
				return component;
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				System.exit(1);
			} catch (IOException e) {
				// TODO Auto-generated catch block
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

