/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.aggregator;

import org.junit.Test;

import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marius Bogoevici
 */
public class HeaderAttributeCorrelationStrategyTests {

	@Test
	public void testHeaderAttributeCorrelationStrategy() {
		String testedHeaderValue = "@!arbitraryTestValue!@";
		String testHeaderName = "header.for.test";
		Message<?> message = MessageBuilder.withPayload("irrelevantData").setHeader(testHeaderName, testedHeaderValue).build();
		HeaderAttributeCorrelationStrategy correlationStrategy = new HeaderAttributeCorrelationStrategy(testHeaderName);
		assertThat(correlationStrategy.getCorrelationKey(message)).isEqualTo(testedHeaderValue);
	}

}
