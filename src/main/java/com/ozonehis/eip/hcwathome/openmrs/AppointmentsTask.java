/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.hcwathome.openmrs;

import static com.ozonehis.eip.hcwathome.openmrs.DbUtils.executeQuery;

import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Encounter.EncounterParticipantComponent;
import org.hl7.fhir.r4.model.Reference;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class AppointmentsTask {
	
	public static final String PROP_INITIAL_DELAY = "appointments.task.initial.delay";
	
	public static final String PROP_DELAY = "appointments.task.delay";
	
	private static final String QUERY = "SELECT patient_appointment_id,patient_id,uuid FROM patient_appointment "
	        + "WHERE appointment_kind = ? " + "AND status = ? AND end_date_time < ? AND voided = ?";
	
	private static final String QUERY_PATIENT_UUID = "SELECT uuid FROM person WHERE person_id = ?";
	
	private static final String QUERY_PROV_UUID = "SELECT uuid FROM provider WHERE provider_id = (" + Utils.QUERY_PROVIDER_ID
	        + ")";
	
	private HcwFhirClient hcwClient;
	
	private DataSource dataSource;
	
	private OpenmrsFhirClient openmrsClient;
	
	public AppointmentsTask(HcwFhirClient hcwClient, OpenmrsFhirClient openmrsClient, DataSource dataSource) {
		this.hcwClient = hcwClient;
		this.openmrsClient = openmrsClient;
		this.dataSource = dataSource;
	}
	
	@Scheduled(initialDelayString = "${" + PROP_INITIAL_DELAY + "}", fixedDelayString = "${" + PROP_DELAY + "}")
	protected void execute() throws Exception {
		List<Object> args = List.of("Virtual", "Requested", LocalDateTimeUtils.getCurrentTime(), 0);
		List<Map<String, Object>> results = DbUtils.executeQuery(QUERY, dataSource, args);
		//TODO Process the appointments in parallel
		for (Map<String, Object> a : results) {
			final String uuid = (String) a.get("uuid");
			Encounter encounter = hcwClient.getEncounterByAppointment(uuid);
			if (encounter == null) {
				if (log.isDebugEnabled()) {
					log.debug("No encounter found in hcw@home associated to appointment with uuid {}", uuid);
				}
				//Multiple reasons for this
				//Not yet synced to hcw
				//It is not ended yet
				continue;
			}
			
			encounter.setSubject(new Reference(getPatientUuid(a)));
			EncounterParticipantComponent participant = new EncounterParticipantComponent();
			participant.setIndividual(new Reference(getProviderUuid(a)));
			encounter.setParticipant(List.of(participant));
			openmrsClient.create(encounter);
		}
	}
	
	private String getPatientUuid(Map<String, Object> appointmentData) throws Exception {
		Integer patientId = Utils.getPatientId(appointmentData);
		return (String) DbUtils.executeQuery(QUERY_PATIENT_UUID, dataSource, List.of(patientId)).get(0).get("uuid");
	}
	
	private String getProviderUuid(Map<String, Object> appointmentData) throws Exception {
		Object appointmentId = appointmentData.get("patient_appointment_id");
		return (String) executeQuery(QUERY_PROV_UUID, dataSource, List.of(appointmentId)).get(0).get("uuid");
	}
	
}
