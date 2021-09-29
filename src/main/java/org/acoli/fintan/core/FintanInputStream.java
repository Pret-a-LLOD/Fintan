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
package org.acoli.fintan.core;

/**
 * Interface with basic read operations for non-serialized object streams.
 * Primarily used for segmented RDF streams (T = Model).
 * 
 * @author Christian Faeth {@literal faeth@em.uni-frankfurt.de}
 *
 * @param <T> The type of object to be streamed.
 */
public interface FintanInputStream<T> {
	
	/**
	 * Reads an object of type <T> from the stream.
	 * @return object of type <T>
	 * @throws InterruptedException if waiting thread is interrupted.
	 */
	public T read() throws InterruptedException;
	
	/**
	 * Checks whether data can be read from the stream.
	 * @return true if data is available.
	 */
	public boolean canRead();
	
	/**
	 * Checks whether this stream is still active.
	 * @return true if stream is not fully terminated yet.
	 */
	public boolean active();
	
	/**
	 * Terminate the stream. No further data can be written to the write end.
	 */
	public void terminate();

}
