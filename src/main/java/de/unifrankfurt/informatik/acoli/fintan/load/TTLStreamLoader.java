package de.unifrankfurt.informatik.acoli.fintan.load;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.unifrankfurt.informatik.acoli.fintan.core.StreamLoader;

public class TTLStreamLoader extends StreamLoader {

	//TODO: add base functionality of CoNLLRDFFormatter  -- derive/extend Formatter from ...this...
	
		protected static final Logger LOG = LogManager.getLogger(TTLStreamLoader.class.getName());


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
