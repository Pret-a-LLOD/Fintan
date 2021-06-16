package de.unifrankfurt.informatik.acoli.fintan.core;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class StreamLoader extends FintanStreamComponent{

	protected static final Logger LOG = LogManager.getLogger(StreamLoader.class.getName());

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
