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

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;


/**
 * Implements a BlockingQueue for streaming non-serialized objects between threads.
 * 
 * 
 * 
 * @author Christian Faeth {@literal faeth@em.uni-frankfurt.de}
 *
 * @param <T> The type of object to be streamed.
 */
public class FintanStreamHandler<T> implements FintanInputStream<T>, FintanOutputStream<T> {

	private boolean active = true;
	private BlockingQueue<Object> queue = new ArrayBlockingQueue<Object>(100);
	private static final Object POISON_PILL = new Object();
	
	
	@Override
	public void terminate() {
		if (active) {
			active = false;
			if (queue.isEmpty())
				//in case a reader Thread is already waiting, add poison pill.
				//this is recommended for BlockingQueues:  "Java Concurrency in Practice", pp. 155-156
				//only one poison pill is required for multiple threads, since read() is synchronized.
				//ONLY T1 waits for take(), 
				//  T2...Tn wait for T1(synchronized read()), 
				//if pp is swallowed by T1(sync), T1 returns null.
				//  T2..Tn then checks for canRead(); terminates the regular way
				queue.add(POISON_PILL);
		}
	}
	
	@Override
	public boolean canRead() {
		return active || queue.size()>0;
	}
	
	@Override
	public boolean canWrite() {
		return active;
	}
	
	@Override
	public boolean active() {
		return active;
	}

	/**
	 * Write will block if buffer is full. Calling thread will resume operation
	 * as soon as buffer is free.
	 */
	@Override
	public void write(T m) throws InterruptedException {
		if (!canWrite()) 
			throw new InterruptedException("Stream has already been marked for termination.");
		queue.put(m);
	}


	/**
	 * Test for canRead() before taking the next Element.
	 * If queue is empty on take operation, block the calling thread. It will 
	 * resume operation, as soon as data is available or stream is terminated.
	 * 
	 * Method is synchronized. If more than one thread attempts to read(), the
	 * other ones will be blocked until the read operation is finished.
	 * 
	 * @return parameterized Entry
	 * 		may return null in case the queue has been emptied and terminated.
	 */
	@Override
	public synchronized T read() throws InterruptedException {
		if (!canRead()) return null;
		Object obj = queue.take();
		
		//this is recommended for BlockingQueues:  "Java Concurrency in Practice", pp. 155-156
		//only one poison pill is required for multiple threads, since read() is synchronized.
		//ONLY T1 waits for take(), 
		//  T2...Tn wait for T1(synchronized read()), 
		//if pp is swallowed by T1(sync), T1 returns null.
		//  T2..Tn then checks for canRead(); terminates the regular way
		if (obj == POISON_PILL) {
			return null;
		} else {
			return (T) obj;
		}
	}
	
	



	
}
