/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.hcwathome.openmrs;

import java.lang.reflect.Field;

import org.apache.commons.lang3.reflect.FieldUtils;

public class TestUtils {
	
	public static void setFieldValue(Object target, String fieldName, Object value) {
		Class<?> clazz;
		if (target instanceof Class) {
			clazz = (Class<?>) target;
		} else {
			clazz = target.getClass();
		}
		
		Field field = FieldUtils.getField(clazz, fieldName, true);
		try {
			FieldUtils.writeField(field, target, value, true);
		}
		catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}
	
}
