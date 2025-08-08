/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.gateway;

import org.springframework.messaging.Message;

/**
 * @author Mark Fisher
 */
public class TestHandler {

	public String handle(Message<?> message) {
		return message.getPayload() + "!!!";
	}

}
