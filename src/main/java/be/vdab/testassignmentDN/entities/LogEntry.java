package be.vdab.testassignmentDN.entities;

import java.time.LocalDateTime;

public class LogEntry {

	private LocalDateTime timestamp; 
	private String thread; 
	private String logMessage; 
	
	public LogEntry(LocalDateTime timestamp, String thread, String logMessage) {
		this.timestamp = timestamp;
		this.thread=thread;
		this.logMessage=logMessage; 
	}

	public LocalDateTime getTimestamp() {
		return timestamp;
	}

	public String getThread() {
		return thread;
	}

	public String getLogMessage() {
		return logMessage;
	}
	
}
