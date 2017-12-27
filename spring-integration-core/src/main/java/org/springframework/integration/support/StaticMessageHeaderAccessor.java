/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.support;

import java.io.Closeable;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.util.MimeType;

/**
 * Lightweight type-safe header accessor avoiding object
 * creation just to access a header.
 *
 * @author Gary Russell
 * @since 5.0.1
 *
 * @see IntegrationMessageHeaderAccessor
 */
public final class StaticMessageHeaderAccessor {

	private StaticMessageHeaderAccessor() {
		super();
	}

	@Nullable
	public static UUID getId(Message<?> message) {
		Object value = message.getHeaders().get(MessageHeaders.ID);
		if (value == null) {
			return null;
		}
		return (value instanceof UUID ? (UUID) value : UUID.fromString(value.toString()));
	}

	@Nullable
	public static Long getTimestamp(Message<?> message) {
		Object value = message.getHeaders().get(MessageHeaders.TIMESTAMP);
		if (value == null) {
			return null;
		}
		return (value instanceof Long ? (Long) value : Long.parseLong(value.toString()));
	}

	@Nullable
	public static MimeType getContentType(Message<?> message) {
		Object value = message.getHeaders().get(MessageHeaders.CONTENT_TYPE);
		if (value == null) {
			return null;
		}
		return (value instanceof MimeType ? (MimeType) value : MimeType.valueOf(value.toString()));
	}

	@Nullable
	public static Long getExpirationDate(Message<?> message) {
		return message.getHeaders().get(IntegrationMessageHeaderAccessor.EXPIRATION_DATE, Long.class);
	}

	public static int getSequenceNumber(Message<?> message) {
		Number sequenceNumber = message.getHeaders().get(IntegrationMessageHeaderAccessor.SEQUENCE_NUMBER,
				Number.class);
		return (sequenceNumber != null ? sequenceNumber.intValue() : 0);
	}

	public static int getSequenceSize(Message<?> message) {
		Number sequenceSize = message.getHeaders().get(IntegrationMessageHeaderAccessor.SEQUENCE_SIZE, Number.class);
		return (sequenceSize != null ? sequenceSize.intValue() : 0);
	}

	@Nullable
	public static Integer getPriority(Message<?> message) {
		Number priority = message.getHeaders().get(IntegrationMessageHeaderAccessor.PRIORITY, Number.class);
		return (priority != null ? priority.intValue() : null);
	}

	@Nullable
	public static Closeable getCloseableResource(Message<?> message) {
		return message.getHeaders().get(IntegrationMessageHeaderAccessor.CLOSEABLE_RESOURCE, Closeable.class);
	}

	@Nullable
	public static AtomicInteger getDeliveryAttempt(Message<?> message) {
		return message.getHeaders().get(IntegrationMessageHeaderAccessor.DELIVERY_ATTEMPT, AtomicInteger.class);
	}

}
