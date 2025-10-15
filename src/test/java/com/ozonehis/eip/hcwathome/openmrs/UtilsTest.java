/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.hcwathome.openmrs;

import org.hl7.fhir.r4.model.Appointment.AppointmentStatus;
import org.hl7.fhir.r4.model.Enumerations.AdministrativeGender;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class UtilsTest {
	
	@Test
	public void convertGender_shouldConvertTheGenderStringToFhirEquivalent() throws Exception {
		Assertions.assertEquals(AdministrativeGender.MALE, Utils.convertGender("m"));
		Assertions.assertEquals(AdministrativeGender.FEMALE, Utils.convertGender("f"));
		Assertions.assertEquals(AdministrativeGender.OTHER, Utils.convertGender("o"));
		Assertions.assertEquals(AdministrativeGender.UNKNOWN, Utils.convertGender("u"));
		Assertions.assertEquals(AdministrativeGender.NULL, Utils.convertGender(null));
		Assertions.assertEquals(AdministrativeGender.NULL, Utils.convertGender("b"));
	}
	
	@Test
	public void convertStatus_shouldConvertTheStatusStringToFhirEquivalent() throws Exception {
		Assertions.assertEquals(AppointmentStatus.PROPOSED, Utils.convertStatus("Requested"));
		Assertions.assertEquals(AppointmentStatus.WAITLIST, Utils.convertStatus("WaitList"));
		Assertions.assertEquals(AppointmentStatus.BOOKED, Utils.convertStatus("Scheduled"));
		Assertions.assertEquals(AppointmentStatus.CHECKEDIN, Utils.convertStatus("CheckedIn"));
		Assertions.assertEquals(AppointmentStatus.FULFILLED, Utils.convertStatus("Completed"));
		Assertions.assertEquals(AppointmentStatus.CANCELLED, Utils.convertStatus("Cancelled"));
		Assertions.assertEquals(AppointmentStatus.NOSHOW, Utils.convertStatus("Missed"));
		Assertions.assertEquals(AppointmentStatus.NULL, Utils.convertStatus(null));
		Assertions.assertEquals(AppointmentStatus.NULL, Utils.convertStatus("bad"));
	}
	
}
