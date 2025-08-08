/*
 * Copyright © 2022 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2022-present the original author or authors.
 */

package org.springframework.integration.kafka.inbound;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;

import org.springframework.core.AttributeAccessor;
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

	ThreadLocal<AttributeAccessor> ATTRIBUTES_HOLDER = new ThreadLocal<>();

	/**
	 * Execute the runnable with the retry template and recovery callback.
	 * @param template the template.
	 * @param callback the callback.
	 * @param record the record (or records).
	 * @param acknowledgment the acknowledgment.
	 * @param consumer the consumer.
	 * @param runnable the runnable.
	 */
	default void doWithRetry(RetryTemplate template, RecoveryCallback<?> callback, ConsumerRecord<?, ?> record,
			Acknowledgment acknowledgment, Consumer<?, ?> consumer, Runnable runnable) {

		try {
			template.execute(context -> {
				if (context.getRetryCount() == 0) {
					context.setAttribute(CONTEXT_RECORD, record);
					context.setAttribute(CONTEXT_ACKNOWLEDGMENT, acknowledgment);
					context.setAttribute(CONTEXT_CONSUMER, consumer);
					ATTRIBUTES_HOLDER.set(context);
				}
				runnable.run();
				return null;
			}, callback);
		}
		catch (Exception ex) {
			throw new KafkaException("Failed to execute runnable", ex);
		}
		finally {
			ATTRIBUTES_HOLDER.remove();
		}
	}

}
