/*
 * Copyright 2016-present the original author or authors.
 */

package org.springframework.integration.store;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Value Object holding metadata about a Message in the MessageStore.
 *
 * @author Artem Bilan
 *
 * @since 5.0
 */
public class MessageMetadata implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	private final UUID messageId;

	private volatile long timestamp;

	@JsonCreator
	public MessageMetadata(UUID messageId) {
		this.messageId = messageId;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public UUID getMessageId() {
		return this.messageId;
	}

	public long getTimestamp() {
		return this.timestamp;
	}

}
