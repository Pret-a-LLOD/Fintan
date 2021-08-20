package org.acoli.conll.rdf;

import static org.acoli.conll.rdf.CoNLLRDFCommandLine.readString;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.acoli.conll.rdf.CoNLLRDFFormatter.Mode;
import org.acoli.conll.rdf.CoNLLRDFFormatter.Module;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.log4j.Logger;

public class CoNLLRDFManager {
	static Logger LOG = Logger.getLogger(CoNLLRDFManager.class);

	private ObjectNode config;
	private ArrayList<CoNLLRDFComponent> componentStack;

	PrintStream output;
	BufferedReader input;

	public static void main(String[] args) throws IOException {
		final CoNLLRDFManager manager;
		try {
			manager = new CoNLLRDFManagerFactory().buildFromCLI(args);
			manager.buildComponentStack();
		} catch (ParseException e) {
			LOG.error(e);
			System.exit(1);
			return;
		}

		manager.start();
	}

	public void parseConfig(String jsonString) throws IOException {
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.configure(Feature.ALLOW_COMMENTS, true);

		JsonNode node = objectMapper.readTree(jsonString);
		if (!node.getNodeType().equals(JsonNodeType.OBJECT)) {
			throw new IOException("File is no valid JSON config.");
		}
		config = (ObjectNode) node;

//		TODO: remove --- Car car = objectMapper.readValue(file, Car.class);
	}

	protected static BufferedReader parseConfAsInputStream(String confEntry) throws IOException {
		BufferedReader input;
		if (confEntry.equals("System.in")) {
			input = new BufferedReader(new InputStreamReader(System.in));
		} else if (new File(confEntry).canRead()) {
			if (confEntry.endsWith(".gz")) {
				input = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(confEntry))));
			} else {
				input = new BufferedReader(new FileReader(confEntry));
			}
		} else {
			throw new IOException("Could not read from " + confEntry);
		}
		return input;
	}

	protected static PrintStream parseConfAsOutputStream(String confEntry) throws IOException {
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

	public void buildComponentStack() throws IOException, ParseException {
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
			componentStack = new ArrayList<CoNLLRDFComponent>();
		else
			componentStack.clear();

		// First inputStream is always main input
		BufferedReader nextInput = input;
		// Traverse pipeline array
		for (JsonNode pipelineElement:config.withArray("pipeline")) {
			if (!pipelineElement.getNodeType().equals(JsonNodeType.OBJECT)) {
				throw new IOException("File is no valid JSON config.");
			}

			// Create CoNLLRDFComponents (StreamExtractor, Updater, Formatter ...)
			CoNLLRDFComponent component;
			if (pipelineElement.get("class").asText().equals(CoNLLStreamExtractor.class.getSimpleName())) {
				component = buildStreamExtractor((ObjectNode) pipelineElement);
			} else if (pipelineElement.get("class").asText().equals(CoNLLRDFUpdater.class.getSimpleName())) {
				component = buildUpdater((ObjectNode) pipelineElement);
			} else if (pipelineElement.get("class").asText().equals(CoNLLRDFFormatter.class.getSimpleName())) {
				component = buildFormatter((ObjectNode) pipelineElement);
			} else if (pipelineElement.get("class").asText().equals(SimpleLineBreakSplitter.class.getSimpleName())) {
				component = buildSimpleLineBreakSplitter((ObjectNode) pipelineElement);
			} else {
				throw new IOException("File is no valid JSON config.");
			}
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
				componentStack.get(componentStack.size()-1).setOutputStream(new PrintStream(compOutput));
				nextInput = new BufferedReader(new InputStreamReader(new PipedInputStream(compOutput)));
			}
		}
	}


	private CoNLLRDFComponent buildStreamExtractor(ObjectNode conf) throws IOException {
		return new CoNLLStreamExtractorFactory().buildFromJsonConf(conf);
	}

	private CoNLLRDFComponent buildUpdater(ObjectNode conf) throws IOException, ParseException {

		// READ THREAD PARAMETERS
		int threads = 0;
		if (conf.get("threads") != null)
			threads = conf.get("threads").asInt(0);
		CoNLLRDFUpdater updater = new CoNLLRDFUpdater("","",threads);

		// READ GRAPHSOUT PARAMETERS
		if (conf.get("graphsoutDIR") != null) {
			String graphOutputDir = conf.get("graphsoutDIR").asText("");
			if (!graphOutputDir.equals("")) {
				List<String> graphOutputSentences = new ArrayList<String>();
				for (JsonNode snt:conf.withArray("graphsoutSNT")) {
					graphOutputSentences.add(snt.asText());
				}
				updater.activateGraphsOut(graphOutputDir, graphOutputSentences);
			}
		}

		// READ TRIPLESOUT PARAMETERS
		if (conf.get("triplesoutDIR") != null) {
			String triplesOutputDir = conf.get("triplesoutDIR").asText("");
			if (!triplesOutputDir.equals("")) {
				List<String> triplesOutputSentences = new ArrayList<String>();
				for (JsonNode snt:conf.withArray("triplesoutSNT")) {
					triplesOutputSentences.add(snt.asText());
				}
				updater.activateTriplesOut(triplesOutputDir, triplesOutputSentences);
			}
		}

		// READ LOOKAHEAD PARAMETERS
		if (conf.get("lookahead") != null) {
			int lookahead_snts = conf.get("lookahead").asInt(0);
			if (lookahead_snts > 0)
				updater.activateLookahead(lookahead_snts);
		}

		// READ LOOKBACK PARAMETERS
		if (conf.get("lookback") != null) {
			int lookback_snts = conf.get("lookback").asInt(0);
			if (lookback_snts > 0)
				updater.activateLookback(lookback_snts);
		}

		// READ PREFIX DEDUPLICATION
		if (conf.get("prefixDeduplication") != null) {
			Boolean prefixDeduplication = conf.get("prefixDeduplication").asBoolean();
			if (prefixDeduplication)
				updater.activatePrefixDeduplication();
		}

		// READ ALL UPDATES
		// should be <#UPDATEFILENAMEORSTRING, #UPDATESTRING, #UPDATEITER>
		List<Triple<String, String, String>> updates = new ArrayList<Triple<String, String, String>>();
		for (JsonNode update:conf.withArray("updates")) {
			String freq = update.get("iter").asText("1");
			if (freq.equals("u"))
				freq = "*";
			try {
				Integer.parseInt(freq);
			} catch (NumberFormatException e) {
				if (!"*".equals(freq))
					throw e;
			}
			String path = update.get("path").asText();
			updates.add(new ImmutableTriple<String, String, String>(path, path, freq));
		}
		updater.parseUpdates(updates);

		// READ ALL MODELS
		for (JsonNode model:conf.withArray("models")) {
			List<String> models = new ArrayList<String>();
			String uri = model.get("source").asText();
			if (!uri.equals("")) models.add(uri);
			uri = model.get("graph").asText();
			if (!uri.equals("")) models.add(uri);
			if (models.size()==1) {
				try {
					updater.loadGraph(new URI(models.get(0)), new URI(models.get(0)));
				} catch (URISyntaxException e) {
					throw new IOException(e);
				}
			} else if (models.size()==2){
				try {
					updater.loadGraph(new URI(models.get(0)), new URI(models.get(1)));
				} catch (URISyntaxException e) {
					throw new IOException(e);
				}
			} else if (models.size()>2){
				throw new IOException("Error while loading model: Please specify model source URI and graph destination.");
			}
			models.removeAll(models);
		}

		return updater;
	}

	private CoNLLRDFComponent buildFormatter(ObjectNode conf) throws IOException {
		CoNLLRDFFormatter formatter = new CoNLLRDFFormatter();
		ObjectMapper mapper = new ObjectMapper();
		if (conf.path("output").isTextual()) {
			PrintStream output = parseConfAsOutputStream(conf.get("output").asText());
			formatter.setOutputStream(output);
		}
		for (JsonNode modConf : conf.withArray("modules")) {
			Mode mode;
			JsonNode columnsArray = null;
			String select = "";
			PrintStream outputStream = null;
			String modeString = modConf.get("mode").asText();
			switch (modeString) {
				case "RDF":
				case "CONLLRDF":
					mode = Mode.CONLLRDF;
					columnsArray = modConf.withArray("columns");
					break;
				case "CONLL":
					mode = Mode.CONLL;
					columnsArray = modConf.withArray("columns");
					break;
				case "DEBUG":
					mode = Mode.DEBUG;
					outputStream = System.err;
					break;
				case "SPARQLTSV":
					// TODO case "QUERY":
					mode = Mode.QUERY;
					select = readString(Paths.get(modConf.get("select").asText()));
					// TODO Attach context to IOExceptions thrown by readString
					break;
				case "GRAMMAR":
					mode = Mode.GRAMMAR;
					break;
				case "SEMANTICS":
					mode = Mode.SEMANTICS;
					break;
				case "GRAMMAR+SEMANTICS":
					mode = Mode.GRAMMAR_SEMANTICS;
					break;

				default:
					throw new IllegalArgumentException("Unknown mode: " + modeString);
			}
			Module module = formatter.addModule(mode);

			// select is either "" or a selectQuery as String
			module.setSelect(select);
			// convert JSON array to Java List
			if (columnsArray != null) {
				List<String> columnList = mapper.convertValue(columnsArray, new TypeReference<List<String>>() {});
				module.setCols(columnList);
			}
			// Set outputStream, if config has a property "output"
			if (modConf.path("output").isTextual()) {
				outputStream = parseConfAsOutputStream(modConf.get("output").asText());
			}
			// outputStream can be null or System.err
			module.setOutputStream(outputStream);
		}
		if (formatter.getModules().size() == 0) {
			formatter.addModule(Mode.CONLLRDF);
		}
		return formatter;
	}

	private CoNLLRDFComponent buildSimpleLineBreakSplitter(ObjectNode conf) {
		return new SimpleLineBreakSplitter();
	}

	public void start() {
		for (CoNLLRDFComponent component:componentStack) {
			Thread t = new Thread(component);
	        t.start();
		}
	}
}
