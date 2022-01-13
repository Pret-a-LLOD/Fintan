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
package org.acoli.fintan.load;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.acoli.fintan.core.FintanManager;
import org.acoli.fintan.core.FintanStreamComponentFactory;
import org.acoli.fintan.core.StreamLoader;
import org.acoli.fintan.core.util.IOUtils;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDF;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import com.fasterxml.jackson.databind.node.ObjectNode;

import tbx2rdf.Main;
import tbx2rdf.Mappings;
import tbx2rdf.SAXHandler;
import tbx2rdf.TBX2RDF_Converter;
import tbx2rdf.TBXFormatException;
import tbx2rdf.datasets.iate.SubjectFields;
import tbx2rdf.types.LexicalEntry;
import tbx2rdf.types.MartifHeader;
import tbx2rdf.types.TBX_Terminology;
import tbx2rdf.types.Term;
import tbx2rdf.vocab.DC;
import tbx2rdf.vocab.IATE;
import tbx2rdf.vocab.LIME;
import tbx2rdf.vocab.ONTOLEX;
import tbx2rdf.vocab.TBX;

/**
 * Uses tbx2rdf for converting TBX data to RDF.. 
 * The [tbx2rdf](https://github.com/cimiano/tbx2rdf) library originally designed 
 * using Jena 2 which employed different class names and packages than newer 
 * releases. In order to ensure compatiblity with Fintan's segmented RDF streams, 
 * Fintan imports a [fork of tbx2rdf](https://github.com/cfaeth/tbx2rdf) whose 
 * dependencies are kept concurrent with the Fintan backend releases.
 * 
 * The original design of tbx2rdf allows for two different modes of operation:
 *   * The `default` mode attempts to parse convert and output a file as is in a
 *     streamed operation. To be used in Fintan pipelines, it expects the TBX 
 *     data on the default input stream slot and writes the converted RDF data 
 *     to the default output stream. Named output streams are unnecessary and 
 *     not supported.
 *   * The `bigFile` mode instead caches the default input stream in a file and 
 *     does preprocessing steps independently from the bulk data. Therefore it 
 *     requires multiple output streams to be defined:
 *     * `http://martifHeader`
 *     * `http://subjectFields`
 *     * `http://lexicons`
 *     * the default output stream carries the bulk data
 *     
 * For both modes of operation, there are [example configurations](samples/tbx2rdf).
 * 
 * Output: Segmented RDF streams (Loader class).
 * 
 * @author Christian Faeth {@literal faeth@em.uni-frankfurt.de}
 *
 */
public class TBX2RDFStreamLoader extends StreamLoader implements FintanStreamComponentFactory {

	public TBX2RDFStreamLoader buildFromJsonConf(ObjectNode conf) throws IOException, IllegalArgumentException {
		TBX2RDFStreamLoader loader = new TBX2RDFStreamLoader();
		loader.setConfig(conf);

		if (conf.hasNonNull("mappings")) {
			String mapping_file = conf.get("mappings").asText();
			InputStreamReader reader = new InputStreamReader(IOUtils.parseAsInputStream(mapping_file));
			loader.setMappings(Mappings.readInMappings(reader));
		}
		
		if (conf.hasNonNull("lenient")) {
			Main.lenient = conf.get("lenient").asBoolean();
		}
		
		if (conf.hasNonNull("bigFile")) {
			loader.setBigFile(conf.get("bigFile").asBoolean());
		}
		
		if (conf.hasNonNull("namespace")) {
			try {
				loader.setNamespace(conf.get("namespace").asText());
			} catch (URISyntaxException e) {
				throw new IllegalArgumentException(e);
			}
		}
		
		if (conf.hasNonNull("cachePath")) {
			loader.initCache(conf.get("cachePath").asText());
		} else {
			loader.initCache(null);
		}
		return loader;
	}

	public TBX2RDFStreamLoader buildFromCLI(String[] args) throws IOException, IllegalArgumentException {
		// TODO Auto-generated method stub
		return null;
	}
	
	private Mappings mappings;
	private String namespace = Main.DATA_NAMESPACE;
	private boolean bigFile = false;
	private String cachePath = FintanManager.DEFAULT_CACHE_PATH;
	
	public static final String URI_MAPPINGS = "http://mappings";
	public static final String URI_MARTIF_HEADER = "http://martifHeader";
	public static final String URI_SUBJECT_FIELDS = "http://subjectFields";
	public static final String URI_LEXICONS = "http://lexicons";
	

	
	public Mappings getMappings() {
		return mappings;
	}

	public void setMappings(Mappings mappings) {
		this.mappings = mappings;
	}

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(String namespace) throws URISyntaxException {
		new URI(namespace);
		this.namespace = namespace;
	}

	public String getCachePath() {
		return cachePath;
	}

	public boolean isBigFile() {
		return bigFile;
	}

	public void setBigFile(boolean bigFile) {
		this.bigFile = bigFile;
	}
	
	public void initCache(String path) {
		if (path == null) path = FintanManager.DEFAULT_CACHE_PATH;
		if (!path.endsWith("/")) path+="/";
		File f = new File(path+this.getClass().getName()+this.hashCode()+"/");
		if (f.exists() && f.isDirectory()) {
			try {
				FileUtils.deleteDirectory(f);
			} catch (IOException e) {
				LOG.error("Could not delete directory <"+f.getAbsolutePath()+">. "
						+ "Preexisting data may corrupt the current stream! "
						+ "Error message:"+e);
			}
		}
		f.mkdirs();
		Runtime.getRuntime().addShutdownHook(new Thread(() -> FileUtils.deleteQuietly(f)));
		this.cachePath = f.getAbsolutePath()+"/";
	}
	
	private void processStream() throws Exception {
		try {
			if (mappings == null) {
				mappings = Mappings.readInMappings(new InputStreamReader(getInputStream(URI_MAPPINGS)));
			}
			if (isBigFile()) {
				convertBigFileCached();
			} else {
				convertSmallFile();
			}
		}catch (Exception e) {
			LOG.error(e, e);
			throw e;
		} finally {
			for (String name:listOutputStreamNames()) {
				getOutputStream(name).terminate();
			}
		}
	}
	
	 /**
     * This is the conversion to be invoked for large files, that will be processed in a stream
     * The output is serialized as the conversion is being done     
	 * @throws Exception 
     */
    private void convertBigFileCached() throws Exception {

    	String file = cachePath+"temp.xml";
    	FileUtils.copyInputStreamToFile(getInputStream(), new File(file));


    	TBX2RDF_Converter converter = new TBX2RDF_Converter();

    	//          taken from  converter.convertAndSerializeLargeFile(input_file, out, mappings);
    	String resourceURI = new String(namespace);
    	FileInputStream inputStream = null;
    	Scanner sc = null;
    	int count = 0;
    	int errors = 0;

    	//We first count the lexicons we have
    	SAXHandler handler = null;
    	HashMap<String, Resource> lexicons = new HashMap();
    	InputStream xmlInput = new FileInputStream(file);
    	SAXParserFactory factory = SAXParserFactory.newInstance();
    	SAXParser saxParser = factory.newSAXParser();
    	handler = new SAXHandler(mappings);
    	saxParser.parse(xmlInput, handler);
    	lexicons = handler.getLexicons(namespace);
    	xmlInput.close();

    	//WE PROCESS HERE THE MARTIF HEADER
    	MartifHeader martifheader = converter.extractAndReadMartifHeader(file, mappings);


    	if (martifheader==null)
    		throw new ParseException("No martifHeader found.");

    	//First we serialize the header
    	Model mdataset = ModelFactory.createDefaultModel();
    	//The whole dataset!
    	final Resource rdataset = mdataset.createResource(resourceURI);
    	rdataset.addProperty(DCTerms.type, handler.getMartifType());
    	//This should be generalized
    	rdataset.addProperty(RDF.type, mdataset.createResource("http://www.w3.org/ns/dcat#Dataset"));
    	rdataset.addProperty(DC.rights, IATE.rights);
    	rdataset.addProperty(DC.source, IATE.iate);
    	rdataset.addProperty(DC.attribution, "Download IATE, European Union, 2014");
    	martifheader.toRDF(mdataset, rdataset);

    	getOutputStream(URI_MARTIF_HEADER).write(mdataset);
    	getOutputStream(URI_MARTIF_HEADER).terminate();


    	Model msubjectFields = SubjectFields.generateSubjectFields();

    	getOutputStream(URI_SUBJECT_FIELDS).write(msubjectFields);
    	getOutputStream(URI_SUBJECT_FIELDS).terminate();


    	//We declare that every lexicon belongs to 
    	Iterator it = lexicons.entrySet().iterator();
    	Property prootresource=mdataset.createProperty("http://www.w3.org/TR/void/rootResource");
    	while (it.hasNext()) {
    		Map.Entry e = (Map.Entry) it.next();
    		Resource rlexicon = (Resource) e.getValue();
    		rlexicon.addProperty(prootresource, rdataset);
    	}


    	boolean dentro = false;
    	try {
    		inputStream = new FileInputStream(file);
    		sc = new Scanner(inputStream, "UTF-8");
    		String xml = "";

    		while (sc.hasNextLine()) {
    			String line = sc.nextLine();
    			//We identify the terms by scanning the strings. Not a very nice practice, though.
    			int index = line.indexOf("<termEntry");
    			if (index != -1) {
    				dentro = true;
    				xml = line.substring(index) + "\n";
    			}
    			if (dentro == true && index == -1) {
    				xml = xml + line + "\n";
    			}
    			index = line.indexOf("</termEntry>");
    			if (index != -1) {
    				xml = xml + line.substring(0, index) + "\n";
    				count++;
    				//We do a partial parsing of this XML fragment
    				Document doc = TBX2RDF_Converter.loadXMLFromString(xml);
    				if (doc == null) {
    					continue;
    				}
    				Element root = doc.getDocumentElement();
    				if (root != null) {
    					try {
    						Term term = converter.processTermEntry(root, mappings);
    						Model model = ModelFactory.createDefaultModel();
    						TBX.addPrefixesToModel(model);
    						model.setNsPrefix("", namespace);
    						final Resource rterm = term.getRes(model);
    						rterm.addProperty(RDF.type, ONTOLEX.Concept);
    						term.toRDF(model, rterm);
    						for (LexicalEntry le : term.Lex_entries) {
    							final Resource lexicon = lexicons.get(le.lang);
    							lexicon.addProperty(LIME.entry, le.getRes(model));
    							le.toRDF(model, rterm);
    						}
    						getOutputStream().write(model);
    					} catch (Exception e) {
    						errors++;
    						LOG.error("Error " + e.getMessage());
    					}
    					if (count % 1000 == 0) {
    						LOG.error("Total: " + count + " Errors: " + errors);
    					}
    				}
    				xml = "";
    			}
    		} //end of while

    		getOutputStream().terminate();

    		//Now we serialize the lexicons
    		getOutputStream(URI_LEXICONS).write(handler.getLexiconsModel());
    		getOutputStream(URI_LEXICONS).terminate();



    		// note that Scanner suppresses exceptions
    		if (sc.ioException() != null) {
    			throw sc.ioException();
    		}
    	} catch (Exception e) {
    		throw e;
    	} finally {
    		if (sc != null) {
    			sc.close();
    		}
    	}
    }

    /**
     * Standard conversion
     * This is the conversion invoked from the web service. 
     * Input file is read as a whole and kept in memory.
     * @throws IOException 
     * @throws SAXException 
     * @throws ParserConfigurationException 
     * @throws TBXFormatException 
     * @throws InterruptedException 
     */
    private void convertSmallFile() throws IOException, TBXFormatException, ParserConfigurationException, SAXException, InterruptedException {
            BufferedReader reader = new BufferedReader(new InputStreamReader(getInputStream()));
            TBX2RDF_Converter converter = new TBX2RDF_Converter();
            TBX_Terminology terminology = converter.convert(reader, mappings);
            getOutputStream().write(terminology.getModel(namespace));  
            getOutputStream().terminate();
            reader.close();

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
