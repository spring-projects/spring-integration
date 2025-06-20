/*
 * Copyright 2022-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.support.management.observation;

import io.micrometer.observation.transport.SenderContext;

import org.springframework.integration.support.MutableMessage;
import org.springframework.messaging.Message;

/**
 * The {@link SenderContext} extension for {@link Message} context.
 *
 * @author Artem Bilan
 *
 * @since 6.0
 */
public class MessageSenderContext extends SenderContext<MutableMessage<?>> {

	private final MutableMessage<?> message;

	private final String producerName;

	public MessageSenderContext(MutableMessage<?> message, String producerName) {
		super((carrier, key, value) -> carrier.getHeaders().put(key, value));
		this.message = message;
		this.producerName = producerName;
	}

	@Override
	public MutableMessage<?> getCarrier() {
		return this.message;
	}

	public String getProducerName() {
		return this.producerName;
	}

}
