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
	

	@Override
	public final ObjectInputStream getInputStream() {
		return (ObjectInputStream) super.getInputStream();
	}

	
	@Override
	public final ObjectInputStream getInputStream(String name) {
		return (ObjectInputStream) super.getInputStream(name);
	}

	@Override
	public final void setInputStream(InputStream inputStream) {
		try {
			super.setInputStream(new ObjectInputStream(inputStream));
		} catch (IOException e) {
			LOG.error(e, e);
			System.exit(1);
		}
	}
	
	@Override
	public final void setInputStream(InputStream inputStream, String name) {
		try {
			super.setInputStream(new ObjectInputStream(inputStream), name);
		} catch (IOException e) {
			LOG.error(e, e);
			System.exit(1);
		}	
	}

	@Override
	public final ObjectOutputStream getOutputStream() {
		return (ObjectOutputStream) super.getOutputStream();
	}
	
	@Override
	public final ObjectOutputStream getOutputStream(String name) {
		return (ObjectOutputStream) super.getOutputStream(name);
	}
	
	@Override
	public final void setOutputStream(OutputStream outputStream) {
		try {
			super.setOutputStream(new ObjectOutputStream(outputStream));
		} catch (IOException e) {
			LOG.error(e, e);
			System.exit(1);
		}
	}
	
	@Override
	public final void setOutputStream(OutputStream outputStream, String name) {
		try {
			super.setOutputStream(new ObjectOutputStream(outputStream), name);
		} catch (IOException e) {
			LOG.error(e, e);
			System.exit(1);
		}	
	}
}
