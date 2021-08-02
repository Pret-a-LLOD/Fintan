package de.unifrankfurt.informatik.acoli.fintan.core;

import java.util.HashMap;

import com.fasterxml.jackson.databind.node.ObjectNode;


public abstract class FintanStreamComponent<In, Out> implements Runnable {

	public static final String FINTAN_DEFAULT_SEGMENT_DELIMITER_TTL = "###FINTAN#end#segment###";
	public static final String FINTAN_DEFAULT_SEGMENT_DELIMITER_CoNLL = "###FINTAN#end#segment###";
	public static final String FINTAN_DEFAULT_SEGMENT_DELIMITER_TSV = "###FINTAN#end#segment###";

	private ObjectNode config;
	private HashMap<String,In> inputStreams = new HashMap<String,In>();
	private HashMap<String,Out> outputStreams = new HashMap<String,Out>();

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

	public In getInputStream() {
		return inputStreams.get("");
	}
	
	public In getInputStream(String name) {
		return inputStreams.get(name);
	}

	public void setInputStream(In inputStream) {
		this.inputStreams.put("", inputStream);
	}
	
	public void setInputStream(In inputStream, String name) {
		this.inputStreams.put(name, inputStream);
	}

	public Out getOutputStream() {
		return outputStreams.get("");
	}

	public Out getOutputStream(String name) {
		return outputStreams.get(name);
	}

	public void setOutputStream(Out outputStream) {
		this.outputStreams.put("", outputStream);
	}
	
	public void setOutputStream(Out outputStream, String name) {
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
