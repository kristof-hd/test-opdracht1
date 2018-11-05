package be.vdab.testassignmentDN;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import be.vdab.testassignmentDN.entities.ReportEntry;

public class ReportEntryTest {

	private ReportEntry entry1;
	private ReportEntry entry2;
	private ReportEntry entry3;

	@Before
	public void before() { 
		entry1 = new ReportEntry();
		entry1.setUID("1111111111111-1111");
		entry2 = new ReportEntry();
		entry2.setUID("1111111111111-1111");
		entry3 = new ReportEntry();
		entry3.setUID("1111111111111-1112");
	}

	@Test
	public void equalsTest() {
		// Two ReportEntry objects are equal when their UID values are equal.
		assertTrue(entry1.equals(entry2));
		assertFalse(entry1.equals(entry3));
	}

	@Test
	public void hashCodeTest() {
		//Executing the hashCode() method on two equal objects must yield an identical result. 
		assertEquals(entry1.hashCode(), entry2.hashCode());
	}
}
