/*
 * Copyright 2013-present the original author or authors.
 */

package org.springframework.integration.jmx.inbound;

import java.util.Arrays;

import javax.management.ObjectName;

import org.springframework.util.Assert;

/**
 * @author Stuart Williams
 * @author Artem Bilan
 *
 * @since 7.0
 *
 */
public class NamedFieldsMBeanAttributeFilter implements MBeanAttributeFilter {

	private final String[] namedFields;

	/**
	 * @param namedFields The named fields that should pass the filter.
	 */
	public NamedFieldsMBeanAttributeFilter(String... namedFields) {
		Assert.notNull(namedFields, "'namedFields' must not be null");
		this.namedFields = Arrays.copyOf(namedFields, namedFields.length);
	}

	@Override
	public boolean accept(ObjectName objectName, String attributeName) {
		for (String namedField : this.namedFields) {
			if (namedField.equals(attributeName)) {
				return true;
			}
		}
		return false;
	}

}
