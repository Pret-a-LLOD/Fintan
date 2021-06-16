package de.unifrankfurt.informatik.acoli.fintan.core;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;


public abstract class FintanStreamComponent implements Runnable {

	private HashMap<String,InputStream> inputStreams;
	private HashMap<String,OutputStream> outputStreams;

	//TODO: add segmentation delimiter handling
	//TODO: -TEXT-	StreamComponents: 	add data FORMAT handling
	//									add segmentation DELIMITER handling
	//		-RDF-	StreamComponents: 	add data MODEL / ONTOLOGY handling
	//		-BOTH- 	possibly to be moved to manager classes, or abstraction layer in Fintan Service.
	//				should be compatible with OpenAPI, elexis and teanga.
	
	public InputStream getInputStream() {
		return inputStreams.get("");
	}
	
	public InputStream getInputStream(String name) {
		return inputStreams.get(name);
	}

	public void setInputStream(InputStream inputStream) {
		this.inputStreams.put("", inputStream);
	}
	
	public void setInputStream(InputStream inputStream, String name) {
		this.inputStreams.put(name, inputStream);
	}

	public OutputStream getOutputStream() {
		return outputStreams.get("");
	}

	public OutputStream getOutputStream(String name) {
		return outputStreams.get(name);
	}

	public void setOutputStream(OutputStream outputStream) {
		this.outputStreams.put("", outputStream);
	}
	
	public void setOutputStream(OutputStream outputStream, String name) {
		this.outputStreams.put(name, outputStream);
	}


	public abstract void start();
	
	
	//@fintan-core: Factory Pattern
	//@fintan-submodules: implement 1xFactory, 1xComponent
	
	
}
