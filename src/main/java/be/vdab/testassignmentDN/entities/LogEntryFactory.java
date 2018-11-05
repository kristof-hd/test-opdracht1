package be.vdab.testassignmentDN.entities;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public enum LogEntryFactory {

	INSTANCE;

	public LogEntry createLogEntry(String regel) {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss,SSS");
		String timestampAlsString = regel.substring(0, 23);
		LocalDateTime timestamp = LocalDateTime.parse(timestampAlsString, formatter);
		String thread = regel.substring(24, regel.indexOf("]") + 1);
		String logMessage = regel.substring(regel.indexOf("]:") + 3);
		return new LogEntry(timestamp, thread, logMessage); 
	}

}
