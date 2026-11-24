/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
