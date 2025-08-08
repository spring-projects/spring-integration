/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.dispatcher;

import java.util.ArrayList;
import java.util.List;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.util.StringUtils;

/**
 * An Exception that encapsulates an aggregated group of Exceptions for use by dispatchers
 * that may try multiple handler invocations within a single dispatch operation.
 *
 * @author Mark Fisher
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 1.0.3
 */
@SuppressWarnings("serial")
public class AggregateMessageDeliveryException extends MessageDeliveryException {

	private final List<? extends Exception> aggregatedExceptions;

	public AggregateMessageDeliveryException(Message<?> undeliveredMessage,
			String description, List<? extends Exception> aggregatedExceptions) {

		super(undeliveredMessage, description);
		this.initCause(aggregatedExceptions.get(0));
		this.aggregatedExceptions = new ArrayList<Exception>(aggregatedExceptions);
	}

	/**
	 * Obtain a list aggregated target exceptions.
	 * @return the list of target exceptions
	 */
	public List<? extends Exception> getAggregatedExceptions() {
		return new ArrayList<Exception>(this.aggregatedExceptions);
	}

	@Override
	public String getMessage() {
		String baseMessage = super.getMessage();
		StringBuilder message = new StringBuilder(appendPeriodIfNecessary(baseMessage))
				.append(" Multiple causes:\n");
		for (Exception exception : this.aggregatedExceptions) {
			message.append("    ")
					.append(exception.getMessage())
					.append("\n");
		}
		message.append("See below for the stacktrace of the first cause.");
		return message.toString();
	}

	private String appendPeriodIfNecessary(String baseMessage) {
		if (!StringUtils.hasText(baseMessage)) {
			return "";
		}
		else if (!baseMessage.endsWith(".")) {
			return baseMessage + ".";
		}
		else {
			return baseMessage;
		}
	}

}
