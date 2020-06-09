/*
 * Copyright 2013-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.jmx;

import java.util.Arrays;

import javax.management.ObjectName;

import org.springframework.util.Assert;

/**
 * @author Stuart Williams
 * @author Artem Bilan
 *
 * @since 3.0
 *
 */
public class NotNamedFieldsMBeanAttributeFilter implements MBeanAttributeFilter {

	private final String[] namedFields;

	/**
	 * @param namedFields The named fields that should be filtered.
	 */
	public NotNamedFieldsMBeanAttributeFilter(String... namedFields) {
		Assert.notNull(namedFields, "'namedFields' must not be null");
		this.namedFields = Arrays.copyOf(namedFields, namedFields.length);
	}

	@Override
	public boolean accept(ObjectName objectName, String attributeName) {
		for (String namedField : this.namedFields) {
			if (namedField.equals(attributeName)) {
				return false;
			}
		}
		return true;
	}

}
