/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.hcwathome.openmrs;

import static com.ozonehis.eip.hcwathome.openmrs.DbUtils.executeQuery;
import static org.hl7.fhir.r4.model.Appointment.ParticipationStatus.NEEDSACTION;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.hl7.fhir.r4.model.Appointment;
import org.hl7.fhir.r4.model.Appointment.AppointmentStatus;
import org.hl7.fhir.r4.model.ContactPoint;
import org.hl7.fhir.r4.model.Enumerations.AdministrativeGender;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.Reference;
import org.openmrs.eip.EIPException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Utils {
	
	private static final String REF_PATIENT = "#patient";
	
	private static final String REF_PRACTITIONER = "#practitioner";
	
	private static final String QUERY_PERSON = "SELECT gender FROM person WHERE person_id = ?";
	
	private static final String QUERY_NAME = "SELECT given_name,family_name FROM person WHERE person_id = ? AND "
	        + "voided = 0 ORDER BY preferred DESC LIMIT 1";
	
	private static final String QUERY_PERSON_ATTR_TYPE_ID = "SELECT person_attribute_type_id FROM "
	        + "PERSON_ATTRIBUTE_TYPE WHERE uuid = ?";
	
	private static final String QUERY_EMAIL = "SELECT value FROM person_attribute WHERE person_id = ? AND "
	        + "person_attribute_type_id = ? AND voided = 0";
	
	private static final String QUERY_PROV_PERSON_ID = "SELECT person_id FROM provider WHERE provider_id = ?";
	
	private static Integer emailPersonAttTypeId = null;
	
	/**
	 * Converts the specified gender value to the fhir {@link AdministrativeGender}
	 *
	 * @param gender the OpenMRS gender value to convert
	 * @return AdministrativeGender
	 */
	public static AdministrativeGender convertGender(Object gender) {
		String openmrsGender = "";
		if (gender != null) {
			openmrsGender = gender.toString().toUpperCase();
		}
		
		switch (openmrsGender) {
			case "M":
				return AdministrativeGender.MALE;
			case "F":
				return AdministrativeGender.FEMALE;
			case "O":
				return AdministrativeGender.OTHER;
			case "U":
				return AdministrativeGender.UNKNOWN;
			default:
				return AdministrativeGender.NULL;
		}
	}
	
	/**
	 * Converts the specified status value to the fhir {@link AppointmentStatus}
	 *
	 * @param status the OpenMRS status value to convert
	 * @return AppointmentStatus
	 */
	public static AppointmentStatus convertStatus(Object status) {
		String openmrsStatus = "";
		if (status != null) {
			openmrsStatus = status.toString();
		}
		
		switch (openmrsStatus) {
			case "Requested":
				return AppointmentStatus.PROPOSED;
			case "WaitList":
				return AppointmentStatus.WAITLIST;
			case "Scheduled":
				return AppointmentStatus.BOOKED;
			case "CheckedIn":
				return AppointmentStatus.CHECKEDIN;
			case "Completed":
				return AppointmentStatus.FULFILLED;
			case "Cancelled":
				return AppointmentStatus.CANCELLED;
			case "Missed":
				return AppointmentStatus.NOSHOW;
			default:
				return AppointmentStatus.NULL;
		}
	}
	
	public static Appointment buildFhirAppointment(String uuid, Map<String, Object> appointmentData,
	                                               String emailPersonAttTypeUuid, DataSource dataSource)
	    throws SQLException {
		//TODO Skip canceled or voided appointment
		Integer patientId = (Integer) appointmentData.get("patient_id");
		Integer provId = (Integer) appointmentData.get("provider_id");
		//TODO Only process appointment in Requested status
		AppointmentStatus status = Utils.convertStatus(appointmentData.get("status"));
		List<Map<String, Object>> patientData = executeQuery(QUERY_PERSON, dataSource, List.of(patientId));
		Appointment appointment = new Appointment();
		Identifier identifier = new Identifier();
		identifier.setValue(uuid);
		appointment.setIdentifier(List.of(identifier));
		appointment.setStatus(status);
		appointment.setStart((Date) appointmentData.get("start_date_time"));
		appointment.setEnd((Date) appointmentData.get("end_date_time"));
		Patient patient = new Patient();
		patient.setId(REF_PATIENT);
		AdministrativeGender gender = Utils.convertGender(patientData.get(0).get("gender"));
		patient.setGender(gender);
		List<Map<String, Object>> patientNameData = executeQuery(QUERY_NAME, dataSource, List.of(patientId));
		HumanName patientName = new HumanName();
		patientName.addGiven(patientNameData.get(0).get("given_name").toString());
		patientName.setFamily(patientNameData.get(0).get("family_name").toString());
		if (patientNameData.get(0).get("middle_name") != null) {
			patientName.setUse(HumanName.NameUse.USUAL).addGiven(patientNameData.get(0).get("middle_name").toString());
		}
		
		patient.addName(patientName);
		Integer emailAttTypeId = getEmailPersonAttTypeId(emailPersonAttTypeUuid, dataSource);
		List<Map<String, Object>> patientEmailData = executeQuery(QUERY_EMAIL, dataSource,
		    List.of(patientId, emailAttTypeId));
		if (patientEmailData.isEmpty()) {
			throw new EIPException("Patient email address not found");
		}
		
		final String patientEmail = patientEmailData.get(0).get("value").toString();
		patient.addTelecom().setSystem(ContactPoint.ContactPointSystem.EMAIL).setValue(patientEmail);
		Practitioner practitioner = new Practitioner();
		practitioner.setId(REF_PRACTITIONER);
		List<Map<String, Object>> provPersonIdRows = executeQuery(QUERY_PROV_PERSON_ID, dataSource, List.of(provId));
		List<Map<String, Object>> provEmailData = executeQuery(QUERY_EMAIL, dataSource,
		    List.of(provPersonIdRows.get(0).get("person_id"), emailAttTypeId));
		if (provEmailData.isEmpty()) {
			throw new EIPException("Provider email address not found");
		}
		
		final String provEmail = provEmailData.get(0).get("value").toString();
		practitioner.addTelecom().setSystem(ContactPoint.ContactPointSystem.EMAIL).setValue(provEmail)
		        .setUse(ContactPoint.ContactPointUse.WORK);
		appointment.addParticipant().setStatus(NEEDSACTION).setActor(new Reference(REF_PATIENT));
		appointment.addParticipant().setStatus(NEEDSACTION).setActor(new Reference(REF_PRACTITIONER));
		appointment.addContained(patient);
		appointment.addContained(practitioner);
		return appointment;
	}
	
	private static Integer getEmailPersonAttTypeId(String emailPersonAttTypeUuid, DataSource dataSource)
	    throws SQLException {
		if (emailPersonAttTypeId == null) {
			if (log.isDebugEnabled()) {
				log.debug("Loading the id for email person attribute type");
			}
			
			List<Map<String, Object>> row = executeQuery(QUERY_PERSON_ATTR_TYPE_ID, dataSource,
			    List.of(emailPersonAttTypeUuid));
			emailPersonAttTypeId = (Integer) row.get(0).get("person_attribute_type_id");
		}
		
		return emailPersonAttTypeId;
	}
	
}
