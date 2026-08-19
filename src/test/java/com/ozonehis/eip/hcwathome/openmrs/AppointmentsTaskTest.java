/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.hcwathome.openmrs;

import static com.ozonehis.eip.hcwathome.openmrs.AppointmentTaskUtils.createOpenMrsEncounter;
import static com.ozonehis.eip.hcwathome.openmrs.AppointmentTaskUtils.createOpenMrsObs;
import static com.ozonehis.eip.hcwathome.openmrs.AppointmentsTask.QUERY;
import static com.ozonehis.eip.hcwathome.openmrs.AppointmentsTask.QUERY_PATIENT_UUID;
import static com.ozonehis.eip.hcwathome.openmrs.AppointmentsTask.QUERY_PROV_UUID;
import static com.ozonehis.eip.hcwathome.openmrs.AppointmentsTask.SQL_UPDATE_APPT;
import static com.ozonehis.eip.hcwathome.openmrs.DbUtils.executeQuery;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.hl7.fhir.r4.model.Appointment;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.StringType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.powermock.reflect.Whitebox;

@ExtendWith(MockitoExtension.class)
public class AppointmentsTaskTest {
	
	private static final String ENC_TYPE_UUID = "encounter-type-uuid";
	
	private static final String QN_CONCEPT_UUID = "qn-concept-uuid";
	
	private static final String NOTES_EXT_URL = "test-system-url";
	
	private MockedStatic<DbUtils> mockDbUtils;
	
	private MockedStatic<LocalDateTimeUtils> mockDateTimeUtils;
	
	private MockedStatic<AppointmentTaskUtils> mockTaskUtils;
	
	@Mock
	private DataSource mockDataSource;
	
	@Mock
	private Connection mockConnection;
	
	@Mock
	private PreparedStatement mockStatement;
	
	@Mock
	private HcwFhirClient mockHcwClient;
	
	@Mock
	private OpenmrsFhirClient mockOpenMrsClient;
	
	@Mock
	private OpenMrsRestClient mockOpenMrsRestClient;
	
	private AppointmentsTask task;
	
	@BeforeEach
	void setup() throws Exception {
		mockDbUtils = Mockito.mockStatic(DbUtils.class);
		mockDateTimeUtils = Mockito.mockStatic(LocalDateTimeUtils.class);
		mockTaskUtils = Mockito.mockStatic(AppointmentTaskUtils.class);
		task = new AppointmentsTask(mockHcwClient, mockOpenMrsClient, mockOpenMrsRestClient, mockDataSource);
		Whitebox.setInternalState(task, "encounterTypeUuid", ENC_TYPE_UUID);
		Whitebox.setInternalState(task, "questionConceptUuid", QN_CONCEPT_UUID);
		Whitebox.setInternalState(task, "notesExtensionUrl", NOTES_EXT_URL);
		lenient().when(mockDataSource.getConnection()).thenReturn(mockConnection);
		lenient().when(mockConnection.prepareStatement(SQL_UPDATE_APPT)).thenReturn(mockStatement);
	}
	
	@AfterEach
	void tearDown() {
		mockDbUtils.close();
		mockDateTimeUtils.close();
		mockTaskUtils.close();
	}
	
	@Test
	public void execute_shouldProcessCompletedAppointments() throws Exception {
		final int appId1 = 1;
		final String appUuid1 = "appointment-uuid-1";
		final int patientId1 = 3;
		final String patientUuid1 = "patient-uuid-1";
		final String providerUuid1 = "provider-uuid-1";
		final String visitUuid1 = "visit-uuid-1";
		final String clinicalNotes1 = "Patient has a fever";
		Date startDate1 = new Date();
		Date endDate1 = new Date();
		Period period1 = new Period();
		period1.setStart(startDate1);
		period1.setEnd(endDate1);
		final int appId2 = 2;
		final String appUuid2 = "appointment-uuid-2";
		final int patientId2 = 4;
		final String patientUuid2 = "patient-uuid-2";
		final String providerUuid2 = "provider-uuid-2";
		Date startDate2 = new Date();
		Date endDate2 = new Date();
		Period period2 = new Period();
		period2.setStart(startDate2);
		period2.setEnd(endDate2);
		final Encounter mockEnc1 = Mockito.mock(Encounter.class);
		when(mockEnc1.getPeriod()).thenReturn(period1);
		final Extension mockExt1 = Mockito.mock(Extension.class);
		when(mockExt1.getValue()).thenReturn(new StringType(clinicalNotes1));
		when(mockEnc1.getExtensionByUrl(NOTES_EXT_URL)).thenReturn(mockExt1);
		final Encounter mockEnc2 = Mockito.mock(Encounter.class);
		when(mockEnc2.getPeriod()).thenReturn(period2);
		final Extension mockExt2 = Mockito.mock(Extension.class);
		when(mockEnc2.getExtensionByUrl(NOTES_EXT_URL)).thenReturn(mockExt2);
		final LocalDateTime asOf = LocalDateTime.of(2025, 10, 21, 12, 00, 00);
		when(LocalDateTimeUtils.getCurrentTime()).thenReturn(asOf);
		Map<String, Object> a1 = Map.of("patient_appointment_id", appId1, "uuid", appUuid1, "patient_id", patientId1,
		    "provider_uuid", providerUuid1);
		Map<String, Object> a2 = Map.of("patient_appointment_id", appId2, "uuid", appUuid2, "patient_id", patientId2,
		    "provider_uuid", providerUuid2);
		List<Object> args = List.of("Virtual", "Scheduled", asOf, 0);
		when(executeQuery(QUERY, mockDataSource, args)).thenReturn(List.of(a1, a2));
		when(executeQuery(QUERY_PATIENT_UUID, mockDataSource, List.of(patientId1)))
		        .thenReturn(List.of(Map.of("uuid", patientUuid1)));
		when(executeQuery(QUERY_PROV_UUID, mockDataSource, List.of(appId1)))
		        .thenReturn(List.of(Map.of("uuid", providerUuid1)));
		when(executeQuery(QUERY_PATIENT_UUID, mockDataSource, List.of(patientId2)))
		        .thenReturn(List.of(Map.of("uuid", patientUuid2)));
		when(executeQuery(QUERY_PROV_UUID, mockDataSource, List.of(appId2)))
		        .thenReturn(List.of(Map.of("uuid", providerUuid2)));
		when(mockHcwClient.getAppointmentByIdentifier(appUuid1)).thenReturn(Mockito.mock(Appointment.class));
		when(mockHcwClient.getAppointmentByIdentifier(appUuid2)).thenReturn(Mockito.mock(Appointment.class));
		when(mockHcwClient.getEncounterByAppointment(appUuid1)).thenReturn(mockEnc1);
		when(mockHcwClient.getEncounterByAppointment(appUuid2)).thenReturn(mockEnc2);
		when(AppointmentTaskUtils.getActiveVisitUuid(patientUuid1, mockOpenMrsRestClient)).thenReturn(visitUuid1);
		Map<String, Object> encData1 = Map.of("uuid", "enc-uuid-1");
		Map<String, Object> encData2 = Map.of("uuid", "enc-uuid-2");
		when(createOpenMrsEncounter(mockEnc1, appUuid1, ENC_TYPE_UUID, patientUuid1, visitUuid1, providerUuid1, startDate1,
		    endDate1, mockDataSource, mockOpenMrsClient)).thenReturn(encData1);
		when(createOpenMrsEncounter(mockEnc2, appUuid2, ENC_TYPE_UUID, patientUuid2, null, providerUuid2, startDate2,
		    endDate2, mockDataSource, mockOpenMrsClient)).thenReturn(encData2);
		
		task.execute();
		
		//Only appointment 1 had clinical notes
		mockTaskUtils.verify(() -> createOpenMrsObs(any(), any(), any(), any(), any(), any(), any(), any()));
		mockTaskUtils.verify(() -> createOpenMrsObs(appUuid1, patientUuid1, encData1, QN_CONCEPT_UUID, clinicalNotes1,
		    endDate1, mockDataSource, mockOpenMrsClient));
		Mockito.verify(mockStatement).setInt(1, appId1);
		Mockito.verify(mockStatement).setInt(1, appId2);
		Mockito.verify(mockStatement, times(2)).executeUpdate();
	}
	
	@Test
	public void execute_shouldSkipNoEncounterForTheAppointmentIsNotFoundInHcwAtHome() throws Exception {
		final int appId = 1;
		final String appUuid = "appointment-uuid-1";
		final LocalDateTime asOf = LocalDateTime.of(2025, 10, 21, 12, 00, 00);
		when(LocalDateTimeUtils.getCurrentTime()).thenReturn(asOf);
		Map<String, Object> a1 = Map.of("patient_appointment_id", appId, "uuid", appUuid);
		List<Object> args = List.of("Virtual", "Scheduled", asOf, 0);
		when(executeQuery(QUERY, mockDataSource, args)).thenReturn(List.of(a1));
		when(mockHcwClient.getAppointmentByIdentifier(appUuid)).thenReturn(Mockito.mock(Appointment.class));
		
		task.execute();
		
		Mockito.verifyNoInteractions(mockConnection);
		Mockito.verifyNoInteractions(mockStatement);
		mockTaskUtils.verifyNoInteractions();
	}
	
	@Test
	public void execute_shouldSkipIfAppointmentIsNotFoundInHcwAtHome() throws Exception {
		final int appId = 1;
		final String appUuid = "appointment-uuid-1";
		final LocalDateTime asOf = LocalDateTime.of(2025, 10, 21, 12, 00, 00);
		when(LocalDateTimeUtils.getCurrentTime()).thenReturn(asOf);
		Map<String, Object> a1 = Map.of("patient_appointment_id", appId, "uuid", appUuid);
		List<Object> args = List.of("Virtual", "Scheduled", asOf, 0);
		when(executeQuery(QUERY, mockDataSource, args)).thenReturn(List.of(a1));
		
		task.execute();
		
		Mockito.verify(mockHcwClient, never()).getEncounterByAppointment(appUuid);
		Mockito.verifyNoInteractions(mockConnection);
		Mockito.verifyNoInteractions(mockStatement);
		mockTaskUtils.verifyNoInteractions();
	}
	
}
