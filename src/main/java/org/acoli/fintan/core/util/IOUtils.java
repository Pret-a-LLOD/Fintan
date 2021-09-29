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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URL;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Utilities for accessing streams, files and web resources in Fintan. 
 * 
 * @author Christian Faeth {@literal faeth@em.uni-frankfurt.de}
 *
 */
public class IOUtils {

	private static boolean sysInOccupied = false;
	private static boolean sysOutOccupied = false;
	
	/**
	 * Parse a given path or URL as an InputStream.
	 * 
	 * Supports GZIP (if pathOrURL ends with .gz)
	 * 
	 * @param pathOrURL as String
	 * @return InputStream
	 * @throws IOException if resource cannot be accessed.
	 */
	public static InputStream parseAsInputStream(String pathOrURL) throws IOException {
		InputStream inputStream;
		File f = new File(pathOrURL);
		if (f.canRead()) {
			inputStream = new FileInputStream(f);
		} else {
			URL url = new URL(pathOrURL);
			inputStream = url.openStream();
		}
		if (pathOrURL.endsWith(".gz")) 
			inputStream = new GZIPInputStream(inputStream);
		return inputStream;
	}
	
	/**
	 * Parse a given path as an OutputStream.
	 * Create subfolders if necessary.
	 * 
	 * Supports GZIP (if path ends with .gz)
	 * 
	 * @param path as String
	 * @return OutputStream
	 * @throws IOException if location cannot be accessed.
	 */
	public static OutputStream parseAsOutputStream(String path) throws IOException {
		OutputStream outputStream;
		File f = new File(path);
		if (!f.canWrite()) {
			f.getParentFile().mkdirs();
			if (!f.createNewFile()) {
				throw new IOException("Could not write to " + path);
			}
		}
		outputStream = new FileOutputStream(f);
		if (path.endsWith(".gz")) 
			outputStream = new GZIPOutputStream(outputStream);
		outputStream = new PrintStream(outputStream);
		return outputStream;
	}

	/**
	 * Same as parseAsInputStream(String pathOrURL)
	 * 
	 * if confEntry is "System.in" return System.in
	 * 
	 * @param confEntry as String
	 * @return InputStream
	 * @throws IOException if location cannot be accessed or System.in is occupied.
	 */
	public static InputStream parseConfEntryAsInputStream(String confEntry) throws IOException {
		InputStream input;
		if (confEntry.equals("System.in")) {
			if (sysInOccupied)
				throw new IOException("System.in can only be defined as input for one Instance");
			input = System.in;
			sysInOccupied = true;
		} else {
			input = parseAsInputStream(confEntry);
		}
		return input;
	}
	
	/**
	 * Same as parseAsOutputStream(String pathOrURL)
	 * 
	 * if confEntry is "System.out" return System.out
	 * 
	 * @param confEntry as String
	 * @return OutputStream
	 * @throws IOException if location cannot be accessed or System.out is occupied.
	 */
	public static OutputStream parseConfEntryAsOutputStream(String confEntry) throws IOException {
		OutputStream output;
		if (confEntry.equals("System.out")) {
			if (sysOutOccupied)
				throw new IOException("System.out can only be defined as output for one Instance");
			output = System.out;
			sysOutOccupied = true;
		} else {
			output = parseAsOutputStream(confEntry);
		}
		return output;
	}
	
	/**
	 * Reads the content of a given path or URL as String. Can be used for reading queries etc.
	 * @param pathOrURL
	 * @return file content as String
	 * @throws IOException if unreadable.
	 */
	public static String readSourceAsString (String pathOrURL) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(parseAsInputStream(pathOrURL)));
		
		StringBuilder out = new StringBuilder();
		for (String line = reader.readLine(); line != null; line = reader.readLine())
			out.append(line + "\n");
		return out.toString();
	}
}
