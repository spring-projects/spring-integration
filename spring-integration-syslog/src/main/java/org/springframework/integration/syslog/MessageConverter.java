/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.syslog;

import org.springframework.messaging.Message;

/**
 * A converter to convert the raw message created by the underlying
 * UDP/TCP endpoint to a specific form of Syslog message.
 * @author Gary Russell
 * @since 3.0
 *
 */
@FunctionalInterface
public interface MessageConverter {

	Message<?> fromSyslog(Message<?> syslog);

}
