/*
 * Copyright 2013-present the original author or authors.
 */

package org.springframework.integration.jmx.inbound;

import javax.management.ObjectName;

/**
 * The strategy to filter out MBean attributes before retrieval of their values.
 *
 * @author Stuart Williams
 * @author Artem Bilan
 *
 * @since 7.0
 */
@FunctionalInterface
public interface MBeanAttributeFilter {

	/**
	 * @param objectName The object name.
	 * @param attributeName The attribute name.
	 * @return true if the attribute passes the filter.
	 */
	boolean accept(ObjectName objectName, String attributeName);

}
