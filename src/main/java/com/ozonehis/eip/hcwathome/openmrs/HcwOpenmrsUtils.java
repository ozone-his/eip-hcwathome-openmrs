/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.hcwathome.openmrs;

import com.fasterxml.jackson.databind.ObjectMapper;

public class HcwOpenmrsUtils {
	
	private static final ObjectMapper MAPPER = new ObjectMapper();
	
	public static ObjectMapper getMapper() {
		return MAPPER;
	}
	
}
