/*
 * Copyright [2021] [ACoLi Lab, Prof. Dr. Chiarcos, Christian Faeth, Goethe University Frankfurt]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.acoli.fintan.core.util;

import java.util.HashMap;

/**
 * Class for configuring custom CSV formats. 
 * Used e.g. for formatting SPARQL output.
 * 
 * @author Christian Faeth {@literal faeth@em.uni-frankfurt.de}
 *
 */
public class CustomCSVFormat {
	
    public static final CustomCSVFormat CoNLL = new CustomCSVFormat(null, "\t", null, "_");
    public static final CustomCSVFormat TSV = new CustomCSVFormat(null, "\t", null, null);
    public static final CustomCSVFormat CSV = new CustomCSVFormat(null, ",", null, null);
    
    private static final HashMap<String, CustomCSVFormat> formats = new HashMap<String, CustomCSVFormat>();
    static {
    	formats.put("conll", CoNLL);
    	formats.put("tsv", TSV);
    	formats.put("CSV", CSV);
    }

    public static CustomCSVFormat lookup(String name) {
    	return formats.get(name.toLowerCase().trim());
    }
	
	public final String escapeChar;
	public final String delimiterCSV;
	public final String quoteChar;
	public final String emptyChar;
	
	/**
	 * Create a custom format with the following parameters:
	 * 
	 * @param escapeChar
	 * @param delimiterCSV
	 * @param quoteChar
	 * @param emptyChar
	 */
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
	
	/**
	 * Escape and format a given String using this custom CSV format.
	 * @param content 
	 * 		The content of a column
	 * @return 
	 * 		the escaped and formatted content
	 */
	public String writeColumnContent(String content) {
		if(escapeChar != null) {
			if (!quoteChar.equals("")) {
				content = content.replace(quoteChar, escapeChar+quoteChar);
			} else if (!delimiterCSV.equals("")){
				content = content.replace(delimiterCSV, escapeChar+delimiterCSV);
			}
		}
		return quoteChar+content+quoteChar;
	}
	
}