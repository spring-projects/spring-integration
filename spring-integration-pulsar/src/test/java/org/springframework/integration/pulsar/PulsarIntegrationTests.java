/*
 * Copyright 2026-present the original author or authors.
 */

package org.springframework.integration.pulsar;

import java.util.List;
import java.util.Set;

import org.apache.pulsar.client.api.MessageId;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.PulsarClientException;
import org.apache.pulsar.client.api.Schema;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.pulsar.dsl.Pulsar;
import org.springframework.integration.pulsar.support.PulsarIntegrationHeaders;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.pulsar.core.DefaultPulsarConsumerFactory;
import org.springframework.pulsar.core.DefaultPulsarProducerFactory;
import org.springframework.pulsar.core.PulsarConsumerFactory;
import org.springframework.pulsar.core.PulsarTemplate;
import org.springframework.pulsar.listener.AckMode;
import org.springframework.pulsar.listener.Acknowledgement;
import org.springframework.pulsar.listener.DefaultPulsarMessageListenerContainer;
import org.springframework.pulsar.listener.PulsarContainerProperties;
import org.springframework.pulsar.support.PulsarHeaders;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the Apache Pulsar channel adapters against a real broker.
 *
 * @author Sharang Gupta
 *
 * @since 7.2
 */
@SpringJUnitConfig
@DirtiesContext
class PulsarIntegrationTests implements PulsarContainerTest {

	static final String ORDERS_TOPIC = "integration-orders";

	static final String MANUAL_ACK_TOPIC = "integration-manual-ack";

	static final String SUBSCRIPTION = "integration-subscription";

	@Autowired
	MessageChannel toPulsar;

	@Autowired
	QueueChannel sendResults;

	@Autowired
	QueueChannel fromPulsar;

	@Autowired
	QueueChannel fromPulsarWithManualAck;

	@Autowired
	PulsarTemplate<String> pulsarTemplate;

	@Test
	void messageSentByTheOutboundAdapterIsReceivedByTheMessageDrivenAdapter() {
		this.toPulsar.send(MessageBuilder.withPayload("hello")
				.setHeader(PulsarHeaders.KEY, "customer-1")
				.setHeader("trace-id", "abc")
				.build());

		Message<?> sent = this.sendResults.receive(30_000);
		assertThat(sent).isNotNull();
		assertThat(sent.getHeaders().get(PulsarHeaders.MESSAGE_ID)).isInstanceOf(MessageId.class);

		Message<?> received = this.fromPulsar.receive(30_000);
		assertThat(received).isNotNull();
		assertThat(received.getPayload()).isEqualTo("hello");
		assertThat(received.getHeaders()).containsEntry(PulsarHeaders.KEY, "customer-1")
				.containsEntry("trace-id", "abc")
				.doesNotContainKey(PulsarIntegrationHeaders.ACKNOWLEDGMENT);
		assertThat((String) received.getHeaders().get(PulsarHeaders.TOPIC_NAME)).endsWith(ORDERS_TOPIC);
	}

	@Test
	void acknowledgementIsAvailableWhenTheContainerUsesTheManualAcknowledgementMode() throws Exception {
		this.pulsarTemplate.newMessage("needs-ack").withTopic(MANUAL_ACK_TOPIC).send();

		Message<?> received = this.fromPulsarWithManualAck.receive(30_000);
		assertThat(received).isNotNull();
		assertThat(received.getPayload()).isEqualTo("needs-ack");
		assertThat(received.getHeaders().get(PulsarIntegrationHeaders.ACKNOWLEDGMENT))
				.isInstanceOf(Acknowledgement.class)
				.satisfies((acknowledgement) -> ((Acknowledgement) acknowledgement).acknowledge());
	}

	@Configuration
	@EnableIntegration
	static class Config {

		@Bean
		PulsarClient pulsarClient() throws PulsarClientException {
			PulsarClient client = PulsarClient.builder().serviceUrl(PULSAR_CONTAINER.getPulsarBrokerUrl()).build();
			// A topic without subscriptions discards its messages: create them before sending
			for (String topic : new String[] { ORDERS_TOPIC, MANUAL_ACK_TOPIC }) {
				client.newConsumer(Schema.STRING).topic(topic).subscriptionName(SUBSCRIPTION).subscribe().close();
			}
			return client;
		}

		@Bean
		PulsarTemplate<String> pulsarTemplate(PulsarClient pulsarClient) {
			return new PulsarTemplate<>(new DefaultPulsarProducerFactory<>(pulsarClient, ORDERS_TOPIC));
		}

		@Bean
		PulsarConsumerFactory<String> pulsarConsumerFactory(PulsarClient pulsarClient) {
			return new DefaultPulsarConsumerFactory<>(pulsarClient, List.of());
		}

		@Bean
		DefaultPulsarMessageListenerContainer<String> ordersContainer(
				PulsarConsumerFactory<String> pulsarConsumerFactory) {

			return container(pulsarConsumerFactory, ORDERS_TOPIC, AckMode.RECORD);
		}

		@Bean
		DefaultPulsarMessageListenerContainer<String> manualAckContainer(
				PulsarConsumerFactory<String> pulsarConsumerFactory) {

			return container(pulsarConsumerFactory, MANUAL_ACK_TOPIC, AckMode.MANUAL);
		}

		private static DefaultPulsarMessageListenerContainer<String> container(
				PulsarConsumerFactory<String> pulsarConsumerFactory, String topic, AckMode ackMode) {

			// A single String argument would be taken as a topic pattern: use setTopics()
			PulsarContainerProperties properties = new PulsarContainerProperties();
			properties.setTopics(Set.of(topic));
			properties.setSubscriptionName(SUBSCRIPTION);
			properties.setSchema(Schema.STRING);
			properties.setAckMode(ackMode);
			return new DefaultPulsarMessageListenerContainer<>(pulsarConsumerFactory, properties);
		}

		@Bean
		QueueChannel sendResults() {
			return new QueueChannel();
		}

		@Bean
		QueueChannel fromPulsar() {
			return new QueueChannel();
		}

		@Bean
		QueueChannel fromPulsarWithManualAck() {
			return new QueueChannel();
		}

		@Bean
		IntegrationFlow toPulsarFlow(PulsarTemplate<String> pulsarTemplate) {
			return IntegrationFlow.from("toPulsar")
					.handle(Pulsar.outboundAdapter(pulsarTemplate)
							.topic(ORDERS_TOPIC)
							.sendSuccessChannelName("sendResults"))
					.get();
		}

		@Bean
		IntegrationFlow fromPulsarFlow(DefaultPulsarMessageListenerContainer<String> ordersContainer) {
			return IntegrationFlow.from(Pulsar.messageDrivenChannelAdapter(ordersContainer))
					.channel("fromPulsar")
					.get();
		}

		@Bean
		IntegrationFlow fromPulsarWithManualAckFlow(
				DefaultPulsarMessageListenerContainer<String> manualAckContainer) {

			return IntegrationFlow.from(Pulsar.messageDrivenChannelAdapter(manualAckContainer))
					.channel("fromPulsarWithManualAck")
					.get();
		}

	}

}
