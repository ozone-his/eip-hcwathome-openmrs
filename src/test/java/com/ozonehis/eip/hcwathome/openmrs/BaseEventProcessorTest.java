/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.hcwathome.openmrs;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.openmrs.eip.EIPException;
import org.openmrs.eip.mysql.watcher.Event;

public class BaseEventProcessorTest {
	
	private BaseEventProcessor eventProcessor;
	
	private AppointmentProcessor mockProcessor;
	
	@BeforeEach
	public void setup() {
		mockProcessor = Mockito.mock(AppointmentProcessor.class);
		eventProcessor = Mockito.spy(new AppointmentEventProcessor(mockProcessor));
	}
	
	@Test
	public void process_ShouldThrowExceptionWhenAppointmentUuidIsBlank() {
		Event event = new Event();
		Mockito.doReturn(null).when(eventProcessor).getAppointmentUuid(event);
		Mockito.doReturn(null).when(eventProcessor).getAppointmentId(event);
		
		EIPException exception = assertThrows(EIPException.class, () -> eventProcessor.process(event));
		
		assertTrue(exception.getMessage().contains("Cannot resolve appointment uuid for event: " + event));
	}
	
	@Test
	public void process_ShouldThrowExceptionWhenActionIsNull() {
		final String operation = "r";
		Event event = new Event();
		event.setOperation(operation);
		Mockito.doReturn("test-uuid").when(eventProcessor).getAppointmentUuid(event);
		Mockito.doReturn(null).when(eventProcessor).getAction(event);
		
		EIPException exception = assertThrows(EIPException.class, () -> eventProcessor.process(event));
		
		assertTrue(exception.getMessage().contains("Don't know how to process DB event with operation" + operation));
	}
	
	@Test
	public void process_ShouldInvokeAppointmentProcessorWhenInputIsValid() throws Exception {
		final String uuid = "test-uuid";
		Event event = new Event();
		Mockito.doReturn(uuid).when(eventProcessor).getAppointmentUuid(event);
		Mockito.doReturn(Action.UPDATE).when(eventProcessor).getAction(event);
		
		eventProcessor.process(event);
		
		Mockito.verify(mockProcessor).process(uuid, Action.UPDATE);
	}
	
}
