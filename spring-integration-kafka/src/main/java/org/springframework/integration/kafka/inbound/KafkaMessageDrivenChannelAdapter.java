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

import java.util.Map;

import kafka.serializer.Decoder;
import kafka.serializer.DefaultDecoder;

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
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.util.Assert;

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
			sendMessage(toMessage(key, payload, metadata, null));
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

			sendMessage(toMessage(key, payload, metadata, acknowledgment));
		}
	}

	private Message<Object> toMessage(Object key, Object payload, KafkaMessageMetadata metadata,
			Acknowledgment acknowledgment) {

		final MessageHeaderAccessor headerAccessor = new MessageHeaderAccessor();

		headerAccessor.setHeader(KafkaHeaders.MESSAGE_KEY, key);
		headerAccessor.setHeader(KafkaHeaders.TOPIC, metadata.getPartition().getTopic());
		headerAccessor.setHeader(KafkaHeaders.PARTITION_ID, metadata.getPartition().getId());
		headerAccessor.setHeader(KafkaHeaders.OFFSET, metadata.getOffset());
		headerAccessor.setHeader(KafkaHeaders.NEXT_OFFSET, metadata.getNextOffset());

		// pre-set the message id header if set to not generate
		headerAccessor.setLeaveMutable(!(this.generateMessageId || this.generateTimestamp));


		if (!this.autoCommitOffset) {
			headerAccessor.setHeader(KafkaHeaders.ACKNOWLEDGMENT, acknowledgment);
		}

		if (this.useMessageBuilderFactory) {
			return getMessageBuilderFactory()
					.withPayload(payload)
					.copyHeaders(headerAccessor.toMessageHeaders())
					.build();
		}
		else {
			return MessageBuilder.createMessage(payload, headerAccessor.getMessageHeaders());
		}

	}

}
