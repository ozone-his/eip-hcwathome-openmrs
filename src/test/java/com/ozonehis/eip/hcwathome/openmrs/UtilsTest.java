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

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.hl7.fhir.r4.model.Appointment;
import org.hl7.fhir.r4.model.Appointment.AppointmentStatus;
import org.hl7.fhir.r4.model.Enumerations.AdministrativeGender;
import org.junit.jupiter.api.Test;

public class UtilsTest {
	
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
		Map<String, Object> openmrsData = new HashMap<>();
		LocalDateTime start = LocalDateTime.of(2025, 10, 21, 14, 00, 00);
		LocalDateTime end = LocalDateTime.of(2025, 10, 21, 14, 30, 00);
		openmrsData.put("start_date_time", start);
		openmrsData.put("end_date_time", end);
		Appointment hcwAppointment = new Appointment();
		hcwAppointment.setStart(Utils.convertToDate(LocalDateTime.of(2025, 10, 21, 12, 00, 00)));
		hcwAppointment.setEnd(Utils.convertToDate(LocalDateTime.of(2025, 10, 21, 12, 30, 00)));
		
		assertTrue(Utils.updateFhirAppointment(hcwAppointment, openmrsData, null, null));
		
		assertEquals(Utils.convertToDate(start), hcwAppointment.getStart());
		assertEquals(Utils.convertToDate(end), hcwAppointment.getEnd());
	}
	
}
