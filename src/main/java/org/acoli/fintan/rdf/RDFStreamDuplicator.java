package org.acoli.fintan.rdf;

import java.io.IOException;
import java.io.OutputStream;

import org.acoli.fintan.core.FintanInputStream;
import org.acoli.fintan.core.FintanOutputStream;
import org.acoli.fintan.core.FintanStreamComponentFactory;
import org.acoli.fintan.core.StreamRdfUpdater;
import org.apache.commons.cli.ParseException;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Duplicates the contents of default FintanInputStream to all attached OutputStreams.
 * 
 * Throws IOException, if a named InputStream is set. Can only take  a single input.
 * @author CF
 *
 */
public final class RDFStreamDuplicator extends StreamRdfUpdater implements FintanStreamComponentFactory {

	
	protected static final Logger LOG = LogManager.getLogger(RDFStreamDuplicator.class.getName());

	@Override
	public RDFStreamDuplicator buildFromJsonConf(ObjectNode conf)
			throws IOException, IllegalArgumentException, ParseException {
		return new RDFStreamDuplicator();
	}

	@Override
	public RDFStreamDuplicator buildFromCLI(String[] args)
			throws IOException, IllegalArgumentException, ParseException {
		return new RDFStreamDuplicator();
	}
	
	
	@Override
	public void setInputStream(FintanInputStream<Model> inputStream, String name) throws IOException {
		if (name == null || FINTAN_DEFAULT_STREAM_NAME.equals(name)) {
			setInputStream(inputStream);
		} else {
			throw new IOException("Only default InputStream is supported for "+RDFStreamDuplicator.class.getName());
		}
	}
	
	/**
	 * Override in order to allow underspecified stream names.
	 * Graph names are unnecessary in duplicators.
	 */
	@Override
	public void setOutputStream(FintanOutputStream<Model> outputStream) throws IOException {
		super.setOutputStream(outputStream, Integer.toString(outputStream.hashCode()));
	}
	
	/**
	 * Override in order to allow underspecified stream names.
	 * Graph names are unnecessary in duplicators.
	 */
	@Override
	public void setOutputStream(FintanOutputStream<Model> outputStream, String name) throws IOException {
			setOutputStream(outputStream);
	}
	
	private void processStream() {
		while (getInputStream().canRead()) {
			try {
				Model model_in = getInputStream().read();
				if (model_in == null) continue;
				for (String name:listOutputStreamNames()) {
					Model model_out = ModelFactory.createDefaultModel();
					model_out.add(model_in);
					getOutputStream(name).write(model_out);
				}
			} catch (InterruptedException e) {
				LOG.error("Resuming from interrupted thread when reading from default Stream: " +e);
			}
		}

		for (String name:listOutputStreamNames()) {
			getOutputStream(name).terminate();
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