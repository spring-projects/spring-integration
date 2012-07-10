/*
 * Copyright 2002-2012 the original author or authors.
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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;

import org.aopalliance.aop.Advice;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.handler.DelayHandler;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.interceptor.MatchAlwaysTransactionAttributeSource;
import org.springframework.transaction.interceptor.NameMatchTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 * @since 1.0.3
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class DelayerParserTests {

	@Autowired
	private ApplicationContext context;

	@Test
	public void defaultScheduler() {
		Object endpoint = context.getBean("delayerWithDefaultScheduler");
		assertEquals(EventDrivenConsumer.class, endpoint.getClass());
		Object handler = TestUtils.getPropertyValue(endpoint, "handler");
		assertEquals(DelayHandler.class, handler.getClass());
		DelayHandler delayHandler = (DelayHandler) handler;
		assertEquals(99, delayHandler.getOrder());
		DirectFieldAccessor accessor = new DirectFieldAccessor(delayHandler);
		assertEquals(context.getBean("output"), accessor.getPropertyValue("outputChannel"));
		assertEquals(new Long(1234), accessor.getPropertyValue("defaultDelay"));
		assertEquals("foo", accessor.getPropertyValue("delayHeaderName"));
		assertEquals(new Long(987), new DirectFieldAccessor(
				accessor.getPropertyValue("messagingTemplate")).getPropertyValue("sendTimeout"));
		assertNull(accessor.getPropertyValue("taskScheduler"));
	}

	@Test
	public void customScheduler() {
		Object endpoint = context.getBean("delayerWithCustomScheduler");
		assertEquals(EventDrivenConsumer.class, endpoint.getClass());
		Object handler = TestUtils.getPropertyValue(endpoint, "handler");
		assertEquals(DelayHandler.class, handler.getClass());
		DelayHandler delayHandler = (DelayHandler) handler;
		assertEquals(Ordered.LOWEST_PRECEDENCE, delayHandler.getOrder());
		DirectFieldAccessor accessor = new DirectFieldAccessor(delayHandler);
		assertEquals(context.getBean("output"), accessor.getPropertyValue("outputChannel"));
		assertEquals(new Long(0), accessor.getPropertyValue("defaultDelay"));
		assertEquals(context.getBean("testScheduler"), accessor.getPropertyValue("taskScheduler"));
		assertNotNull(accessor.getPropertyValue("taskScheduler"));
		assertEquals(Boolean.TRUE, new DirectFieldAccessor(
				accessor.getPropertyValue("taskScheduler")).getPropertyValue("waitForTasksToCompleteOnShutdown"));
	}

	@Test
	public void customMessageStore() {
		Object endpoint = context.getBean("delayerWithCustomMessageStore");
		assertEquals(EventDrivenConsumer.class, endpoint.getClass());
		Object handler = TestUtils.getPropertyValue(endpoint, "handler");
		assertEquals(DelayHandler.class, handler.getClass());
		DelayHandler delayHandler = (DelayHandler) handler;
		DirectFieldAccessor accessor = new DirectFieldAccessor(delayHandler);
		assertEquals(context.getBean("testMessageStore"), accessor.getPropertyValue("messageStore"));
	}

	@Test //INT-2649
	public void transactionalSubElement() {
		Object endpoint = context.getBean("delayerWithTransactional");
		DelayHandler delayHandler = TestUtils.getPropertyValue(endpoint, "handler", DelayHandler.class);
		List adviceChain = TestUtils.getPropertyValue(delayHandler, "adviceChain", List.class);
		assertEquals(1, adviceChain.size());
		Object advice = adviceChain.get(0);
		assertTrue(advice instanceof TransactionInterceptor);
		TransactionAttributeSource transactionAttributeSource = ((TransactionInterceptor) advice).getTransactionAttributeSource();
		assertTrue(transactionAttributeSource instanceof MatchAlwaysTransactionAttributeSource);
		TransactionDefinition definition = transactionAttributeSource.getTransactionAttribute(null, null);
		assertEquals(TransactionDefinition.PROPAGATION_REQUIRED, definition.getPropagationBehavior());
		assertEquals(TransactionDefinition.ISOLATION_DEFAULT, definition.getIsolationLevel());
		assertEquals(TransactionDefinition.TIMEOUT_DEFAULT, definition.getTimeout());
		assertFalse(definition.isReadOnly());
	}

	@Test //INT-2649
	public void adviceChainSubElement() {
		Object endpoint = context.getBean("delayerWithAdviceChain");
		DelayHandler delayHandler = TestUtils.getPropertyValue(endpoint, "handler", DelayHandler.class);
		List adviceChain = TestUtils.getPropertyValue(delayHandler, "adviceChain", List.class);
		assertEquals(2, adviceChain.size());
		assertSame(context.getBean("testAdviceBean"), adviceChain.get(0));

		Object txAdvice = adviceChain.get(1);
		assertEquals(TransactionInterceptor.class, txAdvice.getClass());
		TransactionAttributeSource transactionAttributeSource = ((TransactionInterceptor) txAdvice).getTransactionAttributeSource();
		assertEquals(NameMatchTransactionAttributeSource.class, transactionAttributeSource.getClass());
		HashMap nameMap = TestUtils.getPropertyValue(transactionAttributeSource, "nameMap", HashMap.class);
		assertEquals("{*=PROPAGATION_REQUIRES_NEW,ISOLATION_DEFAULT,readOnly}", nameMap.toString());
	}

}
