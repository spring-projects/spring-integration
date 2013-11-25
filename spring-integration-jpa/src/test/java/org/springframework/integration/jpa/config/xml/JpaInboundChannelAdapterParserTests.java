/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.springframework.integration.jpa.config.xml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Test;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.expression.spel.standard.SpelExpression;
import org.springframework.integration.channel.AbstractMessageChannel;
import org.springframework.integration.endpoint.SourcePollingChannelAdapter;
import org.springframework.integration.jpa.core.JpaExecutor;
import org.springframework.integration.jpa.core.JpaOperations;
import org.springframework.integration.test.util.TestUtils;

/**
 * @author Gunnar Hillert
 * @author Amol Nayak
 * @author Artem Bilan
 *
 * @since 2.2
 *
 */
public class JpaInboundChannelAdapterParserTests {

	private ConfigurableApplicationContext context;

	private SourcePollingChannelAdapter consumer;

	@Test
	public void testJpaInboundChannelAdapterParser() throws Exception {

		setUp("JpaInboundChannelAdapterParserTests.xml", getClass(), "jpaInboundChannelAdapter1");

		final AbstractMessageChannel outputChannel = TestUtils.getPropertyValue(this.consumer, "outputChannel", AbstractMessageChannel.class);

		assertEquals("out", outputChannel.getComponentName());

		final JpaExecutor jpaExecutor = TestUtils.getPropertyValue(this.consumer, "source.jpaExecutor", JpaExecutor.class);

		assertNotNull(jpaExecutor);

		final Class<?> entityClass = TestUtils.getPropertyValue(jpaExecutor, "entityClass", Class.class);

		assertEquals("org.springframework.integration.jpa.test.entity.StudentDomain", entityClass.getName());

		final JpaOperations jpaOperations = TestUtils.getPropertyValue(jpaExecutor, "jpaOperations", JpaOperations.class);

		assertNotNull(jpaOperations);

		assertTrue(TestUtils.getPropertyValue(jpaExecutor, "expectSingleResult", Boolean.class));

	}

	@Test
	public void testJpaInboundChannelAdapterParserWithMaxResults() throws Exception {

		setUp("JpaInboundChannelAdapterParserTests.xml", getClass(), "jpaInboundChannelAdapter2");

		final AbstractMessageChannel outputChannel = TestUtils.getPropertyValue(this.consumer, "outputChannel", AbstractMessageChannel.class);

		assertEquals("out", outputChannel.getComponentName());

		final JpaExecutor jpaExecutor = TestUtils.getPropertyValue(this.consumer, "source.jpaExecutor", JpaExecutor.class);

		assertNotNull(jpaExecutor);

		final Class<?> entityClass = TestUtils.getPropertyValue(jpaExecutor, "entityClass", Class.class);

		assertEquals("org.springframework.integration.jpa.test.entity.StudentDomain", entityClass.getName());

		final JpaOperations jpaOperations = TestUtils.getPropertyValue(jpaExecutor, "jpaOperations", JpaOperations.class);

		assertNotNull(jpaOperations);

		LiteralExpression expression = TestUtils.getPropertyValue(jpaExecutor, "maxResultsExpression", LiteralExpression.class);

		assertNotNull(expression);

		assertEquals("13", TestUtils.getPropertyValue(expression, "literalValue"));

		assertTrue(TestUtils.getPropertyValue(jpaExecutor, "deleteAfterPoll", Boolean.class));
		assertTrue(TestUtils.getPropertyValue(jpaExecutor, "flush", Boolean.class));
	}

	@Test
	public void testJpaInboundChannelAdapterParserWithMaxResultsExpression() throws Exception {

		setUp("JpaInboundChannelAdapterParserTests.xml", getClass(), "jpaInboundChannelAdapter3");

		final AbstractMessageChannel outputChannel = TestUtils.getPropertyValue(this.consumer, "outputChannel", AbstractMessageChannel.class);

		assertEquals("out", outputChannel.getComponentName());

		final JpaExecutor jpaExecutor = TestUtils.getPropertyValue(this.consumer, "source.jpaExecutor", JpaExecutor.class);

		assertNotNull(jpaExecutor);

		final Class<?> entityClass = TestUtils.getPropertyValue(jpaExecutor, "entityClass", Class.class);

		assertEquals("org.springframework.integration.jpa.test.entity.StudentDomain", entityClass.getName());

		final JpaOperations jpaOperations = TestUtils.getPropertyValue(jpaExecutor, "jpaOperations", JpaOperations.class);

		assertNotNull(jpaOperations);

		SpelExpression expression = TestUtils.getPropertyValue(jpaExecutor, "maxResultsExpression", SpelExpression.class);

		assertNotNull(expression);

		assertEquals("@maxNumberOfResults", TestUtils.getPropertyValue(expression, "expression"));

	}


	@Test
	public void testJpaExecutorBeanIdNaming() throws Exception {

		this.context = new ClassPathXmlApplicationContext("JpaInboundChannelAdapterParserTests.xml", getClass());

		assertNotNull(context.getBean("jpaInboundChannelAdapter1.jpaExecutor", JpaExecutor.class));
		assertNotNull(context.getBean("jpaInboundChannelAdapter2.jpaExecutor", JpaExecutor.class));

	}

	@After
	public void tearDown(){
		if(context != null){
			context.close();
		}
	}

	public void setUp(String name, Class<?> cls, String consumerId){
		context    = new ClassPathXmlApplicationContext(name, cls);
		consumer   = this.context.getBean(consumerId, SourcePollingChannelAdapter.class);
	}

}
