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
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.hl7.fhir.r4.model.Appointment;
import org.hl7.fhir.r4.model.Appointment.AppointmentStatus;
import org.hl7.fhir.r4.model.ContactPoint;
import org.hl7.fhir.r4.model.Enumerations.AdministrativeGender;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.HumanName.NameUse;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.Reference;
import org.openmrs.eip.EIPException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Utils {
	
	private static final String ID_PATIENT = "patient";
	
	private static final String REF_PATIENT = "#" + ID_PATIENT;
	
	private static final String ID_PRACTITIONER = "practitioner";
	
	private static final String REF_PRACTITIONER = "#" + ID_PRACTITIONER;
	
	private static final String QUERY_PERSON = "SELECT gender FROM person WHERE person_id = ?";
	
	private static final String QUERY_NAME = "SELECT given_name,family_name FROM person_name WHERE person_id = ? AND "
	        + "voided = 0 ORDER BY preferred DESC LIMIT 1";
	
	private static final String QUERY_PERSON_ATTR_TYPE_ID = "SELECT person_attribute_type_id FROM "
	        + "person_attribute_type WHERE uuid = ?";
	
	private static final String QUERY_EMAIL = "SELECT value FROM person_attribute WHERE person_id = ? AND "
	        + "person_attribute_type_id = ? AND voided = 0";
	
	private static final String QUERY_PROVIDER_ID = "SELECT provider_id FROM patient_appointment_provider WHERE "
	        + "patient_appointment_id = ?";
	
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
	
	/**
	 * Create {@link Appointment} instance from the specified OpenMRS appointment data.
	 * 
	 * @param uuid the uuid of the appointment
	 * @param appointmentData Map of column names and values of the OpenMRS appointment
	 * @param emailPersonAttTypeUuid the uuid of the email person attribute type uuid.
	 * @param dataSource OpenMRS {@link DataSource}
	 * @return Appointment
	 * @throws SQLException
	 */
	public static Appointment buildFhirAppointment(String uuid, Map<String, Object> appointmentData,
	                                               String emailPersonAttTypeUuid, DataSource dataSource)
	    throws SQLException {
		//TODO Skip canceled or voided appointment
		Integer patientId = (Integer) appointmentData.get("patient_id");
		//TODO Only process appointment in Requested status
		AppointmentStatus status = Utils.convertStatus(appointmentData.get("status"));
		List<Map<String, Object>> patientData = getPatient(patientId, dataSource);
		Appointment appointment = new Appointment();
		Identifier identifier = new Identifier();
		identifier.setValue(uuid);
		appointment.setIdentifier(List.of(identifier));
		appointment.setStatus(status);
		appointment.setStart(getStartDate(appointmentData));
		appointment.setEnd(getEndDate(appointmentData));
		Patient patient = new Patient();
		patient.setId(ID_PATIENT);
		patient.setGender(getGender(patientData));
		List<Map<String, Object>> patientNames = getPatientNames(patientId, dataSource);
		HumanName patientName = new HumanName();
		patientName.setUse(NameUse.USUAL).addGiven(getGivenName(patientNames)).addGiven(getMiddleName(patientNames))
		        .setFamily(getFamilyName(patientNames));
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
		practitioner.setId(ID_PRACTITIONER);
		Object appointmentId = appointmentData.get("patient_appointment_id");
		List<Map<String, Object>> idRows = executeQuery(QUERY_PROVIDER_ID, dataSource, List.of(appointmentId));
		Object provId = idRows.get(0).get("provider_id");
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
	
	/**
	 * Updates the specified {@link Appointment} from hcw@home with the most recent data from OpenMRS
	 * and returns true if any changes have been applied otherwise false.
	 * 
	 * @param hcwAppointment {@link Appointment} from hcw@home
	 * @param openmrsAppointment Map of column names and values of the OpenMRS appointment
	 * @param emailPersonAttTypeUuid the uuid of the email person attribute type uuid.
	 * @param dataSource OpenMRS {@link DataSource}
	 * @return true if the hcwAppointment has been modified otherwise false
	 * @throws SQLException
	 */
	public static boolean updateFhirAppointment(Appointment hcwAppointment, Map<String, Object> openmrsAppointment,
	                                            String emailPersonAttTypeUuid, DataSource dataSource)
	    throws SQLException {
		
		boolean isModified = false;
		//patient email, provider email, gender, name.
		//TODO if appointment kind has changed from Virtual, cancel it delete it from hcw@home.
		Date openmrsStart = getStartDate(openmrsAppointment);
		Date openmrsEnd = getEndDate(openmrsAppointment);
		if (!hcwAppointment.getStart().equals(openmrsStart)) {
			hcwAppointment.setStart(openmrsStart);
			isModified = true;
		}
		
		if (!hcwAppointment.getEnd().equals(openmrsEnd)) {
			hcwAppointment.setEnd(openmrsEnd);
			isModified = true;
		}
		
		return isModified;
	}
	
	protected static Date convertToDate(LocalDateTime localDateTime) {
		return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
	}
	
	private static Date getStartDate(Map<String, Object> appointmentData) {
		return convertToDate((LocalDateTime) appointmentData.get("start_date_time"));
	}
	
	private static Date getEndDate(Map<String, Object> appointmentData) {
		return convertToDate((LocalDateTime) appointmentData.get("end_date_time"));
	}
	
	private static List<Map<String, Object>> getPatient(Integer patientId, DataSource dataSource) throws SQLException {
		return executeQuery(QUERY_PERSON, dataSource, List.of(patientId));
	}
	
	private static AdministrativeGender getGender(List<Map<String, Object>> patientData) {
		return Utils.convertGender(patientData.get(0).get("gender"));
	}
	
	private static List<Map<String, Object>> getPatientNames(Integer patientId, DataSource dataSource) throws SQLException {
		return executeQuery(QUERY_NAME, dataSource, List.of(patientId));
	}
	
	private static String getGivenName(List<Map<String, Object>> names) {
		return names.get(0).get("given_name").toString();
	}
	
	private static String getFamilyName(List<Map<String, Object>> names) {
		return names.get(0).get("family_name").toString();
	}
	
	private static String getMiddleName(List<Map<String, Object>> names) {
		return (String) names.get(0).get("middle_name");
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
