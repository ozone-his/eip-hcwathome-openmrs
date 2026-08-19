/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.hcwathome.openmrs;

import static com.ozonehis.eip.hcwathome.openmrs.AppointmentTaskUtils.QUERY_ENCOUNTER;
import static com.ozonehis.eip.hcwathome.openmrs.AppointmentTaskUtils.QUERY_OBS;
import static com.ozonehis.eip.hcwathome.openmrs.AppointmentTaskUtils.createOpenMrsEncounter;
import static com.ozonehis.eip.hcwathome.openmrs.AppointmentTaskUtils.createOpenMrsObs;
import static com.ozonehis.eip.hcwathome.openmrs.AppointmentsTask.ENC_TYPE_SYSTEM;
import static com.ozonehis.eip.hcwathome.openmrs.DbUtils.executeQuery;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Observation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openmrs.eip.EIPException;

@ExtendWith(MockitoExtension.class)
public class AppointmentTaskUtilsTest {
	
	private static MockedStatic<DbUtils> mockDbUtils;
	
	@Mock
	private DataSource mockDataSource;
	
	@Mock
	private OpenmrsFhirClient mockOpenmrsClient;
	
	@BeforeEach
	public void setUp() {
		mockDbUtils = Mockito.mockStatic(DbUtils.class);
	}
	
	@AfterEach
	public void tearDown() {
		mockDbUtils.close();
	}
	
	@Test
	public void createOpenMrsEncounter_shouldCreateNewEncounterWhenNoneExists() throws Exception {
		String appointmentUuid = "test-uuid";
		String encTypeUuid = "enc-type-uuid";
		String patientUuid = "pat-uuid";
		String providerUuid = "prov-uuid";
		String visitUuid = "visit-uuid";
		Date startDate = new Date();
		Date endDate = new Date();
		Map<String, Object> createdEnc = Map.of("encounter_id", 1, "uuid", "enc_uuid");
		when(executeQuery(QUERY_ENCOUNTER, mockDataSource, List.of(patientUuid, startDate))).thenReturn(List.of())
		        .thenReturn(List.of(createdEnc));
		Encounter encounter = new Encounter();
		
		Map<String, Object> result = createOpenMrsEncounter(encounter, appointmentUuid, encTypeUuid, patientUuid, visitUuid,
		    providerUuid, startDate, endDate, mockDataSource, mockOpenmrsClient);
		
		verify(mockOpenmrsClient).create(encounter);
		assertEquals(createdEnc, result);
		assertEquals(1, encounter.getType().size());
		assertEquals(1, encounter.getTypeFirstRep().getCoding().size());
		assertEquals(ENC_TYPE_SYSTEM, encounter.getTypeFirstRep().getCodingFirstRep().getSystem());
		assertEquals(encTypeUuid, encounter.getTypeFirstRep().getCodingFirstRep().getCode());
		assertEquals("Patient/" + patientUuid, encounter.getSubject().getReference());
		assertEquals("Encounter/" + visitUuid, encounter.getPartOf().getReference());
		assertEquals(1, encounter.getParticipant().size());
		assertEquals("Practitioner/" + providerUuid, encounter.getParticipantFirstRep().getIndividual().getReference());
		assertEquals(startDate, encounter.getPeriod().getStart());
		assertEquals(endDate, encounter.getPeriod().getEnd());
	}
	
	@Test
	public void createOpenMrsEncounter_shouldReturnExistingEncounterIfItExists() throws Exception {
		String appointmentUuid = "uuid-2";
		String patientUuid = "pat-uuid";
		Date startDate = new Date();
		Date endDate = new Date();
		Map<String, Object> existingEnc = Map.of("encounter_id", 1, "uuid", "enc_uuid");
		when(executeQuery(QUERY_ENCOUNTER, mockDataSource, List.of(patientUuid, startDate)))
		        .thenReturn(List.of(existingEnc));
		Encounter encounter = new Encounter();
		
		Map<String, Object> result = createOpenMrsEncounter(encounter, appointmentUuid, "enc-type-uuid", patientUuid, null,
		    "prov-uuid", startDate, endDate, mockDataSource, mockOpenmrsClient);
		
		verify(mockOpenmrsClient, never()).create(any());
		assertEquals(existingEnc, result);
	}
	
	@Test
	public void createOpenMrsEncounter_shouldThrowExceptionWhenMultipleEncountersExist() throws Exception {
		String uuid = "tst-uuid";
		String patientUuid = "pat-uuid";
		Date startDate = new Date();
		Map<String, Object> existingEnc = Map.of("encounter_id", 1, "uuid", "enc_uuid");
		when(executeQuery(QUERY_ENCOUNTER, mockDataSource, List.of(patientUuid, startDate)))
		        .thenReturn(List.of(existingEnc, existingEnc));
		Encounter encounter = new Encounter();
		
		Exception e = assertThrows(EIPException.class, () -> createOpenMrsEncounter(encounter, uuid, "enc-type-uuid",
		    patientUuid, null, "prov-uuid", startDate, null, mockDataSource, mockOpenmrsClient));
		
		assertEquals("Found 2 associated to appointment with uuid " + uuid, e.getMessage());
	}
	
	@Test
	public void createOpenMrsObs_shouldCreateNewOpenmrsObsIfNoneExists() throws Exception {
		String appointmentUuid = "test-obs-uuid";
		String patientUuid = "pat-uuid";
		String encUuid = "enc-uuid";
		Integer encId = 4;
		Map<String, Object> encData = Map.of("encounter_id", encId, "uuid", encUuid);
		String qnConceptUuid = "concept-uuid";
		String value = "test-value";
		Date obsDate = new Date();
		List<Object> queryValues = List.of(patientUuid, encId, obsDate);
		
		when(executeQuery(QUERY_OBS, mockDataSource, queryValues)).thenReturn(List.of());
		
		createOpenMrsObs(appointmentUuid, patientUuid, encData, qnConceptUuid, value, obsDate, mockDataSource,
		    mockOpenmrsClient);
		
		mockDbUtils.verify(() -> executeQuery(QUERY_OBS, mockDataSource, queryValues));
		ArgumentCaptor<Observation> obsCaptor = ArgumentCaptor.forClass(Observation.class);
		verify(mockOpenmrsClient).create(obsCaptor.capture());
		Observation createdObs = obsCaptor.getValue();
		assertEquals("Patient/" + patientUuid, createdObs.getSubject().getReference());
		assertEquals(1, createdObs.getCode().getCoding().size());
		assertEquals(qnConceptUuid, createdObs.getCode().getCodingFirstRep().getCode());
		assertEquals(value, createdObs.getValueStringType().getValue());
		assertEquals(obsDate, createdObs.getEffectiveDateTimeType().getValue());
		assertEquals(Observation.ObservationStatus.FINAL, createdObs.getStatus());
		assertEquals("Encounter/" + encUuid, createdObs.getEncounter().getReference());
	}
	
	@Test
	public void createOpenMrsObs_shouldNotCreateOpenmrsObsIfItAlreadyExists() throws Exception {
		String appointmentUuid = "obs-uuid";
		String patientUuid = "patient-uuid";
		Integer encId = 42;
		Map<String, Object> encData = Map.of("encounter_id", encId, "uuid", "enc-uuid");
		Date obsDate = new Date();
		List<Object> queryValues = List.of(patientUuid, encId, obsDate);
		when(executeQuery(QUERY_OBS, mockDataSource, queryValues)).thenReturn(List.of(Map.of("obs_id", 7)));
		
		createOpenMrsObs(appointmentUuid, patientUuid, encData, "qn-concept", "some-value", obsDate, mockDataSource,
		    mockOpenmrsClient);
		
		verify(mockOpenmrsClient, never()).create(any(Observation.class));
		mockDbUtils.verify(() -> executeQuery(QUERY_OBS, mockDataSource, queryValues));
		
	}
	
}
