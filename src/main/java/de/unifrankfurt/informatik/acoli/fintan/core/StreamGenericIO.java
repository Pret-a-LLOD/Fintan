package de.unifrankfurt.informatik.acoli.fintan.core;

import java.io.InputStream;
import java.io.OutputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class StreamGenericIO extends FintanStreamComponent<InputStream, OutputStream> {

	protected static final Logger LOG = LogManager.getLogger(StreamGenericIO.class.getName());


}
