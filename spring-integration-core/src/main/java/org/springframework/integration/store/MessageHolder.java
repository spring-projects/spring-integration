/*
 * Copyright © 2016 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2016-present the original author or authors.
 */

package org.springframework.integration.store;

import java.io.Serializable;

import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * The {@link MessageStore} specific value object to keep the {@link Message} and its metadata.
 *
 * @author Artem Bilan
 *
 * @since 5.0
 */
public class MessageHolder implements Serializable {

	private static final long serialVersionUID = 1L;

	private Message<?> message;

	private MessageMetadata messageMetadata;

	private MessageHolder() {
		//For Jackson deserialization
	}

	public MessageHolder(Message<?> message) {
		Assert.notNull(message, "'message' must not be null.");
		this.message = message;
		this.messageMetadata = new MessageMetadata(message.getHeaders().getId());
		this.messageMetadata.setTimestamp(System.currentTimeMillis());
	}

	public void setTimestamp(long timestamp) {
		this.messageMetadata.setTimestamp(timestamp);
	}

	public Message<?> getMessage() {
		return this.message;
	}

	public MessageMetadata getMessageMetadata() {
		return this.messageMetadata;
	}

}
