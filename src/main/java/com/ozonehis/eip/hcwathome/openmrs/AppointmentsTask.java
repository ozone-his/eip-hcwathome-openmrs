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
import static com.ozonehis.eip.hcwathome.openmrs.DbUtils.executeQuery;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Appointment;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Type;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class AppointmentsTask {
	
	public static final String PROP_INITIAL_DELAY = "appointments.task.initial.delay";
	
	public static final String PROP_DELAY = "appointments.task.delay";
	
	private static final String QUERY = "SELECT patient_appointment_id,patient_id,uuid FROM patient_appointment "
	        + "WHERE appointment_kind = ? AND status = ? AND end_date_time < ? AND voided = ?";
	
	private static final String QUERY_PATIENT_UUID = "SELECT uuid FROM person WHERE person_id = ?";
	
	private static final String QUERY_PROV_UUID = "SELECT uuid FROM provider WHERE provider_id = (" + Utils.QUERY_PROVIDER_ID
	        + ")";
	
	private static final String SQL_UPDATE_APPT = "UPDATE patient_appointment SET status = 'Completed' WHERE "
	        + "patient_appointment_id = ?";
	
	public static final String ENC_TYPE_SYSTEM = "http://fhir.openmrs.org/code-system/encounter-type";
	
	private HcwFhirClient hcwClient;
	
	private DataSource dataSource;
	
	private OpenmrsFhirClient openmrsClient;
	
	@Value("${openmrs.encounter.type.uuid}")
	private String encounterTypeUuid;
	
	@Value("${openmrs.obs.question.concept.uuid}")
	private String questionConceptUuid;
	
	@Value("${hcwathome.fhir.clinical.notes.ext.url}")
	private String notesExtensionUrl;
	
	public AppointmentsTask(HcwFhirClient hcwClient, OpenmrsFhirClient openmrsClient, DataSource dataSource) {
		this.hcwClient = hcwClient;
		this.openmrsClient = openmrsClient;
		this.dataSource = dataSource;
	}
	
	@Scheduled(initialDelayString = "${" + PROP_INITIAL_DELAY + "}", fixedDelayString = "${" + PROP_DELAY + "}")
	protected void execute() throws Exception {
		List<Object> args = List.of("Virtual", "Requested", LocalDateTimeUtils.getCurrentTime(), 0);
		List<Map<String, Object>> results = DbUtils.executeQuery(QUERY, dataSource, args);
		if (log.isDebugEnabled()) {
			log.debug("Found {} virtual appointments that should have ended by now", results.size());
		}
		
		//TODO Process the appointments in parallel
		for (Map<String, Object> a : results) {
			final String uuid = (String) a.get("uuid");
			Appointment appointment = hcwClient.getAppointmentByIdentifier(uuid);
			if (appointment == null) {
				if (log.isDebugEnabled()) {
					log.debug("No appointment found in hcw@home with uuid {}", uuid);
				}
				
				//Could be not yet synced to hcw.
				continue;
			}
			
			Encounter encounter = hcwClient.getEncounterByAppointment(uuid);
			if (encounter == null) {
				if (log.isDebugEnabled()) {
					log.debug("No encounter found in hcw@home associated to appointment with uuid {}", uuid);
				}
				//Multiple reasons for this, could be not yet synced to hcw, or it is not ended yet.
				continue;
			}
			
			final String patientUuid = getPatientUuid(a);
			final String providerUuid = getProviderUuid(a);
			final Date startDate = encounter.getPeriod().getStart();
			final Date endDate = encounter.getPeriod().getEnd();
			Map<String, Object> encData = createOpenMrsEncounter(encounter, uuid, encounterTypeUuid, patientUuid,
			    providerUuid, startDate, endDate, dataSource, openmrsClient);
			Type clinicalNotes = encounter.getExtensionByUrl(notesExtensionUrl).getValue();
			if (clinicalNotes != null) {
				final String notes = clinicalNotes.toString();
				if (StringUtils.isNotBlank(notes)) {
					createOpenMrsObs(uuid, patientUuid, encData, questionConceptUuid, notes, endDate, dataSource,
					    openmrsClient);
				}
			}
			
			markAppointmentAsCompleted(a);
		}
	}
	
	private Object getAppointmentId(Map<String, Object> appointmentData) {
		return appointmentData.get("patient_appointment_id");
	}
	
	private String getPatientUuid(Map<String, Object> appointmentData) throws Exception {
		Integer patientId = Utils.getPatientId(appointmentData);
		return (String) DbUtils.executeQuery(QUERY_PATIENT_UUID, dataSource, List.of(patientId)).get(0).get("uuid");
	}
	
	private String getProviderUuid(Map<String, Object> appointmentData) throws Exception {
		Object appointmentId = getAppointmentId(appointmentData);
		return (String) executeQuery(QUERY_PROV_UUID, dataSource, List.of(appointmentId)).get(0).get("uuid");
	}
	
	private void markAppointmentAsCompleted(Map<String, Object> appointmentData) throws Exception {
		try (Connection c = dataSource.getConnection(); PreparedStatement s = c.prepareStatement(SQL_UPDATE_APPT)) {
			s.setInt(1, (Integer) getAppointmentId(appointmentData));
			s.executeUpdate();
		}
	}
	
}
