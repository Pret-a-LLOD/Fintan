package de.unifrankfurt.informatik.acoli.fintan.core;

import java.io.InputStream;

import org.apache.jena.rdf.model.Model;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class StreamLoader extends FintanStreamComponent<InputStream, FintanOutputStream<Model>> {

	protected static final Logger LOG = LogManager.getLogger(StreamLoader.class.getName());


}
