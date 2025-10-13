/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.hcwathome.openmrs;

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
		final Integer appointmentId;
		if (appointmentUuid == null) {
			appointmentId = getAppointmentId(event);
		} else {
			appointmentId = null;
		}
		
		Action action = getAction(event);
		appointmentProcessor.process(appointmentUuid, appointmentId, action);
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
