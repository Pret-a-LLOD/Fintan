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
package org.acoli.fintan.genericIO;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.acoli.fintan.core.FintanStreamComponentFactory;
import org.acoli.fintan.core.StreamTransformerGenericIO;
import org.acoli.fintan.core.util.CustomCSVFormat;
import org.acoli.fintan.core.util.IOUtils;
import org.acoli.fintan.core.util.JenaUtils;
import org.apache.jena.sparql.resultset.ResultsFormat;

import com.fasterxml.jackson.databind.node.ObjectNode;


/**
 * Stream component which reads XML data and applies XSLT transformation to 
 * generate output.
 * 
 * Reads XML and writes Text as specified by an XSLT script.
 * 
 * @author Christian Faeth {@literal faeth@em.uni-frankfurt.de}
 *
 */
public class SQLStreamTransformer extends StreamTransformerGenericIO implements FintanStreamComponentFactory {


	/**
	 * The XSLTStreamTransformer uses the Saxon HE library to transform XML data using XSL transformation. It is implementing the StreamTransformerGenericIO interface and thus does not directly produce segmented RDF streams. It can be used stand-alone or for preprocessing steps in combination with the Loader or Splitter classes. 
	 * 
	 * In the current implementation, it supports only a single input and output stream. Therefore, only the default streams can be connected. The XSL scripts for conversion along their respective parameters can be supplied in a JSON configuration as follows:
	 * * `"xsl" : "path/to/script.xsl param1=value1 paramN=valueN"`
	 * * The parameters, values etc. can be passed on from the command line using the <$param0> wildcards as described in [Pipelines](2-run-pipelines.md).
	 * * The general syntax is the same as with the original Saxon CLI tools.
	 */
	public SQLStreamTransformer buildFromJsonConf(ObjectNode conf) throws IOException, IllegalArgumentException {
		SQLStreamTransformer transformer = new SQLStreamTransformer();
		transformer.setConfig(conf);
		
		if (conf.hasNonNull("driver")) {
			try {
				transformer.loadDriver(conf.get("driver").asText());
			} catch (ClassNotFoundException e) {
				throw new IllegalArgumentException("Driver could not be loaded. Please make sure to supply a fully qualified name.", e);
			}
		}
		
		if (conf.hasNonNull("connectUrl")) {
			transformer.setConnectUrl(conf.get("connectUrl").asText());
		}

		if (conf.hasNonNull("user")) {
			transformer.setUser(conf.get("user").asText());
		}

		if (conf.hasNonNull("password")) {
			transformer.setPassword(conf.get("password").asText());
		}
		
		if (conf.hasNonNull("query")) {
			transformer.setQuery(IOUtils.readSourceAsString(conf.get("query").asText()));
		}
		

		if (conf.hasNonNull("delimiter")) {
			transformer.setSegmentDelimiter(conf.get("delimiter").asText());
		}
		
		if (conf.hasNonNull("outFormat")) {
			transformer.setCustomFormat(CustomCSVFormat.lookup(conf.get("outFormat").asText()));
			if (transformer.getCustomFormat()==null) {
				throw new IllegalArgumentException("'"+conf.get("outFormat").asText()+"' is no valid OutputFormat.");
			}
		} else {
			String escapeChar=null;
			String delimiterCSV=null; 
			String quoteChar=null; 
			String emptyChar=null;
			if (conf.hasNonNull("escapeChar")) {
				escapeChar = conf.get("escapeChar").asText();
			}
			if (conf.hasNonNull("delimiterCSV")) {
				delimiterCSV = conf.get("delimiterCSV").asText();
			}
			if (conf.hasNonNull("quoteChar")) {
				quoteChar = conf.get("quoteChar").asText();
			}
			if (conf.hasNonNull("emptyChar")) {
				emptyChar = conf.get("emptyChar").asText();
			}
			transformer.setCustomFormat(new CustomCSVFormat(escapeChar, delimiterCSV, quoteChar, emptyChar));
		}
		return transformer;
	}

	public SQLStreamTransformer buildFromCLI(String[] args) throws IOException, IllegalArgumentException {
		// TODO Auto-generated method stub
		return null;
	}
	
	/**
	 * Override default method. Since no InputStream can be connected, 
	 * this always returns an array with the FINTAN_DEFAULT_STREAM_NAME.
	 * 
	 * Needed to skip link-state validation in FintanManager.
	 * 
	 */
	@Override
	public String[] listInputStreamNames() {
		return new String[] {FINTAN_DEFAULT_STREAM_NAME};
	}
	
	/**
	 * Override default method. No InputStreams supported for SQL databases.
	 * 
	 * @param inputStream
	 * @throws IOException if slot is not available (for overriding methods)
	 */
	@Override
	public void setInputStream(InputStream inputStream) throws IOException {
		throw new IOException("No InputStream is supported for "+SQLStreamTransformer.class.getName());
	}
	
	/**
	 * Overrides default method. No InputStreams supported for SQL databases.
	 * 
	 * @throws IOException if named stream is set.
	 */
	@Override
	public void setInputStream(InputStream inputStream, String name) throws IOException {
		throw new IOException("No InputStream is supported for "+SQLStreamTransformer.class.getName());
	}
	
	/**
	 * Overrides default method. Only accepts default stream.
	 * 
	 * @throws IOException if named stream is set.
	 */
	@Override
	public void setOutputStream(OutputStream outputStream, String name) throws IOException {
		if (name == null || FINTAN_DEFAULT_STREAM_NAME.equals(name)) {
			setOutputStream(outputStream);
		} else {
			throw new IOException("Only default OutputStream is supported for "+SQLStreamTransformer.class.getName());
		}
	}
	

	private String connectUrl;
	private String user;
	private String password;
	private String query;
	private String segmentDelimiter;
	private CustomCSVFormat customFormat;
	private Class driver;
	
	
	public void loadDriver(String jdbcDriver) throws ClassNotFoundException {
		driver = Class.forName(jdbcDriver);
	}
	
	public String getConnectUrl() {
		return connectUrl;
	}

	public void setConnectUrl(String connectUrl) {
		this.connectUrl = connectUrl;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getQuery() {
		return query;
	}

	public void setQuery(String query) {
		this.query = query;
	}

	public String getSegmentDelimiter() {
		return segmentDelimiter;
	}

	public void setSegmentDelimiter(String segmentDelimiter) {
		this.segmentDelimiter = segmentDelimiter;
	}

	public CustomCSVFormat getCustomFormat() {
		return customFormat;
	}

	public void setCustomFormat(CustomCSVFormat customFormat) {
		this.customFormat = customFormat;
	}
	
	

	private void processStream() throws IOException, SQLException, ClassNotFoundException {
		PrintStream out = new PrintStream(getOutputStream());
		
		Connection conn = DriverManager.getConnection(connectUrl, user, password);
		
		Statement stmt = conn.createStatement();
		ResultSet rs = stmt.executeQuery(query);
		outputCustomCSV(out, rs, customFormat);

		super.getOutputStream().close();
	}
	
	/**
	 * Consumes a Jena ResultSet and creates a custom serialization in the specified
	 * CustomCSVFormat.
	 * 
	 * @param out PrintStream to write to
	 * @param rs ResultSet to consume
	 * @param format CustomCSVFormat for serializing the ResultSet
	 * @throws SQLException 
	 */
	public void outputCustomCSV(PrintStream out, ResultSet rs, CustomCSVFormat format) throws SQLException {
        int colCount = rs.getMetaData().getColumnCount();
		while(rs.next()) {
			
			for(int i = 1; i <= colCount; i++) {
				//no delimiter before first column
				if (i > 1) 
					out.print(format.delimiterCSV);
				
				String content = rs.getString(i);
				if(content == null || content.equals("")) {
					out.print(format.emptyChar);		
				} else {
					out.print(format.writeColumnContent(content));
				}
			}
			
			out.println();

			if (segmentDelimiter != null) {
				out.println(segmentDelimiter);
			}
			
			out.flush();
		}
	}

	public void run() {
		try {
			processStream();	
		} catch (Exception e) {
			LOG.error(e, e);
			System.exit(1);
		}
	}

	@Override
	public void start() {
		run();
	}
}
