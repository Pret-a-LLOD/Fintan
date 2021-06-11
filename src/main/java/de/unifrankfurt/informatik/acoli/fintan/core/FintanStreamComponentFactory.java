package de.unifrankfurt.informatik.acoli.fintan.core;

import java.io.IOException;

import com.fasterxml.jackson.databind.node.ObjectNode;

public interface FintanStreamComponentFactory {
	
	public FintanStreamComponent buildFromJsonConf(ObjectNode conf) throws IOException, IllegalArgumentException;
	public FintanStreamComponent buildFromCLI(String[] args) throws IOException, IllegalArgumentException;

}
