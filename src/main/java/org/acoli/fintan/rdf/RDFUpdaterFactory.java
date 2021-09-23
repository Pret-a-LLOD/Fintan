package org.acoli.fintan.rdf;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.acoli.fintan.core.FintanStreamComponentFactory;
import org.apache.commons.cli.*;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RDFUpdaterFactory implements FintanStreamComponentFactory {
	static Logger LOG = LogManager.getLogger(RDFUpdaterFactory.class);
	@Override
	public RDFUpdater buildFromCLI(String[] args) throws IOException, ParseException {
		//TODO removed.
		return null;
	}

	@Override
	public RDFUpdater buildFromJsonConf(ObjectNode conf) throws IOException, ParseException, IllegalArgumentException {
		// READ THREAD PARAMETERS
		int threads = 0;
		if (conf.get("threads") != null)
			threads = conf.get("threads").asInt(0);
		RDFUpdater updater = new RDFUpdater("","",threads);

//		// READ GRAPHSOUT PARAMETERS (unsupported in default Updater, reserved for CoNLL-RDF at the moment)
//		if (conf.get("graphsoutDIR") != null) {
//			String graphOutputDir = conf.get("graphsoutDIR").asText("");
//			if (!graphOutputDir.equals("")) {
//				List<String> graphOutputSentences = new ArrayList<String>();
//				for (JsonNode snt:conf.withArray("graphsoutSNT")) {
//					graphOutputSentences.add(snt.asText());
//				}
//				updater.activateGraphsOut(graphOutputDir, graphOutputSentences);
//			}
//		}

		// READ TRIPLESOUT PARAMETERS
		if (conf.get("triplesoutDIR") != null) {
			String triplesOutputDir = conf.get("triplesoutDIR").asText("");
			if (!triplesOutputDir.equals("")) {
				List<String> triplesOutputSegments = new ArrayList<String>();
				for (JsonNode snt:conf.withArray("triplesoutSNT")) {
					triplesOutputSegments.add(snt.asText());
				}
				if (conf.get("triplesoutSNTclass") != null) {
					updater.setTriplesOutSegmentClass(conf.get("triplesoutSNTclass").asText());
				}
				updater.activateTriplesOut(triplesOutputDir, triplesOutputSegments);
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
}
