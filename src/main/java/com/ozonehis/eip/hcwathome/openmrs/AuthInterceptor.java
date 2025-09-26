package com.ozonehis.eip.hcwathome.openmrs;

import java.io.IOException;

import org.openmrs.eip.OauthToken;

import ca.uhn.fhir.rest.client.api.IClientInterceptor;
import ca.uhn.fhir.rest.client.api.IHttpRequest;
import ca.uhn.fhir.rest.client.api.IHttpResponse;

/**
 * Authenticates with hcwathome instance to obtain and set the access token header.
 */
public class AuthInterceptor implements IClientInterceptor {
	
	private String email;
	
	private char[] password;
	
	private static OauthToken oauthToken;
	
	public AuthInterceptor(String email, char[] password) {
		this.email = email;
		this.password = password;
	}
	
	@Override
	public void interceptRequest(IHttpRequest iHttpRequest) {
		//TODO set access token as header
	}
	
	@Override
	public void interceptResponse(IHttpResponse iHttpResponse) throws IOException {
		//No-op	
	}
	
}
