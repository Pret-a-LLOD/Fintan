package de.unifrankfurt.informatik.acoli.fintan.write;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.unifrankfurt.informatik.acoli.fintan.core.StreamWriter;

public class RDFStreamWriter extends StreamWriter {

	//TODO: add base functionality of CoNLLRDFFormatter  -- derive/extend Formatter from ...this...
	
	protected static final Logger LOG = LogManager.getLogger(RDFStreamWriter.class.getName());


	private void processStream() {
		// TODO Auto-generated method stub
		
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