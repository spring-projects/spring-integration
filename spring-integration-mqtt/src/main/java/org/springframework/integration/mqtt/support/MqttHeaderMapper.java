/*
 * Copyright 2021-2022 the original author or authors.
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

package org.springframework.integration.mqtt.support;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.paho.mqttv5.common.packet.MqttProperties;
import org.eclipse.paho.mqttv5.common.packet.UserProperty;

import org.springframework.core.log.LogAccessor;
import org.springframework.core.log.LogMessage;
import org.springframework.integration.mapping.HeaderMapper;
import org.springframework.lang.Nullable;
import org.springframework.messaging.MessageHeaders;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;
import org.springframework.util.PatternMatchUtils;

/**
 * The default {@link HeaderMapper} implementation for MQTT v5 message properties mapping.
 *
 * @author Artem Bilan
 *
 * @since 5.5.5
 */
public class MqttHeaderMapper implements HeaderMapper<MqttProperties> {

	private static final LogAccessor LOGGER = new LogAccessor(MqttHeaderMapper.class);

	private String[] inboundHeaderNames = {"*"};

	private String[] outboundHeaderNames = {
			MessageHeaders.CONTENT_TYPE,
			MqttHeaders.MESSAGE_EXPIRY_INTERVAL,
			MqttHeaders.RESPONSE_TOPIC,
			MqttHeaders.CORRELATION_DATA
	};

	/**
	 * Provide a list of patterns to map MQTT message properties into message headers.
	 * By default, it maps all valid MQTT PUBLISH packet headers
	 * (see {@link org.eclipse.paho.mqttv5.common.packet.MqttPublish}), including all the user properties.
	 * @param inboundHeaderNames the MQTT message property patterns to map.
	 */
	public void setInboundHeaderNames(String... inboundHeaderNames) {
		Assert.notNull(inboundHeaderNames, "'inboundHeaderNames' must not be null");
		String[] copy = Arrays.copyOf(inboundHeaderNames, inboundHeaderNames.length);
		Arrays.sort(copy);
		this.inboundHeaderNames = copy;
	}

	/**
	 * Provide a list of patterns to map header into a PUBLISH MQTT message.
	 * Default headers are:
	 * {@link MessageHeaders#CONTENT_TYPE}, {@link MqttHeaders#MESSAGE_EXPIRY_INTERVAL},
	 * {@link MqttHeaders#RESPONSE_TOPIC}, {@link MqttHeaders#CORRELATION_DATA}.
	 * @param outboundHeaderNames the header patterns to map.
	 */
	public void setOutboundHeaderNames(String... outboundHeaderNames) {
		Assert.notNull(outboundHeaderNames, "'outboundHeaderNames' must not be null");
		String[] copy = Arrays.copyOf(outboundHeaderNames, outboundHeaderNames.length);
		Arrays.sort(copy);
		this.outboundHeaderNames = copy;
	}

	@Override
	public void fromHeaders(MessageHeaders headers, MqttProperties target) {
		for (Map.Entry<String, Object> entry : headers.entrySet()) {
			String name = entry.getKey();
			if (shouldMapHeader(name, this.outboundHeaderNames)) {
				Object value = entry.getValue();
				if (value != null) {
					setMqttHeader(target, name, value);
				}
			}
		}
	}

	@Override
	public Map<String, Object> toHeaders(MqttProperties source) {
		Map<String, Object> headers = new HashMap<>();
		if (source.getPayloadFormat()) {
			headers.compute(MessageHeaders.CONTENT_TYPE, (k, v) -> mapPropertyIfMatch(k, source.getContentType()));
		}
		headers.compute(MqttHeaders.TOPIC_ALIAS, (k, v) -> mapPropertyIfMatch(k, source.getTopicAlias()));
		headers.compute(MqttHeaders.RESPONSE_TOPIC, (k, v) -> mapPropertyIfMatch(k, source.getResponseTopic()));
		headers.compute(MqttHeaders.CORRELATION_DATA, (k, v) -> mapPropertyIfMatch(k, source.getCorrelationData()));

		List<UserProperty> userProperties = source.getUserProperties();
		for (UserProperty userProperty : userProperties) {
			String name = userProperty.getKey();
			if (shouldMapHeader(name, this.inboundHeaderNames)) {
				headers.put(name, userProperty.getValue());
			}
		}
		return headers;
	}

	private Object mapPropertyIfMatch(String headerName, @Nullable Object value) {
		return (value != null && shouldMapHeader(headerName, this.inboundHeaderNames)) ? value : null;
	}

	private static boolean shouldMapHeader(String headerName, String[] patterns) {
		if (patterns != null && patterns.length > 0) {
			for (String pattern : patterns) {
				if (PatternMatchUtils.simpleMatch(pattern, headerName)) {
					LOGGER.debug(LogMessage.format("headerName=[{0}] WILL be mapped, matched pattern={1}",
							headerName, pattern));
					return true;
				}
			}
		}
		LOGGER.debug(LogMessage.format("headerName=[{0}] WILL NOT be mapped", headerName));
		return false;
	}

	private static void setMqttHeader(MqttProperties target, String name, Object value) {
		switch (name) {
			case MessageHeaders.CONTENT_TYPE:
				setContentType(target, value);
				target.setPayloadFormat(true);
				break;
			case MqttHeaders.MESSAGE_EXPIRY_INTERVAL:
				setMessageExpiryInterval(target, value);
				break;
			case MqttHeaders.RESPONSE_TOPIC:
				setResponseTopic(target, value);
				break;
			case MqttHeaders.CORRELATION_DATA:
				setCorrelationData(target, value);
				break;
			default:
				if (value instanceof String) {
					target.getUserProperties().add(new UserProperty(name, (String) value));
				}
				else if (value != null) {
					throw new IllegalArgumentException(
							"Expected String value for MQTT user properties, but received: " + value.getClass());
				}
		}
	}

	private static void setContentType(MqttProperties target, Object value) {
		if (value instanceof MimeType) {
			target.setContentType(((MimeType) value).toString());
		}
		else if (value instanceof String) {
			target.setContentType((String) value);
		}
		else {
			throw new IllegalArgumentException(
					"Expected MediaType or String value for 'content-type' header value, but received: "
							+ value.getClass());
		}
	}

	private static void setMessageExpiryInterval(MqttProperties target, Object value) {
		if (value instanceof Long) {
			target.setMessageExpiryInterval((Long) value);
		}
		else if (value instanceof String) {
			target.setMessageExpiryInterval(Long.parseLong((String) value));
		}
		else {
			throw new IllegalArgumentException(
					"Expected Long or String value for 'mqtt_messageExpiryInterval' header value, but received: "
							+ value.getClass());
		}
	}

	private static void setResponseTopic(MqttProperties target, Object value) {
		if (value instanceof String) {
			target.setResponseTopic((String) value);
		}
		else {
			throw new IllegalArgumentException(
					"Expected String value for 'mqtt_responseTopic' header value, but received: " + value.getClass());
		}
	}

	private static void setCorrelationData(MqttProperties target, Object value) {
		if (value instanceof byte[]) {
			target.setCorrelationData((byte[]) value);
		}
		else if (value instanceof String) {
			target.setCorrelationData(((String) value).getBytes(StandardCharsets.UTF_8));
		}
		else {
			throw new IllegalArgumentException(
					"Expected byte[] or String value for 'mqtt_correlationData' header value, but received: "
							+ value.getClass());
		}
	}

}
