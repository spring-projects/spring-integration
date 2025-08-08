/*
 * Copyright © 2017 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2017-present the original author or authors.
 */

package org.springframework.integration.amqp.support;

import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;

/**
 * An exception representing a negatively acknowledged message from a
 * publisher confirm.
 *
 * @author Gary Russell
 * @since 4.3.12
 *
 */
public class NackedAmqpMessageException extends MessagingException {

	private static final long serialVersionUID = 1L;

	private final Object correlationData;

	private final String nackReason;

	public NackedAmqpMessageException(Message<?> message, @Nullable Object correlationData, String nackReason) {
		super(message);
		this.correlationData = correlationData;
		this.nackReason = nackReason;
	}

	public Object getCorrelationData() {
		return this.correlationData;
	}

	public String getNackReason() {
		return this.nackReason;
	}

	@Override
	public String toString() {
		return super.toString() + " [correlationData=" + this.correlationData + ", nackReason=" + this.nackReason
				+ "]";
	}

}
