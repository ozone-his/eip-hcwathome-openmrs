/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.hcwathome.openmrs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.hl7.fhir.r4.model.Appointment;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Encounter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openmrs.eip.EIPException;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.ICriterion;
import ca.uhn.fhir.rest.gclient.ICriterionInternal;
import ca.uhn.fhir.rest.gclient.IDelete;
import ca.uhn.fhir.rest.gclient.IDeleteTyped;
import ca.uhn.fhir.rest.gclient.IQuery;
import ca.uhn.fhir.rest.gclient.IUntypedQuery;
import ca.uhn.fhir.rest.gclient.IUpdate;
import ca.uhn.fhir.rest.gclient.IUpdateTyped;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;

@ExtendWith(MockitoExtension.class)
public class HcwFhirClientTest {
	
	@Mock
	private IGenericClient mockFhirClient;
	
	private HcwFhirClient client = new HcwFhirClient();
	
	@BeforeEach
	public void setUp() {
		client = Mockito.spy(client);
		Mockito.doReturn(mockFhirClient).when(client).getFhirClient();
	}
	
	@Test
	public void getAppointmentByIdentifier_shouldReturnAppointmentMatchingTheUuid() {
		final String uuid = "test-uuid";
		IUntypedQuery mockTypedQuery = Mockito.mock(IUntypedQuery.class);
		IQuery mockQuery = Mockito.mock(IQuery.class);
		Bundle bundle = new Bundle();
		Appointment expected = new Appointment();
		bundle.addEntry().setResource(expected);
		when(mockFhirClient.search()).thenReturn(mockTypedQuery);
		when(mockTypedQuery.forResource(Appointment.class)).thenReturn(mockQuery);
		when(mockQuery.where(Mockito.any(ICriterion.class))).thenReturn(mockQuery);
		when(mockQuery.execute()).thenReturn(bundle);
		
		Appointment actual = client.getAppointmentByIdentifier(uuid);
		
		assertSame(expected, actual);
		ArgumentCaptor<ICriterion> criterionCaptor = ArgumentCaptor.forClass(ICriterion.class);
		verify(mockQuery).where(criterionCaptor.capture());
		ICriterionInternal criterion = (ICriterionInternal) criterionCaptor.getValue();
		assertEquals("identifier", criterion.getParameterName());
		assertEquals(uuid, criterion.getParameterValue(null));
	}
	
	@Test
	public void getAppointmentByIdentifier_shouldFailIfMultipleMatchesAreFound() {
		final String uuid = "test-uuid";
		IUntypedQuery mockTypedQuery = Mockito.mock(IUntypedQuery.class);
		IQuery mockQuery = Mockito.mock(IQuery.class);
		Bundle bundle = new Bundle();
		bundle.addEntry().setResource(new Appointment());
		bundle.addEntry().setResource(new Appointment());
		when(mockFhirClient.search()).thenReturn(mockTypedQuery);
		when(mockTypedQuery.forResource(Appointment.class)).thenReturn(mockQuery);
		when(mockQuery.where(Mockito.any(ICriterion.class))).thenReturn(mockQuery);
		when(mockQuery.execute()).thenReturn(bundle);
		
		EIPException e = assertThrows(EIPException.class, () -> client.getAppointmentByIdentifier(uuid));
		
		assertEquals("Found multiple appointments in hcw@home with external identifier " + uuid, e.getMessage());
		ArgumentCaptor<ICriterion> criterionCaptor = ArgumentCaptor.forClass(ICriterion.class);
		verify(mockQuery).where(criterionCaptor.capture());
		ICriterionInternal criterion = (ICriterionInternal) criterionCaptor.getValue();
		assertEquals("identifier", criterion.getParameterName());
		assertEquals(uuid, criterion.getParameterValue(null));
	}
	
	@Test
	public void getAppointmentByIdentifier_shouldReturnNullWhenNoMatchIsFound() {
		final String uuid = "test-uuid";
		IUntypedQuery mockTypedQuery = Mockito.mock(IUntypedQuery.class);
		IQuery mockQuery = Mockito.mock(IQuery.class);
		when(mockFhirClient.search()).thenReturn(mockTypedQuery);
		when(mockTypedQuery.forResource(Appointment.class)).thenReturn(mockQuery);
		when(mockQuery.where(Mockito.any(ICriterion.class))).thenReturn(mockQuery);
		when(mockQuery.execute()).thenThrow(ResourceNotFoundException.class);
		
		assertNull(client.getAppointmentByIdentifier(uuid));
		
		ArgumentCaptor<ICriterion> criterionCaptor = ArgumentCaptor.forClass(ICriterion.class);
		verify(mockQuery).where(criterionCaptor.capture());
		ICriterionInternal criterion = (ICriterionInternal) criterionCaptor.getValue();
		assertEquals("identifier", criterion.getParameterName());
		assertEquals(uuid, criterion.getParameterValue(null));
	}
	
	@Test
	public void getEncounterByAppointment_shouldReturnEncounterMatchingTheAppointmentUuid() {
		final String appointmentUuid = "test-appointment-uuid";
		IUntypedQuery mockTypedQuery = Mockito.mock(IUntypedQuery.class);
		IQuery mockQuery = Mockito.mock(IQuery.class);
		Bundle bundle = new Bundle();
		Encounter expected = new Encounter();
		bundle.addEntry().setResource(expected);
		when(mockFhirClient.search()).thenReturn(mockTypedQuery);
		when(mockTypedQuery.forResource(Encounter.class)).thenReturn(mockQuery);
		when(mockQuery.where(Mockito.any(ICriterion.class))).thenReturn(mockQuery);
		when(mockQuery.execute()).thenReturn(bundle);
		
		Encounter actual = client.getEncounterByAppointment(appointmentUuid);
		
		assertSame(expected, actual);
		ArgumentCaptor<ICriterion> criterionCaptor = ArgumentCaptor.forClass(ICriterion.class);
		verify(mockQuery).where(criterionCaptor.capture());
		ICriterionInternal criterion = (ICriterionInternal) criterionCaptor.getValue();
		assertEquals("appointment.identifier", criterion.getParameterName());
		assertEquals(appointmentUuid, criterion.getParameterValue(null));
	}
	
	@Test
	public void getEncounterByAppointment_shouldFailIfMultipleMatchesAreFound() {
		final String apptUuid = "test-appointment-uuid";
		IUntypedQuery mockTypedQuery = Mockito.mock(IUntypedQuery.class);
		IQuery mockQuery = Mockito.mock(IQuery.class);
		Bundle bundle = new Bundle();
		bundle.addEntry().setResource(new Encounter());
		bundle.addEntry().setResource(new Encounter());
		when(mockFhirClient.search()).thenReturn(mockTypedQuery);
		when(mockTypedQuery.forResource(Encounter.class)).thenReturn(mockQuery);
		when(mockQuery.where(Mockito.any(ICriterion.class))).thenReturn(mockQuery);
		when(mockQuery.execute()).thenReturn(bundle);
		
		EIPException e = assertThrows(EIPException.class, () -> client.getEncounterByAppointment(apptUuid));
		
		assertEquals("Found multiple encounters from hcw@home for appointment with external identifier " + apptUuid,
		    e.getMessage());
		ArgumentCaptor<ICriterion> criterionCaptor = ArgumentCaptor.forClass(ICriterion.class);
		verify(mockQuery).where(criterionCaptor.capture());
		ICriterionInternal criterion = (ICriterionInternal) criterionCaptor.getValue();
		assertEquals("appointment.identifier", criterion.getParameterName());
		assertEquals(apptUuid, criterion.getParameterValue(null));
	}
	
	@Test
	public void getEncounterByAppointment_shouldReturnNullWhenNoMatchIsFound() {
		final String appointmentUuid = "test-appointment-uuid";
		IUntypedQuery mockTypedQuery = Mockito.mock(IUntypedQuery.class);
		IQuery mockQuery = Mockito.mock(IQuery.class);
		when(mockFhirClient.search()).thenReturn(mockTypedQuery);
		when(mockTypedQuery.forResource(Encounter.class)).thenReturn(mockQuery);
		when(mockQuery.where(Mockito.any(ICriterion.class))).thenReturn(mockQuery);
		when(mockQuery.execute()).thenThrow(ResourceNotFoundException.class);
		
		assertNull(client.getEncounterByAppointment(appointmentUuid));
		
		ArgumentCaptor<ICriterion> criterionCaptor = ArgumentCaptor.forClass(ICriterion.class);
		verify(mockQuery).where(criterionCaptor.capture());
		ICriterionInternal criterion = (ICriterionInternal) criterionCaptor.getValue();
		assertEquals("appointment.identifier", criterion.getParameterName());
		assertEquals(appointmentUuid, criterion.getParameterValue(null));
	}
	
	@Test
	public void updateAppointment_shouldUpdateTheAppointment() {
		Appointment appointment = new Appointment();
		IUpdate mockUpdate = Mockito.mock(IUpdate.class);
		IUpdateTyped mockUpdateTyped = Mockito.mock(IUpdateTyped.class);
		when(mockFhirClient.update()).thenReturn(mockUpdate);
		when(mockUpdate.resource(appointment)).thenReturn(mockUpdateTyped);
		MethodOutcome mockOutcome = Mockito.mock(MethodOutcome.class);
		when(mockOutcome.getResponseStatusCode()).thenReturn(200);
		when(mockUpdateTyped.execute()).thenReturn(mockOutcome);
		
		client.updateAppointment(appointment);
		
		verify(mockUpdateTyped).execute();
	}
	
	@Test
	public void updateAppointment_shouldFailForNon200Response() {
		Appointment appointment = new Appointment();
		IUpdate mockUpdate = Mockito.mock(IUpdate.class);
		IUpdateTyped mockUpdateTyped = Mockito.mock(IUpdateTyped.class);
		when(mockFhirClient.update()).thenReturn(mockUpdate);
		when(mockUpdate.resource(appointment)).thenReturn(mockUpdateTyped);
		MethodOutcome mockOutcome = Mockito.mock(MethodOutcome.class);
		when(mockOutcome.getResponseStatusCode()).thenReturn(500);
		when(mockUpdateTyped.execute()).thenReturn(mockOutcome);
		
		EIPException e = assertThrows(EIPException.class, () -> client.updateAppointment(appointment));

		assertEquals("Failed to update invite in hcw@home, status code 500", e.getMessage());
	}
	
	@Test
	public void updateAppointment_shouldFailWithEipExceptionIfOperationFails() {
		Appointment appointment = new Appointment();
		IUpdate mockUpdate = Mockito.mock(IUpdate.class);
		IUpdateTyped mockUpdateTyped = Mockito.mock(IUpdateTyped.class);
		when(mockFhirClient.update()).thenReturn(mockUpdate);
		when(mockUpdate.resource(appointment)).thenReturn(mockUpdateTyped);
		MethodOutcome mockOutcome = Mockito.mock(MethodOutcome.class);
		lenient().when(mockOutcome.getResponseStatusCode()).thenReturn(500);
		when(mockUpdateTyped.execute()).thenThrow(RuntimeException.class);
		
		EIPException exception = assertThrows(EIPException.class, () -> client.updateAppointment(appointment));

		assertEquals("Failed to update invite in hcw@home", exception.getMessage());
	}
	
	@Test
	public void deleteAppointment_shouldDeleteTheAppointment() {
		Appointment appointment = new Appointment();
		IDelete mockDelete = Mockito.mock(IDelete.class);
		IDeleteTyped mockDeleteTyped = Mockito.mock(IDeleteTyped.class);
		when(mockFhirClient.delete()).thenReturn(mockDelete);
		when(mockDelete.resource(appointment)).thenReturn(mockDeleteTyped);
		MethodOutcome mockOutcome = Mockito.mock(MethodOutcome.class);
		when(mockOutcome.getResponseStatusCode()).thenReturn(200);
		when(mockDeleteTyped.execute()).thenReturn(mockOutcome);
		
		client.deleteAppointment(appointment);
		
		verify(mockDeleteTyped).execute();
	}
	
	@Test
	public void deleteAppointment_shouldFailForNon200Response() {
		Appointment appointment = new Appointment();
		IDelete mockDelete = Mockito.mock(IDelete.class);
		IDeleteTyped mockDeleteTyped = Mockito.mock(IDeleteTyped.class);
		when(mockFhirClient.delete()).thenReturn(mockDelete);
		when(mockDelete.resource(appointment)).thenReturn(mockDeleteTyped);
		MethodOutcome mockOutcome = Mockito.mock(MethodOutcome.class);
		when(mockOutcome.getResponseStatusCode()).thenReturn(500);
		when(mockDeleteTyped.execute()).thenReturn(mockOutcome);
		
		EIPException e = assertThrows(EIPException.class, () -> client.deleteAppointment(appointment));

		assertEquals("Failed to delete invite from hcw@home, status code 500", e.getMessage());
	}
	
	@Test
	public void deleteAppointment_shouldFailWithEipExceptionIfOperationFails() {
		Appointment appointment = new Appointment();
		IDelete mockDelete = Mockito.mock(IDelete.class);
		IDeleteTyped mockDeleteTyped = Mockito.mock(IDeleteTyped.class);
		when(mockFhirClient.delete()).thenReturn(mockDelete);
		when(mockDelete.resource(appointment)).thenReturn(mockDeleteTyped);
		MethodOutcome mockOutcome = Mockito.mock(MethodOutcome.class);
		lenient().when(mockOutcome.getResponseStatusCode()).thenReturn(500);
		when(mockDeleteTyped.execute()).thenThrow(RuntimeException.class);
		
		EIPException exception = assertThrows(EIPException.class, () -> client.deleteAppointment(appointment));

		assertEquals("Failed to delete invite in hcw@home", exception.getMessage());
	}
	
}
