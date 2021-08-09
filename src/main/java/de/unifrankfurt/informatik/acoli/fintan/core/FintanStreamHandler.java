package de.unifrankfurt.informatik.acoli.fintan.core;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import sun.jvm.hotspot.runtime.Threads;


public class FintanStreamHandler<T> implements FintanInputStream<T>, FintanOutputStream<T> {

	private boolean active = true;
	private BlockingQueue<Object> queue = new ArrayBlockingQueue<Object>(100);
	private static final Object POISON_PILL = new Object();
	
	
	@Override
	public void terminate() {
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

	@Override
	public void write(T m) throws InterruptedException {
		if (!canWrite()) 
			throw new InterruptedException("Stream has already been marked for termination.");
		queue.put(m);
	}


	@Override
	/**
	 * Test for canRead() before taking the next Element.
	 * 
	 * @return parameterized Entry
	 * 		may return null in case the queue has been emptied and terminated.
	 */
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
