/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.hcwathome.openmrs;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Base64;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.builder.ExchangeBuilder;
import org.apache.commons.collections4.MapUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.openmrs.eip.camel.OauthProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Submits http requests to OpenMRS.
 */
@Component
public class OpenMrsRestClient {
	
	protected static final String PATH = "/ws/rest/v1/";
	
	protected static final BodyHandler<byte[]> BODY_HANDLER = BodyHandlers.ofByteArray();
	
	@Value("${openmrs.baseUrl}")
	private String baseUrl;
	
	@Value("${openmrs.username}")
	private String username;
	
	@Value("${openmrs.password}")
	private char[] password;
	
	private OauthProcessor oauthProcessor;
	
	private CamelContext camelContext;
	
	private HttpClient client = HttpClient.newHttpClient();
	
	public OpenMrsRestClient(OauthProcessor oauthProcessor, CamelContext camelContext) {
		this.oauthProcessor = oauthProcessor;
		this.camelContext = camelContext;
	}
	
	public byte[] search(String resource, Map<String, String> params) throws Exception {
		HttpResponse<byte[]> response = sendRequest(resource, null, params, null, false, Set.of(HttpStatus.SC_OK));
		return response.body();
	}
	
	public HttpResponse<byte[]> sendRequest(String resource, String uuid, Map<String, String> params, String body,
	                                        boolean delete, Set<Integer> allowedStatuses)
	    throws Exception {
		String uri = baseUrl + PATH + resource;
		if (uuid != null) {
			uri += ("/" + uuid);
		}
		
		HttpRequest.Builder reqBuilder = HttpRequest.newBuilder().setHeader(HttpHeaders.AUTHORIZATION, getAuthHeader());
		if (MapUtils.isNotEmpty(params)) {
			String q = params.entrySet().stream().map(e -> e.getKey() + "=" + URLEncoder.encode(e.getValue(), UTF_8))
			        .collect(Collectors.joining("&"));
			uri += ("?" + q);
		}
		
		reqBuilder.uri(URI.create(uri));
		
		if (body != null) {
			reqBuilder.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");
			BodyPublisher bodyPublisher = BodyPublishers.ofString(body, UTF_8);
			reqBuilder.POST(bodyPublisher);
		} else if (delete) {
			reqBuilder.DELETE();
		} else {
			reqBuilder.GET();
		}
		
		HttpResponse<byte[]> response;
		try {
			response = client.send(reqBuilder.build(), BODY_HANDLER);
		}
		catch (Exception e) {
			throw new Exception("An error occurred while submitting resource to OpenMRS: " + resource, e);
		}
		
		if (!allowedStatuses.contains(response.statusCode())) {
			String msg = " with status code: " + response.statusCode();
			if (response.body() != null) {
				msg += (", " + new String(response.body(), UTF_8));
			}
			
			throw new Exception("Request to OpenMRS failed" + msg);
		}
		
		return response;
	}
	
	protected String getAuthHeader() throws Exception {
		Exchange exchange = ExchangeBuilder.anExchange(camelContext).build();
		oauthProcessor.process(exchange);
		String oauthHeader = exchange.getMessage().getBody(String.class);
		if (oauthHeader != null) {
			return oauthHeader;
		}
		
		final String userAndPass = username + ":" + new String(password);
		byte[] auth = Base64.getEncoder().encode(userAndPass.getBytes(UTF_8));
		return "Basic " + new String(auth, UTF_8);
	}
	
}
