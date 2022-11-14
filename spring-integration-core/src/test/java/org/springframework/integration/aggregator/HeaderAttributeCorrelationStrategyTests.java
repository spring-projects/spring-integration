/*
 * Copyright 2002-2022 the original author or authors.
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
