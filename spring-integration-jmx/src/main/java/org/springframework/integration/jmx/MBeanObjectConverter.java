/*
 * Copyright © 2013 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2013-present the original author or authors.
 */

package org.springframework.integration.jmx;

import javax.management.MBeanServerConnection;
import javax.management.ObjectInstance;

/**
 * @author Stuart Williams
 * @since 3.0
 *
 */
@FunctionalInterface
public interface MBeanObjectConverter {

	/**
	 * @param connection The connection.
	 * @param instance The instance.
	 * @return The mapped object instance.
	 */
	Object convert(MBeanServerConnection connection, ObjectInstance instance);

}
