package de.unifrankfurt.informatik.acoli.fintan.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class StreamRdfUpdater extends FintanStreamComponent {

	protected static final Logger LOG = LogManager.getLogger(StreamRdfUpdater.class.getName());
	
	private ObjectInputStream inputStream;
	private ObjectOutputStream outputStream;
	

	@Override
	public ObjectInputStream getInputStream() {
		return inputStream;
	}

	@Override
	public ObjectOutputStream getOutputStream() {
		return outputStream;
	}

	@Override
	public void setInputStream(InputStream inputStream) {
		super.setInputStream(inputStream);
		try {
			this.inputStream = new ObjectInputStream(super.getInputStream());
		} catch (IOException e) {
			LOG.error(e, e);
			System.exit(1);
		}
	}

	@Override
	public void setOutputStream(OutputStream outputStream) {
		super.setOutputStream(outputStream);
		try {
			this.outputStream = new ObjectOutputStream(super.getOutputStream());
		} catch (IOException e) {
			LOG.error(e, e);
			System.exit(1);
		}
	}
}
