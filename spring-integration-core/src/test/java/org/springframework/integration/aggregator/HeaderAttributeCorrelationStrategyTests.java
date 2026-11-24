/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.aggregator;

import org.junit.jupiter.api.Test;

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
