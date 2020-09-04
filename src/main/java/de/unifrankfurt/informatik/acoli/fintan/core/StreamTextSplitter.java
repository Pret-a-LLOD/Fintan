package de.unifrankfurt.informatik.acoli.fintan.core;

public abstract class StreamTextSplitter extends FintanStreamComponent {
	//highly recommended for use in TTL and other formats allowing #-comments
	public static final String TTL_SEGMENTATION_DELIMITER = "\n#newFintanSegment\n";

}
