/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.aggregator;

import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * Default implementation of {@link CorrelationStrategy}.
 * Uses a provided header attribute to determine the correlation key value.
 *
 * @author Marius Bogoevici
 * @author Artem Bilan
 */
public class HeaderAttributeCorrelationStrategy implements CorrelationStrategy {

	private final String attributeName;

	public HeaderAttributeCorrelationStrategy(String attributeName) {
		Assert.hasText(attributeName, "the 'attributeName' must not be empty");
		this.attributeName = attributeName;
	}

	public Object getCorrelationKey(Message<?> message) {
		return message.getHeaders().get(this.attributeName);
	}

}
