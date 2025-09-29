package com.ozonehis.eip.hcwathome.openmrs;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.Map;

import org.openmrs.eip.EIPException;
import org.openmrs.eip.OauthToken;
import org.openmrs.eip.Utils;

import com.fasterxml.jackson.databind.ObjectMapper;

import ca.uhn.fhir.rest.client.api.IClientInterceptor;
import ca.uhn.fhir.rest.client.api.IHttpRequest;
import ca.uhn.fhir.rest.client.api.IHttpResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * Authenticates with hcwathome instance to obtain and set the access token header.
 */
@Slf4j
public class AuthInterceptor implements IClientInterceptor {
	
	private static final ObjectMapper MAPPER = new ObjectMapper();
	
	protected static final HttpResponse.BodyHandler<byte[]> BODY_HANDLER = HttpResponse.BodyHandlers.ofByteArray();
	
	private String email;
	
	private char[] password;
	
	private static OauthToken oauthToken;
	
	private HttpClient httpClient = HttpClient.newHttpClient();
	
	public AuthInterceptor(String email, char[] password) {
		this.email = email;
		this.password = password;
	}
	
	@Override
	public void interceptRequest(IHttpRequest iHttpRequest) {
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
		
		iHttpRequest.addHeader("x-access-token", oauthToken.getAccessToken());
	}
	
	@Override
	public void interceptResponse(IHttpResponse iHttpResponse) throws IOException {
		//No-op	
	}
	
	private OauthToken authenticate() {
		
		if (log.isDebugEnabled()) {
			log.debug("Authenticating with hcwathome");
		}
		
		Builder reqBuilder = HttpRequest.newBuilder();
		reqBuilder.setHeader("Content-Type", "application/json");
		try {
			final String data = MAPPER.writeValueAsString(Map.of("email", email, "password", new String(password)));
			reqBuilder.POST(HttpRequest.BodyPublishers.ofString(data));
			HttpResponse<byte[]> response = httpClient.send(reqBuilder.build(), BODY_HANDLER);
			Map<String, Object> responseData = MAPPER.readValue(response.body(), Map.class);
			final String accessToken = responseData.get("token").toString();
			long secondsSinceEpoch = Utils.getCurrentSeconds();
			//Renewing of the token should happen 30seconds before it actually expires
			long expiresAt = secondsSinceEpoch + Long.valueOf("") - 30;
			return new OauthToken(accessToken, expiresAt);
		}
		catch (Exception e) {
			throw new EIPException("Failed to authenticate with hcwathome", e);
		}
	}
	
}
