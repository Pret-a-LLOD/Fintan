package de.unifrankfurt.informatik.acoli.fintan.genericIO;

import java.io.File;
import java.io.IOException;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamSource;

import com.fasterxml.jackson.databind.node.ObjectNode;

import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.Serializer;
import net.sf.saxon.s9api.Xslt30Transformer;
import net.sf.saxon.s9api.XsltCompiler;
import net.sf.saxon.s9api.XsltExecutable;
import net.sf.saxon.trans.CommandLineOptions;
import de.unifrankfurt.informatik.acoli.fintan.core.FintanStreamComponent;
import de.unifrankfurt.informatik.acoli.fintan.core.FintanStreamComponentFactory;
import de.unifrankfurt.informatik.acoli.fintan.core.StreamTransformerGenericIO;
import de.unifrankfurt.informatik.acoli.fintan.load.SegmentedRDFStreamLoader;

/**
 * Stream component which reads XML data and applies XSLT transformation to 
 * generate output.
 * 
 * Reads and writes Text
 * 
 * @author CF
 *
 */
public class XSLTStreamTransformer extends StreamTransformerGenericIO implements FintanStreamComponentFactory {


	public XSLTStreamTransformer buildFromJsonConf(ObjectNode conf) throws IOException, IllegalArgumentException {
		XSLTStreamTransformer transformer = new XSLTStreamTransformer();
		transformer.setConfig(conf);
		if (conf.hasNonNull("delimiterIn")) {
			transformer.setSegmentDelimiterIn(conf.get("delimiterIn").asText());
		}
		if (conf.hasNonNull("delimiterOut")) {
			transformer.setSegmentDelimiterOut(conf.get("delimiterOut").asText());
		}
		if (conf.hasNonNull("xsl")) {
			String xslArgString = "-xsl:"+conf.get("xsl").asText();
			transformer.setXslArgs(xslArgString.split("\\s+"));
			try {
				transformer.loadStylesheet(transformer.getXslArgs());
			} catch (SaxonApiException | TransformerException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			throw new IOException("XSLTStreamTransformer requires xsl parameter to be defined.");
		}
		return transformer;
	}

	public FintanStreamComponent buildFromCLI(String[] args) throws IOException, IllegalArgumentException {
		// TODO Auto-generated method stub
		return null;
	}
	
	
	private Processor processor;
	private XsltExecutable stylesheet;
	
	
	private String segmentDelimiterIn = FINTAN_DEFAULT_SEGMENT_DELIMITER_TTL;
	private String segmentDelimiterOut = FINTAN_DEFAULT_SEGMENT_DELIMITER_TTL;
	private String[] xslArgs;

	public String getSegmentDelimiterIn() {
		return segmentDelimiterIn;
	}

	public void setSegmentDelimiterIn(String segmentDelimiter) {
		this.segmentDelimiterIn = segmentDelimiter;
	}
	
	public String getSegmentDelimiterOut() {
		return segmentDelimiterOut;
	}

	public void setSegmentDelimiterOut(String segmentDelimiter) {
		this.segmentDelimiterOut = segmentDelimiter;
	}
	
	
	public String[] getXslArgs() {
		return xslArgs;
	}

	public void setXslArgs(String[] xslArgs) {
		this.xslArgs = xslArgs;
	}

	
	public XSLTStreamTransformer() {
		processor = new Processor(false);
	}
	
	public void loadStylesheet(String[] args) throws SaxonApiException, TransformerException {
		CommandLineOptions options = new CommandLineOptions();
		
		options.addRecognizedOption("xsl", CommandLineOptions.TYPE_FILENAME | CommandLineOptions.VALUE_REQUIRED,
                "Main stylesheet file");
        options.setActualOptions(args);
        
		XsltCompiler compiler = processor.newXsltCompiler();
		options.applyStaticParams(compiler);

		Source styleSource;
		
		String path = options.getOptionValue("xsl");
        if (CommandLineOptions.isImplicitURI(path)) {
            styleSource = processor.getUnderlyingConfiguration().getURIResolver().resolve(path, null);
            if (styleSource == null) {
                styleSource = processor.getUnderlyingConfiguration().getSystemURIResolver().resolve(path, null);
            }
            stylesheet = compiler.compile(styleSource);
        } else if (path != null) {
            stylesheet = compiler.compile(new StreamSource(new File(path)));
        }
	}
	
	private void processStream() throws IOException, SaxonApiException {
		Serializer out = processor.newSerializer(super.getOutputStream());
		out.setOutputProperty(Serializer.Property.METHOD, "text");
		out.setOutputProperty(Serializer.Property.INDENT, "yes");
		
		Xslt30Transformer transformer = stylesheet.load30();
		transformer.transform(new StreamSource(super.getInputStream()), out);
		super.getOutputStream().close();
	}

	public void run() {
		try {
			processStream();	
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
	}

	@Override
	public void start() {
		run();
	}

	public static void main(String[] args) throws Exception {
		System.err.println("synopsis: XSLTStreamLoader -xsl:PATH [param=value]*");
		XSLTStreamTransformer transformer = new XSLTStreamTransformer();

		long start = System.currentTimeMillis();

		transformer.setInputStream(System.in);
		transformer.setOutputStream(System.out);
		transformer.loadStylesheet(args);

		transformer.processStream();
		System.err.println(((System.currentTimeMillis()-start)/1000 + " seconds"));
	}



}
