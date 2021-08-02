package de.unifrankfurt.informatik.acoli.fintan.core;

import java.io.InputStream;
import java.io.OutputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class StreamTransformerGenericIO extends FintanStreamComponent<InputStream, OutputStream> {

	protected static final Logger LOG = LogManager.getLogger(StreamTransformerGenericIO.class.getName());


}
