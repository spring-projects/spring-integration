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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import org.aopalliance.aop.Advice;
import org.junit.Test;
import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.config.TestTrigger;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.scheduling.PollerMetadata;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.scheduling.support.PeriodicTrigger;
import org.springframework.transaction.interceptor.NameMatchTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;

/**
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 */
public class PollerParserTests {

	@Test
	public void defaultPollerWithId() {
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"defaultPollerWithId.xml", PollerParserTests.class);
		Object poller = context.getBean("defaultPollerWithId");
		assertNotNull(poller);
		Object defaultPoller = context.getBean(IntegrationContextUtils.DEFAULT_POLLER_METADATA_BEAN_NAME);
		assertNotNull(defaultPoller);
		assertEquals(defaultPoller, context.getBean("defaultPollerWithId"));
	}

	@Test
	public void defaultPollerWithoutId() {
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"defaultPollerWithoutId.xml", PollerParserTests.class);
		Object defaultPoller = context.getBean(IntegrationContextUtils.DEFAULT_POLLER_METADATA_BEAN_NAME);
		assertNotNull(defaultPoller);
	}

	@Test(expected = BeanDefinitionParsingException.class)
	public void multipleDefaultPollers() {
		new ClassPathXmlApplicationContext(
				"multipleDefaultPollers.xml", PollerParserTests.class);
	}

	@Test(expected = BeanDefinitionParsingException.class)
	public void topLevelPollerWithoutId() {
		new ClassPathXmlApplicationContext(
				"topLevelPollerWithoutId.xml", PollerParserTests.class);
	}

	@Test
	public void pollerWithAdviceChain() {
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"pollerWithAdviceChain.xml", PollerParserTests.class);
		Object poller = context.getBean("poller");
		assertNotNull(poller);
		PollerMetadata metadata = (PollerMetadata) poller;
		assertNotNull(metadata.getAdviceChain());
		assertEquals(4, metadata.getAdviceChain().size());
		assertSame(context.getBean("adviceBean1"), metadata.getAdviceChain().get(0));
		assertEquals(TestAdviceBean.class, metadata.getAdviceChain().get(1).getClass());
		assertEquals(2, ((TestAdviceBean) metadata.getAdviceChain().get(1)).getId());
		assertSame(context.getBean("adviceBean3"), metadata.getAdviceChain().get(2));
		Advice txAdvice = metadata.getAdviceChain().get(3);
		assertEquals(TransactionInterceptor.class, txAdvice.getClass());
		TransactionAttributeSource transactionAttributeSource = ((TransactionInterceptor) txAdvice).getTransactionAttributeSource();
		assertEquals(NameMatchTransactionAttributeSource.class, transactionAttributeSource.getClass());
		@SuppressWarnings("rawtypes")
		HashMap nameMap = TestUtils.getPropertyValue(transactionAttributeSource, "nameMap", HashMap.class);
		assertEquals(1, nameMap.size());
		assertEquals("{*=PROPAGATION_REQUIRES_NEW,ISOLATION_DEFAULT,readOnly}", nameMap.toString());

	}

	@Test
	public void pollerWithReceiveTimeoutAndTimeunit() {
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"pollerWithReceiveTimeout.xml", PollerParserTests.class);
		Object poller = context.getBean("poller");
		assertNotNull(poller);
		PollerMetadata metadata = (PollerMetadata) poller;
		assertEquals(1234, metadata.getReceiveTimeout());
		PeriodicTrigger trigger = (PeriodicTrigger) metadata.getTrigger();
		assertEquals(TimeUnit.SECONDS.toString(), TestUtils.getPropertyValue(trigger, "timeUnit").toString());
	}

    @Test
	public void pollerWithTriggerReference() {
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"pollerWithTriggerReference.xml", PollerParserTests.class);
		Object poller = context.getBean("poller");
		assertNotNull(poller);
		PollerMetadata metadata = (PollerMetadata) poller;
		assertTrue(metadata.getTrigger() instanceof TestTrigger);
	}

    @Test(expected=BeanDefinitionParsingException.class)
	public void pollerWithCronTriggerAndTimeUnit() {
		new ClassPathXmlApplicationContext(
				"cronTriggerWithTimeUnit-fail.xml", PollerParserTests.class);
	}

    @Test(expected=BeanDefinitionParsingException.class)
	public void topLevelPollerWithRef() {
		new ClassPathXmlApplicationContext(
				"defaultPollerWithRef.xml", PollerParserTests.class);
	}

    @Test(expected=BeanDefinitionParsingException.class)
	public void pollerWithCronAndFixedDelay() {
		new ClassPathXmlApplicationContext(
				"pollerWithCronAndFixedDelay.xml", PollerParserTests.class);
	}

	@Test
	public void pollerWithSync() {
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"pollerWithSynchronization.xml", PollerParserTests.class);
		Object poller = context.getBean("noSync");
		assertNotNull(poller);
		PollerMetadata metadata = (PollerMetadata) poller;
		assertEquals(true, metadata.isSynchronized());

		poller = context.getBean("syncTrue");
		assertNotNull(poller);
		metadata = (PollerMetadata) poller;
		assertEquals(true, metadata.isSynchronized());

		poller = context.getBean("syncFalse");
		assertNotNull(poller);
		metadata = (PollerMetadata) poller;
		assertEquals(false, metadata.isSynchronized());
	}

}
