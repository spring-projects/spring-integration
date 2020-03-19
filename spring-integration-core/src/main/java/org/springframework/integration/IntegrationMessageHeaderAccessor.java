/*
 * Copyright 2013-2019 the original author or authors.
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

package org.springframework.integration;

import java.io.Closeable;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;

import org.springframework.integration.acks.AcknowledgmentCallback;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 *
 * Adds standard SI Headers.
 *
 * @author Andy Wilkinson
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 4.0
 *
 */
public class IntegrationMessageHeaderAccessor extends MessageHeaderAccessor {

	public static final String CORRELATION_ID = "correlationId";

	public static final String EXPIRATION_DATE = "expirationDate";

	public static final String PRIORITY = "priority";

	public static final String SEQUENCE_NUMBER = "sequenceNumber";

	public static final String SEQUENCE_SIZE = "sequenceSize";

	public static final String SEQUENCE_DETAILS = "sequenceDetails";

	public static final String ROUTING_SLIP = "routingSlip";

	public static final String DUPLICATE_MESSAGE = "duplicateMessage";

	public static final String CLOSEABLE_RESOURCE = "closeableResource";

	public static final String DELIVERY_ATTEMPT = "deliveryAttempt";

	/**
	 * A callback to acknowledge message delivery. The type of the header value depends on
	 * the context in which the header is used. See the reference manual for more
	 * information.
	 */
	public static final String ACKNOWLEDGMENT_CALLBACK = "acknowledgmentCallback";

	/**
	 * Raw source message.
	 */
	public static final String SOURCE_DATA = "sourceData";

	private static final BiFunction<String, String, String> TYPE_VERIFY_MESSAGE_FUNCTION =
			(name, trailer) -> "The '" + name + trailer;

	private Set<String> readOnlyHeaders = new HashSet<>();

	public IntegrationMessageHeaderAccessor(@Nullable Message<?> message) {
		super(message);
	}

	/**
	 * Specify a list of headers which should be considered as read only and prohibited
	 * from being populated in the message.
	 * @param readOnlyHeaders the list of headers for {@code readOnly} mode. Defaults to
	 * {@link org.springframework.messaging.MessageHeaders#ID} and
	 * {@link org.springframework.messaging.MessageHeaders#TIMESTAMP}.
	 * @since 4.3.2
	 * @see #isReadOnly(String)
	 */
	public void setReadOnlyHeaders(String... readOnlyHeaders) {
		Assert.noNullElements(readOnlyHeaders, "'readOnlyHeaders' must not be contain null items.");
		if (!ObjectUtils.isEmpty(readOnlyHeaders)) {
			this.readOnlyHeaders = new HashSet<>(Arrays.asList(readOnlyHeaders));
		}
	}

	@Nullable
	public Long getExpirationDate() {
		return getHeader(EXPIRATION_DATE, Long.class);
	}

	@Nullable
	public Object getCorrelationId() {
		return getHeader(CORRELATION_ID);
	}

	public int getSequenceNumber() {
		Number sequenceNumber = getHeader(SEQUENCE_NUMBER, Number.class);
		return (sequenceNumber != null ? sequenceNumber.intValue() : 0);
	}

	public int getSequenceSize() {
		Number sequenceSize = getHeader(SEQUENCE_SIZE, Number.class);
		return (sequenceSize != null ? sequenceSize.intValue() : 0);
	}

	@Nullable
	public Integer getPriority() {
		Number priority = getHeader(PRIORITY, Number.class);
		return (priority != null ? priority.intValue() : null);
	}

	/**
	 * If the payload was created by a {@link Closeable} that needs to remain
	 * open until the payload is consumed, the resource will be added to this
	 * header. After the payload is consumed the {@link Closeable} should be
	 * closed. Usually this must occur in an endpoint close to the message
	 * origin in the flow, and in the same JVM.
	 * @return the {@link Closeable}.
	 * @since 4.3
	 */
	@Nullable
	public Closeable getCloseableResource() {
		return getHeader(CLOSEABLE_RESOURCE, Closeable.class);
	}

	/**
	 * Return the acknowledgment callback, if present.
	 * @return the callback.
	 * @since 5.0.1
	 */
	@Nullable
	public AcknowledgmentCallback getAcknowledgmentCallback() {
		return getHeader(ACKNOWLEDGMENT_CALLBACK, AcknowledgmentCallback.class);
	}

	/**
	 * When a message-driven endpoint supports retry implicitly, this
	 * header is incremented for each delivery attempt.
	 * @return the delivery attempt.
	 * @since 5.0.1
	 */
	@Nullable
	public AtomicInteger getDeliveryAttempt() {
		return getHeader(DELIVERY_ATTEMPT, AtomicInteger.class);
	}

	/**
	 * Get the source data header, if present.
	 * @param <T> the data type.
	 * @return the source header.
	 * @since 5.1.6
	 */
	@SuppressWarnings("unchecked")
	@Nullable
	public <T> T getSourceData() {
		return (T) getHeader(SOURCE_DATA);
	}

	@SuppressWarnings("unchecked")
	@Nullable
	public <T> T getHeader(String key, Class<T> type) {
		Object value = getHeader(key);
		if (value == null) {
			return null;
		}
		if (!type.isAssignableFrom(value.getClass())) {
			throw new IllegalArgumentException("Incorrect type specified for header '" + key + "'. Expected [" + type
					+ "] but actual type is [" + value.getClass() + "]");
		}
		return (T) value;
	}

	@Override
	protected void verifyType(String headerName, Object headerValue) {
		if (headerName != null && headerValue != null) {
			super.verifyType(headerName, headerValue);
			if (IntegrationMessageHeaderAccessor.EXPIRATION_DATE.equals(headerName)) {
				Assert.isTrue(headerValue instanceof Date || headerValue instanceof Long,
						TYPE_VERIFY_MESSAGE_FUNCTION.apply(headerName, "' header value must be a Date or Long."));
			}
			else if (IntegrationMessageHeaderAccessor.SEQUENCE_NUMBER.equals(headerName)
					|| IntegrationMessageHeaderAccessor.SEQUENCE_SIZE.equals(headerName)
					|| IntegrationMessageHeaderAccessor.PRIORITY.equals(headerName)) {
				Assert.isTrue(Number.class.isAssignableFrom(headerValue.getClass()),
						TYPE_VERIFY_MESSAGE_FUNCTION.apply(headerName, "' header value must be a Number."));
			}
			else if (IntegrationMessageHeaderAccessor.ROUTING_SLIP.equals(headerName)) {
				Assert.isTrue(Map.class.isAssignableFrom(headerValue.getClass()),
						TYPE_VERIFY_MESSAGE_FUNCTION.apply(headerName, "' header value must be a Map."));
			}
			else if (IntegrationMessageHeaderAccessor.DUPLICATE_MESSAGE.equals(headerName)) {
				Assert.isTrue(Boolean.class.isAssignableFrom(headerValue.getClass()),
						TYPE_VERIFY_MESSAGE_FUNCTION.apply(headerName, "' header value must be an Boolean."));
			}
		}
	}

	@Override
	public boolean isReadOnly(String headerName) {
		return super.isReadOnly(headerName) || this.readOnlyHeaders.contains(headerName);
	}

	@Override
	public Map<String, Object> toMap() {
		if (ObjectUtils.isEmpty(this.readOnlyHeaders)) {
			return super.toMap();
		}
		else {
			Map<String, Object> headers = super.toMap();
			for (String header : this.readOnlyHeaders) {
				headers.remove(header);
			}
			return headers;
		}
	}

}
