package de.unifrankfurt.informatik.acoli.fintan.text.split;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;

import com.fasterxml.jackson.databind.node.ObjectNode;

import de.unifrankfurt.informatik.acoli.fintan.core.FintanStreamComponent;
import de.unifrankfurt.informatik.acoli.fintan.core.FintanStreamComponentFactory;
import de.unifrankfurt.informatik.acoli.fintan.core.StreamTextSplitter;

public class SimpleLineBreakSplitter extends StreamTextSplitter implements FintanStreamComponentFactory {
	
	private BufferedReader inputStream;
	private PrintStream outputStream;
	
	@Override
	public void setInputStream(InputStream inputStream) {
		super.setInputStream(inputStream);
		this.inputStream = new BufferedReader(new InputStreamReader(super.getInputStream()));
	}
	
	@Override
	public void setOutputStream(OutputStream outputStream) {
		super.setOutputStream(outputStream);
		this.outputStream = new PrintStream(super.getOutputStream());
	}
	
	private void processStream() throws IOException {
		String line;
		int empty = 0;
		while((line = inputStream.readLine())!=null) {
			if (line.trim().isEmpty()) {
				empty++;
			} else {
				if (empty > 0) {
					outputStream.print(TTL_SEGMENTATION_DELIMITER);
					empty = 0;
				}
				outputStream.print(line+"\n");
			}
		}
		outputStream.close();
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

	@Override
	public FintanStreamComponent buildFromJsonConf(ObjectNode conf) {
		return new SimpleLineBreakSplitter();
	}

}
