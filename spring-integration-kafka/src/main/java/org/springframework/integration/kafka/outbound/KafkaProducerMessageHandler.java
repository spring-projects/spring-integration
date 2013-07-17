/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.integration.kafka.outbound;

import org.springframework.integration.Message;
import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.integration.kafka.support.KafkaProducerContext;

/**
 * @author Soby Chacko
 * @since 0.5
 */
public class KafkaProducerMessageHandler<K,V> extends AbstractMessageHandler {

	private final KafkaProducerContext<K,V> kafkaProducerContext;

	public KafkaProducerMessageHandler(final KafkaProducerContext<K,V> kafkaProducerContext) {
		this.kafkaProducerContext = kafkaProducerContext;
	}

	public KafkaProducerContext<K,V> getKafkaProducerContext() {
		return kafkaProducerContext;
	}

	@Override
	protected void handleMessageInternal(final Message<?> message) throws Exception {
		kafkaProducerContext.send(message);
	}
}
