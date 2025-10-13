/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.hcwathome.openmrs;

import org.openmrs.eip.mysql.watcher.Event;

/**
 * Processes an {@link Event} and then forwards to the {@link AppointmentProcessor}
 */
public interface EventProcessor {
	
	/**
	 * Processes the specified database event
	 *
	 * @param event Event object
	 * @throws Exception
	 */
	void process(Event event) throws Exception;
	
}
