package ch.qos.logback.core.status;

import java.util.Iterator;

import junit.framework.TestCase;

public class StatusBaseTest extends TestCase {

	public void testAddStatus() {
		{
			InfoStatus status = new InfoStatus("testing", this);
			status.add(new ErrorStatus("error", this));
			Iterator it = status.iterator();
			assertTrue("No status was added", it.hasNext());
			assertTrue("hasChilden method reported wrong result", status
					.hasChildren());
		}
		{
			InfoStatus status = new InfoStatus("testing", this);
			try {
				status.add(null);
				fail("method should have thrown an Exception");
			} catch (NullPointerException ex) {
			}
		}
	}

	public void testRemoveStatus() {
		{
			InfoStatus status = new InfoStatus("testing", this);
			ErrorStatus error = new ErrorStatus("error", this);
			status.add(error);
			boolean result = status.remove(error);
			Iterator it = status.iterator();
			assertTrue("Remove failed", result);
			assertFalse("No status was removed", it.hasNext());
			assertFalse("hasChilden method reported wrong result", status
					.hasChildren());
		}
		{
			InfoStatus status = new InfoStatus("testing", this);
			ErrorStatus error = new ErrorStatus("error", this);
			status.add(error);
			boolean result = status.remove(null);
			assertFalse("Remove result was not false", result);
		}
	}
	
	public void testEffectiveLevel() {
		{
			//effective level = 0 level deep
			ErrorStatus status = new ErrorStatus("error", this);
			WarnStatus warn = new WarnStatus("warning", this);
			status.add(warn);
			assertEquals("effective level misevaluated", status.getEffectiveLevel(), Status.ERROR);
		}
		
		{
			//effective level = 1 level deep
			InfoStatus status = new InfoStatus("info", this);
			WarnStatus warn = new WarnStatus("warning", this);
			status.add(warn);
			assertEquals("effective level misevaluated", status.getEffectiveLevel(), Status.WARN);
		}
		
		{
			//effective level = 2 levels deep
			InfoStatus status = new InfoStatus("info", this);
			WarnStatus warn = new WarnStatus("warning", this);
			ErrorStatus error = new ErrorStatus("error", this);
			status.add(warn);
			warn.add(error);
			assertEquals("effective level misevaluated", status.getEffectiveLevel(), Status.ERROR);
		}
	}

}