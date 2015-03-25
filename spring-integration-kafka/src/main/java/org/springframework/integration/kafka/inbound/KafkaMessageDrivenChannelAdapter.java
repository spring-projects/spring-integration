/*
 * Copyright 2015 the original author or authors.
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

import java.util.HashMap;
import java.util.Map;

import org.springframework.integration.context.OrderlyShutdownCapable;
import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.integration.kafka.core.KafkaMessageMetadata;
import org.springframework.integration.kafka.listener.AbstractDecodingAcknowledgingMessageListener;
import org.springframework.integration.kafka.listener.AbstractDecodingMessageListener;
import org.springframework.integration.kafka.listener.Acknowledgment;
import org.springframework.integration.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.integration.kafka.support.KafkaHeaders;
import org.springframework.integration.support.DefaultMessageBuilderFactory;
import org.springframework.integration.support.MessageBuilderFactory;
import org.springframework.integration.support.MutableMessageBuilderFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.util.Assert;

import kafka.serializer.Decoder;
import kafka.serializer.DefaultDecoder;

/**
 * @author Marius Bogoevici
 */
public class KafkaMessageDrivenChannelAdapter extends MessageProducerSupport implements OrderlyShutdownCapable {

	private final KafkaMessageListenerContainer messageListenerContainer;

	private Decoder<?> keyDecoder = new DefaultDecoder(null);

	private Decoder<?> payloadDecoder = new DefaultDecoder(null);

	private boolean generateMessageId = false;

	private boolean generateTimestamp = false;

	private boolean useMessageBuilderFactory = false;

	private boolean autoCommitOffset = true;

	public KafkaMessageDrivenChannelAdapter(KafkaMessageListenerContainer messageListenerContainer) {
		Assert.notNull(messageListenerContainer);
		Assert.isNull(messageListenerContainer.getMessageListener());
		this.messageListenerContainer = messageListenerContainer;
		this.messageListenerContainer.setAutoStartup(false);
	}

	public void setKeyDecoder(Decoder<?> keyDecoder) {
		this.keyDecoder = keyDecoder;
	}

	public void setPayloadDecoder(Decoder<?> payloadDecoder) {
		this.payloadDecoder = payloadDecoder;
	}

	/**
	 * Automatically commit the offsets when 'true'. When 'false', the
	 * adapter inserts a 'kafka_acknowledgment` header allowing the user to manually
	 * commit the offset using the {@link Acknowledgment#acknowledge()} method.
	 * Default 'true'.
	 * @param autoCommitOffset false to not auto-commit (default true).
	 */
	public void setAutoCommitOffset(boolean autoCommitOffset) {
		this.autoCommitOffset = autoCommitOffset;
	}

	/**
	 * Generate {@link Message} {@code ids} for produced messages.
	 * If set to {@code false}, will try to use a default value. By default set to {@code false}.
	 * Note that this option is only guaranteed to work when
	 * {@link #setUseMessageBuilderFactory(boolean) useMessageBuilderFactory} is false (default).
	 * If the latter is set to {@code true}, then some {@link MessageBuilderFactory} implementations such as
	 * {@link DefaultMessageBuilderFactory} may ignore it.
	 * @param generateMessageId true if a message id should be generated
	 * @since 1.1
	 */
	public void setGenerateMessageId(boolean generateMessageId) {
		this.generateMessageId = generateMessageId;
	}

	/**
	 * Generate {@code timestamp} for produced messages. If set to {@code false}, -1 is used instead.
	 * By default set to {@code false}.
	 * Note that this option is only guaranteed to work when
	 * {@link #setUseMessageBuilderFactory(boolean) useMessageBuilderFactory} is false (default).
	 * If the latter is set to {@code true}, then some {@link MessageBuilderFactory} implementations such as
	 * {@link DefaultMessageBuilderFactory} may ignore it.
	 * @param generateTimestamp true if a timestamp should be generated
	 * @since 1.1
	 */
	public void setGenerateTimestamp(boolean generateTimestamp) {
		this.generateTimestamp = generateTimestamp;
	}

	/**
	 * Use the {@link MessageBuilderFactory} returned by {@link #getMessageBuilderFactory()} to create messages.
	 * @param useMessageBuilderFactory true if the {@link MessageBuilderFactory} returned by
	 * {@link #getMessageBuilderFactory()} should be used.
	 * @since 1.1
	 */
	public void setUseMessageBuilderFactory(boolean useMessageBuilderFactory) {
		this.useMessageBuilderFactory = useMessageBuilderFactory;
	}

	@Override
	protected void onInit() {
		this.messageListenerContainer.setMessageListener(autoCommitOffset ?
				new AutoAcknowledgingChannelForwardingMessageListener()
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

	@SuppressWarnings("rawtypes")
	private class AutoAcknowledgingChannelForwardingMessageListener extends AbstractDecodingMessageListener {

		@SuppressWarnings("unchecked")
		public AutoAcknowledgingChannelForwardingMessageListener() {
			super(keyDecoder, payloadDecoder);
		}

		@Override
		public void doOnMessage(Object key, Object payload, KafkaMessageMetadata metadata) {
			KafkaMessageDrivenChannelAdapter.this.sendMessage(toMessage(key, payload, metadata, null));
		}

	}

	@SuppressWarnings("rawtypes")
	private class AcknowledgingChannelForwardingMessageListener extends AbstractDecodingAcknowledgingMessageListener {

		@SuppressWarnings("unchecked")
		public AcknowledgingChannelForwardingMessageListener() {
			super(keyDecoder, payloadDecoder);
		}

		@Override
		public void doOnMessage(Object key, Object payload, KafkaMessageMetadata metadata,
				Acknowledgment acknowledgment) {
			KafkaMessageDrivenChannelAdapter.this.sendMessage(toMessage(key, payload, metadata, acknowledgment));
		}

	}

	private Message<Object> toMessage(Object key, Object payload, KafkaMessageMetadata metadata,
			Acknowledgment acknowledgment) {

		final Map<String, Object> headers = new HashMap<String, Object>();

		headers.put(KafkaHeaders.MESSAGE_KEY, key);
		headers.put(KafkaHeaders.TOPIC, metadata.getPartition().getTopic());
		headers.put(KafkaHeaders.PARTITION_ID, metadata.getPartition().getId());
		headers.put(KafkaHeaders.OFFSET, metadata.getOffset());
		headers.put(KafkaHeaders.NEXT_OFFSET, metadata.getNextOffset());

		// pre-set the message id header if set to not generate
		if (!this.generateMessageId) {
			headers.put(MessageHeaders.ID, MessageHeaders.ID_VALUE_NONE);
		}

		// pre-set the timestamp header if set to not generate
		if (!this.generateTimestamp) {
			headers.put(MessageHeaders.TIMESTAMP, -1L);
		}

		if (!this.autoCommitOffset) {
			headers.put(KafkaHeaders.ACKNOWLEDGMENT, acknowledgment);
		}

		if (this.useMessageBuilderFactory) {
			return getMessageBuilderFactory()
					.withPayload(payload)
					.copyHeaders(headers)
					.build();
		}
		else {
			return new KafkaMessage(payload, headers);
		}

	}

	/**
	 * Special subclass of {@link Message}. It is used for lower message generation overhead, unless the default
	 * strategy of the outer class is set via {@link #setUseMessageBuilderFactory(boolean)}
	 * @since 1.1
	 */
	private class KafkaMessage implements Message<Object> {

		private final Object payload;

		private final MessageHeaders messageHeaders;

		public KafkaMessage(Object payload, Map<String, Object> headers) {
			this.payload = payload;
			this.messageHeaders = new KafkaMessageHeaders(headers, generateMessageId, generateTimestamp);
		}

		@Override
		public Object getPayload() {
			return this.payload;
		}

		@Override
		public MessageHeaders getHeaders() {
			return this.messageHeaders;
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder(getClass().getSimpleName());
			sb.append(" [payload=");
			if (this.payload instanceof byte[]) {
				sb.append("byte[").append(((byte[]) this.payload).length).append("]");
			}
			else {
				sb.append(this.payload);
			}
			sb.append(", headers=").append(this.messageHeaders).append("]");
			return sb.toString();
		}

	}

	@SuppressWarnings("serial")
	private class KafkaMessageHeaders extends MessageHeaders {

		public KafkaMessageHeaders(Map<String, Object> headers, boolean generateId, boolean generateTimestamp) {
			super(headers, generateId ? null : ID_VALUE_NONE, generateTimestamp ? null : -1L);
		}

	}

}
