package de.unifrankfurt.informatik.acoli.fintan.core;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;

import com.fasterxml.jackson.databind.node.ObjectNode;


public abstract class FintanStreamComponent implements Runnable {

	public static final String FINTAN_DEFAULT_SEGMENT_DELIMITER_TTL = "###FINTAN#end#segment###";
	public static final String FINTAN_DEFAULT_SEGMENT_DELIMITER_CoNLL = "###FINTAN#end#segment###";
	public static final String FINTAN_DEFAULT_SEGMENT_DELIMITER_TSV = "###FINTAN#end#segment###";

	private ObjectNode config;
	private HashMap<String,InputStream> inputStreams = new HashMap<String,InputStream>();
	private HashMap<String,OutputStream> outputStreams = new HashMap<String,OutputStream>();

	//TODO: add segmentation delimiter handling
	//TODO: -TEXT-	StreamComponents: 	add data FORMAT handling
	//									add segmentation DELIMITER handling
	//		-RDF-	StreamComponents: 	add data MODEL / ONTOLOGY handling
	//		-BOTH- 	possibly to be moved to manager classes, or abstraction layer in Fintan Service.
	//				should be compatible with OpenAPI, elexis and teanga.
	
	public ObjectNode getConfig() {
		return config;
	}

	public void setConfig(ObjectNode config) {
		this.config = config;
	}

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
	
	public String[] listInputStreamNames() {
		return inputStreams.keySet().toArray(new String[] {});
	}
	
	public String[] listOutputStreamNames() {
		return outputStreams.keySet().toArray(new String[] {});
	}


	public abstract void start();
	
	
	//@fintan-core: Factory Pattern
	//@fintan-submodules: implement 1xFactory, 1xComponent
	
	
}
