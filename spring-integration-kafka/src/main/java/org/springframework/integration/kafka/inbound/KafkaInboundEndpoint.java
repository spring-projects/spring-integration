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

import org.apache.kafka.clients.consumer.Consumer;

import org.springframework.kafka.KafkaException;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.retry.RecoveryCallback;
import org.springframework.retry.support.RetryTemplate;

/**
 * Implementations of this interface will generally support a retry template for retrying
 * incoming deliveries and this supports adding common attributes to the retry context.
 *
 * @author Gary Russell
 * @since 6.0
 *
 */
public interface KafkaInboundEndpoint {

	/**
	 * {@link org.springframework.retry.RetryContext} attribute key for an acknowledgment
	 * if the listener is capable of acknowledging.
	 */
	String CONTEXT_ACKNOWLEDGMENT = "acknowledgment";

	/**
	 * {@link org.springframework.retry.RetryContext} attribute key for the consumer if
	 * the listener is consumer-aware.
	 */
	String CONTEXT_CONSUMER = "consumer";

	/**
	 * {@link org.springframework.retry.RetryContext} attribute key for the record.
	 */
	String CONTEXT_RECORD = "record";

	/**
	 * Execute the runnable with the retry template and recovery callback.
	 * @param template the template.
	 * @param callback the callback.
	 * @param record the record (or records).
	 * @param acknowledgment the acknowledgment.
	 * @param consumer the consumer.
	 * @param runnable the runnable.
	 */
	default void doWithRetry(RetryTemplate template, RecoveryCallback<?> callback, Object data,
			Acknowledgment acknowledgment, Consumer<?, ?> consumer, Runnable runnable) {

		try {
			template.execute(context -> {
				context.setAttribute(CONTEXT_RECORD, data);
				context.setAttribute(CONTEXT_ACKNOWLEDGMENT, acknowledgment);
				context.setAttribute(CONTEXT_CONSUMER, consumer);
				runnable.run();
				return null;
			}, callback);
		}
		catch (Exception ex) {
			throw new KafkaException("Failed to execute runnable", ex);
		}
	}

}
