/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.config;

import org.springframework.integration.aggregator.CorrelationStrategy;
import org.springframework.messaging.Message;

/**
 * @author Marius Bogoevici
 */
public class TestCorrelationStrategy implements CorrelationStrategy {

	public Object getCorrelationKey(Message<?> message) {
		throw new UnsupportedOperationException("for configuration test only");
	}

}
