/*
 * Copyright 2016-2024 the original author or authors.
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

	@SuppressWarnings("serial")
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
