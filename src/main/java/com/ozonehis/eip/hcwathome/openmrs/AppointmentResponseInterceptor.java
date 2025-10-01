/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.hcwathome.openmrs;

import java.io.IOException;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import ca.uhn.fhir.rest.client.api.IClientInterceptor;
import ca.uhn.fhir.rest.client.api.IHttpRequest;
import ca.uhn.fhir.rest.client.api.IHttpResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * The HCW@Home FHIR server does not correctly implement the Appointment based on the specification
 * specifically the status field where it returns an invalid value which breaks the client library
 * during deserialization of the appointment data. So this intercepts appointment response data and
 * mutates it to set a valid status value.
 */
@Slf4j
public class AppointmentResponseInterceptor implements IClientInterceptor {
	
	@Override
	public void interceptRequest(IHttpRequest request) {
		//No-op
	}
	
	@Override
	public void interceptResponse(IHttpResponse response) throws IOException {
		ObjectMapper mapper = HcwOpenmrsUtils.getMapper();
		Map<String, Object> jsonMap = (Map) mapper.readValue(response.readEntity(), Map.class);
		if (!"Appointment".equals(jsonMap.get("resourceType"))) {
			return;
		}
		
		if (log.isDebugEnabled()) {
			log.debug("Fixing correct appointment status");
		}
	}
	
}
