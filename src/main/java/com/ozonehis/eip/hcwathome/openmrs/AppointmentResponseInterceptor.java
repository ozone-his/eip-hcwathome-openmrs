/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.hcwathome.openmrs;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.rest.client.apache.ModifiedStringApacheHttpResponse;
import ca.uhn.fhir.rest.client.api.ClientResponseContext;
import ca.uhn.fhir.rest.client.api.IHttpResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * The HCW@Home FHIR server does not correctly implement the Appointment based on the specification
 * specifically the status field where it returns an invalid value which breaks the client library
 * during deserialization of the appointment data. So this intercepts appointment response data and
 * mutates it to set a valid status value.
 */
@Slf4j
@Interceptor
public class AppointmentResponseInterceptor {
	
	private static final Map<String, String> STATUS_MAP;
	
	static {
		STATUS_MAP = new HashMap<>();
		STATUS_MAP.put("PENDING", "proposed");
		STATUS_MAP.put("SENT", "pending");
		STATUS_MAP.put("ACCEPTED", "booked");
		STATUS_MAP.put("ACKNOWLEDGED", "booked");
		STATUS_MAP.put("COMPLETE", "fulfilled");
		STATUS_MAP.put("REFUSED", "cancelled");
		STATUS_MAP.put("CANCELED", "cancelled");
		STATUS_MAP.put("SCHEDULED_FOR_INVITE", "booked");
		STATUS_MAP.put("SCHEDULED", "booked");
		
		//Whatsapp statuses
		STATUS_MAP.put("QUEUED", "pending");
		STATUS_MAP.put("SENDING", "pending");
		STATUS_MAP.put("FAILED", "cancelled");
		STATUS_MAP.put("DELIVERED", "booked");
		STATUS_MAP.put("PARTIALLY_DELIVERED", "booked");
		STATUS_MAP.put("UNDELIVERED", "cancelled");
		STATUS_MAP.put("RECEIVING", "booked");
		STATUS_MAP.put("RECEIVED", "booked");
		STATUS_MAP.put("READ", "arrived");
	}
	
	@Hook(Pointcut.CLIENT_RESPONSE)
	public void interceptResponse(IHttpResponse resp, ClientResponseContext ctx) throws IOException {
		ObjectMapper mapper = HcwOpenmrsUtils.getMapper();
		Map<String, Object> jsonMap = (Map) mapper.readValue(resp.readEntity(), Map.class);
		if (!"Appointment".equals(jsonMap.get("resourceType"))) {
			return;
		}
		
		if (log.isDebugEnabled()) {
			log.debug("Fixing appointment status");
		}
		
		jsonMap.put("status", STATUS_MAP.get(jsonMap.get("status").toString()));
		final String modifiedJson = mapper.writeValueAsString(jsonMap);
		ctx.setHttpResponse(new ModifiedStringApacheHttpResponse(resp, modifiedJson, null));
	}
	
}
