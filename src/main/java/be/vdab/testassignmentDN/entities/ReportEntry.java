package be.vdab.testassignmentDN.entities;

import java.time.LocalDateTime;
import java.util.Set;

public class ReportEntry {
	private long documentId;
	private long pageNumber;
	private String UID; 
	private Set<LocalDateTime> timestampsStartRendering;
	private Set<LocalDateTime> timestampsGetRendering;
	
	public ReportEntry() {
	}

	public long getDocumentId() {
		return documentId;
	}

	public void setDocumentId(long documentId) {
		this.documentId = documentId; 
	}

	public long getPageNumber() {
		return pageNumber;
	}

	public void setPageNumber(long pageNumber) {
		this.pageNumber = pageNumber;
	}

	public String getUID() {
		return UID;
	}

	public void setUID(String uID) {
		UID = uID;
	}

	public Set<LocalDateTime> getTimestampsStartRendering() {
		return timestampsStartRendering;
	}

	public void setTimestampsStartRendering(Set<LocalDateTime> timestampsStartRendering) {
		this.timestampsStartRendering = timestampsStartRendering;
	}

	public Set<LocalDateTime> getTimestampsGetRendering() {
		return timestampsGetRendering;
	}

	public void setTimestampsGetRendering(Set<LocalDateTime> timestampsGetRendering) {
		this.timestampsGetRendering = timestampsGetRendering;
	}
	
	@Override
	public boolean equals(Object object) {
		if (!(object instanceof ReportEntry)) {
			return false;
		}
		ReportEntry entry = (ReportEntry) object;
		return UID.equals(entry.getUID());
	}
	
	@Override
	public int hashCode() {
		return UID.hashCode();
	}
	
}
