/*
 * Copyright © 2016 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2016-present the original author or authors.
 */

package org.springframework.integration.store;

import java.io.Serializable;
import java.util.UUID;

/**
 * Value Object holding metadata about a Message in the MessageStore.
 *
 * @author Artem Bilan
 *
 * @since 5.0
 */
public class MessageMetadata implements Serializable {

	private static final long serialVersionUID = 1L;

	private UUID messageId;

	private volatile long timestamp;

	private MessageMetadata() {
		//For Jackson deserialization
	}

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
