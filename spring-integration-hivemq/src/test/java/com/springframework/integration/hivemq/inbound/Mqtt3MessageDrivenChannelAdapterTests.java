/*
 * Copyright 2026-present the original author or authors.
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

package com.springframework.integration.hivemq.inbound;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import com.hivemq.client.internal.mqtt.message.publish.mqtt3.Mqtt3PublishViewBuilder;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient;
import com.hivemq.client.mqtt.mqtt3.Mqtt3Client;
import com.hivemq.client.mqtt.mqtt3.message.publish.Mqtt3Publish;
import com.springframework.integration.hivemq.HiveMQContainer;
import com.springframework.integration.hivemq.event.MqttSubscribedEvent;
import com.springframework.integration.hivemq.support.MqttHeaders;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.AbstractMessageConverter;
import org.springframework.messaging.converter.SmartMessageConverter;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * @author Jiandong Ma
 *
 * @since 7.2
 */
@SpringJUnitConfig
@DirtiesContext
class Mqtt3MessageDrivenChannelAdapterTests implements HiveMQContainer {

	static final String CAR_DEVICE_TOPIC_WILDCARD = "mqtt-v3-inbound-car-device/#";

	static final String CAR_DEVICE_TOPIC_1 = "mqtt-v3-inbound-car-device/1";

	static final String CAR_DEVICE_TOPIC_2 = "mqtt-v3-inbound-car-device/2";

	static final CountDownLatch carDeviceWildcardTopicSubscribedLatch = new CountDownLatch(1);

	static final String PET_DEVICE_TOPIC = "mqtt-v3-inbound-pet-device";

	static final CountDownLatch petDeviceTopicSubscribedLatch = new CountDownLatch(1);

	@Autowired
	Mqtt3AsyncClient mqtt3AsyncClient;

	@Autowired
	QueueChannel carDeviceOutputChannel;

	@Autowired
	QueueChannel petDeviceOutputChannel;

	@Test
	void testCarDeviceWildcardTopic() throws InterruptedException {
		// Ensure subscription done first.
		boolean subscribed = carDeviceWildcardTopicSubscribedLatch.await(10000, TimeUnit.MILLISECONDS);
		Assertions.assertThat(subscribed).isTrue();
		// Given
		mqtt3AsyncClient.publishWith().topic(CAR_DEVICE_TOPIC_1)
				.payload("car-payload-1".getBytes())
				.qos(MqttQos.AT_LEAST_ONCE)
				.send()
				.orTimeout(10000, TimeUnit.MILLISECONDS)
				.join();
		mqtt3AsyncClient.publishWith().topic(CAR_DEVICE_TOPIC_2)
				.payload("car-payload-2".getBytes())
				.qos(MqttQos.AT_MOST_ONCE)
				.send()
				.orTimeout(10000, TimeUnit.MILLISECONDS)
				.join();
		// When
		Message<?> firstCarMessage = carDeviceOutputChannel.receive(10000);
		Message<?> secondCarMessage = carDeviceOutputChannel.receive(10000);
		// Then
		Assertions.assertThat(firstCarMessage)
				.isNotNull()
				.returns("car-payload-1", Message::getPayload)
				.extracting(Message::getHeaders)
				.asInstanceOf(InstanceOfAssertFactories.type(MessageHeaders.class))
				.satisfies(headers -> Assertions.assertThat(headers)
						.containsEntry(MqttHeaders.RECEIVED_QOS, MqttQos.AT_LEAST_ONCE)
						.containsEntry(MqttHeaders.RECEIVED_TOPIC, CAR_DEVICE_TOPIC_1));
		Assertions.assertThat(secondCarMessage)
				.isNotNull()
				.returns("car-payload-2", Message::getPayload)
				.extracting(Message::getHeaders)
				.asInstanceOf(InstanceOfAssertFactories.type(MessageHeaders.class))
				.satisfies(headers -> Assertions.assertThat(headers)
						.containsEntry(MqttHeaders.RECEIVED_QOS, MqttQos.AT_MOST_ONCE) // QoS Downgrade
						.containsEntry(MqttHeaders.RECEIVED_TOPIC, CAR_DEVICE_TOPIC_2));
	}

	@Test
	void testPetDeviceTopic() throws InterruptedException {
		// Ensure subscription done first.
		boolean subscribed = petDeviceTopicSubscribedLatch.await(10000, TimeUnit.MILLISECONDS);
		Assertions.assertThat(subscribed).isTrue();
		// Given
		Mqtt3Publish mqtt3Publish = new Mqtt3PublishViewBuilder.Default()
				.topic(PET_DEVICE_TOPIC)
				.qos(MqttQos.AT_LEAST_ONCE)
				.payload("pet-payload-1".getBytes())
				.build();
		mqtt3AsyncClient.publish(mqtt3Publish)
				.orTimeout(10000, TimeUnit.MILLISECONDS)
				.join();
		// When
		Message<?> petDeviceMessage = petDeviceOutputChannel.receive(10000);
		// Then
		Assertions.assertThat(petDeviceMessage)
				.isNotNull()
				.returns("pet-payload-1".getBytes(), Message::getPayload)
				.extracting(m -> m.getHeaders().get(IntegrationMessageHeaderAccessor.ACKNOWLEDGMENT_CALLBACK))
				.isNotNull()
				.asInstanceOf(InstanceOfAssertFactories.type(Mqtt3Publish.class))
				.satisfies(mqttPublish -> {
					Assertions.assertThat(mqttPublish).isEqualTo(mqtt3Publish);
					mqttPublish.acknowledge();  // manual ack
				});
	}

	@Configuration(proxyBeanMethods = false)
	@EnableIntegration
	static class Config {

		@Bean
		Mqtt3AsyncClient mqtt3AsyncClient() {
			return Mqtt3Client.builder()
					.serverHost(HIVEMQ_CONTAINER.getHost())
					.serverPort(HIVEMQ_CONTAINER.getFirstMappedPort())
					.buildAsync();
		}

		@Bean
		QueueChannel carDeviceOutputChannel() {
			return new QueueChannel();
		}

		@Bean
		Mqtt3MessageDrivenChannelAdapter carDeviceInboundChannelAdapter(Mqtt3AsyncClient mqtt3AsyncClient,
				QueueChannel carDeviceOutputChannel, SmartMessageConverter mqttStringToBytesConverter) {
			var adapter = new Mqtt3MessageDrivenChannelAdapter(mqtt3AsyncClient, CAR_DEVICE_TOPIC_WILDCARD);
			adapter.setOutputChannel(carDeviceOutputChannel);
			adapter.setQos(MqttQos.AT_LEAST_ONCE);
			adapter.setPayloadType(String.class);
			adapter.setMessageConverter(mqttStringToBytesConverter);
			return adapter;
		}

		@Bean
		QueueChannel petDeviceOutputChannel() {
			return new QueueChannel();
		}

		@Bean
		Mqtt3MessageDrivenChannelAdapter petDeviceInboundChannelAdapter(Mqtt3AsyncClient mqtt3AsyncClient,
				QueueChannel petDeviceOutputChannel, Executor executor) {
			var adapter = new Mqtt3MessageDrivenChannelAdapter(mqtt3AsyncClient, PET_DEVICE_TOPIC);
			adapter.setOutputChannel(petDeviceOutputChannel);
			adapter.setManualAcknowledgement(true);
			adapter.setExecutor(executor);
			return adapter;
		}

		@EventListener
		void mqttEvents(MqttSubscribedEvent event) {
			String beanName = ((Mqtt3MessageDrivenChannelAdapter) event.getSource()).getBeanName();
			if (beanName.equals("carDeviceInboundChannelAdapter")) {
				carDeviceWildcardTopicSubscribedLatch.countDown();
			}
			else if (beanName.equals("petDeviceInboundChannelAdapter")) {
				petDeviceTopicSubscribedLatch.countDown();
			}
		}

		@Bean
		public SmartMessageConverter mqttStringToBytesConverter() {
			return new AbstractMessageConverter() {

				@Override
				protected boolean supports(Class<?> clazz) {
					return true;
				}

				@Override
				protected Object convertFromInternal(Message<?> message, Class<?> targetClass,
						Object conversionHint) {

					return message.getPayload().toString().getBytes(StandardCharsets.UTF_8);
				}

				@Override
				protected Object convertToInternal(Object payload, MessageHeaders headers,
						Object conversionHint) {

					return new String((byte[]) payload);
				}

			};
		}

	}

}
