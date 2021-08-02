package de.unifrankfurt.informatik.acoli.fintan.core;


import org.apache.jena.rdf.model.Model;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class StreamRdfUpdater extends FintanStreamComponent<FintanInputStream<Model>, FintanOutputStream<Model>> {

	protected static final Logger LOG = LogManager.getLogger(StreamRdfUpdater.class.getName());
	
}
