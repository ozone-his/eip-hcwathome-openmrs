/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.hcwathome.openmrs;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class AppointmentProcessor {
	
	private HcwFhirClient hcwClient;
	
	public AppointmentProcessor(HcwFhirClient hcwClient) {
		this.hcwClient = hcwClient;
	}
	
	public void process(String uuid, Integer id, Action action) throws Exception {
		
	}
	
}
