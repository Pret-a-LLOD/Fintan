package de.unifrankfurt.informatik.acoli.fintan.core;

import java.io.InputStream;
import java.io.OutputStream;


public abstract class FintanStreamComponent implements Runnable {

	private InputStream inputStream;
	private OutputStream outputStream;
	private InputStream inputStream2;
	private OutputStream outputStream2;

	//TODO: add segmentation delimiter handling
	//TODO: -TEXT-	StreamComponents: 	add data FORMAT handling
	//									add segmentation DELIMITER handling
	//		-RDF-	StreamComponents: 	add data MODEL / ONTOLOGY handling
	//		-BOTH- 	possibly to be moved to manager classes, or abstraction layer in Fintan Service.
	//				should be compatible with OpenAPI, elexis and teanga.
	
	public InputStream getInputStream() {
		return inputStream;
	}

	public void setInputStream(InputStream inputStream) {
		this.inputStream = inputStream;
	}

	public OutputStream getOutputStream() {
		return outputStream;
	}

	public void setOutputStream(OutputStream outputStream) {
		this.outputStream = outputStream;
	}

	
	//OPTIONAL secondary I/O (especially for unprocessed bulk-data in splitters/combiners)
	public InputStream getInputStream2() {
		return inputStream2;
	}

	public void setInputStream2(InputStream inputStream2) {
		this.inputStream2 = inputStream2;
	}

	public OutputStream getOutputStream2() {
		return outputStream2;
	}

	public void setOutputStream2(OutputStream outputStream2) {
		this.outputStream2 = outputStream2;
	}

	public abstract void start();
	
	
	//@fintan-core: Factory Pattern
	//@fintan-submodules: implement 1xFactory, 1xComponent
	
	
}
