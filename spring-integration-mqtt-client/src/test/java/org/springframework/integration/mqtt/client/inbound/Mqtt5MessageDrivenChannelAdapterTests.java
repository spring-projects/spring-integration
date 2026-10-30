/*
 * Copyright 2026-present the original author or authors.
 */

package org.springframework.integration.mqtt.client.inbound;

import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.hivemq.client.internal.mqtt.datatypes.MqttUserPropertyImpl;
import com.hivemq.client.internal.mqtt.message.publish.MqttPublishBuilder;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt5.Mqtt5BlockingClient;
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;
import com.hivemq.client.mqtt.mqtt5.datatypes.Mqtt5UserProperties;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish;
import com.hivemq.client.mqtt.mqtt5.message.subscribe.Mqtt5Subscription;
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
import org.springframework.integration.mqtt.client.core.Mqtt5ClientManager;
import org.springframework.integration.mqtt.client.event.MqttMessageConversionFailedEvent;
import org.springframework.integration.mqtt.client.event.MqttSubscribedEvent;
import org.springframework.integration.mqtt.client.support.MqttHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.InstanceOfAssertFactories.MAP;

/**
 * @author Jiandong Ma
 * @author WooSeung Kim
 *
 * @since 7.2
 */
@SpringJUnitConfig
@DirtiesContext
class Mqtt5MessageDrivenChannelAdapterTests implements MqttContainerTest {

	static final String CAR_DEVICE_TOPIC_WILDCARD = "mqtt-v5-inbound-car-device/#";

	static final String CAR_DEVICE_TOPIC_1 = "mqtt-v5-inbound-car-device/1";

	static final String CAR_DEVICE_TOPIC_2 = "mqtt-v5-inbound-car-device/2";

	static final CountDownLatch carDeviceWildcardTopicSubscribedLatch = new CountDownLatch(1);

	static final String PET_DEVICE_TOPIC = "mqtt-v5-inbound-pet-device";

	static final CountDownLatch petDeviceTopicSubscribedLatch = new CountDownLatch(1);

	static final String ALARM_DEVICE_TOPIC = "mqtt-v5-inbound-alarm-device";

	static final CountDownLatch alarmDeviceTopicSubscribedLatch = new CountDownLatch(1);

	static final String LAMP_DEVICE_TOPIC = "mqtt-v5-inbound-lamp-device";

	static final CountDownLatch lampDeviceTopicSubscribedLatch = new CountDownLatch(1);

	static final BlockingQueue<MqttMessageConversionFailedEvent> lampDeviceConversionFailedEvents =
			new LinkedBlockingQueue<>();

	@Autowired
	QueueChannel carDeviceOutputChannel;

	@Autowired
	QueueChannel petDeviceOutputChannel;

	@Autowired
	QueueChannel alarmDeviceOutputChannel;

	static Mqtt5BlockingClient mqtt5TestClient;

	@BeforeAll
	static void setUp() {
		mqtt5TestClient = Mqtt5Client.builder()
				.identifier("mqtt5-test-client")
				.serverHost(MQTT_CONTAINER.getHost())
				.serverPort(MQTT_CONTAINER.getFirstMappedPort())
				.buildBlocking();
		mqtt5TestClient.connect();
	}

	@Test
	void testCarDeviceWildcardTopic() throws InterruptedException {
		// Ensure subscription done first.
		boolean subscribed = carDeviceWildcardTopicSubscribedLatch.await(10000, TimeUnit.MILLISECONDS);
		assertThat(subscribed).isTrue();
		// Given
		mqtt5TestClient.publishWith().topic(CAR_DEVICE_TOPIC_1)
				.payload("car-payload-1".getBytes())
				.qos(MqttQos.AT_LEAST_ONCE)
				.contentType("text/plain")
				.correlationData("correlation-id".getBytes())
				.userProperties(Mqtt5UserProperties.of(MqttUserPropertyImpl.of("custom-prop", "custom-val")))
				.send();
		mqtt5TestClient.publishWith().topic(CAR_DEVICE_TOPIC_2)
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
				.containsEntry(MqttHeaders.RECEIVED_TOPIC, CAR_DEVICE_TOPIC_1)
				.containsEntry(MessageHeaders.CONTENT_TYPE, "text/plain")
				.containsEntry(MqttHeaders.CORRELATION_DATA, ByteBuffer.wrap("correlation-id".getBytes()))
				.containsEntry("custom-prop", "custom-val");
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
		Mqtt5Publish mqtt5Publish = new MqttPublishBuilder.Default()
				.topic(PET_DEVICE_TOPIC)
				.qos(MqttQos.AT_LEAST_ONCE)
				.payload("pet-payload-1".getBytes())
				.build();
		mqtt5TestClient.publish(mqtt5Publish);
		// When
		Message<?> petDeviceMessage = petDeviceOutputChannel.receive(10000);
		// Then
		assertThat(petDeviceMessage)
				.returns("pet-payload-1".getBytes(), Message::getPayload)
				.extracting(m -> m.getHeaders().get(IntegrationMessageHeaderAccessor.ACKNOWLEDGMENT_CALLBACK))
				.asInstanceOf(InstanceOfAssertFactories.type(SimpleAcknowledgment.class))
				.satisfies(SimpleAcknowledgment::acknowledge);  // manual ack
	}

	@Test
	void testSubscriptionSurvivesUnhandledException() throws InterruptedException {
		// Ensure subscription done first.
		boolean subscribed = alarmDeviceTopicSubscribedLatch.await(10000, TimeUnit.MILLISECONDS);
		assertThat(subscribed).isTrue();
		// Given
		mqtt5TestClient.publishWith().topic(ALARM_DEVICE_TOPIC)
				.payload("rejected-alarm".getBytes())
				.qos(MqttQos.AT_LEAST_ONCE)
				.send();
		mqtt5TestClient.publishWith().topic(ALARM_DEVICE_TOPIC)
				.payload("accepted-alarm".getBytes())
				.qos(MqttQos.AT_LEAST_ONCE)
				.send();
		// When
		Message<?> alarmDeviceMessage = alarmDeviceOutputChannel.receive(10000);
		// Then
		assertThat(alarmDeviceMessage)
				.extracting(Message::getPayload)
				.isEqualTo("accepted-alarm");
	}

	@Test
	void testConversionFailureIsAcknowledgedAndEmitted() throws InterruptedException {
		// Ensure subscription done first.
		boolean subscribed = lampDeviceTopicSubscribedLatch.await(10000, TimeUnit.MILLISECONDS);
		assertThat(subscribed).isTrue();
		// Given
		mqtt5TestClient.publishWith().topic(LAMP_DEVICE_TOPIC)
				.payload("lamp-payload".getBytes())
				.qos(MqttQos.AT_LEAST_ONCE)
				.send();
		// When
		MqttMessageConversionFailedEvent event = lampDeviceConversionFailedEvents.poll(10, TimeUnit.SECONDS);
		// Then
		assertThat(event)
				.extracting(MqttMessageConversionFailedEvent::getCause)
				.isInstanceOf(MessageConversionException.class);
		assertThat(event)
				.extracting(MqttMessageConversionFailedEvent::getMqttPublish)
				.asInstanceOf(InstanceOfAssertFactories.type(Mqtt5Publish.class))
				.returns("lamp-payload".getBytes(), Mqtt5Publish::getPayloadAsBytes)
				// already acknowledged by the adapter
				.satisfies(mqttPublish -> assertThatIllegalStateException().isThrownBy(mqttPublish::acknowledge));
	}

	// No message converter in this test can produce this type from the MQTT payload
	record LampState(String state) {

	}

	@Configuration(proxyBeanMethods = false)
	@EnableIntegration
	static class Config {

		@Bean
		Mqtt5ClientManager mqtt5ClientManager() {
			return new Mqtt5ClientManager(Mqtt5Client.builder()
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
		Mqtt5MessageDrivenChannelAdapter carDeviceInboundChannelAdapter(Mqtt5ClientManager mqtt5ClientManager,
				QueueChannel carDeviceOutputChannel) {

			var adapter = new Mqtt5MessageDrivenChannelAdapter(mqtt5ClientManager, CAR_DEVICE_TOPIC_WILDCARD);
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
		Mqtt5MessageDrivenChannelAdapter petDeviceInboundChannelAdapter(Mqtt5ClientManager mqtt5ClientManager,
				QueueChannel petDeviceOutputChannel, Executor executor) {

			var adapter = new Mqtt5MessageDrivenChannelAdapter(mqtt5ClientManager, Mqtt5Subscription.builder()
					.topicFilter(PET_DEVICE_TOPIC).build());
			adapter.setOutputChannel(petDeviceOutputChannel);
			adapter.setManualAck(true);
			adapter.setExecutor(executor);
			return adapter;
		}

		@Bean
		QueueChannel alarmDeviceOutputChannel() {
			return new QueueChannel();
		}

		@Bean
		Mqtt5MessageDrivenChannelAdapter alarmDeviceInboundChannelAdapter(Mqtt5ClientManager mqtt5ClientManager,
				QueueChannel alarmDeviceOutputChannel) {

			var adapter = new Mqtt5MessageDrivenChannelAdapter(mqtt5ClientManager, ALARM_DEVICE_TOPIC);
			adapter.setPayloadType(String.class);
			adapter.setOutputChannel((message, timeout) -> {
				if ("rejected-alarm".equals(message.getPayload())) {
					throw new IllegalStateException("The alarm is rejected");
				}
				return alarmDeviceOutputChannel.send(message, timeout);
			});
			return adapter;
		}

		@Bean
		Mqtt5MessageDrivenChannelAdapter lampDeviceInboundChannelAdapter(Mqtt5ClientManager mqtt5ClientManager) {
			var adapter = new Mqtt5MessageDrivenChannelAdapter(mqtt5ClientManager, LAMP_DEVICE_TOPIC);
			adapter.setPayloadType(LampState.class);
			adapter.setManualAck(true);
			adapter.setOutputChannel(new QueueChannel());
			return adapter;
		}

		@EventListener
		void mqttEvents(MqttSubscribedEvent event) {
			String beanName = ((Mqtt5MessageDrivenChannelAdapter) event.getSource()).getBeanName();
			if (beanName.equals("carDeviceInboundChannelAdapter")) {
				carDeviceWildcardTopicSubscribedLatch.countDown();
			}
			else if (beanName.equals("petDeviceInboundChannelAdapter")) {
				petDeviceTopicSubscribedLatch.countDown();
			}
			else if (beanName.equals("alarmDeviceInboundChannelAdapter")) {
				alarmDeviceTopicSubscribedLatch.countDown();
			}
			else if (beanName.equals("lampDeviceInboundChannelAdapter")) {
				lampDeviceTopicSubscribedLatch.countDown();
			}
		}

		@EventListener
		void conversionFailedEvents(MqttMessageConversionFailedEvent event) {
			lampDeviceConversionFailedEvents.add(event);
		}

	}

}
