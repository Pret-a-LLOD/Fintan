package de.unifrankfurt.informatik.acoli.fintan.core;

import com.fasterxml.jackson.databind.node.ObjectNode;

public interface FintanStreamComponentFactory {
	
	public FintanStreamComponent buildFromJsonConf(ObjectNode conf);

}
