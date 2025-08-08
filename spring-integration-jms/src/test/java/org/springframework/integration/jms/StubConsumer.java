/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.jms;

import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageConsumer;
import jakarta.jms.MessageListener;

/**
 * @author Mark Fisher
 */
public class StubConsumer implements MessageConsumer {

	private String messageText;

	private String messageSelector;

	public StubConsumer(String messageText, String messageSelector) {
		this.messageText = messageText;
		this.messageSelector = messageSelector;
	}

	public void close() throws JMSException {
	}

	public MessageListener getMessageListener() throws JMSException {
		return null;
	}

	public String getMessageSelector() throws JMSException {
		return this.messageSelector;
	}

	public Message receive() throws JMSException {
		StubTextMessage message = new StubTextMessage();
		String text = this.messageText;
		if (this.messageSelector != null) {
			text += " [with selector: " + this.messageSelector + "]";
		}
		message.setText(text);
		return message;
	}

	public Message receive(long timeout) throws JMSException {
		return this.receive();
	}

	public Message receiveNoWait() throws JMSException {
		return this.receive();
	}

	public void setMessageListener(MessageListener listener) throws JMSException {
	}

}
