/*
 * Copyright (C) Amiyul LLC - All Rights Reserved
 *
 * This source code is protected under international copyright law. All rights
 * reserved and protected by the copyright holder.
 *
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holder. If you encounter this file and do not have
 * permission, please contact the copyright holder and delete this file.
 */
package com.ozonehis.eip.hcwathome.openmrs;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component("eventProcessor")
public class EventProcessor implements Processor {
	
	@Override
	public void process(Exchange exchange) throws Exception {
		Object body = exchange.getIn().getBody();
		log.info("Received database event {}", body);
	}
	
}
