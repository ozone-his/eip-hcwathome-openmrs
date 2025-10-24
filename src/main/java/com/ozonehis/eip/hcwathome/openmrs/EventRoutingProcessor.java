/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.hcwathome.openmrs;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.openmrs.eip.EIPException;
import org.openmrs.eip.mysql.watcher.Event;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Pre-processes a camel {@link Exchange} to extract the {@link Event} from the message and routes
 * to the appropriate {@link BaseEventProcessor} instance.
 */
@Slf4j
@Component("eventRoutingProcessor")
public class EventRoutingProcessor implements Processor {
	
	private AppointmentEventProcessor appointmentProcessor;
	
	public EventRoutingProcessor(AppointmentEventProcessor appointmentProcessor) {
		this.appointmentProcessor = appointmentProcessor;
	}
	
	@Override
	public void process(Exchange exchange) throws Exception {
		Object payload = exchange.getIn().getBody();
		Event event;
		if (payload instanceof Event) {
			if (log.isDebugEnabled()) {
				log.debug("Received payload {}", payload);
			}
			
			event = (Event) payload;
		} else {
			throw new EIPException("Don't know how to process payload " + payload);
		}
		
		EventProcessor processor;
		switch (event.getTableName()) {
			case "patient_appointment":
				processor = appointmentProcessor;
				break;
			default:
				processor = null;
		}
		
		if (processor == null) {
			throw new EIPException("No processor found for payload " + payload);
		}
		
		processor.process(event);
	}
	
}
