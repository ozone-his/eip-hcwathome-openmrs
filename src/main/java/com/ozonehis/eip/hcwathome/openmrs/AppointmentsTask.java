/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.hcwathome.openmrs;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.hl7.fhir.r4.model.Appointment;
import org.hl7.fhir.r4.model.Appointment.AppointmentStatus;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class AppointmentsTask {
	
	private static final String QUERY = "SELECT uuid FROM patient_appointment,end_date_time WHERE appointment_kind = ? "
	        + "AND status = ? AND voided = ?";
	
	private static final List<Object> QUERY_ARGS = List.of("Virtual", "Requested", 0);
	
	private HcwFhirClient hcwClient;
	
	private DataSource dataSource;
	
	public AppointmentsTask(HcwFhirClient hcwClient, DataSource dataSource) {
		this.hcwClient = hcwClient;
		this.dataSource = dataSource;
	}
	
	protected void execute() throws Exception {
		List<Map<String, Object>> results = DbUtils.executeQuery(QUERY, dataSource, QUERY_ARGS);
		//TODO Process the appointments in parallel
		for (Map<String, Object> a : results) {
			final String uuid = (String) a.get("uuid");
			final LocalDateTime endDate = (LocalDateTime) a.get("end_date_time");
			if (endDate.isAfter(LocalDateTimeUtils.getCurrentTime())) {
				if (log.isTraceEnabled()) {
					log.debug("Skipping future or unfinished appointment with uuid {}", uuid);
				}
				
				continue;
			}
			
			Appointment appointment = hcwClient.getAppointmentByIdentifier(uuid);
			if (appointment == null) {
				if (log.isDebugEnabled()) {
					log.debug("Appointment with uuid {} not found in hcw@home", uuid);
				}
				//Multiple reasons for this
				//Not yet synced to hcw
				//It is not ended yet
				continue;
			}
			
			if (appointment.getStatus() == AppointmentStatus.FULFILLED) {
				//TODO Fetch the consultation note
			}
		}
	}
	
}
