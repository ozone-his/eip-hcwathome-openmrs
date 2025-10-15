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

public class Utils {
	
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
	
}
