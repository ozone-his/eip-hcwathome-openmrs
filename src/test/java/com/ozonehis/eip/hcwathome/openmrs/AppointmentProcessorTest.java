/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.hcwathome.openmrs;

import static com.ozonehis.eip.hcwathome.openmrs.AppointmentProcessor.QUERY_APPOINTMENT;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.hl7.fhir.r4.model.Appointment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.powermock.reflect.Whitebox;

@ExtendWith(MockitoExtension.class)
public class AppointmentProcessorTest {
	
	private final String EMAIL_ATTR_TYPE_UUID = "type-uuid";
	
	private final String ID_SYSTEM = "uuid:1234";
	
	private MockedStatic<DbUtils> mockDbUtils;
	
	private MockedStatic<Utils> mockUtils;
	
	private HcwFhirClient mockHcwClient;
	
	private DataSource mockDataSource;
	
	private AppointmentProcessor processor;
	
	@BeforeEach
	public void setUp() {
		mockDbUtils = Mockito.mockStatic(DbUtils.class);
		mockUtils = Mockito.mockStatic(Utils.class);
		mockHcwClient = mock(HcwFhirClient.class);
		mockDataSource = mock(DataSource.class);
		processor = new AppointmentProcessor(mockHcwClient, mockDataSource);
		Whitebox.setInternalState(processor, "emailPersonAttTypeUuid", EMAIL_ATTR_TYPE_UUID);
		Whitebox.setInternalState(processor, "idSystem", ID_SYSTEM);
	}
	
	@AfterEach
	public void tearDown() {
		mockDbUtils.close();
		mockUtils.close();
	}
	
	@Test
	public void process_ShouldCreateAppointmentIfNotExistingAndScheduledForCreateAction() throws Exception {
		String uuid = "test-uuid";
		Action action = Action.CREATE;
		when(mockHcwClient.getAppointmentByIdentifier(uuid)).thenReturn(null);
		Map<String, Object> data = Map.of("status", "Scheduled");
		List<Map<String, Object>> appointments = List.of(data);
		when(DbUtils.executeQuery(QUERY_APPOINTMENT, mockDataSource, List.of(uuid))).thenReturn(appointments);
		Appointment mockAppointment = Mockito.mock(Appointment.class);
		when(Utils.buildFhirAppointment(uuid, data, EMAIL_ATTR_TYPE_UUID, ID_SYSTEM, mockDataSource))
		        .thenReturn(mockAppointment);
		
		processor.process(uuid, action);
		
		verify(mockHcwClient).create(mockAppointment);
	}
	
	@Test
	public void process_ShouldCreateAppointmentIfNotExistingAndScheduledForUpdateAction() throws Exception {
		String uuid = "test-uuid";
		Action action = Action.UPDATE;
		when(mockHcwClient.getAppointmentByIdentifier(uuid)).thenReturn(null);
		Map<String, Object> data = Map.of("status", "Scheduled");
		List<Map<String, Object>> appointments = List.of(data);
		when(DbUtils.executeQuery(QUERY_APPOINTMENT, mockDataSource, List.of(uuid))).thenReturn(appointments);
		Appointment mockAppointment = Mockito.mock(Appointment.class);
		when(Utils.buildFhirAppointment(uuid, data, EMAIL_ATTR_TYPE_UUID, ID_SYSTEM, mockDataSource))
		        .thenReturn(mockAppointment);
		
		processor.process(uuid, action);
		
		verify(mockHcwClient).create(mockAppointment);
	}
	
	@Test
	public void process_ShouldSkipCreateAppointmentIfStatusIsNotScheduled() throws Exception {
		String uuid = "test-uuid";
		Action action = Action.CREATE;
		when(mockHcwClient.getAppointmentByIdentifier(uuid)).thenReturn(null);
		Map<String, Object> data = Map.of("status", "Requested");
		List<Map<String, Object>> appointments = List.of(data);
		when(DbUtils.executeQuery(QUERY_APPOINTMENT, mockDataSource, List.of(uuid))).thenReturn(appointments);
		
		processor.process(uuid, action);
		
		verify(mockHcwClient, never()).create(any());
	}
	
	@Test
	public void process_ShouldNotUpdateAppointmentIfCompleted() throws Exception {
		String uuid = "test-uuid";
		Action action = Action.UPDATE;
		when(mockHcwClient.getAppointmentByIdentifier(uuid)).thenReturn(null);
		Map<String, Object> data = Map.of("status", "Completed");
		List<Map<String, Object>> appointments = List.of(data);
		Appointment mockAppointment = Mockito.mock(Appointment.class);
		when(mockHcwClient.getAppointmentByIdentifier(uuid)).thenReturn(mockAppointment);
		when(DbUtils.executeQuery(QUERY_APPOINTMENT, mockDataSource, List.of(uuid))).thenReturn(appointments);
		when(Utils.updateFhirAppointment(mockAppointment, data, EMAIL_ATTR_TYPE_UUID, mockDataSource)).thenReturn(true);
		
		processor.process(uuid, action);
		
		verify(mockHcwClient, never()).updateAppointment(any());
	}
	
	@Test
	public void process_ShouldUpdateAModifiedAppointmentForCreateAction() throws Exception {
		String uuid = "test-uuid";
		Action action = Action.CREATE;
		when(mockHcwClient.getAppointmentByIdentifier(uuid)).thenReturn(null);
		Map<String, Object> data = Map.of("status", "Scheduled");
		List<Map<String, Object>> appointments = List.of(data);
		when(DbUtils.executeQuery(QUERY_APPOINTMENT, mockDataSource, List.of(uuid))).thenReturn(appointments);
		Appointment mockAppointment = Mockito.mock(Appointment.class);
		when(mockHcwClient.getAppointmentByIdentifier(uuid)).thenReturn(mockAppointment);
		when(Utils.updateFhirAppointment(mockAppointment, data, EMAIL_ATTR_TYPE_UUID, mockDataSource)).thenReturn(true);
		
		processor.process(uuid, action);
		
		verify(mockHcwClient).updateAppointment(mockAppointment);
	}
	
	@Test
	public void process_ShouldUpdateAModifiedAppointmentForUpdateAction() throws Exception {
		String uuid = "test-uuid";
		Action action = Action.UPDATE;
		when(mockHcwClient.getAppointmentByIdentifier(uuid)).thenReturn(null);
		Map<String, Object> data = Map.of("status", "Scheduled");
		List<Map<String, Object>> appointments = List.of(data);
		when(DbUtils.executeQuery(QUERY_APPOINTMENT, mockDataSource, List.of(uuid))).thenReturn(appointments);
		Appointment mockAppointment = Mockito.mock(Appointment.class);
		when(mockHcwClient.getAppointmentByIdentifier(uuid)).thenReturn(mockAppointment);
		when(Utils.updateFhirAppointment(mockAppointment, data, EMAIL_ATTR_TYPE_UUID, mockDataSource)).thenReturn(true);
		
		processor.process(uuid, action);
		
		verify(mockHcwClient).updateAppointment(mockAppointment);
	}
	
	@Test
	public void process_ShouldSkipUpdateIfNoChangesAreDetected() throws Exception {
		String uuid = "test-uuid";
		Action action = Action.UPDATE;
		when(mockHcwClient.getAppointmentByIdentifier(uuid)).thenReturn(null);
		Map<String, Object> data = Map.of("status", "Scheduled");
		List<Map<String, Object>> appointments = List.of(data);
		when(DbUtils.executeQuery(QUERY_APPOINTMENT, mockDataSource, List.of(uuid))).thenReturn(appointments);
		Appointment mockApp = Mockito.mock(Appointment.class);
		when(mockHcwClient.getAppointmentByIdentifier(uuid)).thenReturn(mockApp);
		
		processor.process(uuid, action);
		
		mockUtils.verify(() -> Utils.updateFhirAppointment(mockApp, data, EMAIL_ATTR_TYPE_UUID, mockDataSource));
		verify(mockHcwClient, never()).updateAppointment(any());
	}
	
	@Test
	public void process_ShouldDeleteAppointmentIfPresentInHcwAtHome() throws Exception {
		String uuid = "test-uuid";
		Action action = Action.DELETE;
		Appointment appointment = mock(Appointment.class);
		when(mockHcwClient.getAppointmentByIdentifier(uuid)).thenReturn(appointment);
		
		processor.process(uuid, action);
		
		verify(mockHcwClient).deleteAppointment(appointment);
	}
	
	@Test
	public void process_ShouldSkipDeleteAppointmentIfNotPresentInHcwAtHome() throws Exception {
		String uuid = "test-uuid";
		Action action = Action.DELETE;
		
		processor.process(uuid, action);
		
		verify(mockHcwClient).getAppointmentByIdentifier(uuid);
		verify(mockHcwClient, never()).deleteAppointment(any());
	}
	
}
