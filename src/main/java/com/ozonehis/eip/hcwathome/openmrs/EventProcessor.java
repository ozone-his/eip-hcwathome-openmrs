/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.hcwathome.openmrs;

import java.util.Map;

import org.openmrs.eip.mysql.watcher.Event;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Processes an {@link Event} and forwards to the {@link IntegrationProcessor}
 */
@Slf4j
@Component
public class EventProcessor {
	
	/**
	 * Processes the specified database event
	 * 
	 * @param event Event object
	 * @throws Exception
	 */
	public void process(Event event) throws Exception {
		log.info("Getting the id of the patient associated to the database event");
		
		Integer patientId = getPatientId(event);
		
		if (log.isDebugEnabled()) {
			log.debug("Patient id: {}", patientId);
		}
		
		//TODO Proceed with the integration accordingly.
	}
	
	/**
	 * Retrieves the patient id from the specified database event.
	 * 
	 * @param event Event object
	 * @return the patient id
	 */
	public Integer getPatientId(Event event) {
		switch (event.getTableName()) {
			case "patient_appointment":
				return getPatientIdForAssociationEvent(event);
		}
		
		return null;
	}
	
	private Integer getPatientIdForAssociationEvent(Event event) {
		final String tableName = event.getTableName();
		if (log.isDebugEnabled()) {
			log.debug("Extracting associated patient id from {} table event", tableName);
		}
		
		Object patientId = null;
		Map<String, Object> state = null;
		if ("d".equals(event.getOperation())) {
			state = event.getPreviousState();
		} else {
			state = event.getCurrentState();
		}
		
		if (state != null) {
			patientId = state.get("patient_id");
		}
		
		if (patientId == null) {
			//TODO Query the OpenMRS database as the last resort
		}
		
		return Integer.valueOf(patientId.toString());
	}
	
}
