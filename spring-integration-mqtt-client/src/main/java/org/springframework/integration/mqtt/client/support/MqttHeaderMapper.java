/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.mqtt.client.support;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.hivemq.client.mqtt.mqtt5.datatypes.Mqtt5UserProperty;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish;

import org.springframework.core.log.LogAccessor;
import org.springframework.core.log.LogMessage;
import org.springframework.integration.JavaUtils;
import org.springframework.integration.mapping.HeaderMapper;
import org.springframework.messaging.MessageHeaders;
import org.springframework.util.ObjectUtils;
import org.springframework.util.PatternMatchUtils;

/**
 * The default {@link HeaderMapper} implementation for MQTT v5 message properties mapping.
 *
 * @author Artem Bilan
 * @author Jiandong Ma
 * @author Glenn Renfro
 *
 * @since 7.2
 */
public class MqttHeaderMapper implements HeaderMapper<Mqtt5Publish> {

	private static final LogAccessor LOGGER = new LogAccessor(MqttHeaderMapper.class);

	private String[] inboundHeaderNames = {};

	/**
	 * Construct a {@code MqttHeaderMapper} with the specified inbound user property name patterns.
	 * The native PUBLISH properties ({@link MessageHeaders#CONTENT_TYPE}, {@link MqttHeaders#RESPONSE_TOPIC},
	 * {@link MqttHeaders#CORRELATION_DATA}) are always mapped.
	 * By default, no user properties are mapped; opt in explicitly, e.g. {@code "*"} to map all of them,
	 * only when the publisher is trusted, since user properties may override framework headers.
	 * @param inboundHeaderNames the MQTT user property patterns to map.
	 */
	public MqttHeaderMapper(String... inboundHeaderNames) {
		if (!ObjectUtils.isEmpty(inboundHeaderNames)) {
			this.inboundHeaderNames = inboundHeaderNames.clone();
		}
	}

	@Override
	public void fromHeaders(MessageHeaders headers, Mqtt5Publish target) {
		throw new UnsupportedOperationException("Cannot populate immutable Mqtt5Publish.");
	}

	@Override
	public Map<String, Object> toHeaders(Mqtt5Publish mqtt5Publish) {
		Map<String, Object> headers = new HashMap<>();

		JavaUtils.INSTANCE
				.acceptIfHasText(MessageHeaders.CONTENT_TYPE,
						mqtt5Publish.getContentType().map(Objects::toString).orElse(null), headers::put)
				.acceptIfHasText(MqttHeaders.RESPONSE_TOPIC,
						mqtt5Publish.getResponseTopic().map(Objects::toString).orElse(null), headers::put)
				.acceptIfNotNull(MqttHeaders.CORRELATION_DATA,
						mqtt5Publish.getCorrelationData().orElse(null), headers::put);

		var userProperties = mqtt5Publish.getUserProperties().asList();
		for (Mqtt5UserProperty userProperty : userProperties) {
			String name = userProperty.getName().toString();
			if (shouldMapHeader(name, this.inboundHeaderNames)) {
				headers.put(name, userProperty.getValue().toString());
			}
		}
		return headers;
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
