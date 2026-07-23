/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.hcwathome.openmrs;

import static com.ozonehis.eip.hcwathome.openmrs.AppointmentsTask.ENC_TYPE_SYSTEM;

import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.StringType;
import org.openmrs.eip.EIPException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AppointmentTaskUtils {
	
	private static final String QUERY_PATIENT_ID = "SELECT person_id FROM person WHERE uuid = ?";
	
	protected static final String QUERY_ENCOUNTER_ID = "SELECT encounter_id FROM encounter WHERE patient_id = ("
	        + QUERY_PATIENT_ID + ") AND  encounter_datetime = ? AND voided != 1";
	
	protected static final String QUERY_OBS = "SELECT obs_id FROM obs WHERE person_id = (" + QUERY_PATIENT_ID
	        + ") AND encounter_id = ? AND obs_datetime = ? AND voided != 1";
	
	/**
	 * Adds an Encounter to OpenMRS associated with a given appointment and patient if it does not exist
	 *
	 * @param encounter The FHIR {@link Encounter} object.
	 * @param appointmentUuid The UUID of the appointment
	 * @param encounterTypeUuid The UUID of the encounter type.
	 * @param patientUuid The UUID of the patient associated with the encounter.
	 * @param providerUuid The UUID of the provider participating in the encounter.
	 * @param startDate The start date and time of the encounter.
	 * @param endDate The end date and time of the encounter.
	 * @param ds The {@link DataSource} used to query and interact with the database.
	 * @param openmrsClient The {@link OpenmrsFhirClient} instance
	 * @return The database id of the created or existing encounter.
	 * @throws Exception
	 */
	public static int createOpenMrsEncounter(Encounter encounter, String appointmentUuid, String encounterTypeUuid,
	                                         String patientUuid, String providerUuid, Date startDate, Date endDate,
	                                         DataSource ds, OpenmrsFhirClient openmrsClient)
	    throws Exception {
		
		List<Map<String, Object>> encIds = DbUtils.executeQuery(QUERY_ENCOUNTER_ID, ds, List.of(patientUuid, startDate));
		if (encIds.size() == 0) {
			if (log.isDebugEnabled()) {
				log.debug("Adding encounter associated to appointment with uuid {}", appointmentUuid);
			}
			
			encounter.setType(List.of(new CodeableConcept(new Coding(ENC_TYPE_SYSTEM, encounterTypeUuid, null))));
			encounter.setSubject(new Reference("Patient/" + patientUuid));
			Encounter.EncounterParticipantComponent participant = new Encounter.EncounterParticipantComponent();
			participant.setIndividual(new Reference("Practitioner/" + providerUuid));
			encounter.setParticipant(List.of(participant));
			Period period = new Period();
			period.setStart(startDate);
			period.setEnd(endDate);
			encounter.setPeriod(period);
			openmrsClient.create(encounter);
			if (log.isDebugEnabled()) {
				log.debug("Read id of added encounter associated to appointment with uuid {}", appointmentUuid);
			}
			
			encIds = DbUtils.executeQuery(QUERY_ENCOUNTER_ID, ds, List.of(patientUuid, startDate));
		} else {
			final int size = encIds.size();
			if (size != 1) {
				throw new EIPException("Found " + size + " associated to appointment with uuid " + appointmentUuid);
			}
			
			if (log.isDebugEnabled()) {
				log.debug("There is already an encounter associated to appointment with uuid {}", appointmentUuid);
			}
		}
		
		return (int) encIds.get(0).get("encounter_id");
	}
	
	/**
	 * Adds an Observation to OpenMRS associated with a given appointment and patient if it does not
	 * exist
	 *
	 * @param appointmentUuid the UUID of the appointment
	 * @param patientUuid the UUID the patient
	 * @param encId the id of the encounter associated with the observation
	 * @param qnConceptUuid the UUID of the question concept
	 * @param value the observation value
	 * @param obsDate the date of the observation
	 * @param ds the data source used to query existing observations
	 * @param openmrsClient the {@link OpenmrsFhirClient} instance
	 * @throws Exception
	 */
	public static void createOpenMrsObs(String appointmentUuid, String patientUuid, int encId, String qnConceptUuid,
	                                    String value, Date obsDate, DataSource ds, OpenmrsFhirClient openmrsClient)
	    throws Exception {
		
		List<?> obsIds = DbUtils.executeQuery(QUERY_OBS, ds, List.of(patientUuid, encId, obsDate));
		if (obsIds.size() > 0) {
			if (log.isDebugEnabled()) {
				log.debug("Clinical notes obs already exists associated to appointment with uuid {}", appointmentUuid);
			}
			
			return;
		}
		
		if (log.isDebugEnabled()) {
			log.debug("Adding obs for clinical notes associated to appointment with uuid {}", appointmentUuid);
		}
		
		Observation obs = new Observation();
		obs.setSubject(new Reference("Patient/" + patientUuid));
		obs.setCode(new CodeableConcept(new Coding(null, qnConceptUuid, null)));
		obs.setValue(new StringType(value));
		obs.setEffective(new DateTimeType(obsDate));
		openmrsClient.create(obs);
	}
	
}
