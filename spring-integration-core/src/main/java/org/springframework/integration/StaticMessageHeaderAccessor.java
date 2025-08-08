/*
 * Copyright © 2017 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2017-present the original author or authors.
 */

package org.springframework.integration;

import java.io.Closeable;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import reactor.util.context.Context;
import reactor.util.context.ContextView;

import org.springframework.integration.acks.AcknowledgmentCallback;
import org.springframework.integration.acks.SimpleAcknowledgment;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.util.MimeType;

/**
 * Lightweight type-safe header accessor avoiding object
 * creation just to access a header.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 5.0.1
 *
 * @see IntegrationMessageHeaderAccessor
 */
public final class StaticMessageHeaderAccessor {

	private StaticMessageHeaderAccessor() {
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

	@Nullable
	public static AcknowledgmentCallback getAcknowledgmentCallback(Message<?> message) {
		return message.getHeaders().get(IntegrationMessageHeaderAccessor.ACKNOWLEDGMENT_CALLBACK,
				AcknowledgmentCallback.class);
	}

	@Nullable
	public static SimpleAcknowledgment getAcknowledgment(Message<?> message) {
		return message.getHeaders().get(IntegrationMessageHeaderAccessor.ACKNOWLEDGMENT_CALLBACK,
				SimpleAcknowledgment.class);
	}

	@SuppressWarnings("unchecked")
	@Nullable
	public static <T> T getSourceData(Message<?> message) {
		return (T) message.getHeaders().get(IntegrationMessageHeaderAccessor.SOURCE_DATA);
	}

	/**
	 * Get a {@link ContextView} header if present.
	 * @param message the message to get a header from.
	 * @return the {@link ContextView} header if present.
	 * @since 6.0.5
	 */
	public static ContextView getReactorContext(Message<?> message) {
		ContextView reactorContext = message.getHeaders()
				.get(IntegrationMessageHeaderAccessor.REACTOR_CONTEXT, ContextView.class);
		if (reactorContext == null) {
			reactorContext = Context.empty();
		}
		return reactorContext;
	}

}
