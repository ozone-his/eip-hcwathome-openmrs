/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.hcwathome.openmrs;

import org.apache.commons.lang3.StringUtils;
import org.openmrs.eip.EIPException;
import org.openmrs.eip.mysql.watcher.Event;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class BaseEventProcessor implements EventProcessor {
	
	private AppointmentProcessor appointmentProcessor;
	
	public BaseEventProcessor(AppointmentProcessor appointmentProcessor) {
		this.appointmentProcessor = appointmentProcessor;
	}
	
	/**
	 * Processes the specified database event
	 *
	 * @param event Event object
	 * @throws Exception
	 */
	@Override
	public void process(Event event) throws Exception {
		final String appointmentUuid = getAppointmentUuid(event);
		if (StringUtils.isBlank(appointmentUuid)) {
			//This is an association e.g. patient
			Integer appointmentId = getAppointmentId(event);
			if (appointmentId != null) {
				//TODO Look up the appointment uuid from DB using the appointment id.
			}
		}
		
		if (StringUtils.isBlank(appointmentUuid)) {
			throw new EIPException("Cannot resolve appointment uuid for event: " + event);
		}
		
		Action action = getAction(event);
		if (action == null) {
			throw new EIPException("Don't know how to process DB event with operation" + event.getOperation());
		}
		
		appointmentProcessor.process(appointmentUuid, action);
	}
	
	/**
	 * Retrieves the appointment uuid from the specified database event.
	 *
	 * @param event Event object
	 * @return the appointment uuid
	 */
	protected abstract String getAppointmentUuid(Event event);
	
	/**
	 * Retrieves the appointment id from the specified database event.
	 *
	 * @param event Event object
	 * @return the appointment id
	 */
	protected abstract Integer getAppointmentId(Event event);
	
	/**
	 * Resolves the action to be taken based on the specified database event.
	 *
	 * @param event Event object
	 * @return Action
	 */
	protected abstract Action getAction(Event event);
	
}
