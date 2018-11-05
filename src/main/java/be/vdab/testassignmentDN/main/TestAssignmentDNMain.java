package be.vdab.testassignmentDN.main;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import be.vdab.testassignmentDN.entities.LogEntry;
import be.vdab.testassignmentDN.entities.LogEntryFactory;
import be.vdab.testassignmentDN.entities.ReportEntry;

public class TestAssignmentDNMain {

	private static long exceptionLines = 0;
	private static Map<String, Long> multiplicityMap = new LinkedHashMap<>();
	private static Map<Long, Long> summaryOfMultiplicityMap = new TreeMap<>();
	private static long startRenderingsWithoutGet = 0;

	public static long getExceptionLines() {
		return exceptionLines;
	}

	public static Map<String, Long> getMultiplicityMap() {
		return multiplicityMap;
	}

	public static Map<Long, Long> getSummaryOfMultiplicityMap() {
		return summaryOfMultiplicityMap;
	}

	public static void main(String[] args) {

		List<LogEntry> logEntries = new LinkedList<>();
		List<ReportEntry> reportEntries = new LinkedList<>();
		List<ReportEntry> reportEntriesUpdated = new LinkedList<>();

		readLogFile(logEntries);
		analyzeData(logEntries, reportEntries);
		generateTxtFile(reportEntries);
		analyzeMultipleOccurrences(reportEntries, reportEntriesUpdated);
		generateUpdatedTxtFile(reportEntriesUpdated);
		generateXMLFile(reportEntriesUpdated);

	}

	public static void readLogFile(List<LogEntry> logEntries) {
		try (BufferedReader reader = new BufferedReader(new FileReader("/data/server.log"))) {
			for (String line; (line = reader.readLine()) != null;) {
				try {
					LogEntry logEntry = LogEntryFactory.INSTANCE.createLogEntry(line);
					logEntries.add(logEntry);
				} catch (DateTimeParseException | StringIndexOutOfBoundsException ex) {
					System.out.println(ex.getMessage());
					exceptionLines++;
				}
			}
		} catch (IOException ex) {
			System.out.println(ex.getMessage());
		}
	}

	public static void analyzeData(List<LogEntry> logEntries, List<ReportEntry> reportEntries) {

		for (LogEntry entry : logEntries) {
			String logMessage = entry.getLogMessage();

			if (logMessage.contains("Executing request startRendering")) {
				LocalDateTime timestamp = entry.getTimestamp();
				String thread = entry.getThread();
				long documentId = Long
						.parseLong(logMessage.substring(logMessage.indexOf('[') + 1, logMessage.indexOf(',')));
				long pageNumber = Long
						.parseLong(logMessage.substring(logMessage.indexOf(',') + 2, logMessage.indexOf(']')));
				Set<LocalDateTime> timestampsStartRendering = new TreeSet<>();
				timestampsStartRendering.add(timestamp);

				ReportEntry reportEntry = new ReportEntry();
				reportEntry.setDocumentId(documentId);
				reportEntry.setPageNumber(pageNumber);
				reportEntry.setTimestampsStartRendering(timestampsStartRendering);

				Stream<LogEntry> logEntriesStart = logEntries.stream()
						.filter(entry1 -> entry1.getLogMessage().contains("Service startRendering returned")
								&& entry1.getThread().equals(thread) && entry1.getTimestamp().isAfter(timestamp));
				String logMessageStart = logEntriesStart.findFirst().get().getLogMessage();
				System.out.println(logMessageStart);

				String message = "Service startRendering returned";
				String UID = logMessageStart.substring(message.length() + 1);
				reportEntry.setUID(UID);

				Stream<LogEntry> logEntriesGet = logEntries.stream()
						.filter(entry1 -> matchingGetRendering(entry1, UID));
				Optional<LogEntry> logEntryGet = logEntriesGet.findFirst();
				if (logEntryGet.isPresent()) {
					LocalDateTime timestampGetRendering = logEntryGet.get().getTimestamp();
					Set<LocalDateTime> timestampsGetRendering = new TreeSet<>();
					timestampsGetRendering.add(timestampGetRendering);
					reportEntry.setTimestampsGetRendering(timestampsGetRendering);
				}

				reportEntries.add(reportEntry);
			}

		}

		for (ReportEntry reportEntry : reportEntries) {
			if (reportEntry.getTimestampsGetRendering() == null) {
				startRenderingsWithoutGet++;
			}
		}

	}

	private static boolean matchingGetRendering(LogEntry entry, String UID) {
		String message = entry.getLogMessage();
		return message.contains("Executing request getRendering")
				&& message.substring(message.indexOf('[') + 1, message.indexOf(']')).equals(UID);
	}

	public static void generateTxtFile(List<ReportEntry> reportEntries) {

		try (BufferedWriter writer = Files.newBufferedWriter(Paths.get("/data/auxiliary report 1.txt"))) {

			for (ReportEntry reportEntry : reportEntries) {
				long documentId = reportEntry.getDocumentId();
				writer.write(String.valueOf(documentId) + "\t");
				long pageNumber = reportEntry.getPageNumber();
				writer.write(String.valueOf(pageNumber) + "\t");
				String UID = reportEntry.getUID();
				writer.write(UID + "\t");

				writer.write("startRendering: ");
				Set<LocalDateTime> timestampsStartRendering = reportEntry.getTimestampsStartRendering();
				for (LocalDateTime timestamp : timestampsStartRendering) {
					writer.write(timestamp.toString() + "\t" + "\t");
				}

				writer.write("getRendering: ");
				Set<LocalDateTime> timestampsGetRendering = reportEntry.getTimestampsGetRendering();
				try {
					for (LocalDateTime timestamp : timestampsGetRendering) {
						writer.write(timestamp.toString());
					}
				} catch (NullPointerException ex) {
					System.out.println(ex.getMessage());
				}
				writer.newLine();
			}

		} catch (IOException ex) {
			System.out.println(ex.getMessage());
		}

	}

	public static void analyzeMultipleOccurrences(List<ReportEntry> reportEntries,
			List<ReportEntry> reportEntriesUpdated) {

		for (ReportEntry reportEntry : reportEntries) {
			long documentId = reportEntry.getDocumentId();
			long pageNumber = reportEntry.getPageNumber();
			String UID = reportEntry.getUID();
			
			Stream<ReportEntry> reportEntriesStream = reportEntries.stream();
			List<ReportEntry> filteredReportEntries = reportEntriesStream
					.filter(reportEntry1 -> reportEntry1.getUID().equals(UID)).collect(Collectors.toList());
			long multiplicity = filteredReportEntries.size();
			if (!multiplicityMap.containsKey(reportEntry.getUID())) {
				multiplicityMap.put(reportEntry.getUID(), multiplicity);
			}

			Set<LocalDateTime> timestampsStartRendering = reportEntry.getTimestampsStartRendering();
			for (ReportEntry reportEntry1 : filteredReportEntries) {
				timestampsStartRendering.addAll(reportEntry1.getTimestampsStartRendering());
			}
			Set<LocalDateTime> timestampsGetRendering = reportEntry.getTimestampsGetRendering();
			try {
				for (ReportEntry reportEntry1 : filteredReportEntries) {
					timestampsGetRendering.addAll(reportEntry1.getTimestampsGetRendering());
				}
			} catch (NullPointerException ex) {
				System.out.println(ex.getMessage());
			}

			ReportEntry reportEntryUpdated = new ReportEntry();
			reportEntryUpdated.setDocumentId(documentId);
			reportEntryUpdated.setPageNumber(pageNumber);
			reportEntryUpdated.setUID(UID);
			reportEntryUpdated.setTimestampsStartRendering(timestampsStartRendering);
			reportEntryUpdated.setTimestampsGetRendering(timestampsGetRendering);

			reportEntriesUpdated.add(reportEntryUpdated);

		}

		for (Entry<String, Long> entry : multiplicityMap.entrySet()) {
			long multiplicity = entry.getValue();
			if (!summaryOfMultiplicityMap.containsKey(multiplicity)) {
				summaryOfMultiplicityMap.put(multiplicity, 1L);
			} else {
				long number = summaryOfMultiplicityMap.get(multiplicity);
				summaryOfMultiplicityMap.put(multiplicity, number + 1);
			}
		}
	}

	private static void generateUpdatedTxtFile(List<ReportEntry> reportEntriesUpdated) {

		try (BufferedWriter writer = Files.newBufferedWriter(Paths.get("/data/auxiliary report 2.txt"))) {

			for (ReportEntry reportEntry : reportEntriesUpdated) {
				long documentId = reportEntry.getDocumentId();
				writer.write(String.valueOf(documentId) + "\t");
				long pageNumber = reportEntry.getPageNumber();
				writer.write(String.valueOf(pageNumber) + "\t");
				String UID = reportEntry.getUID();
				writer.write(UID + "\t");

				Set<LocalDateTime> timestampsStartRendering = reportEntry.getTimestampsStartRendering();
				writer.write("startRendering: [");
				String timestamps = "";
				for (LocalDateTime timestamp : timestampsStartRendering) {
					timestamps += timestamp.toString() + " ";
				}
				writer.write(timestamps.trim());
				writer.write("]" + "\t");

				Set<LocalDateTime> timestampsGetRendering = reportEntry.getTimestampsGetRendering();
				writer.write("getRendering: [");
				try {
					timestamps = "";
					for (LocalDateTime timestamp : timestampsGetRendering) {
						timestamps += timestamp.toString() + " ";
					}
					writer.write(timestamps.trim());
				} catch (NullPointerException ex) {
					writer.write("/");
					System.out.println(ex.getMessage());
				}
				writer.write("]");

				writer.newLine();
			}
		} catch (IOException ex) {
			System.out.println(ex.getMessage());
		}
	}

	private static void generateXMLFile(List<ReportEntry> reportEntriesUpdated) {

		try (BufferedWriter writer = Files.newBufferedWriter(Paths.get("/data/final report.xml"))) {

			writer.write("<report>");
			writer.newLine();
			reportEntriesUpdated.stream().distinct().forEach(entry1 -> generateRenderingElement(writer, entry1));
			writer.write("\t" + "<summary>");
			writer.newLine();
			writer.write("\t" + "\t" + "<numberOfRenderings>" + reportEntriesUpdated.stream().distinct().count()
					+ "</numberOfRenderings>");
			writer.newLine();
			writer.write("\t" + "\t" + "<numberOfSingleOccurrences>" + summaryOfMultiplicityMap.get(1L)
					+ "</numberOfSingleOccurrences>");
			writer.newLine();
			writer.write("\t" + "\t" + "<numberOfDoublets>" + summaryOfMultiplicityMap.get(2L) + "</numberOfDoublets>");
			writer.newLine();
			writer.write("\t" + "\t" + "<numberOfNonets>" + summaryOfMultiplicityMap.get(9L) + "</numberOfNonets>");
			writer.newLine();
			writer.write("\t" + "\t" + "<unnecessary>" + startRenderingsWithoutGet + "</unnecessary>");
			writer.newLine();
			writer.write("\t" + "</summary>");
			writer.newLine();
			writer.write("</report>");

		} catch (IOException ex) {
			System.out.println(ex.getMessage());
		}
	}

	private static void generateRenderingElement(BufferedWriter writer, ReportEntry entry) {
		try {
			writer.write("\t" + "<rendering>");
			writer.newLine();
			writer.write("\t" + "\t" + "<document>" + String.valueOf(entry.getDocumentId()) + "</document>");
			writer.newLine();
			writer.write("\t" + "\t" + "<page>" + String.valueOf(entry.getPageNumber()) + "</page>");
			writer.newLine();
			writer.write("\t" + "\t" + "<uid>" + String.valueOf(entry.getUID()) + "</uid>");
			writer.newLine();
			entry.getTimestampsStartRendering().stream().forEach(timestamp -> {
				try {
					writer.write("\t" + "\t" + "<start>" + timestamp.toString() + "</start>");
					writer.newLine();
				} catch (IOException ex) { 
					System.out.println(ex.getMessage());
				}
			});
			try {
				entry.getTimestampsGetRendering().stream().forEach(timestamp -> {
					try {
						writer.write("\t" + "\t" + "<get>" + timestamp.toString() + "</get>");
						writer.newLine();
					} catch (IOException ex) {
						System.out.println(ex.getMessage());
					}
				});
			} catch (NullPointerException ex) {
				System.out.println(ex.getMessage());
			}

			writer.write("\t" + "</rendering>");
			writer.newLine();

		}

		catch (IOException ex) {
			System.out.println(ex.getMessage());
		}
	}

}