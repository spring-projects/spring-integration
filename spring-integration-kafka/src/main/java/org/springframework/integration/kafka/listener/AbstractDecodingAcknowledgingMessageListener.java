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

package org.springframework.integration.kafka.listener;


import org.springframework.integration.kafka.core.KafkaMessage;
import org.springframework.integration.kafka.core.KafkaMessageMetadata;
import org.springframework.integration.kafka.util.MessageUtils;

import kafka.serializer.Decoder;

/**
 * Base {@link AcknowledgingMessageListener} implementation that decodes the key and the
 * payload using the supplied {@link Decoder}s.
 *
 * Users of this class must extend it and implement {@code doOnMessage} and must supply
 * {@link Decoder} implementations for both the key and the payload.
 *
 * @author Marius Bogoevici
 * @since 1.0.1
 */
public abstract class AbstractDecodingAcknowledgingMessageListener<K, P> implements AcknowledgingMessageListener {

	private final Decoder<K> keyDecoder;

	private final Decoder<P> payloadDecoder;

	public AbstractDecodingAcknowledgingMessageListener(Decoder<K> keyDecoder, Decoder<P> payloadDecoder) {
		this.keyDecoder = keyDecoder;
		this.payloadDecoder = payloadDecoder;
	}

	@Override
	public final void onMessage(KafkaMessage message, Acknowledgment acknowledgment) {
		this.doOnMessage(MessageUtils.decodeKey(message, keyDecoder),
				MessageUtils.decodePayload(message, payloadDecoder), message.getMetadata(), acknowledgment);
	}

	/**
	 * Process the decoded message
	 * @param key the message key
	 * @param payload the message body
	 * @param metadata the KafkaMessageMetadata
	 * @param acknowledgment the acknowledgment handle
	 */
	public abstract void doOnMessage(K key, P payload, KafkaMessageMetadata metadata, Acknowledgment acknowledgment);

}
