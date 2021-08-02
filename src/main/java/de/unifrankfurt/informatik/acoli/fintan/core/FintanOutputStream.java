package de.unifrankfurt.informatik.acoli.fintan.core;

public interface FintanOutputStream<T> {
	
	public void write(T m) throws InterruptedException;
	
	public boolean canWrite();
	
	public boolean active();

	public void terminate();
}
