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

package org.springframework.integration.nats.converter;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.Message;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.integration.nats.exception.MessageConversionException;
import org.springframework.lang.NonNull;

/**
 * A generic converter class to convert the provided java object to byte array and vice versa.
 *
 * @param <T> type to which the message should be converted
 *
 * @author Viktor Rohlenko
 * @author Vennila Pazhamalai
 * @author Vivek Duraisamy
 * @since 6.4.x
 *
 * @see <a
 * href="https://rohlenko.github.io/spring-integration-nats-site/gws-spring-integration-nats/index.html#stakeholders">See
 * all stakeholders and contact</a>
 */
public class MessageConverter<T> {

	private static final Log LOG = LogFactory.getLog(MessageConverter.class);

	@NonNull
	private final ObjectMapper mapper;

	@NonNull
	private final Class<T> clazz;

	// insures default behavior of deserialization.
	// Default is the convertor will not fail on unknown property.
	// If failing is required this can be done later by setting this#setFailOnUnknownProperties(true)
	private boolean isFailOnUnknownProperties = false;

	public MessageConverter(@NonNull final Class<T> pClazz) {
		this.mapper = new ObjectMapper();
		this.clazz = pClazz;
		this.mapper.configure(
				DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, this.isFailOnUnknownProperties);
	}

	public boolean isFailOnUnknownProperties() {
		return this.isFailOnUnknownProperties;
	}

	/**
	 * {@link #isFailOnUnknownProperties} is by default false, means the default converter will not
	 * fail on unknown property by default. If this behavior is not desired set value to true or use
	 * your own converter in particular flow.
	 *
	 * @param failOnUnknownProperties - TODO: Add description
	 */
	public void setFailOnUnknownProperties(boolean failOnUnknownProperties) {
		this.isFailOnUnknownProperties = failOnUnknownProperties;
		this.mapper.configure(
				DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, this.isFailOnUnknownProperties);
	}

	public byte[] toMessage(@NonNull final Object object) throws MessageConversionException {
		byte[] message = null;
		try {
			message = this.mapper.writeValueAsBytes(object);
		}
		catch (final JsonProcessingException e) {
			throw new MessageConversionException(
					"Error converting " + object.getClass().getName() + " to byte array.", e);
		}
		return message;
	}

	public T fromMessage(final Message message) throws IOException, MessageConversionException {
		T t = null;
		try {
			t = this.mapper.readValue(message.getData(), this.clazz);
		}
		catch (final JsonProcessingException e) {
			throw new MessageConversionException("Error converting to " + this.clazz.getName(), e);
		}
		return t;
	}
}
