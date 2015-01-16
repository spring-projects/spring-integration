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


import kafka.serializer.Decoder;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.integration.kafka.core.KafkaMessage;
import org.springframework.integration.kafka.core.KafkaMessageMetadata;
import org.springframework.integration.kafka.util.MessageUtils;
import org.springframework.util.Assert;

/**
 * Base {@link MessageListener} implementation that decodes the key and the payload using the supplied
 * {@link Decoder}s.
 *
 * Users of this class must extend it and implement {@code doOnMessage} and must supply {@link Decoder}
 * implementations for both the key and the payload.
 *
 * @author Marius Bogoevici
 */
public abstract class AbstractDecodingMessageListener<K, P> implements MessageListener {

	private Decoder<K> keyDecoder;

	private Decoder<P> payloadDecoder;

	public AbstractDecodingMessageListener(Decoder<K> keyDecoder, Decoder<P> payloadDecoder) {
		this.keyDecoder = keyDecoder;
		this.payloadDecoder = payloadDecoder;
	}

	@Override
	public final void onMessage(KafkaMessage message) {
		this.doOnMessage(MessageUtils.decodeKey(message, keyDecoder),
				MessageUtils.decodePayload(message, payloadDecoder), message.getMetadata());
	}

	/**
	 * Process the decoded message
	 * @param key the message key
	 * @param payload the message body
	 * @param metadata the KafkaMessageMetadata
	 */
	public abstract void doOnMessage(K key, P payload, KafkaMessageMetadata metadata);

}
