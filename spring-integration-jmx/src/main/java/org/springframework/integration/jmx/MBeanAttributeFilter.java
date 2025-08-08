/*
 * Copyright © 2013 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2013-present the original author or authors.
 */

package org.springframework.integration.jmx;

import javax.management.ObjectName;

/**
 * @author Stuart Williams
 * @since 3.0
 *
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
