/*
 * Copyright 2014-present the original author or authors.
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

package org.springframework.integration.config.xml;

import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.handler.advice.RequestHandlerRetryAdvice;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 4.0
 *
 */
@SpringJUnitConfig
public class RetryAdviceParserTests {

	@Autowired
	private RequestHandlerRetryAdvice a1;

	@Autowired
	private RequestHandlerRetryAdvice a2;

	@Autowired
	private RequestHandlerRetryAdvice a3;

	@Autowired
	private RequestHandlerRetryAdvice a4;

	@Autowired
	private RequestHandlerRetryAdvice a5;

	@Autowired
	private RequestHandlerRetryAdvice a6;

	@Autowired
	private RequestHandlerRetryAdvice a7;

	@Autowired
	@Qualifier("sa1.handler")
	private MessageHandler handler1;

	@Autowired
	@Qualifier("sa2.handler")
	private MessageHandler handler2;

	@Autowired
	@Qualifier("saDefaultRetry.handler")
	private MessageHandler defaultRetryHandler;

	@Autowired
	private MessageChannel foo;

	@Test
	public void testAll() {
		assertThat(TestUtils.getPropertyValue(a1, "retryTemplate.retryPolicy.maxAttempts")).isEqualTo(3);
		assertThat(TestUtils.getPropertyValue(a2, "retryTemplate.retryPolicy.maxAttempts")).isEqualTo(4);
		assertThat(TestUtils.getPropertyValue(a3, "retryTemplate.retryPolicy.maxAttempts")).isEqualTo(5);
		assertThat(TestUtils.getPropertyValue(a4, "retryTemplate.retryPolicy.maxAttempts")).isEqualTo(6);
		assertThat(TestUtils.getPropertyValue(a5, "retryTemplate.retryPolicy.maxAttempts")).isEqualTo(7);
		assertThat(TestUtils.getPropertyValue(a6, "retryTemplate.retryPolicy.maxAttempts")).isEqualTo(8);
		assertThat(TestUtils.getPropertyValue(a7, "retryTemplate.retryPolicy.maxAttempts")).isEqualTo(3);

		assertThat(
				TestUtils.getPropertyValue(a3, "retryTemplate.backOffPolicy", FixedBackOffPolicy.class)
						.getBackOffPeriod())
				.isEqualTo(1000L);
		assertThat(
				TestUtils.getPropertyValue(a4, "retryTemplate.backOffPolicy", FixedBackOffPolicy.class)
						.getBackOffPeriod())
				.isEqualTo(1234L);

		assertThat(TestUtils.getPropertyValue(a5, "retryTemplate.backOffPolicy.initialInterval")).isEqualTo(100L);
		assertThat(TestUtils.getPropertyValue(a5, "retryTemplate.backOffPolicy.multiplier")).isEqualTo(2.0);
		assertThat(TestUtils.getPropertyValue(a5, "retryTemplate.backOffPolicy.maxInterval")).isEqualTo(30000L);
		assertThat(TestUtils.getPropertyValue(a6, "retryTemplate.backOffPolicy.initialInterval")).isEqualTo(1000L);
		assertThat(TestUtils.getPropertyValue(a6, "retryTemplate.backOffPolicy.multiplier")).isEqualTo(3.0);
		assertThat(TestUtils.getPropertyValue(a6, "retryTemplate.backOffPolicy.maxInterval")).isEqualTo(10000L);

		assertThat(TestUtils.getPropertyValue(a1, "recoveryCallback")).isNull();
		assertThat(TestUtils.getPropertyValue(a7, "recoveryCallback")).isNotNull();
		assertThat(TestUtils.getPropertyValue(a7, "recoveryCallback.channel")).isSameAs(this.foo);
		assertThat(TestUtils.getPropertyValue(a7, "recoveryCallback.messagingTemplate.sendTimeout")).isEqualTo(4567L);

		assertThat(TestUtils.getPropertyValue(this.handler1, "adviceChain", List.class).get(0)).isSameAs(this.a1);
		assertThat(TestUtils.getPropertyValue(
				TestUtils.getPropertyValue(this.handler2, "adviceChain", List.class).get(0),
				"retryTemplate.retryPolicy.maxAttempts")).isEqualTo(9);
		assertThat(TestUtils.getPropertyValue(
				TestUtils.getPropertyValue(this.defaultRetryHandler, "adviceChain", List.class).get(0),
				"retryTemplate.retryPolicy.maxAttempts")).isEqualTo(3);
	}

}
