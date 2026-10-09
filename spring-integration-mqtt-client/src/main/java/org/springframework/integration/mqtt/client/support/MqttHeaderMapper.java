/*
 * Copyright 2026-present the original author or authors.
 */

package org.springframework.integration.mqtt.client.support;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.hivemq.client.mqtt.mqtt5.datatypes.Mqtt5UserProperty;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish;
import org.jspecify.annotations.Nullable;

import org.springframework.core.log.LogAccessor;
import org.springframework.core.log.LogMessage;
import org.springframework.integration.mapping.HeaderMapper;
import org.springframework.messaging.MessageHeaders;
import org.springframework.util.PatternMatchUtils;

/**
 * The default {@link HeaderMapper} implementation for MQTT v5 message properties mapping.
 *
 * @author Artem Bilan
 * @author Jiandong Ma
 *
 * @since 7.2
 */
public class MqttHeaderMapper implements HeaderMapper<Mqtt5Publish> {

	private static final LogAccessor LOGGER = new LogAccessor(MqttHeaderMapper.class);

	private final String[] inboundHeaderNames = {"*"};

	@Override
	public void fromHeaders(MessageHeaders headers, Mqtt5Publish target) {

	}

	@Override
	public Map<String, Object> toHeaders(Mqtt5Publish mqtt5Publish) {
		Map<String, Object> headers = new HashMap<>();

		headers.compute(MessageHeaders.CONTENT_TYPE, (k, v) ->
				mapPropertyIfMatch(k, mqtt5Publish.getContentType().map(Object::toString).orElse(null)));
		headers.compute(MqttHeaders.RESPONSE_TOPIC, (k, v) ->
				mapPropertyIfMatch(k, mqtt5Publish.getResponseTopic().map(Objects::toString).orElse(null)));
		headers.compute(MqttHeaders.CORRELATION_DATA, (k, v) ->
				mapPropertyIfMatch(k, mqtt5Publish.getCorrelationData().orElse(null)));

		var userProperties = mqtt5Publish.getUserProperties().asList();
		for (Mqtt5UserProperty userProperty : userProperties) {
			String name = userProperty.getName().toString();
			if (shouldMapHeader(name, this.inboundHeaderNames)) {
				headers.put(name, userProperty.getValue().toString());
			}
		}
		return headers;
	}

	private @Nullable Object mapPropertyIfMatch(String headerName, @Nullable Object value) {
		return (value != null && shouldMapHeader(headerName, this.inboundHeaderNames)) ? value : null;
	}

	private static boolean shouldMapHeader(String headerName, String[] patterns) {
		for (String pattern : patterns) {
			if (PatternMatchUtils.simpleMatch(pattern, headerName)) {
				LOGGER.debug(LogMessage.format("headerName=[%s] WILL be mapped, matched pattern=%s",
						headerName, pattern));
				return true;
			}
		}
		LOGGER.debug(LogMessage.format("headerName=[%s] WILL NOT be mapped", headerName));
		return false;
	}

}
