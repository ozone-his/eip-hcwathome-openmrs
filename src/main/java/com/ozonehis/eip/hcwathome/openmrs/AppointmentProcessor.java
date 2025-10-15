/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.hcwathome.openmrs;

import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.hl7.fhir.r4.model.Appointment;
import org.hl7.fhir.r4.model.Appointment.AppointmentStatus;
import org.hl7.fhir.r4.model.Enumerations.AdministrativeGender;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Patient;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class AppointmentProcessor {
	
	private static final String REF_PATIENT = "#patient";
	
	private static final String QUERY_APPOINTMENT = "SELECT patient_id,status,start_date_time,end_date_time,"
	        + "voided FROM patient_appointment WHERE uuid = ?";
	
	private static final String QUERY_PERSON = "SELECT gender FROM person WHERE person_id = ?";
	
	private static final String QUERY_NAME = "SELECT given_name,family_name FROM person WHERE person_id = ? AND "
	        + "voided = 0 ORDER BY preferred DESC LIMIT 1";
	
	private static final String QUERY_PROVIDER_ID = "SELECT provider_id FROM patient_appointment_provider WHERE "
	        + "patient_appointment_id = ?";
	
	private static final String QUERY_PROVIDER_NAME = "SELECT given_name,family_name FROM person WHERE person_id = "
	        + "(SELECT person_id FROM provider WHERE provider_id = (" + QUERY_PROVIDER_ID + ")) AND  voided = 0 "
	        + "ORDER BY preferred DESC " + "LIMIT 1";
	
	private HcwFhirClient hcwClient;
	
	private DataSource dataSource;
	
	public AppointmentProcessor(HcwFhirClient hcwClient, DataSource dataSource) {
		this.hcwClient = hcwClient;
		this.dataSource = dataSource;
	}
	
	public void process(String uuid, Action action) throws Exception {
		Appointment a = hcwClient.getAppointmentByIdentifier(uuid);
		List<Map<String, Object>> data = DbUtils.executeQuery(QUERY_APPOINTMENT, dataSource, List.of(uuid));
		if (data.isEmpty()) {
			log.info("No appointment found matching uuid: {}", uuid);
		}
		
		if (action != Action.DELETE) {
			if (a == null) {
				create(uuid, data.get(0));
			} else {
				
			}
		} else {
			delete(a);
		}
	}
	
	private void create(String uuid, Map<String, Object> data) throws Exception {
		//TODO Skip canceled or voided appointment
		Integer patientId = (Integer) data.get("patient_id");
		Integer provId = (Integer) data.get("provider_id");
		//TODO Only process appointment in Requested status
		AppointmentStatus status = Utils.convertStatus(data.get("status"));
		List<Map<String, Object>> patientData = DbUtils.executeQuery(QUERY_PERSON, dataSource, List.of(patientId));
		List<Map<String, Object>> patientNameData = DbUtils.executeQuery(QUERY_NAME, dataSource, List.of(patientId));
		List<Map<String, Object>> provNameData = DbUtils.executeQuery(QUERY_PROVIDER_NAME, dataSource, List.of(provId));
		Appointment appointment = new Appointment();
		Identifier identifier = new Identifier();
		identifier.setValue(uuid);
		appointment.setIdentifier(List.of(identifier));
		appointment.setStatus(status);
		appointment.setStart((Date) data.get("start_date_time"));
		appointment.setEnd((Date) data.get("end_date_time"));
		Patient patient = new Patient();
		patient.setId(REF_PATIENT);
		AdministrativeGender gender = Utils.convertGender(patientData.get(0).get("gender"));
		patient.setGender(gender);
		HumanName patientName = new HumanName();
		patientName.addGiven(patientNameData.get(0).get("given_name").toString());
		patientName.setFamily(patientNameData.get(0).get("family_name").toString());
		if (patientNameData.get(0).get("middle_name") != null) {
			patientName.addGiven(patientNameData.get(0).get("middle_name").toString());
		}
		
		patient.addName(patientName);
	}
	
	private void delete(Appointment appointment) {
		if (appointment == null) {
			if (log.isDebugEnabled()) {
				log.debug("Skip delete because appointment does not exist in hcw@home");
			}
			
			return;
		}
		
		hcwClient.deleteAppointment(appointment);
	}
	
}
