/*
 * Copyright © 2017 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2017-present the original author or authors.
 */

package org.springframework.integration.amqp.support;

import org.springframework.amqp.core.Message;
import org.springframework.messaging.MessagingException;

/**
 * A MessagingException for a returned message.
 *
 * @author Gary Russell
 * @since 4.3.12
 *
 */
public class ReturnedAmqpMessageException extends MessagingException {

	private static final long serialVersionUID = 1L;

	private final Message amqpMessage;

	private final int replyCode;

	private final String replyText;

	private final String exchange;

	private final String routingKey;

	public ReturnedAmqpMessageException(org.springframework.messaging.Message<?> message, Message amqpMessage,
			int replyCode, String replyText, String exchange, String routingKey) {
		super(message);
		this.amqpMessage = amqpMessage;
		this.replyCode = replyCode;
		this.replyText = replyText;
		this.exchange = exchange;
		this.routingKey = routingKey;
	}

	public Message getAmqpMessage() {
		return this.amqpMessage;
	}

	public int getReplyCode() {
		return this.replyCode;
	}

	public String getReplyText() {
		return this.replyText;
	}

	public String getExchange() {
		return this.exchange;
	}

	public String getRoutingKey() {
		return this.routingKey;
	}

	@Override
	public String toString() {
		return super.toString() + " [amqpMessage=" + this.amqpMessage + ", replyCode=" + this.replyCode
				+ ", replyText=" + this.replyText + ", exchange=" + this.exchange + ", routingKey=" + this.routingKey
				+ "]";
	}

}
