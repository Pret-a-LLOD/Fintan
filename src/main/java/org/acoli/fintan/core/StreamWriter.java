package org.acoli.fintan.core;


import java.io.OutputStream;

import org.apache.jena.rdf.model.Model;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class StreamWriter extends FintanStreamComponent<FintanInputStream<Model>, OutputStream> {

	protected static final Logger LOG = LogManager.getLogger(StreamWriter.class.getName());
	
}
