package org.acoli.fintan.genericIO;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamSource;

import org.acoli.fintan.core.FintanStreamComponentFactory;
import org.acoli.fintan.core.StreamTransformerGenericIO;

import com.fasterxml.jackson.databind.node.ObjectNode;

import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.Serializer;
import net.sf.saxon.s9api.Xslt30Transformer;
import net.sf.saxon.s9api.XsltCompiler;
import net.sf.saxon.s9api.XsltExecutable;
import net.sf.saxon.trans.CommandLineOptions;

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
			throw new IllegalArgumentException("XSLTStreamTransformer requires xsl parameter to be defined.");
		}
		return transformer;
	}

	public XSLTStreamTransformer buildFromCLI(String[] args) throws IOException, IllegalArgumentException {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public void setInputStream(InputStream inputStream, String name) throws IOException {
		if (name == null || FINTAN_DEFAULT_STREAM_NAME.equals(name)) {
			setInputStream(inputStream);
		} else {
			throw new IOException("Only default InputStream is supported for "+XSLTStreamTransformer.class.getName());
		}
	}
	
	@Override
	public void setOutputStream(OutputStream outputStream, String name) throws IOException {
		if (name == null || FINTAN_DEFAULT_STREAM_NAME.equals(name)) {
			setOutputStream(outputStream);
		} else {
			throw new IOException("Only default OutputStream is supported for "+XSLTStreamTransformer.class.getName());
		}
	}
	
	
	private Processor processor;
	private XsltExecutable stylesheet;
	
	private String[] xslArgs;
	
	
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
			LOG.error(e, e);
			System.exit(1);
		}
	}

	@Override
	public void start() {
		run();
	}
}
