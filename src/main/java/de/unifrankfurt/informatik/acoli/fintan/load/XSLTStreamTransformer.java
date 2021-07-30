package de.unifrankfurt.informatik.acoli.fintan.load;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamSource;

import org.xml.sax.XMLReader;

import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.Serializer;
import net.sf.saxon.s9api.Xslt30Transformer;
import net.sf.saxon.s9api.XsltCompiler;
import net.sf.saxon.s9api.XsltExecutable;
import net.sf.saxon.trans.CommandLineOptions;
import net.sf.saxon.trans.XPathException;
import de.unifrankfurt.informatik.acoli.fintan.core.FintanStreamComponent;
import de.unifrankfurt.informatik.acoli.fintan.core.StreamLoader;

/**
 * Stream component which reads XML data and applies XSLT transformation to 
 * generate output.
 * 
 * Reads and writes Text
 * 
 * @author CF
 *
 */
public class XSLTStreamTransformer extends FintanStreamComponent {

	//TODO: double check compatibility and add support for multiple streams.
	
	private Processor processor;
	private XsltExecutable stylesheet;
	
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
	}

	@Override
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
		XSLTStreamTransformer splitter = new XSLTStreamTransformer();

		long start = System.currentTimeMillis();

		splitter.setInputStream(System.in);
		splitter.setOutputStream(System.out);
		splitter.loadStylesheet(args);

		splitter.processStream();
		System.err.println(((System.currentTimeMillis()-start)/1000 + " seconds"));
	}


}
