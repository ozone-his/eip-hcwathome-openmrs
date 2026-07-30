package com.ozonehis.eip.hcwathome.openmrs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.openmrs.eip.mysql.watcher.Event;

public class AppointmentEventProcessorTest {
	
	private final AppointmentEventProcessor processor = new AppointmentEventProcessor(null);
	
	@Test
	public void getAction_ShouldReturnCreateAction() {
		Event event = new Event();
		event.setOperation("c");
		
		Action actualAction = processor.getAction(event);
		
		assertEquals(Action.CREATE, actualAction);
	}
	
	@Test
	public void getAction_ShouldReturnUpdateAction() {
		Event event = new Event();
		event.setOperation("u");
		
		Action actualAction = processor.getAction(event);
		
		assertEquals(Action.UPDATE, actualAction);
	}
	
	@Test
	public void getAction_ShouldReturnDeleteAction() {
		Event event = new Event();
		event.setOperation("d");
		
		Action actualAction = processor.getAction(event);
		
		assertEquals(Action.DELETE, actualAction);
	}
	
	@Test
	public void getAction_ShouldReturnNullForUnSupportedAction() {
		Event event = new Event();
		event.setOperation("x");
		
		Action actualAction = processor.getAction(event);
		
		assertNull(actualAction);
	}
	
	@Test
	public void getAppointmentUuid_ShouldReturnIdentifierFromEvent() {
		Event event = new Event();
		event.setIdentifier("uuid-1234");
		
		String actualUuid = processor.getAppointmentUuid(event);
		
		assertEquals("uuid-1234", actualUuid);
	}
	
	@Test
	public void getAppointmentUuid_ShouldReturnNullForEventWithNoIdentifier() {
		Event event = new Event();
		
		String actualUuid = processor.getAppointmentUuid(event);
		
		assertNull(actualUuid);
	}
	
	@Test
	public void getAppointmentId_ShouldReturnAppointmentIdFromEvent() {
		final Integer id = 123;
		Event event = new Event();
		event.setTableName("appointment");
		event.setPrimaryKeyId(id.toString());
		
		assertEquals(id, processor.getAppointmentId(event));
	}
	
}
