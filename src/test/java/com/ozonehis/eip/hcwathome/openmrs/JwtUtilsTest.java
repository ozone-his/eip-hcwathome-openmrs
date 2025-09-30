/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.hcwathome.openmrs;

import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class JwtUtilsTest {
	
	@Test
	public void parseAndVerifyToken() throws Exception {
		final String token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpZCI6IjY4MmMzZGMyZTRhN2NlZGVmZWQ4NjFlMCI"
		        + "sImlhdCI6MTc1OTE3MjMxNCwiZXhwIjoxNzU5MTczMjE0fQ.Wh03-cN3VLlLbqmKRARECm6d_9DnZz6Rwrd_Lf_PzH4";
		
		Map<String, Object> body = JwtUtils.parseToken(token);
		
		Assertions.assertEquals(1759172314, body.get("iat"));
		Assertions.assertEquals(1759173214, body.get("exp"));
	}
	
}
