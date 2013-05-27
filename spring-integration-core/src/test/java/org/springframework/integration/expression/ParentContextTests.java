/*
 * Copyright 2013 the original author or authors.
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
package org.springframework.integration.expression;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.expression.EvaluationContext;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.test.util.TestUtils;

/**
 * @author Gary Russell
 * @since 3.0
 *
 */
public class ParentContextTests {

	private static final List<EvaluationContext> evalContexts = new ArrayList<EvaluationContext>();

	/**
	 * Verifies that beans in hierarchical contexts get an evaluation context that has the proper
	 * BeanResolver. Verifies that the two Foos in the parent context get an evaluation context
	 * with the same bean resolver. Verifies that the one Foo in the child context gets a different
	 * bean resolver. Verifies that bean references in SpEL expressions to beans in the child
	 * and parent contexts work.
	 */
	@Test
	public void testSpelBeanReferencesInChildAndParent() {
		AbstractApplicationContext parent = new ClassPathXmlApplicationContext("ParentContext-context.xml", this.getClass());
		assertEquals(2, evalContexts.size());
		ClassPathXmlApplicationContext child = new ClassPathXmlApplicationContext(parent);
		child.setConfigLocation("org/springframework/integration/expression/ChildContext-context.xml");
		child.refresh();
		assertEquals(3, evalContexts.size());
		assertSame(evalContexts.get(0).getBeanResolver(), evalContexts.get(1).getBeanResolver());
		assertNotSame(evalContexts.get(1).getBeanResolver(), evalContexts.get(2).getBeanResolver());
		assertSame(parent.getBeanFactory(), TestUtils.getPropertyValue(evalContexts.get(0).getBeanResolver(), "beanFactory"));
		assertSame(child.getBeanFactory(), TestUtils.getPropertyValue(evalContexts.get(2).getBeanResolver(), "beanFactory"));

		// Test transformer expressions
		child.getBean("input", MessageChannel.class).send(new GenericMessage<String>("baz"));
		Message<?> out = child.getBean("output", QueueChannel.class).receive(0);
		assertNotNull(out);
		assertEquals("foobar", out.getPayload());
		child.getBean("parentIn", MessageChannel.class).send(new GenericMessage<String>("bar"));
		out = child.getBean("parentOut", QueueChannel.class).receive(0);
		assertNotNull(out);
		assertEquals("foo", out.getPayload());
	}

	public static class Foo implements IntegrationEvaluationContextAware {

		@Override
		public void setIntegrationEvaluationContext(EvaluationContext evaluationContext) {
			evalContexts.add(evaluationContext);
		}

	}
}
