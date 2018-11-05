package be.vdab.testassignmentDN;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import be.vdab.testassignmentDN.entities.LogEntry;
import be.vdab.testassignmentDN.entities.ReportEntry;
import be.vdab.testassignmentDN.main.TestAssignmentDNMain;

public class MainTest {

	private List<LogEntry> logEntries = new LinkedList<>();
	private List<ReportEntry> reportEntries = new LinkedList<>();
	private List<ReportEntry> reportEntriesUpdated = new LinkedList<>();
	private static final String MESSAGE_FIRST_ENTRY = "Object to encode for ObjectId { com.dn.gaverzicht.dms.models.DocumentStatus - 19537 } (encoding depth = 1): { DocumentStatus: 19537 }";	
	private static final String MESSAGE_LAST_ENTRY = "Object to encode for ObjectId { com.dn.gaverzicht.dms.models.TreeNode - 166 } (encoding depth = 0): null";
	
	@Before 
	public void before() { 
		TestAssignmentDNMain.readLogFile(logEntries); 
		TestAssignmentDNMain.analyzeData(logEntries, reportEntries); 
		TestAssignmentDNMain.generateTxtFile(reportEntries);
		TestAssignmentDNMain.analyzeMultipleOccurrences(reportEntries, reportEntriesUpdated);
	}

	@Test
	public void readLogFile() {
		LogEntry firstEntry = ((LinkedList<LogEntry>) logEntries).getFirst();
		assertEquals("2010-10-06T09:02:10.357", firstEntry.getTimestamp().toString());
		assertEquals("[WorkerThread-5]", firstEntry.getThread()); 
		assertEquals(MESSAGE_FIRST_ENTRY, firstEntry.getLogMessage());

		LogEntry lastEntry = ((LinkedList<LogEntry>) logEntries).getLast(); 
		assertEquals("2010-10-07T11:01:16.574", lastEntry.getTimestamp().toString());
		assertEquals("[WorkerThread-12]", lastEntry.getThread()); 
		assertEquals(MESSAGE_LAST_ENTRY, lastEntry.getLogMessage());

		assertEquals(180902, logEntries.size()+TestAssignmentDNMain.getExceptionLines());
	}

	@Test
	public void analyzeData() {

		//Correctly retrieving the first report entry:  
		ReportEntry firstEntry = ((LinkedList<ReportEntry>) reportEntries).getFirst();
		assertEquals("[2010-10-06T09:02:13.631]", firstEntry.getTimestampsStartRendering().toString()); 
		assertEquals("[2010-10-06T09:02:14.825]", firstEntry.getTimestampsGetRendering().toString()); 
		assertEquals(114466, firstEntry.getDocumentId());
		assertEquals(0, firstEntry.getPageNumber()); 
		assertEquals("1286373733634-5423", firstEntry.getUID()); 

		//Correctly retrieving a report entry, for data located in the middle portion of the log file: 
		assertEquals(1, reportEntries.stream()
										.filter(entry1 -> 
												entry1.getDocumentId()==115623
												&& entry1.getPageNumber()==0
												&& entry1.getUID().equals("1286381499728-5775")
												&& entry1.getTimestampsStartRendering().toString().equals("[2010-10-06T11:11:39.725]")
												&& entry1.getTimestampsGetRendering().toString().equals("[2010-10-06T11:11:41.375]"))
										.count());
		
		//Do we handle ALL the startRendering requests? 937 is the number of occurrences of the string "Executing request startRendering" in the file server.log.
		assertEquals(937, reportEntries.size()); 

		//Retrieving the correct UID and the correct getRendering timestamp corresponding to a startRendering command: 
		ReportEntry entry = reportEntries.stream().filter(entry1 -> 
			entry1.getDocumentId()==114466
			&& entry1.getPageNumber()==0
			&& entry1.getTimestampsStartRendering().toString().equals("[2010-10-06T09:02:13.631]")).findFirst().get(); 
		assertEquals("1286373733634-5423", entry.getUID());
		assertEquals("[2010-10-06T09:02:14.825]", entry.getTimestampsGetRendering().toString());

		//Retrieving the correct UID and the correct getRendering timestamp corresponding to a startRendering command (another example):
		entry = reportEntries.stream().filter(entry1 -> 
			entry1.getDocumentId()==115392
			&& entry1.getPageNumber()==0
			&& entry1.getTimestampsStartRendering().toString().equals("[2010-10-06T09:06:11.540]")).findFirst().get(); 
		assertEquals("1286373971603-7884", entry.getUID());
		assertEquals("[2010-10-06T09:06:12.760]", entry.getTimestampsGetRendering().toString());
		
		//Correctly dealing with the case where for a given UID, there is no get rendering request: 
		entry = reportEntries.stream().filter(entry1 -> 
			entry1.getUID().equals("1286380905286-5852")).findFirst().get();
		assertNull(entry.getTimestampsGetRendering());

		//Testing whether the first 10 document id values and page numbers, mentioned in a "Executing request startRendering" line, do occur in the first 10 report entries.
		long[] first10DocumentIdValues = {114466, 114466, 114466, 114273, 114273, 114273, 114273, 114273, 115392, 115392};
		assertArrayEquals(first10DocumentIdValues, 
							reportEntries.stream().limit(10).mapToLong(entry1 -> entry1.getDocumentId()).toArray());
		long[] first10PageNumbers = {0, 0, 0, 0, 0, 1, 0, 1, 0, 0};
		assertArrayEquals(first10PageNumbers, 
							reportEntries.stream().limit(10).mapToLong(entry1 -> entry1.getPageNumber()).toArray());

		//Each report entry must have a non-default value for the fields documentId, UID and timestampsStartRendering, and a value for the field pageNumber that is >= 0. 
		reportEntries.stream().forEach(entry1 -> {
								assertTrue(entry1.getDocumentId()>0); 
								assertNotNull(entry1.getUID()); 
								assertNotNull(entry1.getTimestampsStartRendering());
								assertTrue(entry1.getPageNumber()>=0);
								});	
		
		//The sorting of the report entries must be in ascending order, based on the timestamp of the startRendering command
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss,SSS");
		LocalDateTime timestampInitial = LocalDateTime.parse("2010-10-06 00:00:00,000", formatter);		
		for (ReportEntry reportEntry: reportEntries) {
			LocalDateTime timestamp = reportEntry.getTimestampsStartRendering().stream().findFirst().get(); 
			assertTrue(timestamp.isAfter(timestampInitial));
		}
		
		//For each report entry, the get timestamp must be after the start timestamp. 
		for (ReportEntry reportEntry: reportEntries) {
			if(! (reportEntry.getTimestampsGetRendering()==null)) {
				LocalDateTime timestampStart = reportEntry.getTimestampsStartRendering().stream().findFirst().get(); 
				LocalDateTime timestampGet = reportEntry.getTimestampsGetRendering().stream().findFirst().get(); 
				assertTrue(timestampGet.isAfter(timestampStart)); 
			}
		}
	
	}

	@Test
	public void analyzeMultipleOccurrences() {

		//An example where for a given UID, there is 1 start rendering request and 1 get rendering request: 
		assertEquals(1, reportEntriesUpdated.stream().filter(
				entry1 -> entry1.getDocumentId()==110534 
						&& entry1.getPageNumber()==0 
						&& entry1.getUID().equals("1286379255529-288")
						&& entry1.getTimestampsStartRendering().toString().equals("[2010-10-06T10:34:15.525]")
						&& entry1.getTimestampsGetRendering().toString().equals("[2010-10-06T10:34:16]")).count());
		
		//An example where for a given UID, there are 2 start rendering requests and 1 get rendering request (the case of a "duplicate"):  
		//Correctly updating the field timestampsStartRendering: 
		assertEquals(2, reportEntriesUpdated.stream().filter(
				entry1 -> entry1.getDocumentId()==115117 
						&& entry1.getPageNumber()==0 
						&& entry1.getUID().equals("1286374534748-5725")
						&& entry1.getTimestampsStartRendering().toString().equals("[2010-10-06T09:15:34.744, 2010-10-06T09:15:35.212]")
						&& entry1.getTimestampsGetRendering().toString().equals("[2010-10-06T09:15:36.607]")).count());

		//We have to retain the same number of elements in the list reportEntriesUpdated as in the list reportEntries.
		assertEquals(937, reportEntriesUpdated.size()); 
		
		//Checking the multiplicities: 
		assertEquals(937, TestAssignmentDNMain.getMultiplicityMap().entrySet().stream()
								.mapToLong(entry -> entry.getValue()).sum()); 

		//If we do have multiplets, then is the distinct() method working properly? I.e. do we obtain a unique set of report entries in the list reportEntriesUpdated?  
		if (TestAssignmentDNMain.getSummaryOfMultiplicityMap().size()>1) {
			assertTrue(reportEntriesUpdated.stream().distinct().count() < reportEntriesUpdated.stream().count()); 
		}
	}
}