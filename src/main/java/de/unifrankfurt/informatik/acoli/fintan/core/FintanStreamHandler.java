package de.unifrankfurt.informatik.acoli.fintan.core;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;


public class FintanStreamHandler<T> implements FintanInputStream<T>, FintanOutputStream<T> {

	private boolean active = true;
	private BlockingQueue<T> queue = new ArrayBlockingQueue<T>(100);
	
	
	@Override
	public synchronized void terminate() {
		active = false;
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
	public synchronized void write(T m) throws InterruptedException {
		queue.put(m);
	}

	@Override
	public T read() throws InterruptedException {
		if (!canRead()) return null;
		return queue.take();
	}
	
	



	
}
