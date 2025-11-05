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

import org.hl7.fhir.r4.model.Appointment;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class AppointmentProcessor {
	
	private static final String QUERY_APPOINTMENT = "SELECT patient_appointment_id,patient_id,status,start_date_time,"
	        + "end_date_time,voided FROM patient_appointment WHERE uuid = ?";
	
	private HcwFhirClient hcwClient;
	
	private DataSource dataSource;
	
	@Value("${openmrs.email.person.attr.type.uuid}")
	private String emailPersonAttTypeUuid;
	
	public AppointmentProcessor(HcwFhirClient hcwClient, DataSource dataSource) {
		this.hcwClient = hcwClient;
		this.dataSource = dataSource;
	}
	
	public void process(String uuid, Action action) throws Exception {
		Appointment a = hcwClient.getAppointmentByIdentifier(uuid);
		List<Map<String, Object>> data = executeQuery(QUERY_APPOINTMENT, dataSource, List.of(uuid));
		if (data.isEmpty()) {
			log.info("No appointment found matching uuid: {}", uuid);
		}
		
		if (action != Action.DELETE) {
			if (a == null) {
				create(uuid, data.get(0));
			} else {
				update(a, data.get(0));
			}
		} else {
			delete(a);
		}
	}
	
	protected void create(String uuid, Map<String, Object> appointmentData) throws Exception {
		Appointment appointment = Utils.buildFhirAppointment(uuid, appointmentData, emailPersonAttTypeUuid, dataSource);
		hcwClient.create(appointment);
	}
	
	protected void update(Appointment hcwAppointment, Map<String, Object> openmrsAppointment) throws Exception {
		boolean isModified = Utils.updateFhirAppointment(hcwAppointment, openmrsAppointment, emailPersonAttTypeUuid,
		    dataSource);
		if (!isModified) {
			if (log.isDebugEnabled()) {
				log.debug("No changes detected on the appointment");
			}
			
			return;
		}
		
		hcwClient.updateAppointment(hcwAppointment);
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
