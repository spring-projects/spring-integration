/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.integration.config.xml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.handler.advice.RequestHandlerRetryAdvice;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Gary Russell
 * @since 4.0
 *
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
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

	@Autowired @Qualifier("sa1.handler")
	private MessageHandler handler1;

	@Autowired @Qualifier("sa2.handler")
	private MessageHandler handler2;

	@Autowired
	private MessageChannel foo;

	@Test
	public void testAll() {
		assertEquals(Integer.valueOf(3), TestUtils.getPropertyValue(a1, "retryTemplate.retryPolicy.maxAttempts", Integer.class));
		assertEquals(Integer.valueOf(4), TestUtils.getPropertyValue(a2, "retryTemplate.retryPolicy.maxAttempts", Integer.class));
		assertEquals(Integer.valueOf(5), TestUtils.getPropertyValue(a3, "retryTemplate.retryPolicy.maxAttempts", Integer.class));
		assertEquals(Integer.valueOf(6), TestUtils.getPropertyValue(a4, "retryTemplate.retryPolicy.maxAttempts", Integer.class));
		assertEquals(Integer.valueOf(7), TestUtils.getPropertyValue(a5, "retryTemplate.retryPolicy.maxAttempts", Integer.class));
		assertEquals(Integer.valueOf(8), TestUtils.getPropertyValue(a6, "retryTemplate.retryPolicy.maxAttempts", Integer.class));
		assertEquals(Integer.valueOf(3), TestUtils.getPropertyValue(a7, "retryTemplate.retryPolicy.maxAttempts", Integer.class));

		assertEquals(Long.valueOf(1000), TestUtils.getPropertyValue(a3, "retryTemplate.backOffPolicy.backOffPeriod", Long.class));
		assertEquals(Long.valueOf(1234), TestUtils.getPropertyValue(a4, "retryTemplate.backOffPolicy.backOffPeriod", Long.class));

		assertEquals(Long.valueOf(100), TestUtils.getPropertyValue(a5, "retryTemplate.backOffPolicy.initialInterval", Long.class));
		assertEquals(Double.valueOf(2.0), TestUtils.getPropertyValue(a5, "retryTemplate.backOffPolicy.multiplier", Double.class));
		assertEquals(Long.valueOf(30000), TestUtils.getPropertyValue(a5, "retryTemplate.backOffPolicy.maxInterval", Long.class));
		assertEquals(Long.valueOf(1000), TestUtils.getPropertyValue(a6, "retryTemplate.backOffPolicy.initialInterval", Long.class));
		assertEquals(Double.valueOf(3.0), TestUtils.getPropertyValue(a6, "retryTemplate.backOffPolicy.multiplier", Double.class));
		assertEquals(Long.valueOf(10000), TestUtils.getPropertyValue(a6, "retryTemplate.backOffPolicy.maxInterval", Long.class));

		assertNull(TestUtils.getPropertyValue(a1, "recoveryCallback"));
		assertNotNull(TestUtils.getPropertyValue(a7, "recoveryCallback"));
		assertSame(foo, TestUtils.getPropertyValue(a7, "recoveryCallback.messagingTemplate.defaultDestination"));
		assertEquals(Long.valueOf(4567), TestUtils.getPropertyValue(a7, "recoveryCallback.messagingTemplate.sendTimeout"));

		assertSame(a1, TestUtils.getPropertyValue(handler1, "adviceChain", List.class).get(0));
		assertEquals(Integer.valueOf(9), TestUtils.getPropertyValue(TestUtils.getPropertyValue(handler2, "adviceChain", List.class).get(0),
				"retryTemplate.retryPolicy.maxAttempts", Integer.class));
	}

}
