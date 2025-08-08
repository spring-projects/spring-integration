/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.config;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;

/**
 * @author Mark Fisher
 */
public class TestConsumer implements MessageHandler {

	private volatile Message<?> lastMessage;

	public Message<?> getLastMessage() {
		return this.lastMessage;
	}

	public void handleMessage(Message<?> message) {
		this.lastMessage = message;
	}

}
