/*
 * Copyright 2026-present the original author or authors.
 */

package org.springframework.integration.mqtt.support;

import java.util.List;

import org.eclipse.paho.mqttv5.common.packet.MqttProperties;
import org.eclipse.paho.mqttv5.common.packet.UserProperty;
import org.junit.jupiter.api.Test;

import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.messaging.MessageHeaders;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Glenn Renfro
 *
 * @since 7.0.8
 */
class MqttHeaderMapperTests {

	@Test
	void defaultInboundMappingIsEmpty() {
		MqttHeaderMapper mapper = new MqttHeaderMapper();

		MqttProperties properties = new MqttProperties();
		properties.setPayloadFormat(true);
		properties.setContentType("application/json");
		properties.setUserProperties(List.of(
				new UserProperty(MessageHeaders.REPLY_CHANNEL, "replyChannel"),
				new UserProperty(MessageHeaders.ERROR_CHANNEL, "errorChannel"),
				new UserProperty(IntegrationMessageHeaderAccessor.ROUTING_SLIP, "route"),
				new UserProperty("test", "value")));

		assertThat(mapper.toHeaders(properties)).containsEntry(MessageHeaders.CONTENT_TYPE, "application/json")
				.doesNotContainKeys("test", MessageHeaders.REPLY_CHANNEL, MessageHeaders.ERROR_CHANNEL,
				IntegrationMessageHeaderAccessor.ROUTING_SLIP);
	}

	@Test
	void nativePropertiesMappedRegardlessOfInboundHeaderNames() {
		MqttHeaderMapper mapper = new MqttHeaderMapper();

		MqttProperties properties = new MqttProperties();
		properties.setPayloadFormat(true);
		properties.setContentType("text/plain");
		properties.setResponseTopic("responseTopic");
		properties.setCorrelationData("correlation".getBytes());

		assertThat(mapper.toHeaders(properties)).containsEntry(MessageHeaders.CONTENT_TYPE, "text/plain")
				.containsEntry(MqttHeaders.RESPONSE_TOPIC, "responseTopic")
				.containsEntry(MqttHeaders.CORRELATION_DATA, "correlation".getBytes());
	}

	@Test
	void explicitInboundHeaderNamesMapOnlyMatchingUserProperties() {
		MqttHeaderMapper mapper = new MqttHeaderMapper();
		mapper.setInboundHeaderNames("test");

		MqttProperties properties = new MqttProperties();
		properties.setUserProperties(List.of(
				new UserProperty("test", "value"),
				new UserProperty(MessageHeaders.ERROR_CHANNEL, "errorChannel")));

		assertThat(mapper.toHeaders(properties))
				.containsEntry("test", "value")
				.doesNotContainKey(MessageHeaders.ERROR_CHANNEL);
	}

}
