/*
 * Copyright 2016-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
