package com.ozonehis.eip.hcwathome.openmrs;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class EventListener extends RouteBuilder {
	
	private static final String ROUTE_ID = "event-listener";
	
	private static final String ROUTE_ENDPOINT = "direct:" + ROUTE_ID;
	
	@Override
	public void configure() {
		errorHandler("watcherErrorHandler");
		
		from(ROUTE_ENDPOINT).routeId(ROUTE_ID).process("eventProcessor");
	}
	
}
