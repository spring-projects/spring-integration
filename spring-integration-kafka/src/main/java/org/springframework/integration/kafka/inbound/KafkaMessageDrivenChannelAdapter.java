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

import org.springframework.integration.context.OrderlyShutdownCapable;
import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.integration.kafka.core.KafkaMessageMetadata;
import org.springframework.integration.kafka.listener.AbstractDecodingAcknowledgingMessageListener;
import org.springframework.integration.kafka.listener.AbstractDecodingMessageListener;
import org.springframework.integration.kafka.listener.Acknowledgment;
import org.springframework.integration.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.integration.kafka.support.KafkaHeaders;
import org.springframework.integration.support.AbstractIntegrationMessageBuilder;
import org.springframework.messaging.Message;
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

	public void setAutoCommitOffset(boolean autoCommitOffset) {
		this.autoCommitOffset = autoCommitOffset;
	}

	@Override
	protected void onInit() {
		this.messageListenerContainer.setMessageListener(autoCommitOffset ?
				new AutoAcknowledgingChannelForwardingMessageListener()
				: new AcknowledgingChannelForwardingMessageListener());
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
		AbstractIntegrationMessageBuilder<Object> messageBuilder = getMessageBuilderFactory().withPayload(payload)
				.setHeader(KafkaHeaders.MESSAGE_KEY, key)
				.setHeader(KafkaHeaders.TOPIC, metadata.getPartition().getTopic())
				.setHeader(KafkaHeaders.PARTITION_ID, metadata.getPartition().getId())
				.setHeader(KafkaHeaders.OFFSET, metadata.getOffset())
				.setHeader(KafkaHeaders.NEXT_OFFSET, metadata.getNextOffset());
		if (acknowledgment != null) {
			messageBuilder.setHeader(KafkaHeaders.ACKNOWLEDGMENT, acknowledgment);
		}
		return messageBuilder.build();
	}

}
