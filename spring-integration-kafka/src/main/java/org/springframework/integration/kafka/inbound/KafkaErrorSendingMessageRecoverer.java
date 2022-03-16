/*
 * Copyright 2022 the original author or authors.
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

package org.springframework.integration.kafka.inbound;

import org.apache.kafka.clients.consumer.ConsumerRecord;

import org.springframework.core.AttributeAccessor;
import org.springframework.integration.core.ErrorMessagePublisher;
import org.springframework.integration.kafka.support.RawRecordHeaderErrorMessageStrategy;
import org.springframework.integration.support.ErrorMessageStrategy;
import org.springframework.integration.support.ErrorMessageUtils;
import org.springframework.kafka.listener.ConsumerRecordRecoverer;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.MessageChannel;

/**
 * An extension of {@link ErrorMessagePublisher} that can be used in a
 * {@link org.springframework.kafka.listener.CommonErrorHandler} for recovering Kafka
 * delivery failures.
 *
 * @author Gary Russell
 * @since 6.0
 *
 */
public class KafkaErrorSendingMessageRecoverer extends ErrorMessagePublisher implements ConsumerRecordRecoverer {

	/**
	 * Construct an instance to send to the channel with the
	 * {@link RawRecordHeaderErrorMessageStrategy}.
	 * @param channel the channel.
	 */
	public KafkaErrorSendingMessageRecoverer(MessageChannel channel) {
		this(channel, new RawRecordHeaderErrorMessageStrategy());
	}

	/**
	 * Construct an instance to send the channel, using the error message strategy.
	 * @param channel the channel.
	 * @param errorMessageStrategy the strategy.
	 */
	public KafkaErrorSendingMessageRecoverer(MessageChannel channel, ErrorMessageStrategy errorMessageStrategy) {
		setChannel(channel);
		setErrorMessageStrategy(errorMessageStrategy);
	}

	@Override
	public void accept(ConsumerRecord<?, ?> record, Exception ex) {
		Throwable thrown = ex.getCause();
		if (thrown == null) {
			thrown = ex;
		}
		AttributeAccessor attrs = ErrorMessageUtils.getAttributeAccessor(null, null);
		attrs.setAttribute(KafkaHeaders.RAW_DATA, record);
		publish(thrown, attrs);
	}

}
