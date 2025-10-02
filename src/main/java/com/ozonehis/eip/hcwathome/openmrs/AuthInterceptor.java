/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.hcwathome.openmrs;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Map;

import org.openmrs.eip.EIPException;
import org.openmrs.eip.OauthToken;

import com.fasterxml.jackson.databind.ObjectMapper;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.rest.client.api.IHttpRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * Authenticates with hcwathome instance to obtain and set the access token header.
 */
@Slf4j
@Interceptor
public class AuthInterceptor {
	
	protected static final HttpResponse.BodyHandler<byte[]> BODY_HANDLER = HttpResponse.BodyHandlers.ofByteArray();
	
	private URI authUri;
	
	private String email;
	
	private char[] password;
	
	private static OauthToken oauthToken;
	
	private HttpClient httpClient = HttpClient.newHttpClient();
	
	public AuthInterceptor(String baseApiUrl, String email, char[] password) {
		this.authUri = URI.create(baseApiUrl + "login-local");
		this.email = email;
		this.password = password;
	}
	
	@Hook(Pointcut.CLIENT_REQUEST)
	public void interceptRequest(IHttpRequest request) {
		if (oauthToken == null || oauthToken.isExpired(LocalDateTime.now())) {
			synchronized (this) {
				if (oauthToken == null || oauthToken.isExpired(LocalDateTime.now())) {
					if (oauthToken != null) {
						if (log.isDebugEnabled()) {
							log.debug("Auth token is expired");
						}
					}
					
					oauthToken = authenticate();
				}
			}
		}
		
		request.addHeader("x-access-token", oauthToken.getAccessToken());
	}
	
	private OauthToken authenticate() {
		if (log.isDebugEnabled()) {
			log.debug("Authenticating with hcwathome");
		}
		
		Builder reqBuilder = HttpRequest.newBuilder().version(Version.HTTP_1_1).uri(authUri);
		reqBuilder.setHeader("Content-Type", "application/json");
		try {
			ObjectMapper mapper = HcwOpenmrsUtils.getMapper();
			final byte[] data = mapper.writeValueAsBytes(Map.of("email", email, "password", new String(password)));
			reqBuilder.POST(BodyPublishers.ofByteArray(data));
			HttpResponse<byte[]> response = httpClient.send(reqBuilder.build(), BODY_HANDLER);
			if (response.statusCode() != 200) {
				String msg = "Authentication error";
				if (response.body() != null) {
					msg = new String(response.body(), StandardCharsets.UTF_8);
				}
				
				throw new EIPException(msg);
			}
			
			Map<String, Object> userData = (Map) mapper.readValue(response.body(), Map.class).get("user");
			final String accessToken = userData.get("token").toString();
			Map<String, Object> claims = JwtUtils.parseToken(accessToken);
			long expiry = Long.valueOf(claims.get("exp").toString());
			//Renewing of the token should happen 30seconds before it actually expires
			long expiresAt = expiry - 30;
			if (log.isDebugEnabled()) {
				log.info("Auth token expires at {}", new Date(expiresAt * 1000));
			}
			
			return new OauthToken(accessToken, expiresAt);
		}
		catch (Exception e) {
			throw new EIPException("Failed to authenticate with hcwathome", e);
		}
	}
	
}
