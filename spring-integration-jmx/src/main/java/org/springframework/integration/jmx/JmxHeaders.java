/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.jmx;

/**
 * Constants for JMX related Message Header keys.
 *
 * @author Mark Fisher
 * @since 2.0
 */
public abstract class JmxHeaders {

	public static final String PREFIX = "jmx_";

	public static final String OBJECT_NAME = PREFIX + "objectName";

	public static final String OPERATION_NAME = PREFIX + "operationName";

	public static final String NOTIFICATION_TYPE = PREFIX + "notificationType";

	public static final String NOTIFICATION_HANDBACK = PREFIX + "notificationHandback";

}
