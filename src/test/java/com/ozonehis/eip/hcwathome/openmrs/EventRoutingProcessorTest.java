/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.hcwathome.openmrs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openmrs.eip.EIPException;
import org.openmrs.eip.mysql.watcher.Event;

@ExtendWith(MockitoExtension.class)
public class EventRoutingProcessorTest {
	
	@Mock
	private Exchange mockExchange;
	
	@Mock
	private Message mockMessage;
	
	@Mock
	private AppointmentEventProcessor mockAppointmentProcessor;
	
	private EventRoutingProcessor processor;
	
	@BeforeEach
	public void setup() {
		processor = new EventRoutingProcessor(mockAppointmentProcessor);
		when(mockExchange.getIn()).thenReturn(mockMessage);
	}
	
	@Test
	public void process_shouldProcessWithValidEvent() throws Exception {
		Event event = Mockito.mock(Event.class);
		when(mockMessage.getBody()).thenReturn(event);
		when(event.getTableName()).thenReturn("patient_appointment");
		
		processor.process(mockExchange);
		
		verify(mockAppointmentProcessor).process(event);
	}
	
	@Test
	public void process_shouldFailForAnInvalidPayload() {
		final String payload = "InvalidPayload";
		when(mockMessage.getBody()).thenReturn(payload);
		
		EIPException exception = assertThrows(EIPException.class, () -> processor.process(mockExchange));
		
		assertEquals("Don't know how to process payload " + payload, exception.getMessage());
	}
	
	@Test
	public void process_shouldFailForUnsupportedTable() {
		Event event = Mockito.mock(Event.class);
		when(mockMessage.getBody()).thenReturn(event);
		when(event.getTableName()).thenReturn("bad_table");
		
		EIPException exception = assertThrows(EIPException.class, () -> processor.process(mockExchange));
		
		assertEquals("No processor found for payload " + event, exception.getMessage());
	}
	
}
