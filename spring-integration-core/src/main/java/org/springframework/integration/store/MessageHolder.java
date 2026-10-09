/*
 * Copyright 2016-present the original author or authors.
 */

package org.springframework.integration.store;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;

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

	@Serial
	private static final long serialVersionUID = 1L;

	@SuppressWarnings("serial")
	private final Message<?> message;

	private final MessageMetadata messageMetadata;

	@JsonCreator
	public MessageHolder(Message<?> message) {
		Assert.notNull(message, "'message' must not be null.");
		this.message = message;
		UUID id = message.getHeaders().getId();
		Assert.notNull(id, "Message 'id' must not be null.");
		this.messageMetadata = new MessageMetadata(id);
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
