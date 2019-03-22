/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.integration.jpa.config.xml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.expression.spel.standard.SpelExpression;
import org.springframework.integration.channel.AbstractMessageChannel;
import org.springframework.integration.endpoint.SourcePollingChannelAdapter;
import org.springframework.integration.jpa.core.JpaExecutor;
import org.springframework.integration.jpa.core.JpaOperations;
import org.springframework.integration.jpa.support.parametersource.ParameterSource;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author Gunnar Hillert
 * @author Amol Nayak
 * @author Artem Bilan
 *
 * @since 2.2
 *
 */
@RunWith(SpringRunner.class)
@DirtiesContext
public class JpaInboundChannelAdapterParserTests {

	@Autowired
	private ConfigurableApplicationContext context;

	@Autowired
	private SourcePollingChannelAdapter jpaInboundChannelAdapter1;

	@Autowired
	private SourcePollingChannelAdapter jpaInboundChannelAdapter2;

	@Autowired
	private SourcePollingChannelAdapter jpaInboundChannelAdapter3;

	@Test
	public void testJpaInboundChannelAdapterParser() throws Exception {
		AbstractMessageChannel outputChannel =
				TestUtils.getPropertyValue(this.jpaInboundChannelAdapter1, "outputChannel", AbstractMessageChannel.class);

		assertEquals("out", outputChannel.getComponentName());

		JpaExecutor jpaExecutor =
				TestUtils.getPropertyValue(this.jpaInboundChannelAdapter1, "source.jpaExecutor", JpaExecutor.class);

		assertNotNull(jpaExecutor);

		Class<?> entityClass = TestUtils.getPropertyValue(jpaExecutor, "entityClass", Class.class);

		assertEquals("org.springframework.integration.jpa.test.entity.StudentDomain", entityClass.getName());

		JpaOperations jpaOperations = TestUtils.getPropertyValue(jpaExecutor, "jpaOperations", JpaOperations.class);

		assertNotNull(jpaOperations);

		assertTrue(TestUtils.getPropertyValue(jpaExecutor, "expectSingleResult", Boolean.class));
		ParameterSource parameterSource = this.context.getBean(ParameterSource.class);
		assertSame(parameterSource, TestUtils.getPropertyValue(jpaExecutor, "parameterSource"));
	}

	@Test
	public void testJpaInboundChannelAdapterParserWithMaxResults() throws Exception {
		AbstractMessageChannel outputChannel =
				TestUtils.getPropertyValue(this.jpaInboundChannelAdapter2, "outputChannel", AbstractMessageChannel.class);

		assertEquals("out", outputChannel.getComponentName());

		JpaExecutor jpaExecutor =
				TestUtils.getPropertyValue(this.jpaInboundChannelAdapter2, "source.jpaExecutor", JpaExecutor.class);

		assertNotNull(jpaExecutor);

		Class<?> entityClass = TestUtils.getPropertyValue(jpaExecutor, "entityClass", Class.class);

		assertEquals("org.springframework.integration.jpa.test.entity.StudentDomain", entityClass.getName());

		JpaOperations jpaOperations = TestUtils.getPropertyValue(jpaExecutor, "jpaOperations", JpaOperations.class);

		assertNotNull(jpaOperations);

		LiteralExpression expression = TestUtils.getPropertyValue(jpaExecutor, "maxResultsExpression", LiteralExpression.class);

		assertNotNull(expression);

		assertEquals("13", TestUtils.getPropertyValue(expression, "literalValue"));

		assertTrue(TestUtils.getPropertyValue(jpaExecutor, "deleteAfterPoll", Boolean.class));
		assertTrue(TestUtils.getPropertyValue(jpaExecutor, "flush", Boolean.class));
	}

	@Test
	public void testJpaInboundChannelAdapterParserWithMaxResultsExpression() throws Exception {
		AbstractMessageChannel outputChannel =
				TestUtils.getPropertyValue(this.jpaInboundChannelAdapter3, "outputChannel", AbstractMessageChannel.class);

		assertEquals("out", outputChannel.getComponentName());

		JpaExecutor jpaExecutor =
				TestUtils.getPropertyValue(this.jpaInboundChannelAdapter3, "source.jpaExecutor", JpaExecutor.class);

		assertNotNull(jpaExecutor);

		Class<?> entityClass = TestUtils.getPropertyValue(jpaExecutor, "entityClass", Class.class);

		assertEquals("org.springframework.integration.jpa.test.entity.StudentDomain", entityClass.getName());

		final JpaOperations jpaOperations = TestUtils.getPropertyValue(jpaExecutor, "jpaOperations", JpaOperations.class);

		assertNotNull(jpaOperations);

		SpelExpression expression = TestUtils.getPropertyValue(jpaExecutor, "maxResultsExpression", SpelExpression.class);

		assertNotNull(expression);

		assertEquals("@maxNumberOfResults", TestUtils.getPropertyValue(expression, "expression"));
	}


	@Test
	public void testJpaExecutorBeanIdNaming() throws Exception {
		JpaExecutor jpaExecutor1 = this.context.getBean("jpaInboundChannelAdapter1.jpaExecutor", JpaExecutor.class);
		JpaExecutor jpaExecutor2 = this.context.getBean("jpaInboundChannelAdapter2.jpaExecutor", JpaExecutor.class);

		assertNotNull(jpaExecutor1);
		assertNotNull(jpaExecutor2);
		assertNotSame(jpaExecutor1, jpaExecutor2);

		assertEquals(5, this.context.getBeansOfType(JpaExecutor.class).size());

		JpaExecutor jpaExecutorWithoutId0 =
				this.context.getBean("org.springframework.integration.config.SourcePollingChannelAdapterFactoryBean#0.jpaExecutor",
						JpaExecutor.class);
		JpaExecutor jpaExecutorWithoutId1 =
				this.context.getBean("org.springframework.integration.config.SourcePollingChannelAdapterFactoryBean#1.jpaExecutor",
						JpaExecutor.class);

		assertNotNull(jpaExecutorWithoutId0);
		assertNotNull(jpaExecutorWithoutId1);
		assertNotSame(jpaExecutorWithoutId0, jpaExecutorWithoutId1);
	}

}
