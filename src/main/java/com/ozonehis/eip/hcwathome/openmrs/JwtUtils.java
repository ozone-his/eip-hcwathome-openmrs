/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.hcwathome.openmrs;

import java.util.Base64;
import java.util.Map;

public class JwtUtils {
	
	/**
	 * Parses a JWT token
	 *
	 * @param jwt the JWT token to decode
	 * @return Map object
	 * @throws Exception
	 */
	public static Map<String, Object> parseToken(String jwt) throws Exception {
		final String base64 = jwt.substring(jwt.indexOf(".") + 1, jwt.lastIndexOf("."));
		return HcwOpenmrsUtils.getMapper().readValue(Base64.getDecoder().decode(base64), Map.class);
	}
	
}
