/*
 * Copyright 2013-present the original author or authors.
 */

package org.springframework.integration.jmx;

/**
 * @author Stuart Williams
 * @author Artem Bilan
 *
 * @since 3.0
 *
 * @deprecated since 7.0 in favor {@link org.springframework.integration.jmx.inbound.NotNamedFieldsMBeanAttributeFilter}
 */
@Deprecated(forRemoval = true, since = "7.0")
public class NotNamedFieldsMBeanAttributeFilter
		extends org.springframework.integration.jmx.inbound.NotNamedFieldsMBeanAttributeFilter {

	/**
	 * @param namedFields The named fields that should be filtered.
	 */
	public NotNamedFieldsMBeanAttributeFilter(String... namedFields) {
		super(namedFields);
	}

}
