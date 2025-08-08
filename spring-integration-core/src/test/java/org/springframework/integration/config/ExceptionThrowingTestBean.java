/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.config;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandlingException;

/**
 * @author Mark Fisher
 */
public class ExceptionThrowingTestBean {

	public void handle(Message<?> message) {
		throw new MessageHandlingException(message, "intentional test failure");
	}

}
