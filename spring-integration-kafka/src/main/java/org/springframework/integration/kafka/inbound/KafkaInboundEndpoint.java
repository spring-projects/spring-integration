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

package org.springframework.integration.kafka.inbound;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.jspecify.annotations.Nullable;

import org.springframework.core.AttributeAccessor;
import org.springframework.core.AttributeAccessorSupport;
import org.springframework.core.retry.RetryException;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.integration.core.RecoveryCallback;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.support.Acknowledgment;

/**
 * Implementations of this interface will generally support a retry template for retrying
 * incoming deliveries, and this supports adding common attributes to the retry context.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 6.0
 *
 */
public interface KafkaInboundEndpoint {

	/**
	 * The {@link RetryContext} attribute key for an acknowledgment
	 * if the listener is capable of acknowledging.
	 */
	String CONTEXT_ACKNOWLEDGMENT = "acknowledgment";

	/**
	 * The {@link RetryContext} attribute key for the consumer if
	 * the listener is consumer-aware.
	 */
	String CONTEXT_CONSUMER = "consumer";

	/**
	 * The {@link RetryContext} attribute key for the record.
	 */
	String CONTEXT_RECORD = "record";

	ThreadLocal<@Nullable AttributeAccessor> ATTRIBUTES_HOLDER = new ThreadLocal<>();

	/**
	 * Execute the runnable with the retry template and recovery callback.
	 * @param template the template.
	 * @param callback the callback.
	 * @param record the record (or records).
	 * @param acknowledgment the acknowledgment.
	 * @param consumer the consumer.
	 * @param runnable the runnable.
	 */
	default void doWithRetry(RetryTemplate template, @Nullable RecoveryCallback<?> callback, ConsumerRecord<?, ?> record,
			@Nullable Acknowledgment acknowledgment, @Nullable Consumer<?, ?> consumer, Runnable runnable) {

		RetryContext context = new RetryContext();
		context.setAttribute(CONTEXT_RECORD, record);
		context.setAttribute(CONTEXT_ACKNOWLEDGMENT, acknowledgment);
		context.setAttribute(CONTEXT_CONSUMER, consumer);
		ATTRIBUTES_HOLDER.set(context);

		try {
			template.<@Nullable Object>execute(() -> {
				try {
					runnable.run();
				}
				catch (Throwable ex) {
					context.retryCount++;
					throw ex;
				}
				return null;
			});
		}
		catch (RetryException ex) {
			if (callback != null) {
				callback.recover(context, ex);
			}
			else {
				throw new KafkaException("Failed to execute runnable", ex);
			}
		}
		finally {
			ATTRIBUTES_HOLDER.remove();
		}
	}

	@SuppressWarnings("serial")
	final class RetryContext extends AttributeAccessorSupport {

		private int retryCount;

		public int getRetryCount() {
			return this.retryCount;
		}

	}

}
