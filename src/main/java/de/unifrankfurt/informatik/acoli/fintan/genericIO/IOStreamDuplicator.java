package de.unifrankfurt.informatik.acoli.fintan.genericIO;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import org.apache.commons.cli.ParseException;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.node.ObjectNode;

import de.unifrankfurt.informatik.acoli.fintan.core.FintanStreamComponent;
import de.unifrankfurt.informatik.acoli.fintan.core.FintanStreamComponentFactory;
import de.unifrankfurt.informatik.acoli.fintan.core.StreamTransformerGenericIO;

/**
 * Duplicates the contents of default InputStream to all attached OutputStreams.
 * 
 * Throws IOException, if a named InputStream is set. Can only take  a single input.
 * @author CF
 *
 */
public class IOStreamDuplicator extends StreamTransformerGenericIO implements FintanStreamComponentFactory {
	
	protected static final Logger LOG = LogManager.getLogger(IOStreamDuplicator.class.getName());
	
	public static final int DEFAULT_BUFFER_SIZE = 8192;
	
	@Override
	public FintanStreamComponent buildFromJsonConf(ObjectNode conf)
			throws IOException, IllegalArgumentException, ParseException {
		return new IOStreamDuplicator();
	}

	@Override
	public FintanStreamComponent buildFromCLI(String[] args)
			throws IOException, IllegalArgumentException, ParseException {
		return new IOStreamDuplicator();
	}

	@Override
	public void setInputStream(InputStream inputStream, String name) throws IOException {
		throw new IOException();
	}
	
	private void processStream() throws IOException {
		BufferedInputStream in = new BufferedInputStream(getInputStream(), DEFAULT_BUFFER_SIZE);
		int len = 0;
		byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
		while ((len = in.read(buffer)) > -1) {
			byte[] outBuffer;
			if (len < buffer.length) {
				outBuffer = Arrays.copyOf(buffer, len);
			} else {
				outBuffer = buffer;
			}
			for (String name:listOutputStreamNames()) {
				getOutputStream(name).write(outBuffer);
			}
		}

		for (String name:listOutputStreamNames()) {
			getOutputStream(name).close();
		}
	}
	
	@Override
	public void start() {
		run();
	}

	@Override
	public void run() {
		try {
			processStream();
		} catch (Exception e) {
			LOG.error(e, e);
			System.exit(1);
		}
	}

	




	
}