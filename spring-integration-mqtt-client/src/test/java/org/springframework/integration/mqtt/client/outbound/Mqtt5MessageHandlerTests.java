/*
 * Copyright 2026-present the original author or authors.
 */

package org.springframework.integration.mqtt.client.outbound;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.hivemq.client.internal.mqtt.datatypes.MqttUserPropertyImpl;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient;
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5PayloadFormatIndicator;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5PublishResult;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.core.serializer.support.DeserializingConverter;
import org.springframework.core.serializer.support.SerializingConverter;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.expression.FunctionExpression;
import org.springframework.integration.mqtt.client.MqttContainerTest;
import org.springframework.integration.mqtt.client.core.ClientManager;
import org.springframework.integration.mqtt.client.core.Mqtt5ClientManager;
import org.springframework.integration.mqtt.client.event.MqttMessageDeliveredEvent;
import org.springframework.integration.mqtt.client.event.MqttMessageDeliveryEvent;
import org.springframework.integration.mqtt.client.event.MqttMessageSentEvent;
import org.springframework.integration.mqtt.client.support.MqttHeaders;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.AbstractMessageConverter;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Jiandong Ma
 *
 * @since 7.2
 */
@SpringJUnitConfig
@DirtiesContext
class Mqtt5MessageHandlerTests implements MqttContainerTest {

	static CountDownLatch connectedLatch = new CountDownLatch(1);

	static final String CAR_DEVICE_DEFAULT_TOPIC = "mqtt-v5-outbound-car-default-topic";

	static CountDownLatch petDeviceMessageSentLatch = new CountDownLatch(1);

	static MqttMessageSentEvent petDeviceMessageSentEvent;

	static CountDownLatch petDeviceMessageDeliveredLatch = new CountDownLatch(1);

	static MqttMessageDeliveredEvent petDeviceMessageDeliveredEvent;

	Mqtt5AsyncClient mqtt5AsyncClient;

	@Autowired
	ClientManager<Mqtt5Client> mqtt5ClientManager;

	@Autowired
	@Qualifier("carDeviceMessageOutboundAdapter.input")
	MessageChannel carDeviceInputChannel;

	@Autowired
	@Qualifier("petDeviceMessageOutboundAdapter.input")
	MessageChannel petDeviceInputChannel;

	@BeforeEach
	void beforeEach() throws InterruptedException {
		assertThat(connectedLatch.await(10, TimeUnit.SECONDS)).isTrue();
		mqtt5AsyncClient = mqtt5ClientManager.getClient().toAsync();
	}

	@Test
	void testCarDeviceMessageHandlerDefaultTopicBytePayload() throws Exception {
		// Given
		CountDownLatch callbackLatch = new CountDownLatch(1);
		var subscribeFuture = mqtt5AsyncClient.subscribeWith()
				.topicFilter(CAR_DEVICE_DEFAULT_TOPIC)
				.qos(MqttQos.AT_LEAST_ONCE)
				.callback(mqtt5Publish -> {
					// Then
					assertThat(mqtt5Publish).satisfies(publish -> {
						assertThat(publish.getTopic().toString()).isEqualTo(CAR_DEVICE_DEFAULT_TOPIC);
						assertThat(publish.getPayloadAsBytes()).isEqualTo("simple-car-payload".getBytes());
						assertThat(publish.getQos()).isEqualTo(MqttQos.AT_MOST_ONCE);
						assertThat(publish.getContentType().get().toString()).isEqualTo("plain/text");
						assertThat(publish.getCorrelationData().get())
								.isEqualTo(ByteBuffer.wrap("correlationData".getBytes()));
						assertThat(publish.getMessageExpiryInterval().getAsLong()).isEqualTo(30L);
						assertThat(publish.getResponseTopic().get().toString()).isEqualTo("response-topic");
						assertThat(publish.getPayloadFormatIndicator().get())
								.isEqualTo(Mqtt5PayloadFormatIndicator.UTF_8);
						assertThat(publish.getUserProperties().asList().get(0))
								.isEqualTo(MqttUserPropertyImpl.of("user_properties_key", "user_properties_value"));
					});
					callbackLatch.countDown();
				})
				.send();
		subscribeFuture.get(10, TimeUnit.SECONDS);
		// When
		carDeviceInputChannel.send(MessageBuilder.withPayload("simple-car-payload".getBytes())
				.setHeader(MqttHeaders.QOS, MqttQos.AT_MOST_ONCE)
				.setHeader(MqttHeaders.CONTENT_TYPE, "plain/text")
				.setHeader(MqttHeaders.CORRELATION_DATA, "correlationData".getBytes())
				.setHeader(MqttHeaders.MESSAGE_EXPIRY_INTERVAL, 30L)
				.setHeader(MqttHeaders.RESPONSE_TOPIC, "response-topic")
				.setHeader(MqttHeaders.PAYLOAD_FORMAT_INDICATOR, Mqtt5PayloadFormatIndicator.UTF_8)
				.setHeader(MqttHeaders.USER_PROPERTIES, Map.of("user_properties_key", "user_properties_value"))
				.build());
		// Await
		assertThat(callbackLatch.await(10, TimeUnit.SECONDS)).isTrue();
	}

	@Test
	void testCarDeviceMessageHandlerHeaderTopicStringPayload() throws Exception {
		// Given
		String carTopic = "mqtt-v5-outbound-car-header-topic";
		CountDownLatch callbackLatch = new CountDownLatch(1);
		var subscribeFuture = mqtt5AsyncClient.subscribeWith()
				.topicFilter(carTopic)
				.qos(MqttQos.AT_LEAST_ONCE)
				.callback(mqtt5Publish -> {
					// Then
					assertThat(mqtt5Publish).satisfies(publish -> {
						assertThat(publish.getTopic().toString()).isEqualTo(carTopic);
						assertThat(publish.getPayloadAsBytes()).isEqualTo("simple-car-payload".getBytes());
						assertThat(publish.getQos()).isEqualTo(MqttQos.AT_LEAST_ONCE);
					});
					callbackLatch.countDown();
				})
				.send();
		subscribeFuture.get(10, TimeUnit.SECONDS);
		// When
		carDeviceInputChannel.send(MessageBuilder.withPayload("simple-car-payload")
				.setHeader(MqttHeaders.TOPIC, carTopic)
				.build());
		// Await
		assertThat(callbackLatch.await(10, TimeUnit.SECONDS)).isTrue();
	}

	@Test
	void testCarDeviceMessageHandlerHeaderTopicObjectPayload() throws Exception {
		// Given
		String carTopic = "mqtt-v5-outbound-car-header-topic-object-payload";
		record CarObject(String model, String serialNumber) implements Serializable {

		}

		CarObject carObject = new CarObject("Tesla", "1234");
		CountDownLatch callbackLatch = new CountDownLatch(1);
		var subscribeFuture = mqtt5AsyncClient.subscribeWith()
				.topicFilter(carTopic)
				.qos(MqttQos.AT_LEAST_ONCE)
				.callback(mqtt5Publish -> {
					// Then
					assertThat(mqtt5Publish).satisfies(publish -> {
						assertThat(publish.getTopic().toString()).isEqualTo(carTopic);
						Object deserializedPayload = new DeserializingConverter().convert(publish.getPayloadAsBytes());
						assertThat(deserializedPayload).isInstanceOf(CarObject.class).isEqualTo(carObject);
					});
					callbackLatch.countDown();
				})
				.send();
		subscribeFuture.get(10, TimeUnit.SECONDS);
		// When
		carDeviceInputChannel.send(MessageBuilder.withPayload(carObject)
				.setHeader(MqttHeaders.TOPIC, carTopic)
				.build());
		// Await
		assertThat(callbackLatch.await(10, TimeUnit.SECONDS)).isTrue();
	}

	@Test
	void testCarDeviceMessageHandlerHeaderTopicMqtt5PublishPayload() throws Exception {
		// Given
		String carTopic = "mqtt-v5-outbound-car-header-topic-mqtt5publish-payload";
		Mqtt5Publish mqtt5Payload = Mqtt5Publish.builder()
				.topic(carTopic)
				.payload("simple-car-payload".getBytes())
				.build();
		CountDownLatch callbackLatch = new CountDownLatch(1);
		var subscribeFuture = mqtt5AsyncClient.subscribeWith()
				.topicFilter(carTopic)
				.qos(MqttQos.AT_LEAST_ONCE)
				.callback(mqtt5Publish -> {
					// Then
					assertThat(mqtt5Publish).isEqualTo(mqtt5Payload);
					callbackLatch.countDown();
				})
				.send();
		subscribeFuture.get(10, TimeUnit.SECONDS);
		// When
		carDeviceInputChannel.send(MessageBuilder.withPayload(mqtt5Payload)
				.setHeader(MqttHeaders.TOPIC, carTopic)
				.build());
		// Await
		assertThat(callbackLatch.await(10, TimeUnit.SECONDS)).isTrue();
	}

	@Test
	void testPetDeviceMessageHandlerAsyncEvent() throws Exception {
		// Given
		String petTopic = "mqtt-v5-outbound-pet-header-topic";
		// When
		petDeviceInputChannel.send(MessageBuilder
				.withPayload("simple-pet-payload")
				.setHeader(MqttHeaders.TOPIC, petTopic)
				.setHeader(MqttHeaders.QOS, MqttQos.AT_LEAST_ONCE)
				.build());
		// Then
		assertThat(petDeviceMessageSentLatch.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(petDeviceMessageDeliveredLatch.await(10000, TimeUnit.MILLISECONDS)).isTrue();
		assertThat(petDeviceMessageSentEvent)
				.extracting(MqttMessageSentEvent::getMessage)
				.extracting("payload")
				.isEqualTo("simple-pet-payload");
		assertThat(petDeviceMessageDeliveredEvent)
				.extracting(MqttMessageDeliveryEvent::getMqttPublishResult)
				.isInstanceOf(Mqtt5PublishResult.class);
	}

	@Configuration(proxyBeanMethods = false)
	@EnableIntegration
	static class Config {

		@Bean
		Mqtt5ClientManager mqtt5ClientManager() {
			Mqtt5ClientManager mqtt5ClientManager = new Mqtt5ClientManager(Mqtt5Client.builder()
					.serverHost(MQTT_CONTAINER.getHost())
					.serverPort(MQTT_CONTAINER.getFirstMappedPort())
					.build()
					.getConfig());
			mqtt5ClientManager.addCallback(connectedContext -> connectedLatch.countDown());
			return mqtt5ClientManager;
		}

		@Bean
		Mqtt5MessageHandler carDeviceMessageHandler(Mqtt5ClientManager mqtt5ClientManager) {
			var messageHandler = new Mqtt5MessageHandler(mqtt5ClientManager);
			messageHandler.setDefaultTopic(CAR_DEVICE_DEFAULT_TOPIC);
			messageHandler.setDefaultQos(MqttQos.AT_LEAST_ONCE);
			messageHandler.setDefaultRetained(false);
			messageHandler.setMessageConverter(new AbstractMessageConverter() {

				@Override
				protected boolean supports(@NonNull Class<?> clazz) {
					return true;
				}

				@Override
				protected Object convertToInternal(@NonNull Object payload, @Nullable MessageHeaders headers,
						@Nullable Object conversionHint) {

					return new SerializingConverter().convert(payload);
				}
			});
			// for coverage
			messageHandler.setTopicExpressionString("headers['mqtt_topic']");
			messageHandler.setQosExpressionString("headers['mqtt_qos']");
			messageHandler.setRetainedExpressionString("headers['mqtt_retained']");
			messageHandler.setContentTypeExpressionString("headers['mqtt_contentType']");
			messageHandler.setCorrelationDataExpressionString("headers['mqtt_correlationData']");
			messageHandler.setMessageExpiryIntervalExpressionString("headers['mqtt_messageExpiryInterval']");
			messageHandler.setResponseTopicExpressionString("headers['mqtt_responseTopic']");
			messageHandler.setPayloadFormatIndicatorExpressionString("headers['mqtt_payloadFormatIndicator']");
			messageHandler.setUserPropertiesExpressionString("headers['mqtt_userProperties']");
			return messageHandler;
		}

		@Bean
		IntegrationFlow carDeviceMessageOutboundAdapter(Mqtt5MessageHandler carDeviceMessageHandler) {
			return flow -> flow.handle(carDeviceMessageHandler);
		}

		@Bean
		Mqtt5MessageHandler petDeviceMessageHandler(Mqtt5ClientManager mqtt5ClientManager) {
			var messageHandler = new Mqtt5MessageHandler(mqtt5ClientManager);
			messageHandler.setAsync(true);
			// for coverage
			messageHandler.setTopicExpression(
					new FunctionExpression<Message<?>>(message -> message.getHeaders().get(MqttHeaders.TOPIC)));
			messageHandler.setQosExpression(
					new FunctionExpression<Message<?>>(message -> message.getHeaders().get(MqttHeaders.QOS)));
			messageHandler.setRetainedExpression(
					new FunctionExpression<Message<?>>(message -> message.getHeaders().get(MqttHeaders.RETAINED)));

			return messageHandler;
		}

		@Bean
		IntegrationFlow petDeviceMessageOutboundAdapter(Mqtt5MessageHandler petDeviceMessageHandler) {
			return flow -> flow.handle(petDeviceMessageHandler);
		}

		@EventListener
		void petDeviceMqttEvents(MqttMessageDeliveryEvent deliveryEvent) {
			if (deliveryEvent instanceof MqttMessageSentEvent messageSentEvent) {
				petDeviceMessageSentEvent = messageSentEvent;
				petDeviceMessageSentLatch.countDown();
			}
			else if (deliveryEvent instanceof MqttMessageDeliveredEvent messageDeliveredEvent) {
				petDeviceMessageDeliveredEvent = messageDeliveredEvent;
				petDeviceMessageDeliveredLatch.countDown();
			}
		}

	}

}
