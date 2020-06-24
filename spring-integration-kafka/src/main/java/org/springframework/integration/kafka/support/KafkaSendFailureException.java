/*
 * Copyright 2017-2019 the original author or authors.
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

package org.springframework.integration.kafka.support;

import org.apache.kafka.clients.producer.ProducerRecord;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;

/**
 * An exception that is the payload of an {@code ErrorMessage} when a send fails.
 *
 * @author Gary Russell
 *
 * @since 5.4
 *
 */
public class KafkaSendFailureException extends MessagingException {

	private static final long serialVersionUID = 1L;

	private final ProducerRecord<?, ?> record;

	public KafkaSendFailureException(Message<?> message, ProducerRecord<?, ?> record, Throwable cause) {
		super(message, cause);
		this.record = record;
	}

	public ProducerRecord<?, ?> getRecord() {
		return this.record;
	}

	@Override
	public String toString() {
		return super.toString() + " [record=" + this.record + "]";
	}

}
