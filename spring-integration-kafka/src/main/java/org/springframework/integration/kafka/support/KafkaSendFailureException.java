/*
 * Copyright 2017-present the original author or authors.
 */

package org.springframework.integration.kafka.support;

import org.apache.kafka.clients.producer.ProducerRecord;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;

/**
 * An exception that is the payload of an {@code ErrorMessage} when a send fails.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 5.4
 *
 */
public class KafkaSendFailureException extends MessagingException {

	private static final long serialVersionUID = 1L;

	private final transient ProducerRecord<?, ?> record;

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
