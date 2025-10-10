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
import org.openmrs.eip.mysql.watcher.management.entity.DebeziumEvent;
import org.openmrs.eip.mysql.watcher.management.entity.SenderRetryQueueItem;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Pre-processes a camel {@link Exchange} to extract the {@link Event} from the message and forwards
 * to the {@link EventProcessor} for actual processing.
 */
@Slf4j
@Component
public class EventPreProcessor implements Processor {
	
	private EventProcessor processor;
	
	public EventPreProcessor(EventProcessor processor) {
		this.processor = processor;
	}
	
	@Override
	public void process(Exchange exchange) throws Exception {
		Object payload = exchange.getIn().getBody();
		Event event;
		if (payload instanceof DebeziumEvent) {
			if (log.isDebugEnabled()) {
				log.debug("Received database event {}", payload);
			}
			
			event = ((DebeziumEvent) payload).getEvent();
		} else if (payload instanceof SenderRetryQueueItem) {
			if (log.isDebugEnabled()) {
				log.debug("Received retry {}", payload);
			}
			
			event = ((SenderRetryQueueItem) payload).getEvent();
		} else {
			throw new EIPException("Don't know how to process event " + payload);
		}
		
		processor.process(event);
	}
	
}
