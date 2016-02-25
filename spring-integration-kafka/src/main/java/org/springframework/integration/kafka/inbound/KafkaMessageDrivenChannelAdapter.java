/*
 * Copyright 2015-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.kafka.inbound;

import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerRecord;

import org.springframework.integration.context.OrderlyShutdownCapable;
import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.integration.support.DefaultMessageBuilderFactory;
import org.springframework.integration.support.MessageBuilderFactory;
import org.springframework.integration.support.MutableMessageBuilderFactory;
import org.springframework.kafka.listener.AbstractMessageListenerContainer;
import org.springframework.kafka.listener.AbstractMessageListenerContainer.AckMode;
import org.springframework.kafka.listener.AcknowledgingMessageListener;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.Assert;

/**
 * @author Marius Bogoevici
 * @author Gary Russell
 *
 * TODO: Use the MessagingMessageConverter from spring-kafka
 */
public class KafkaMessageDrivenChannelAdapter<K, V> extends MessageProducerSupport implements OrderlyShutdownCapable {

	private final AbstractMessageListenerContainer<K, V> messageListenerContainer;

	private boolean generateMessageId = false;

	private boolean generateTimestamp = false;

	private boolean useMessageBuilderFactory = false;

	public KafkaMessageDrivenChannelAdapter(AbstractMessageListenerContainer<K, V> messageListenerContainer) {
		Assert.notNull(messageListenerContainer, "messageListenerContainer is required");
		Assert.isNull(messageListenerContainer.getMessageListener(), "Container must not already have a listener");
		this.messageListenerContainer = messageListenerContainer;
		this.messageListenerContainer.setAutoStartup(false);
	}

	/**
	 * Generate {@link Message} {@code ids} for produced messages. If set to {@code false}
	 * , will try to use a default value. By default set to {@code false}. Note that this
	 * option is only guaranteed to work when {@link #setUseMessageBuilderFactory(boolean)
	 * useMessageBuilderFactory} is false (default). If the latter is set to {@code true},
	 * then some {@link MessageBuilderFactory} implementations such as
	 * {@link DefaultMessageBuilderFactory} may ignore it.
	 * @param generateMessageId true if a message id should be generated
	 * @since 1.1
	 */
	public void setGenerateMessageId(boolean generateMessageId) {
		this.generateMessageId = generateMessageId;
	}

	/**
	 * Generate {@code timestamp} for produced messages. If set to {@code false}, -1 is
	 * used instead. By default set to {@code false}. Note that this option is only
	 * guaranteed to work when {@link #setUseMessageBuilderFactory(boolean)
	 * useMessageBuilderFactory} is false (default). If the latter is set to {@code true},
	 * then some {@link MessageBuilderFactory} implementations such as
	 * {@link DefaultMessageBuilderFactory} may ignore it.
	 * @param generateTimestamp true if a timestamp should be generated
	 * @since 1.1
	 */
	public void setGenerateTimestamp(boolean generateTimestamp) {
		this.generateTimestamp = generateTimestamp;
	}

	/**
	 * Use the {@link MessageBuilderFactory} returned by
	 * {@link #getMessageBuilderFactory()} to create messages.
	 * @param useMessageBuilderFactory true if the {@link MessageBuilderFactory} returned
	 * by {@link #getMessageBuilderFactory()} should be used.
	 * @since 1.1
	 */
	public void setUseMessageBuilderFactory(boolean useMessageBuilderFactory) {
		this.useMessageBuilderFactory = useMessageBuilderFactory;
	}

	@Override
	protected void onInit() {
		this.messageListenerContainer.setMessageListener(
				!AckMode.MANUAL.equals(this.messageListenerContainer.getAckMode())
					? new AutoAcknowledgingChannelForwardingMessageListener()
					: new AcknowledgingChannelForwardingMessageListener());
		if (!this.generateMessageId && !this.generateTimestamp
				&& (getMessageBuilderFactory() instanceof DefaultMessageBuilderFactory)) {
			setMessageBuilderFactory(new MutableMessageBuilderFactory());
		}
		super.onInit();
	}

	@Override
	protected void doStart() {
		this.messageListenerContainer.start();
	}

	@Override
	protected void doStop() {
		this.messageListenerContainer.stop();
	}

	@Override
	public String getComponentType() {
		return "kafka:message-driven-channel-adapter";
	}

	@Override
	public int beforeShutdown() {
		this.messageListenerContainer.stop();
		return getPhase();
	}

	@Override
	public int afterShutdown() {
		return getPhase();
	}

	private class AutoAcknowledgingChannelForwardingMessageListener implements MessageListener<K, V> {

		@Override
		public void onMessage(ConsumerRecord<K, V> record) {
			sendMessage(toMessage(record, null));
		}

	}

	private class AcknowledgingChannelForwardingMessageListener implements AcknowledgingMessageListener<K, V> {

		@Override
		public void onMessage(ConsumerRecord<K, V> record, Acknowledgment acknowledgment) {
			sendMessage(toMessage(record, acknowledgment));
		}

	}

	private Message<V> toMessage(ConsumerRecord<K, V> record, Acknowledgment acknowledgment) {

		KafkaMessageHeaders kafkaMessageHeaders = new KafkaMessageHeaders(generateMessageId, generateTimestamp);

		Map<String, Object> rawHeaders = kafkaMessageHeaders.getRawHeaders();
		rawHeaders.put(KafkaHeaders.MESSAGE_KEY, record.key());
		rawHeaders.put(KafkaHeaders.TOPIC, record.topic());
		rawHeaders.put(KafkaHeaders.PARTITION_ID, record.partition());
		rawHeaders.put(KafkaHeaders.OFFSET, record.offset());

		if (acknowledgment != null) {
			rawHeaders.put(KafkaHeaders.ACKNOWLEDGMENT, acknowledgment);
		}

		if (this.useMessageBuilderFactory) {
			return getMessageBuilderFactory()
					.withPayload(record.value())
					.copyHeaders(kafkaMessageHeaders)
					.build();
		}
		else {
			return MessageBuilder.createMessage(record.value(), kafkaMessageHeaders);
		}
	}

	@SuppressWarnings("serial")
	private static class KafkaMessageHeaders extends MessageHeaders {

		public KafkaMessageHeaders(boolean generateId, boolean generateTimestamp) {
			super(null, generateId ? null : ID_VALUE_NONE, generateTimestamp ? null : -1L);
		}

		@Override
		public Map<String, Object> getRawHeaders() {
			return super.getRawHeaders();
		}

	}

}
