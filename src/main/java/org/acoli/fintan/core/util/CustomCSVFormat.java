package org.acoli.fintan.core.util;

import java.util.HashMap;

public class CustomCSVFormat {
	
    public static final CustomCSVFormat CoNLL = new CustomCSVFormat(null, "\t", null, "_");
    
    private static final HashMap<String, CustomCSVFormat> formats = new HashMap<String, CustomCSVFormat>();
    static {
    	formats.put("conll", CoNLL);
    }

    public static CustomCSVFormat lookup(String name) {
    	return formats.get(name.toLowerCase().trim());
    }
	
	public final String escapeChar;
	public final String delimiterCSV;
	public final String quoteChar;
	public final String emptyChar;
	
	public CustomCSVFormat(String escapeChar, String delimiterCSV, String quoteChar, String emptyChar) {
		this.escapeChar = escapeChar;
		if (delimiterCSV == null)
			this.delimiterCSV = "";
		else
			this.delimiterCSV = delimiterCSV;
		if (quoteChar == null)
			this.quoteChar = "";
		else 
			this.quoteChar = quoteChar;
		if (emptyChar == null)
			this.emptyChar = "";
		else
			this.emptyChar = emptyChar;
	}
	
}