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
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.util.backoff.FixedBackOff;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Gary Russell
 * @author Artem Bilan
 * @author Glenn Renfro
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
	private MessageChannel inputChannel;

	@Test
	public void testAll() {
		assertThat(TestUtils.<Long>getPropertyValue(a1, "retryTemplate.retryPolicy.backOff.maxAttempts")).isEqualTo(3L);
		assertThat(TestUtils.<Long>getPropertyValue(a2, "retryTemplate.retryPolicy.backOff.maxAttempts")).isEqualTo(4L);
		assertThat(TestUtils.<Long>getPropertyValue(a3, "retryTemplate.retryPolicy.backOff.maxAttempts")).isEqualTo(5L);
		assertThat(TestUtils.<Long>getPropertyValue(a4, "retryTemplate.retryPolicy.backOff.maxAttempts")).isEqualTo(6L);
		assertThat(TestUtils.<Long>getPropertyValue(a5, "retryTemplate.retryPolicy.backOff.maxAttempts")).isEqualTo(7L);
		assertThat(TestUtils.<Long>getPropertyValue(a6, "retryTemplate.retryPolicy.backOff.maxAttempts")).isEqualTo(8L);
		assertThat(TestUtils.<Long>getPropertyValue(a7, "retryTemplate.retryPolicy.backOff.maxAttempts")).isEqualTo(3L);

		assertThat(
				TestUtils.<FixedBackOff>getPropertyValue(a3, "retryTemplate.retryPolicy.backOff").getInterval())
				.isEqualTo(1000L);
		assertThat(
				TestUtils.<FixedBackOff>getPropertyValue(a4, "retryTemplate.retryPolicy.backOff").getInterval())
				.isEqualTo(1234L);

		assertThat(TestUtils.<Long>getPropertyValue(a5, "retryTemplate.retryPolicy.backOff.initialInterval"))
				.isEqualTo(100L);
		assertThat(TestUtils.<Double>getPropertyValue(a5, "retryTemplate.retryPolicy.backOff.multiplier"))
				.isEqualTo(2.0);
		assertThat(TestUtils.<Long>getPropertyValue(a5, "retryTemplate.retryPolicy.backOff.maxInterval"))
				.isEqualTo(30000L);
		assertThat(TestUtils.<Long>getPropertyValue(a6, "retryTemplate.retryPolicy.backOff.initialInterval"))
				.isEqualTo(1000L);
		assertThat(TestUtils.<Double>getPropertyValue(a6, "retryTemplate.retryPolicy.backOff.multiplier"))
				.isEqualTo(3.0);
		assertThat(TestUtils.<Long>getPropertyValue(a6, "retryTemplate.retryPolicy.backOff.maxInterval"))
				.isEqualTo(10000L);
		assertThat(TestUtils.<Long>getPropertyValue(a6, "retryTemplate.retryPolicy.backOff.jitter")).isEqualTo(6L);

		assertThat(TestUtils.<Object>getPropertyValue(a1, "recoveryCallback")).isNull();
		assertThat(TestUtils.<Object>getPropertyValue(a7, "recoveryCallback")).isNotNull();
		assertThat(TestUtils.<Object>getPropertyValue(a7, "recoveryCallback.channel")).isSameAs(this.inputChannel);
		assertThat(TestUtils.<Long>getPropertyValue(a7, "recoveryCallback.messagingTemplate.sendTimeout"))
				.isEqualTo(4567L);

		assertThat(TestUtils.<List<?>>getPropertyValue(this.handler1, "adviceChain").get(0)).isSameAs(this.a1);
		assertThat(TestUtils.<Long>getPropertyValue(
				TestUtils.<List<?>>getPropertyValue(this.handler2, "adviceChain").get(0),
				"retryTemplate.retryPolicy.backOff.maxAttempts")).isEqualTo(9L);
		assertThat(TestUtils.<Long>getPropertyValue(
				TestUtils.<List<?>>getPropertyValue(this.defaultRetryHandler, "adviceChain").get(0),
				"retryTemplate.retryPolicy.backOff.maxAttempts")).isEqualTo(3L);
	}

}
