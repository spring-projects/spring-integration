/*
 * Copyright 2026-present the original author or authors.
 */

package org.springframework.integration.mqtt.client.dsl;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt3.Mqtt3Client;
import com.hivemq.client.mqtt.mqtt3.message.subscribe.Mqtt3Subscription;
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5PayloadFormatIndicator;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish;
import com.hivemq.client.mqtt.mqtt5.message.subscribe.Mqtt5RetainHandling;
import com.hivemq.client.mqtt.mqtt5.message.subscribe.Mqtt5Subscription;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.acks.SimpleAcknowledgment;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.mqtt.client.MqttContainerTest;
import org.springframework.integration.mqtt.client.core.ClientManager;
import org.springframework.integration.mqtt.client.core.Mqtt3ClientManager;
import org.springframework.integration.mqtt.client.core.Mqtt5ClientManager;
import org.springframework.integration.mqtt.client.event.MqttMessageDeliveredEvent;
import org.springframework.integration.mqtt.client.event.MqttMessageDeliveryEvent;
import org.springframework.integration.mqtt.client.event.MqttMessageSentEvent;
import org.springframework.integration.mqtt.client.event.MqttSubscribedEvent;
import org.springframework.integration.mqtt.client.support.MqttHeaderMapper;
import org.springframework.integration.mqtt.client.support.MqttHeaders;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.MAP;

/**
 * @author YeongJae Min
 *
 * @since 7.3
 */
@SpringJUnitConfig
@DirtiesContext
class MqttDslTests implements MqttContainerTest {

	private static final CountDownLatch SUBSCRIBED = new CountDownLatch(4);

	private static final CountDownLatch MQTT3_EXECUTOR_INVOKED = new CountDownLatch(1);

	private static final CountDownLatch MQTT5_EXECUTOR_INVOKED = new CountDownLatch(1);

	private static final BlockingQueue<MqttMessageDeliveryEvent> DELIVERY_EVENTS = new LinkedBlockingQueue<>();

	@Autowired
	ApplicationContext context;

	@BeforeEach
	void awaitSubscriptions() throws InterruptedException {
		assertThat(SUBSCRIBED.await(10, TimeUnit.SECONDS)).isTrue();
	}

	@Test
	void mqtt3RoundTripWithDynamicValuesAndFallbacks() throws InterruptedException {
		MessageChannel input = this.context.getBean("mqtt3Outbound.input", MessageChannel.class);
		input.send(MessageBuilder.withPayload("dynamic payload")
				.setHeader("destination", "dsl/v3/topics")
				.setHeader("quality", MqttQos.AT_LEAST_ONCE)
				.setHeader("retain", false)
				.build());
		assertThat(receive("mqtt3TopicsOutput"))
				.returns("dynamic payload", Message::getPayload)
				.extracting(Message::getHeaders, MAP)
				.containsEntry(MqttHeaders.RECEIVED_TOPIC, "dsl/v3/topics")
				.containsEntry(MqttHeaders.RECEIVED_QOS, MqttQos.AT_LEAST_ONCE);

		input.send(MessageBuilder.withPayload("fallback payload").build());
		Message<?> fallback = receive("mqtt3SubscriptionsOutput");
		assertThat(fallback)
				.returns("fallback payload".getBytes(), Message::getPayload)
				.extracting(Message::getHeaders, MAP)
				.containsEntry(MqttHeaders.RECEIVED_TOPIC, "dsl/v3/subscriptions")
				.containsEntry(MqttHeaders.RECEIVED_QOS, MqttQos.AT_LEAST_ONCE);
		assertThat(fallback.getHeaders().get(IntegrationMessageHeaderAccessor.ACKNOWLEDGMENT_CALLBACK))
				.isInstanceOfSatisfying(SimpleAcknowledgment.class, SimpleAcknowledgment::acknowledge);
		assertThat(MQTT3_EXECUTOR_INVOKED.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(this.context.getBeansOfType(ClientManager.class)).hasSize(2);
	}

	@Test
	void mqtt5PropertiesCustomMappingAndAsyncEvents() throws InterruptedException {
		MessageChannel input = this.context.getBean("mqtt5Outbound.input", MessageChannel.class);
		Message<?> outbound = MessageBuilder.withPayload("mqtt5 payload")
				.setHeader("destination", "dsl/v5/subscriptions")
				.build();
		input.send(outbound);
		Message<?> received = receive("mqtt5SubscriptionsOutput");
		assertThat(received.getHeaders()).containsEntry("mappedBy", "custom")
				.containsEntry("device", "sensor")
				.containsEntry(MqttHeaders.RECEIVED_QOS, MqttQos.AT_LEAST_ONCE);
		assertThat(received.getPayload()).isInstanceOfSatisfying(Mqtt5Publish.class, publish -> {
			assertThat(publish.getPayloadAsBytes()).isEqualTo("mqtt5 payload".getBytes());
			assertThat(publish.getContentType()).hasValueSatisfying(type ->
					assertThat(type.toString()).isEqualTo("text/plain"));
			assertThat(publish.getCorrelationData()).hasValue(ByteBuffer.wrap(new byte[] {1, 2}));
			assertThat(publish.getResponseTopic()).hasValueSatisfying(topic ->
					assertThat(topic.toString()).isEqualTo("dsl/responses"));
			assertThat(publish.getMessageExpiryInterval()).isPresent();
			assertThat(publish.getMessageExpiryInterval().orElseThrow()).isPositive().isLessThanOrEqualTo(60L);
			assertThat(publish.getPayloadFormatIndicator()).hasValue(Mqtt5PayloadFormatIndicator.UTF_8);
		});
		assertThat(received.getHeaders().get(IntegrationMessageHeaderAccessor.ACKNOWLEDGMENT_CALLBACK))
				.isInstanceOfSatisfying(SimpleAcknowledgment.class, SimpleAcknowledgment::acknowledge);
		assertThat(MQTT5_EXECUTOR_INVOKED.await(10, TimeUnit.SECONDS)).isTrue();
		MqttMessageDeliveryEvent first = DELIVERY_EVENTS.poll(10, TimeUnit.SECONDS);
		MqttMessageDeliveryEvent second = DELIVERY_EVENTS.poll(10, TimeUnit.SECONDS);
		assertThat(first).isNotNull();
		assertThat(second).isNotNull();
		assertThat(new Object[] {first, second}).anySatisfy(event ->
				assertThat(event).isInstanceOfSatisfying(MqttMessageSentEvent.class, sent ->
						assertThat(sent.getMessage()).isSameAs(outbound)))
				.anySatisfy(event -> assertThat(event).isInstanceOf(MqttMessageDeliveredEvent.class));

		try {
			input.send(MessageBuilder.withPayload("topic payload")
					.setHeader("destination", "dsl/v5/topics").setHeader("retain", true).build());
			assertThat(receive("mqtt5TopicsOutput"))
					.returns("topic payload", Message::getPayload)
					.extracting(Message::getHeaders, MAP)
					.containsEntry(MqttHeaders.RECEIVED_TOPIC, "dsl/v5/topics")
					.containsEntry(MqttHeaders.RECEIVED_QOS, MqttQos.AT_MOST_ONCE)
					.containsEntry(MqttHeaders.RECEIVED_RETAINED, true);
		}
		finally {
			this.context.getBean(Mqtt5ClientManager.class).getClient().toBlocking().publishWith()
					.topic("dsl/v5/topics").payload(new byte[0]).retain(true).qos(MqttQos.AT_LEAST_ONCE).send();
		}
	}

	private Message<?> receive(String channel) {
		Message<?> received = this.context.getBean(channel, QueueChannel.class).receive(10000);
		assertThat(received).isNotNull();
		return received;
	}

	@Configuration(proxyBeanMethods = false)
	@EnableIntegration
	static class Config {

		@Bean
		Mqtt3ClientManager mqtt3ClientManager() {
			return new Mqtt3ClientManager(Mqtt3Client.builder()
					.serverHost(MQTT_CONTAINER.getHost())
					.serverPort(MQTT_CONTAINER.getFirstMappedPort()).build().getConfig());
		}

		@Bean
		Mqtt5ClientManager mqtt5ClientManager() {
			return new Mqtt5ClientManager(Mqtt5Client.builder()
					.serverHost(MQTT_CONTAINER.getHost())
					.serverPort(MQTT_CONTAINER.getFirstMappedPort()).build().getConfig());
		}

		@Bean
		IntegrationFlow mqtt3Topics(Mqtt3ClientManager manager) {
			return IntegrationFlow.from(Mqtt3.inboundAdapter(manager, "dsl/v3/topics")
					.qos(MqttQos.AT_LEAST_ONCE)
					.messageConverter(new StringMessageConverter())
					.payloadType(String.class))
					.channel(channels -> channels.queue("mqtt3TopicsOutput"))
					.get();
		}

		@Bean
		IntegrationFlow mqtt3Subscriptions(Mqtt3ClientManager manager) {
			return IntegrationFlow.from(Mqtt3.inboundAdapter(manager, Mqtt3Subscription.builder()
					.topicFilter("dsl/v3/subscriptions").qos(MqttQos.AT_LEAST_ONCE).build())
					.qos(MqttQos.AT_MOST_ONCE).manualAck(true).executor(command -> {
						MQTT3_EXECUTOR_INVOKED.countDown();
						command.run();
					}))
					.channel(channels -> channels.queue("mqtt3SubscriptionsOutput"))
					.get();
		}

		@Bean
		IntegrationFlow mqtt3Outbound(Mqtt3ClientManager manager) {
			return flow -> flow.handle(Mqtt3.outboundAdapter(manager)
					.defaultTopic("dsl/v3/subscriptions").defaultQos(MqttQos.EXACTLY_ONCE)
					.defaultRetained(false)
					.topicExpression("headers['destination']")
					.qosFunction(message -> message.getHeaders().get("quality", MqttQos.class))
					.retainedExpression("headers['retain']"));
		}

		@Bean
		IntegrationFlow mqtt5Topics(Mqtt5ClientManager manager) {
			return IntegrationFlow.from(Mqtt5.inboundAdapter(manager, "dsl/v5/topics")
					.qos(MqttQos.AT_MOST_ONCE).payloadType(String.class)
					.noLocal(false).retainHandling(Mqtt5RetainHandling.DO_NOT_SEND).retainAsPublished(true))
					.channel(channels -> channels.queue("mqtt5TopicsOutput"))
					.get();
		}

		@Bean
		IntegrationFlow mqtt5Subscriptions(Mqtt5ClientManager manager) {
			return IntegrationFlow.from(Mqtt5.inboundAdapter(manager, Mqtt5Subscription.builder()
					.topicFilter("dsl/v5/subscriptions").qos(MqttQos.AT_LEAST_ONCE).noLocal(false).build())
					.qos(MqttQos.AT_MOST_ONCE).manualAck(true).executor(command -> {
						MQTT5_EXECUTOR_INVOKED.countDown();
						command.run();
					})
					.payloadType(Mqtt5Publish.class).noLocal(true)
					.retainHandling(Mqtt5RetainHandling.DO_NOT_SEND).retainAsPublished(true)
					.headerMapper(new MqttHeaderMapper() {

						@Override
						public Map<String, Object> toHeaders(Mqtt5Publish publish) {
							Map<String, Object> headers = super.toHeaders(publish);
							headers.put("mappedBy", "custom");
							return headers;
						}

					}))
					.channel(channels -> channels.queue("mqtt5SubscriptionsOutput"))
					.get();
		}

		@Bean
		IntegrationFlow mqtt5Outbound(Mqtt5ClientManager manager) {
			return flow -> flow.handle(Mqtt5.outboundAdapter(manager)
					.async(true).defaultQos(MqttQos.AT_LEAST_ONCE)
					.retainedFunction(message -> message.getHeaders().get("retain", Boolean.class))
					.topicFunction(message -> message.getHeaders().get("destination", String.class))
					.contentTypeFunction(message -> "text/plain")
					.correlationDataFunction(message -> new byte[] {1, 2})
					.responseTopicFunction(message -> "dsl/responses")
					.messageExpiryIntervalFunction(message -> 60L)
					.payloadFormatIndicatorFunction(message -> Mqtt5PayloadFormatIndicator.UTF_8)
					.userPropertiesFunction(message -> Map.of("device", "sensor")));
		}

		@EventListener
		void subscribed(MqttSubscribedEvent event) {
			SUBSCRIBED.countDown();
		}

		@EventListener
		void delivered(MqttMessageDeliveryEvent event) {
			DELIVERY_EVENTS.add(event);
		}

	}

}
