/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.hcwathome.openmrs;

import org.openmrs.eip.mysql.watcher.Event;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class AppointmentEventProcessor extends BaseEventProcessor {
	
	public AppointmentEventProcessor(AppointmentProcessor integrationProcessor) {
		super(integrationProcessor);
	}
	
	@Override
	protected String getAppointmentUuid(Event event) {
		return null;
	}
	
	@Override
	public Integer getAppointmentId(Event event) {
		final String tableName = event.getTableName();
		if (log.isDebugEnabled()) {
			log.debug("Extracting appointment id from {} table event", tableName);
		}
		
		return Integer.valueOf(event.getPrimaryKeyId().toString());
	}
	
	@Override
	protected Action getAction(Event event) {
		return null;
	}
	
}
