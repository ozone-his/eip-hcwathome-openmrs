/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.hcwathome.openmrs;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class HcwFhirClient {
	
	private static final String SUB_PATH_API_V1 = "/api/v1/";
	
	private static final String SUB_PATH_FHIR = SUB_PATH_API_V1 + "fhir";
	
	@Value("${hcwathome.backend.url}")
	private String baseUrl;
	
	@Value("${hcwathome.user.email}")
	private String email;
	
	@Value("${hcwathome.password}")
	private char[] password;
	
	private FhirContext fhirContext;
	
	private IGenericClient fhirClient;
	
	private HcwFhirClient() {
	}
	
	private static final class InstanceHolder {
		
		private static final HcwFhirClient INSTANCE = new HcwFhirClient();
	}
	
	/**
	 * Gets an instance of this client.
	 *
	 * @return the {@link HcwFhirClient} instance
	 */
	public static HcwFhirClient getInstance() {
		return InstanceHolder.INSTANCE;
	}
	
	private IGenericClient getFhirClient() {
		if (fhirClient == null) {
			synchronized (this) {
				if (fhirClient == null) {
					fhirContext = FhirContext.forR4();
					fhirContext.getRestfulClientFactory().setConnectTimeout(30000);
					fhirContext.getRestfulClientFactory().setConnectionRequestTimeout(120000);
					fhirContext.getRestfulClientFactory().setSocketTimeout(120000);
					fhirClient = fhirContext.newRestfulGenericClient(baseUrl + SUB_PATH_FHIR);
					fhirClient.registerInterceptor(new AuthInterceptor(email, password));
				}
			}
		}
		
		return fhirClient;
	}
	
}
