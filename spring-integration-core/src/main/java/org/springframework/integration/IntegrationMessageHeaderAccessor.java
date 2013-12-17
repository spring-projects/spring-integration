/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *	  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.integration;

import java.util.Date;

import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.util.Assert;

/**
 *
 * Adds standard SI Headers.
 *
 * @author Andy Wilkinson
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

	public static final String POSTPROCESS_RESULT = "postProcessResult";

	public IntegrationMessageHeaderAccessor(Message<?> message) {
		super(message);
	}

	public Long getExpirationDate() {
		return this.getHeader(EXPIRATION_DATE, Long.class);
	}

	public Object getCorrelationId() {
		return this.getHeader(CORRELATION_ID);
	}

	public Integer getSequenceNumber() {
		Integer sequenceNumber = this.getHeader(SEQUENCE_NUMBER, Integer.class);
		return (sequenceNumber != null ? sequenceNumber : 0);
	}

	public Integer getSequenceSize() {
		Integer sequenceSize = this.getHeader(SEQUENCE_SIZE, Integer.class);
		return (sequenceSize != null ? sequenceSize : 0);
	}

	public Integer getPriority() {
		return this.getHeader(PRIORITY, Integer.class);
	}

	@SuppressWarnings("unchecked")
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
				Assert.isTrue(headerValue instanceof Date || headerValue instanceof Long, "The '" + headerName
						+ "' header value must be a Date or Long.");
			}
			else if (IntegrationMessageHeaderAccessor.SEQUENCE_NUMBER.equals(headerName)
					|| IntegrationMessageHeaderAccessor.SEQUENCE_SIZE.equals(headerName)) {
				Assert.isTrue(Integer.class.isAssignableFrom(headerValue.getClass()), "The '" + headerName
						+ "' header value must be an Integer.");
			}
			else if (IntegrationMessageHeaderAccessor.PRIORITY.equals(headerName)) {
				Assert.isTrue(Integer.class.isAssignableFrom(headerValue.getClass()), "The '" + headerName
						+ "' header value must be an Integer.");
			}
		}
	}
}
