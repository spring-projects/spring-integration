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

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.hamcrest.Matchers;
import org.junit.Test;

import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.PropertyAccessor;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.IntegrationEvaluationContextFactoryBean;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.json.JsonPathUtils;
import org.springframework.integration.support.MutableMessageBuilder;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.GenericMessage;

/**
 * @author Gary Russell
 * @author Artem Bilan
 * @since 3.0
 */
public class ParentContextTests {

	private static final List<EvaluationContext> evalContexts = new ArrayList<EvaluationContext>();

	/**
	 * Verifies that beans in hierarchical contexts get an evaluation context that has the proper
	 * BeanResolver. Verifies that the two Foos in the parent context get an evaluation context
	 * with the same bean resolver. Verifies that the one Foo in the child context gets a different
	 * bean resolver. Verifies that bean references in SpEL expressions to beans in the child
	 * and parent contexts work. Verifies that PropertyAccessors are inherited in the child context
	 * and the parent's ones are last in the propertyAccessors list of EvaluationContext.
	 * Verifies that SpEL functions are inherited from parent context and overridden with the same 'id'.
	 * Verifies that child and parent contexts can have different message builders.
	 */
	@Test
	@SuppressWarnings("unchecked")
	public void testSpelBeanReferencesInChildAndParent() throws Exception {
		AbstractApplicationContext parent = new ClassPathXmlApplicationContext("ParentContext-context.xml", this.getClass());

		Object parentEvaluationContextFactoryBean = parent.getBean(IntegrationEvaluationContextFactoryBean.class);
		Map<?, ?> parentFunctions = TestUtils.getPropertyValue(parentEvaluationContextFactoryBean, "functions", Map.class);
		assertEquals(3, parentFunctions.size());
		Object jsonPath = parentFunctions.get("jsonPath");
		assertNotNull(jsonPath);
		assertThat((Method) jsonPath, Matchers.isOneOf(JsonPathUtils.class.getMethods()));
		assertEquals(2, evalContexts.size());
		ClassPathXmlApplicationContext child = new ClassPathXmlApplicationContext(parent);
		child.setConfigLocation("org/springframework/integration/expression/ChildContext-context.xml");
		child.refresh();

		Object childEvaluationContextFactoryBean = child.getBean(IntegrationEvaluationContextFactoryBean.class);
		Map<?, ?> childFunctions = TestUtils.getPropertyValue(childEvaluationContextFactoryBean, "functions", Map.class);
		assertEquals(4, childFunctions.size());
		assertTrue(childFunctions.containsKey("barParent"));
		jsonPath = childFunctions.get("jsonPath");
		assertNotNull(jsonPath);
		assertThat((Method) jsonPath, Matchers.not(Matchers.isOneOf(JsonPathUtils.class.getMethods())));
		assertEquals(3, evalContexts.size());
		assertSame(evalContexts.get(0).getBeanResolver(), evalContexts.get(1).getBeanResolver());

		List<PropertyAccessor> propertyAccessors = evalContexts.get(0).getPropertyAccessors();
		assertEquals(4, propertyAccessors.size());
		PropertyAccessor parentPropertyAccessorOverride = parent.getBean("jsonPropertyAccessor", PropertyAccessor.class);
		PropertyAccessor parentPropertyAccessor = parent.getBean("parentJsonPropertyAccessor", PropertyAccessor.class);
		assertTrue(propertyAccessors.contains(parentPropertyAccessorOverride));
		assertTrue(propertyAccessors.contains(parentPropertyAccessor));
		assertTrue(propertyAccessors.indexOf(parentPropertyAccessorOverride) > propertyAccessors.indexOf(parentPropertyAccessor));

		Map<String, Object> variables = (Map<String, Object>) TestUtils.getPropertyValue(evalContexts.get(0), "variables");
		assertEquals(3, variables.size());
		assertTrue(variables.containsKey("bar"));
		assertTrue(variables.containsKey("barParent"));
		assertTrue(variables.containsKey("jsonPath"));

		assertNotSame(evalContexts.get(1).getBeanResolver(), evalContexts.get(2).getBeanResolver());
		propertyAccessors = evalContexts.get(1).getPropertyAccessors();
		assertEquals(4, propertyAccessors.size());
		assertTrue(propertyAccessors.contains(parentPropertyAccessorOverride));

		variables = (Map<String, Object>) TestUtils.getPropertyValue(evalContexts.get(1), "variables");
		assertEquals(3, variables.size());
		assertTrue(variables.containsKey("bar"));
		assertTrue(variables.containsKey("barParent"));
		assertTrue(variables.containsKey("jsonPath"));

		propertyAccessors = evalContexts.get(2).getPropertyAccessors();
		assertEquals(4, propertyAccessors.size());
		PropertyAccessor childPropertyAccessor = child.getBean("jsonPropertyAccessor", PropertyAccessor.class);
		assertTrue(propertyAccessors.contains(childPropertyAccessor));
		assertTrue(propertyAccessors.contains(parentPropertyAccessor));
		assertFalse(propertyAccessors.contains(parentPropertyAccessorOverride));
		assertTrue(propertyAccessors.indexOf(childPropertyAccessor) < propertyAccessors.indexOf(parentPropertyAccessor));

		variables = (Map<String, Object>) TestUtils.getPropertyValue(evalContexts.get(2), "variables");
		assertEquals(4, variables.size());
		assertTrue(variables.containsKey("bar"));
		assertTrue(variables.containsKey("barParent"));
		assertTrue(variables.containsKey("barChild"));
		assertTrue(variables.containsKey("jsonPath"));

		// Test transformer expressions
		child.getBean("input", MessageChannel.class).send(new GenericMessage<String>("baz"));
		Message<?> out = child.getBean("output", QueueChannel.class).receive(0);
		assertNotNull(out);
		assertEquals("foobar", out.getPayload());
		child.getBean("parentIn", MessageChannel.class).send(MutableMessageBuilder.withPayload("bar").build());
		out = child.getBean("parentOut", QueueChannel.class).receive(0);
		assertNotNull(out);
		assertThat(out, instanceOf(GenericMessage.class));
		assertEquals("foo", out.getPayload());

		IntegrationEvaluationContextFactoryBean evaluationContextFactoryBean =
				child.getBean("&" + IntegrationContextUtils.INTEGRATION_EVALUATION_CONTEXT_BEAN_NAME,
						IntegrationEvaluationContextFactoryBean.class);
		try {
			evaluationContextFactoryBean.setPropertyAccessors(Collections.<String, PropertyAccessor>emptyMap());
			fail("IllegalArgumentException expected.");
		}
		catch (Exception e) {
			assertThat(e, Matchers.instanceOf(IllegalArgumentException.class));
		}

		parent.getBean("fromParentToChild", MessageChannel.class).send(new GenericMessage<String>("foo"));
		out = child.getBean("output", QueueChannel.class).receive(0);
		assertNotNull(out);
		assertEquals("org.springframework.integration.support.MutableMessage", out.getClass().getName());
		assertEquals("FOO", out.getPayload());

		child.close();
		parent.close();
	}

	public static class Foo implements IntegrationEvaluationContextAware {

		@Override
		public void setIntegrationEvaluationContext(EvaluationContext evaluationContext) {
			evalContexts.add(evaluationContext);
		}

	}

	public static class Bar {

		public static Object bar(Object o) {
			return o;
		}

	}
}
