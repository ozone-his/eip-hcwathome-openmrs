/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.hcwathome.openmrs;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.rest.client.api.IHttpRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * Authenticates with HCW@Home instance to obtain and set the authentication token in the
 * Authorization header.
 */
@Slf4j
@Interceptor
public class AuthInterceptor {
	
	private char[] password;
	
	public AuthInterceptor(char[] password) {
		this.password = password;
	}
	
	@Hook(Pointcut.CLIENT_REQUEST)
	public void interceptRequest(IHttpRequest request) {
		request.addHeader("Authorization", "Token " + new String(password));
	}
	
}
