/*
 * Copyright 2026-present the original author or authors.
 */

package org.springframework.integration.mqtt.client.support;

import java.nio.ByteBuffer;

import com.hivemq.client.mqtt.mqtt5.datatypes.Mqtt5UserProperties;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish;
import org.junit.jupiter.api.Test;

import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.messaging.MessageHeaders;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Glenn Renfro
 *
 * @since 7.2
 */
class MqttHeaderMapperTests {

	private static final Mqtt5UserProperties USER_PROPERTIES = Mqtt5UserProperties.builder()
			.add(MessageHeaders.REPLY_CHANNEL, "replyChannel")
			.add(MessageHeaders.ERROR_CHANNEL, "errorChannel")
			.add(IntegrationMessageHeaderAccessor.ROUTING_SLIP, "route")
			.add("test", "value")
			.build();

	@Test
	void defaultInboundMappingIsEmpty() {
		MqttHeaderMapper mapper = new MqttHeaderMapper();

		Mqtt5Publish publish = Mqtt5Publish.builder()
				.topic("testTopic")
				.contentType("application/json")
				.userProperties(USER_PROPERTIES)
				.build();

		assertThat(mapper.toHeaders(publish))
				.containsEntry(MessageHeaders.CONTENT_TYPE, "application/json")
				.doesNotContainKeys("test", MessageHeaders.REPLY_CHANNEL, MessageHeaders.ERROR_CHANNEL,
						IntegrationMessageHeaderAccessor.ROUTING_SLIP);
	}

	@Test
	void nativePropertiesAlwaysMappedAndUserPropertiesMatchPatterns() {
		MqttHeaderMapper mapper = new MqttHeaderMapper("test");

		Mqtt5Publish publish = Mqtt5Publish.builder()
				.topic("testTopic")
				.contentType("text/plain")
				.responseTopic("responseTopic")
				.correlationData("correlation".getBytes())
				.userProperties(USER_PROPERTIES)
				.build();

		assertThat(mapper.toHeaders(publish))
				.containsEntry(MessageHeaders.CONTENT_TYPE, "text/plain")
				.containsEntry(MqttHeaders.RESPONSE_TOPIC, "responseTopic")
				.containsEntry(MqttHeaders.CORRELATION_DATA, ByteBuffer.wrap("correlation".getBytes()))
				.containsEntry("test", "value")
				.doesNotContainKeys(MessageHeaders.REPLY_CHANNEL, MessageHeaders.ERROR_CHANNEL,
						IntegrationMessageHeaderAccessor.ROUTING_SLIP);
	}

	@Test
	void wildcardInboundHeaderNameMapsAllUserProperties() {
		MqttHeaderMapper mapper = new MqttHeaderMapper("*");

		Mqtt5Publish publish = Mqtt5Publish.builder()
				.topic("testTopic")
				.userProperties(USER_PROPERTIES)
				.build();

		assertThat(mapper.toHeaders(publish))
				.containsEntry("test", "value")
				.containsEntry(MessageHeaders.REPLY_CHANNEL, "replyChannel")
				.containsEntry(MessageHeaders.ERROR_CHANNEL, "errorChannel")
				.containsEntry(IntegrationMessageHeaderAccessor.ROUTING_SLIP, "route");
	}

}
