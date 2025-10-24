/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.hcwathome.openmrs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.hl7.fhir.r4.model.Appointment;
import org.hl7.fhir.r4.model.Appointment.AppointmentStatus;
import org.hl7.fhir.r4.model.ContactPoint.ContactPointSystem;
import org.hl7.fhir.r4.model.Enumerations.AdministrativeGender;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Practitioner;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class UtilsTest {
	
	private static MockedStatic<DbUtils> mockedDbUtils;
	
	@Mock
	private DataSource mockDataSource;
	
	@BeforeEach
	public void setUp() {
		mockedDbUtils = Mockito.mockStatic(DbUtils.class);
	}
	
	@AfterEach
	public void tearDown() {
		mockedDbUtils.close();
		TestUtils.setFieldValue(Utils.class, "emailPersonAttrTypeId", null);
	}
	
	@Test
	public void convertGender_shouldConvertTheGenderStringToFhirEquivalent() {
		assertEquals(AdministrativeGender.MALE, Utils.convertGender("m"));
		assertEquals(AdministrativeGender.FEMALE, Utils.convertGender("f"));
		assertEquals(AdministrativeGender.OTHER, Utils.convertGender("o"));
		assertEquals(AdministrativeGender.UNKNOWN, Utils.convertGender("u"));
		assertEquals(AdministrativeGender.NULL, Utils.convertGender(null));
		assertEquals(AdministrativeGender.NULL, Utils.convertGender("b"));
	}
	
	@Test
	public void convertStatus_shouldConvertTheStatusStringToFhirEquivalent() {
		assertEquals(AppointmentStatus.PROPOSED, Utils.convertStatus("Requested"));
		assertEquals(AppointmentStatus.WAITLIST, Utils.convertStatus("WaitList"));
		assertEquals(AppointmentStatus.BOOKED, Utils.convertStatus("Scheduled"));
		assertEquals(AppointmentStatus.CHECKEDIN, Utils.convertStatus("CheckedIn"));
		assertEquals(AppointmentStatus.FULFILLED, Utils.convertStatus("Completed"));
		assertEquals(AppointmentStatus.CANCELLED, Utils.convertStatus("Cancelled"));
		assertEquals(AppointmentStatus.NOSHOW, Utils.convertStatus("Missed"));
		assertEquals(AppointmentStatus.NULL, Utils.convertStatus(null));
		assertEquals(AppointmentStatus.NULL, Utils.convertStatus("bad"));
	}
	
	@Test
	public void updateFhirAppointment_shouldUpdateTheHcwAppointmentWithOpenmrsData() throws Exception {
		final Integer appointmentId = 2;
		final Integer patientId = 5;
		final Integer providerPersonId = 8;
		final Integer emailPersonAttrTypeId = 10;
		final String newPatientEmail = "email@new.new";
		final String newProviderEmail = "provider@new.new";
		final String newGivenName = "John";
		final String newFamilyName = "Doe";
		Map<String, Object> openmrsAppointment = new HashMap<>();
		LocalDateTime start = LocalDateTime.of(2025, 10, 21, 14, 00, 00);
		LocalDateTime end = LocalDateTime.of(2025, 10, 21, 14, 30, 00);
		openmrsAppointment.put("status", "Cancelled");
		openmrsAppointment.put("patient_appointment_id", appointmentId);
		openmrsAppointment.put("patient_id", patientId);
		openmrsAppointment.put("start_date_time", start);
		openmrsAppointment.put("end_date_time", end);
		Appointment hcwAppointment = new Appointment();
		hcwAppointment.setStatus(AppointmentStatus.PROPOSED);
		Patient hcwPatient = new Patient();
		hcwPatient.setGender(AdministrativeGender.MALE);
		hcwPatient.addTelecom().setSystem(ContactPointSystem.EMAIL).setValue("email@old.old");
		Practitioner hcwPractitioner = new Practitioner();
		hcwPractitioner.addTelecom().setSystem(ContactPointSystem.EMAIL).setValue("provider@old.old");
		hcwAppointment.addContained(hcwPatient);
		hcwAppointment.addContained(hcwPractitioner);
		hcwAppointment.setStart(Utils.convertToDate(LocalDateTime.of(2025, 10, 21, 12, 00, 00)));
		hcwAppointment.setEnd(Utils.convertToDate(LocalDateTime.of(2025, 10, 21, 12, 30, 00)));
		hcwPatient.addName().setUse(HumanName.NameUse.USUAL).addGiven("Horatio").setFamily("Hornblower");
		when(DbUtils.executeQuery(Utils.QUERY_PERSON, mockDataSource, List.of(patientId)))
		        .thenReturn(List.of(Map.of("gender", "F")));
		TestUtils.setFieldValue(Utils.class, "emailPersonAttrTypeId", emailPersonAttrTypeId);
		when(DbUtils.executeQuery(Utils.QUERY_EMAIL, mockDataSource, List.of(patientId, emailPersonAttrTypeId)))
		        .thenReturn(List.of(Map.of("value", newPatientEmail)));
		when(DbUtils.executeQuery(Utils.QUERY_NAME, mockDataSource, List.of(patientId)))
		        .thenReturn(List.of(Map.of("given_name", newGivenName, "family_name", newFamilyName)));
		when(DbUtils.executeQuery(Utils.QUERY_PROV_PERSON_ID, mockDataSource, List.of(appointmentId)))
		        .thenReturn(List.of(Map.of("person_id", providerPersonId)));
		when(DbUtils.executeQuery(Utils.QUERY_EMAIL, mockDataSource, List.of(providerPersonId, emailPersonAttrTypeId)))
		        .thenReturn(List.of(Map.of("value", newProviderEmail)));
		
		assertTrue(Utils.updateFhirAppointment(hcwAppointment, openmrsAppointment, null, mockDataSource));
		
		assertEquals(AppointmentStatus.CANCELLED, hcwAppointment.getStatus());
		assertEquals(Utils.convertToDate(start), hcwAppointment.getStart());
		assertEquals(Utils.convertToDate(end), hcwAppointment.getEnd());
		assertEquals(AdministrativeGender.FEMALE, hcwPatient.getGender());
		assertEquals(1, hcwPatient.getTelecom().size());
		assertEquals(newPatientEmail, hcwPatient.getTelecom().get(0).getValue());
		assertEquals(newGivenName, hcwPatient.getName().get(0).getGivenAsSingleString());
		assertEquals(newFamilyName, hcwPatient.getName().get(0).getFamily());
		assertEquals(newProviderEmail, hcwPractitioner.getTelecom().get(0).getValue());
	}
	
}
