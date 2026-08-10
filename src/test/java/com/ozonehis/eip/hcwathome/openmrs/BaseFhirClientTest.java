/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.hcwathome.openmrs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.hl7.fhir.r4.model.DomainResource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.ICreate;
import ca.uhn.fhir.rest.gclient.ICreateTyped;

@ExtendWith(MockitoExtension.class)
public class BaseFhirClientTest {
	
	@Mock
	private IGenericClient mockFhirClient;
	
	@Mock
	private DomainResource resource;
	
	private BaseFhirClient client = new HcwFhirClient();
	
	@BeforeEach
	public void setup() {
		client = Mockito.spy(client);
		Mockito.doReturn(mockFhirClient).when(client).getFhirClient();
	}
	
	@Test
	public void create_shouldCreateTheResource() {
		ICreate mockCreate = Mockito.mock(ICreate.class);
		ICreateTyped mockCreateTyped = Mockito.mock(ICreateTyped.class);
		when(mockFhirClient.create()).thenReturn(mockCreate);
		when(mockCreate.resource(resource)).thenReturn(mockCreateTyped);
		MethodOutcome mockOutcome = Mockito.mock(MethodOutcome.class);
		when(mockOutcome.getCreated()).thenReturn(true);
		when(mockCreateTyped.execute()).thenReturn(mockOutcome);
		
		client.create(resource);
		
		verify(mockCreateTyped).execute();
	}
	
	@Test
	public void create_shouldFailIfTheOutcomeIsNotCreated() {
		ICreate mockCreate = Mockito.mock(ICreate.class);
		ICreateTyped mockCreateTyped = Mockito.mock(ICreateTyped.class);
		when(mockFhirClient.create()).thenReturn(mockCreate);
		when(mockCreate.resource(resource)).thenReturn(mockCreateTyped);
		MethodOutcome mockOutcome = Mockito.mock(MethodOutcome.class);
		when(mockOutcome.getCreated()).thenReturn(false);
		when(mockCreateTyped.execute()).thenReturn(mockOutcome);
		
		Exception e = Assertions.assertThrows(Exception.class, () -> client.create(resource));
		
		final String msg = "Unexpected outcome " + mockOutcome + " when creating invite in hcw@home";
		assertEquals(msg, e.getMessage());
	}
	
	@Test
	public void create_shouldFailWithEipExceptionIfOperationFails() {
		ICreate mockCreate = Mockito.mock(ICreate.class);
		ICreateTyped mockCreateTyped = Mockito.mock(ICreateTyped.class);
		when(mockFhirClient.create()).thenReturn(mockCreate);
		when(mockCreate.resource(resource)).thenReturn(mockCreateTyped);
		MethodOutcome mockOutcome = Mockito.mock(MethodOutcome.class);
		lenient().when(mockOutcome.getCreated()).thenReturn(false);
		when(mockCreateTyped.execute()).thenThrow(RuntimeException.class);
		
		Exception e = Assertions.assertThrows(Exception.class, () -> client.create(resource));
		
		assertEquals("Failed to create invite in hcw@home", e.getMessage());
	}
	
}
