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

package org.springframework.integration.mqtt.client.inbound;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import com.hivemq.client.internal.mqtt.message.publish.mqtt3.Mqtt3PublishViewBuilder;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt3.Mqtt3BlockingClient;
import com.hivemq.client.mqtt.mqtt3.Mqtt3Client;
import com.hivemq.client.mqtt.mqtt3.message.publish.Mqtt3Publish;
import com.hivemq.client.mqtt.mqtt3.message.subscribe.Mqtt3Subscription;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.acks.SimpleAcknowledgment;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.mqtt.client.MqttContainerTest;
import org.springframework.integration.mqtt.client.core.Mqtt3ClientManager;
import org.springframework.integration.mqtt.client.event.MqttSubscribedEvent;
import org.springframework.integration.mqtt.client.support.MqttHeaders;
import org.springframework.messaging.Message;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.MAP;

/**
 * @author Jiandong Ma
 *
 * @since 7.2
 */
@SpringJUnitConfig
@DirtiesContext
class Mqtt3MessageDrivenChannelAdapterTests implements MqttContainerTest {

	static final String CAR_DEVICE_TOPIC_WILDCARD = "mqtt-v3-inbound-car-device/#";

	static final String CAR_DEVICE_TOPIC_1 = "mqtt-v3-inbound-car-device/1";

	static final String CAR_DEVICE_TOPIC_2 = "mqtt-v3-inbound-car-device/2";

	static final CountDownLatch carDeviceWildcardTopicSubscribedLatch = new CountDownLatch(1);

	static final String PET_DEVICE_TOPIC = "mqtt-v3-inbound-pet-device";

	static final CountDownLatch petDeviceTopicSubscribedLatch = new CountDownLatch(1);

	@Autowired
	QueueChannel carDeviceOutputChannel;

	@Autowired
	QueueChannel petDeviceOutputChannel;

	static Mqtt3BlockingClient mqtt3TestClient;

	@BeforeAll
	static void setUp() {
		mqtt3TestClient = Mqtt3Client.builder()
				.identifier("mqtt5-test-client")
				.serverHost(MQTT_CONTAINER.getHost())
				.serverPort(MQTT_CONTAINER.getFirstMappedPort())
				.buildBlocking();
		mqtt3TestClient.connect();
	}

	@Test
	void testCarDeviceWildcardTopic() throws InterruptedException {
		// Ensure subscription done first.
		boolean subscribed = carDeviceWildcardTopicSubscribedLatch.await(10000, TimeUnit.MILLISECONDS);
		assertThat(subscribed).isTrue();
		// Given
		mqtt3TestClient.publishWith().topic(CAR_DEVICE_TOPIC_1)
				.payload("car-payload-1".getBytes())
				.qos(MqttQos.AT_LEAST_ONCE)
				.send();
		mqtt3TestClient.publishWith().topic(CAR_DEVICE_TOPIC_2)
				.payload("car-payload-2".getBytes())
				.qos(MqttQos.AT_MOST_ONCE)
				.send();
		// When
		Message<?> firstCarMessage = carDeviceOutputChannel.receive(10000);
		Message<?> secondCarMessage = carDeviceOutputChannel.receive(10000);
		// Then
		assertThat(firstCarMessage)
				.returns("car-payload-1", Message::getPayload)
				.extracting(Message::getHeaders, MAP)
				.containsEntry(MqttHeaders.RECEIVED_QOS, MqttQos.AT_LEAST_ONCE)
				.containsEntry(MqttHeaders.RECEIVED_TOPIC, CAR_DEVICE_TOPIC_1);
		assertThat(secondCarMessage)
				.returns("car-payload-2", Message::getPayload)
				.extracting(Message::getHeaders, MAP)
				.containsEntry(MqttHeaders.RECEIVED_QOS, MqttQos.AT_MOST_ONCE) // QoS Downgrade
				.containsEntry(MqttHeaders.RECEIVED_TOPIC, CAR_DEVICE_TOPIC_2);
	}

	@Test
	void testPetDeviceTopic() throws InterruptedException {
		// Ensure subscription done first.
		boolean subscribed = petDeviceTopicSubscribedLatch.await(10000, TimeUnit.MILLISECONDS);
		assertThat(subscribed).isTrue();
		// Given
		Mqtt3Publish mqtt3Publish = new Mqtt3PublishViewBuilder.Default()
				.topic(PET_DEVICE_TOPIC)
				.qos(MqttQos.AT_LEAST_ONCE)
				.payload("pet-payload-1".getBytes())
				.build();
		mqtt3TestClient.publish(mqtt3Publish);
		// When
		Message<?> petDeviceMessage = petDeviceOutputChannel.receive(10000);
		// Then
		assertThat(petDeviceMessage)
				.returns("pet-payload-1".getBytes(), Message::getPayload)
				.extracting(m -> m.getHeaders().get(IntegrationMessageHeaderAccessor.ACKNOWLEDGMENT_CALLBACK))
				.asInstanceOf(InstanceOfAssertFactories.type(SimpleAcknowledgment.class))
				.satisfies(SimpleAcknowledgment::acknowledge); // manual ack
	}

	@Configuration(proxyBeanMethods = false)
	@EnableIntegration
	static class Config {

		@Bean
		Mqtt3ClientManager mqtt3ClientManager() {
			return new Mqtt3ClientManager(Mqtt3Client.builder()
					.serverHost(MQTT_CONTAINER.getHost())
					.serverPort(MQTT_CONTAINER.getFirstMappedPort())
					.build()
					.getConfig());
		}

		@Bean
		QueueChannel carDeviceOutputChannel() {
			return new QueueChannel();
		}

		@Bean
		Mqtt3MessageDrivenChannelAdapter carDeviceInboundChannelAdapter(Mqtt3ClientManager mqtt3ClientManager,
				QueueChannel carDeviceOutputChannel) {

			var adapter = new Mqtt3MessageDrivenChannelAdapter(mqtt3ClientManager, CAR_DEVICE_TOPIC_WILDCARD);
			adapter.setOutputChannel(carDeviceOutputChannel);
			adapter.setQos(MqttQos.AT_LEAST_ONCE);
			adapter.setPayloadType(String.class);
			return adapter;
		}

		@Bean
		QueueChannel petDeviceOutputChannel() {
			return new QueueChannel();
		}

		@Bean
		Mqtt3MessageDrivenChannelAdapter petDeviceInboundChannelAdapter(Mqtt3ClientManager mqtt3ClientManager,
				QueueChannel petDeviceOutputChannel, Executor executor) {

			var adapter = new Mqtt3MessageDrivenChannelAdapter(mqtt3ClientManager, Mqtt3Subscription.builder()
					.topicFilter(PET_DEVICE_TOPIC).build());
			adapter.setOutputChannel(petDeviceOutputChannel);
			adapter.setManualAck(true);
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

	}

}
