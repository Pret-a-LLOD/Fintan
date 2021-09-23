package org.acoli.fintan.core;

public interface FintanInputStream<T> {
	
	public T read() throws InterruptedException;
	
	public boolean canRead();
	
	public boolean active();
	
	public void terminate();

}
