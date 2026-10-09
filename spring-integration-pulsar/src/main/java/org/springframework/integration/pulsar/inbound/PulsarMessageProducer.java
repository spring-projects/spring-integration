/*
 * Copyright 2026-present the original author or authors.
 */

package org.springframework.integration.pulsar.inbound;

import org.apache.pulsar.client.api.Consumer;
import org.apache.pulsar.client.api.Message;
import org.jspecify.annotations.Nullable;

import org.springframework.integration.IntegrationPatternType;
import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.integration.pulsar.support.PulsarIntegrationHeaders;
import org.springframework.integration.support.AbstractIntegrationMessageBuilder;
import org.springframework.pulsar.listener.Acknowledgement;
import org.springframework.pulsar.listener.PulsarAcknowledgingMessageListener;
import org.springframework.pulsar.listener.PulsarMessageListenerContainer;
import org.springframework.pulsar.support.converter.PulsarRecordMessageConverter;
import org.springframework.pulsar.support.header.JsonPulsarHeaderMapper;
import org.springframework.pulsar.support.header.PulsarHeaderMapper;
import org.springframework.util.Assert;

/**
 * A message-driven channel adapter that sends the messages that a
 * {@link PulsarMessageListenerContainer} receives from Apache Pulsar to a message
 * channel.
 * <p>
 * Each Pulsar message becomes a message with the value of the Pulsar message as its
 * payload, or {@link org.springframework.pulsar.support.PulsarNull#INSTANCE} when the
 * value is {@code null}, and the headers that the
 * {@link #setHeaderMapper(PulsarHeaderMapper) header mapper} creates, which include the
 * {@link org.springframework.pulsar.support.PulsarHeaders}. When the container uses the
 * {@link org.springframework.pulsar.listener.AckMode#MANUAL} acknowledgement mode, the
 * {@link Acknowledgement} is available in the
 * {@link PulsarIntegrationHeaders#ACKNOWLEDGMENT} header.
 * <p>
 * When sending a message to the output channel fails, an
 * {@link org.springframework.messaging.support.ErrorMessage} is sent to the
 * {@link #setErrorChannel(org.springframework.messaging.MessageChannel) error channel},
 * if any. Otherwise, the exception is thrown to the container, which applies its error
 * handling, such as to negatively acknowledge the message so that it is redelivered.
 * <p>
 * The adapter supports the record listener containers, not the batch ones.
 *
 * @author Sharang Gupta
 *
 * @since 7.2
 *
 * @see PulsarMessageListenerContainer
 */
public class PulsarMessageProducer extends MessageProducerSupport {

	private final PulsarMessageListenerContainer container;

	private PulsarHeaderMapper headerMapper = JsonPulsarHeaderMapper.builder().build();

	/**
	 * Create an instance that consumes the messages using the provided container.
	 * @param container the container, for example a
	 * {@link org.springframework.pulsar.listener.DefaultPulsarMessageListenerContainer}.
	 */
	public PulsarMessageProducer(PulsarMessageListenerContainer container) {
		Assert.notNull(container, "'container' must not be null");
		this.container = container;
	}

	/**
	 * Set the mapper that converts the Pulsar message properties and metadata to the
	 * message headers. The default is a {@link JsonPulsarHeaderMapper}.
	 * @param headerMapper the mapper.
	 */
	public void setHeaderMapper(PulsarHeaderMapper headerMapper) {
		Assert.notNull(headerMapper, "'headerMapper' must not be null");
		this.headerMapper = headerMapper;
	}

	@Override
	public String getComponentType() {
		return "pulsar:message-driven-channel-adapter";
	}

	@Override
	public IntegrationPatternType getIntegrationPatternType() {
		return IntegrationPatternType.inbound_channel_adapter;
	}

	@Override
	protected void onInit() {
		super.onInit();
		this.container.setupMessageListener(new IntegrationMessageListener(this.headerMapper));
	}

	@Override
	protected void doStart() {
		this.container.start();
	}

	@Override
	protected void doStop() {
		this.container.stop();
	}

	@SuppressWarnings("serial")
	private final class IntegrationMessageListener implements PulsarAcknowledgingMessageListener<Object> {

		private final transient PulsarRecordMessageConverter<Object> converter;

		IntegrationMessageListener(PulsarHeaderMapper headerMapper) {
			this.converter = new PulsarRecordMessageConverter<>(headerMapper);
		}

		@Override
		public void received(Consumer<Object> consumer, Message<Object> record,
				@Nullable Acknowledgement acknowledgement) {

			org.springframework.messaging.Message<?> converted = this.converter.toMessage(record, consumer,
					Object.class);
			AbstractIntegrationMessageBuilder<?> builder = getMessageBuilderFactory()
					.withPayload(converted.getPayload())
					.copyHeaders(converted.getHeaders());
			if (acknowledgement != null) {
				builder.setHeader(PulsarIntegrationHeaders.ACKNOWLEDGMENT, acknowledgement);
			}
			sendMessage(builder.build());
		}

	}

}
