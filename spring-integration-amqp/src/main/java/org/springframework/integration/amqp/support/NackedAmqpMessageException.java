/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.amqp.support;

import java.io.Serial;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;

/**
 * An exception representing a negatively acknowledged message from a
 * publisher confirm.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 4.3.12
 *
 */
public class NackedAmqpMessageException extends MessagingException {

	@Serial
	private static final long serialVersionUID = 1L;

	private final String nackReason;

	private final transient Object correlationData;

	public NackedAmqpMessageException(Message<?> message, Object correlationData, String nackReason) {
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
