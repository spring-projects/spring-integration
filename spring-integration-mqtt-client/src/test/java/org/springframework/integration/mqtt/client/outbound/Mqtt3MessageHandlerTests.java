/*
 * Copyright 2026-present the original author or authors.
 */

package org.springframework.integration.mqtt.client.outbound;

import java.io.Serializable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient;
import com.hivemq.client.mqtt.mqtt3.Mqtt3Client;
import com.hivemq.client.mqtt.mqtt3.message.publish.Mqtt3Publish;
import com.hivemq.client.mqtt.mqtt3.message.subscribe.suback.Mqtt3SubAck;
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
import org.springframework.integration.mqtt.client.core.Mqtt3ClientManager;
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
class Mqtt3MessageHandlerTests implements MqttContainerTest {

	static final String CAR_DEVICE_DEFAULT_TOPIC = "mqtt-v3-outbound-car-default-topic";

	static CountDownLatch connectedLatch = new CountDownLatch(1);

	static CountDownLatch petDeviceMessageSentLatch = new CountDownLatch(1);

	static MqttMessageSentEvent petDeviceMessageSentEvent;

	static CountDownLatch petDeviceMessageDeliveredLatch = new CountDownLatch(1);

	static MqttMessageDeliveredEvent petDeviceMessageDeliveredEvent;

	Mqtt3AsyncClient mqtt3AsyncClient;

	@Autowired
	ClientManager<Mqtt3Client> mqtt3ClientManager;

	@Autowired
	@Qualifier("carDeviceMessageOutboundAdapter.input")
	MessageChannel carDeviceInputChannel;

	@Autowired
	@Qualifier("petDeviceMessageOutboundAdapter.input")
	MessageChannel petDeviceInputChannel;

	@BeforeEach
	void beforeEach() throws InterruptedException {
		assertThat(connectedLatch.await(10, TimeUnit.SECONDS)).isTrue();
		mqtt3AsyncClient = mqtt3ClientManager.getClient().toAsync();
	}

	@Test
	void testCarDeviceMessageHandlerDefaultTopicBytePayload() throws Exception {
		// Given
		CountDownLatch callbackLatch = new CountDownLatch(1);
		CompletableFuture<Mqtt3SubAck> subscribeFuture = mqtt3AsyncClient.subscribeWith()
				.topicFilter(CAR_DEVICE_DEFAULT_TOPIC)
				.qos(MqttQos.AT_LEAST_ONCE)
				.callback(mqtt3Publish -> {
					// Then
					assertThat(mqtt3Publish).satisfies(publish -> {
						assertThat(publish.getTopic().toString()).isEqualTo(CAR_DEVICE_DEFAULT_TOPIC);
						assertThat(publish.getPayloadAsBytes()).isEqualTo("simple-car-payload".getBytes());
						assertThat(publish.getQos()).isEqualTo(MqttQos.AT_MOST_ONCE);
					});
					callbackLatch.countDown();
				})
				.send();
		subscribeFuture.get(10, TimeUnit.SECONDS);
		// When
		carDeviceInputChannel.send(MessageBuilder
				.withPayload("simple-car-payload".getBytes())
				.setHeader(MqttHeaders.QOS, MqttQos.AT_MOST_ONCE)
				.build());
		// Await
		assertThat(callbackLatch.await(10, TimeUnit.SECONDS)).isTrue();
	}

	@Test
	void testCarDeviceMessageHandlerHeaderTopicStringPayload() throws Exception {
		// Given
		String carTopic = "mqtt-v3-outbound-car-header-topic";
		CountDownLatch callbackLatch = new CountDownLatch(1);
		var subscribeFuture = mqtt3AsyncClient.subscribeWith()
				.topicFilter(carTopic)
				.qos(MqttQos.AT_LEAST_ONCE)
				.callback(mqtt3Publish -> {
					// Then
					assertThat(mqtt3Publish).satisfies(publish -> {
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
				.setHeader(MqttHeaders.QOS, MqttQos.AT_LEAST_ONCE)
				.build());
		// Await
		assertThat(callbackLatch.await(10, TimeUnit.SECONDS)).isTrue();
	}

	@Test
	void testCarDeviceMessageHandlerHeaderTopicObjectPayload() throws Exception {
		// Given
		String carTopic = "mqtt-v3-outbound-car-header-topic-object-payload";
		record CarObject(String model, String serialNumber) implements Serializable {

		}

		CarObject carObject = new CarObject("Tesla", "1234");
		CountDownLatch callbackLatch = new CountDownLatch(1);
		var subscribeFuture = mqtt3AsyncClient.subscribeWith()
				.topicFilter(carTopic)
				.qos(MqttQos.AT_LEAST_ONCE)
				.callback(mqtt3Publish -> {
					// Then
					assertThat(mqtt3Publish).satisfies(publish -> {
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
	void testCarDeviceMessageHandlerHeaderTopicMqtt3PublishPayload() throws Exception {
		// Given
		String carTopic = "mqtt-v3-outbound-car-header-topic-mqtt3publish-payload";
		Mqtt3Publish mqtt3Payload = Mqtt3Publish.builder()
				.topic(carTopic)
				.payload("simple-car-payload".getBytes())
				.build();
		CountDownLatch callbackLatch = new CountDownLatch(1);
		var subscribeFuture = mqtt3AsyncClient.subscribeWith()
				.topicFilter(carTopic)
				.qos(MqttQos.AT_LEAST_ONCE)
				.callback(mqtt3Publish -> {
					// Then
					assertThat(mqtt3Publish).isEqualTo(mqtt3Payload);
					callbackLatch.countDown();
				})
				.send();
		subscribeFuture.get(10, TimeUnit.SECONDS);
		// When
		carDeviceInputChannel.send(MessageBuilder.withPayload(mqtt3Payload)
				.setHeader(MqttHeaders.TOPIC, carTopic)
				.build());
		// Await
		assertThat(callbackLatch.await(10, TimeUnit.SECONDS)).isTrue();
	}

	@Test
	void testPetDeviceMessageHandlerAsyncEvent() throws Exception {
		// Given
		String petTopic = "mqtt-v3-outbound-pet-header-topic";
		// When
		petDeviceInputChannel.send(MessageBuilder
				.withPayload("simple-pet-payload")
				.setHeader(MqttHeaders.TOPIC, petTopic)
				.setHeader(MqttHeaders.QOS, MqttQos.AT_LEAST_ONCE)
				.build());
		// Then
		assertThat(petDeviceMessageSentLatch.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(petDeviceMessageDeliveredLatch.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(petDeviceMessageSentEvent)
				.extracting(MqttMessageSentEvent::getMessage)
				.extracting("payload")
				.isEqualTo("simple-pet-payload");
		assertThat(petDeviceMessageDeliveredEvent)
				.extracting(MqttMessageDeliveryEvent::getMqttPublishResult)
				.isInstanceOf(Mqtt3Publish.class);
	}

	@Configuration(proxyBeanMethods = false)
	@EnableIntegration
	static class Config {

		@Bean
		Mqtt3ClientManager mqtt3ClientManager() {
			Mqtt3ClientManager mqtt3ClientManager = new Mqtt3ClientManager(Mqtt3Client.builder()
					.serverHost(MQTT_CONTAINER.getHost())
					.serverPort(MQTT_CONTAINER.getFirstMappedPort())
					.build()
					.getConfig());
			mqtt3ClientManager.addCallback(connectedContext -> connectedLatch.countDown());
			return mqtt3ClientManager;
		}

		@Bean
		Mqtt3MessageHandler carDeviceMessageHandler(Mqtt3ClientManager mqtt3ClientManager) {
			var messageHandler = new Mqtt3MessageHandler(mqtt3ClientManager);
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

			return messageHandler;
		}

		@Bean
		IntegrationFlow carDeviceMessageOutboundAdapter(Mqtt3MessageHandler carDeviceMessageHandler) {
			return flow -> flow.handle(carDeviceMessageHandler);
		}

		@Bean
		Mqtt3MessageHandler petDeviceMessageHandler(Mqtt3ClientManager mqtt3ClientManager) {
			var messageHandler = new Mqtt3MessageHandler(mqtt3ClientManager);
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
		IntegrationFlow petDeviceMessageOutboundAdapter(Mqtt3MessageHandler petDeviceMessageHandler) {
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
