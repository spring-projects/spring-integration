/*
 * Copyright 2013-2021 the original author or authors.
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

package org.springframework.integration.expression;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.PropertyAccessor;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.IntegrationEvaluationContextFactoryBean;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.json.JsonPathUtils;
import org.springframework.integration.json.TestPerson;
import org.springframework.integration.support.MutableMessageBuilder;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;

/**
 * @author Gary Russell
 * @author Artem Bilan
 * @author Pierre Lakreb
 *
 * @since 3.0
 */
public class ParentContextTests {

	private static final List<EvaluationContext> evalContexts = new ArrayList<>();

	/**
	 * Verifies that beans in hierarchical contexts get an evaluation context that has the proper
	 * BeanResolver. Verifies that the two Foos in the parent context get an evaluation context
	 * with the same bean resolver. Verifies that the one Foo in the child context gets a different
	 * bean resolver. Verifies that bean references in SpEL expressions to beans in the child
	 * and parent contexts work. Verifies that PropertyAccessors are inherited in the child context
	 * and the parent's ones are last in the propertyAccessors list of EvaluationContext.
	 * Verifies that SpEL functions are inherited from parent context and overridden with the same 'id'.
	 * Verifies that child and parent contexts can have different message builders.
	 * <p>
	 * Only single test method is allowed for 'ParentContext-context.xml',
	 * since it relies on static 'evalContexts' variable.
	 */
	@Test
	@SuppressWarnings("unchecked")
	public void testSpelBeanReferencesInChildAndParent() {
		AbstractApplicationContext parent = new ClassPathXmlApplicationContext("ParentContext-context.xml",
				this.getClass());

		Object parentEvaluationContextFactoryBean = parent.getBean(IntegrationEvaluationContextFactoryBean.class);
		Map<?, ?> parentFunctions = TestUtils.getPropertyValue(parentEvaluationContextFactoryBean, "functions",
				Map.class);
		assertThat(parentFunctions.size()).isEqualTo(4);
		Object jsonPath = parentFunctions.get("jsonPath");
		assertThat(jsonPath).isNotNull();
		assertThat(jsonPath).isIn(Arrays.asList(JsonPathUtils.class.getMethods()));
		assertThat(evalContexts.size()).isEqualTo(2);
		ClassPathXmlApplicationContext child = new ClassPathXmlApplicationContext(parent);
		child.setConfigLocation("org/springframework/integration/expression/ChildContext-context.xml");
		child.refresh();

		Object childEvaluationContextFactoryBean = child.getBean(IntegrationEvaluationContextFactoryBean.class);
		Map<?, ?> childFunctions = TestUtils.getPropertyValue(childEvaluationContextFactoryBean, "functions",
				Map.class);
		assertThat(childFunctions.size()).isEqualTo(5);
		assertThat(childFunctions.containsKey("barParent")).isTrue();
		assertThat(childFunctions.containsKey("fooFunc")).isTrue();
		jsonPath = childFunctions.get("jsonPath");
		assertThat(jsonPath).isNotNull();
		assertThat(jsonPath).isNotIn(Arrays.asList((JsonPathUtils.class.getMethods())));
		assertThat(evalContexts.size()).isEqualTo(3);
		assertThat(evalContexts.get(1).getBeanResolver()).isSameAs(evalContexts.get(0).getBeanResolver());

		List<PropertyAccessor> propertyAccessors = evalContexts.get(0).getPropertyAccessors();
		assertThat(propertyAccessors.size()).isEqualTo(4);
		PropertyAccessor parentPropertyAccessorOverride = parent
				.getBean("jsonPropertyAccessor", PropertyAccessor.class);
		PropertyAccessor parentPropertyAccessor = parent.getBean("parentJsonPropertyAccessor", PropertyAccessor.class);
		assertThat(propertyAccessors.contains(parentPropertyAccessorOverride)).isTrue();
		assertThat(propertyAccessors.contains(parentPropertyAccessor)).isTrue();
		assertThat(propertyAccessors.indexOf(parentPropertyAccessorOverride) > propertyAccessors
				.indexOf(parentPropertyAccessor)).isTrue();

		Map<String, Object> variables = (Map<String, Object>) TestUtils.getPropertyValue(evalContexts.get(0),
				"variables");
		assertThat(variables).hasSize(4);
		assertThat(variables).containsKeys("bar", "barParent", "fooFunc", "jsonPath");

		assertThat(evalContexts.get(2).getBeanResolver()).isNotSameAs(evalContexts.get(1).getBeanResolver());
		propertyAccessors = evalContexts.get(1).getPropertyAccessors();
		assertThat(propertyAccessors.size()).isEqualTo(4);
		assertThat(propertyAccessors.contains(parentPropertyAccessorOverride)).isTrue();

		variables = (Map<String, Object>) TestUtils.getPropertyValue(evalContexts.get(1), "variables");
		assertThat(variables).hasSize(4);
		assertThat(variables).containsKeys("bar", "barParent", "fooFunc", "jsonPath");

		propertyAccessors = evalContexts.get(2).getPropertyAccessors();
		assertThat(propertyAccessors.size()).isEqualTo(4);
		PropertyAccessor childPropertyAccessor = child.getBean("jsonPropertyAccessor", PropertyAccessor.class);
		assertThat(propertyAccessors.contains(childPropertyAccessor)).isTrue();
		assertThat(propertyAccessors.contains(parentPropertyAccessor)).isTrue();
		assertThat(propertyAccessors.contains(parentPropertyAccessorOverride)).isFalse();
		assertThat(propertyAccessors.indexOf(childPropertyAccessor) < propertyAccessors.indexOf(parentPropertyAccessor))
				.isTrue();

		variables = (Map<String, Object>) TestUtils.getPropertyValue(evalContexts.get(2), "variables");
		assertThat(variables).hasSize(5);
		assertThat(variables).containsKeys("bar", "barParent", "fooFunc", "barChild", "jsonPath");

		// Test transformer expressions
		child.getBean("input", MessageChannel.class).send(new GenericMessage<>("baz"));
		Message<?> out = child.getBean("output", QueueChannel.class).receive(0);
		assertThat(out).isNotNull();
		assertThat(out.getPayload()).isEqualTo("foobar");
		child.getBean("parentIn", MessageChannel.class).send(MutableMessageBuilder.withPayload("bar").build());
		out = child.getBean("parentOut", QueueChannel.class).receive(0);
		assertThat(out).isNotNull();
		assertThat(out).isInstanceOf(GenericMessage.class);
		assertThat(out.getPayload()).isEqualTo("foo");

		IntegrationEvaluationContextFactoryBean evaluationContextFactoryBean =
				child.getBean("&" + IntegrationContextUtils.INTEGRATION_EVALUATION_CONTEXT_BEAN_NAME,
						IntegrationEvaluationContextFactoryBean.class);
		try {
			evaluationContextFactoryBean.setPropertyAccessors(Collections.emptyMap());
			fail("IllegalArgumentException expected.");
		}
		catch (Exception e) {
			assertThat(e).isInstanceOf(IllegalArgumentException.class);
		}

		parent.getBean("fromParentToChild", MessageChannel.class).send(new GenericMessage<>("foo"));
		out = child.getBean("output", QueueChannel.class).receive(0);
		assertThat(out).isNotNull();
		assertThat(out.getClass().getName()).isEqualTo("org.springframework.integration.support.MutableMessage");
		assertThat(out.getPayload()).isEqualTo("FOO");

		assertThat(parent
				.containsBean(IntegrationContextUtils.JSON_NODE_WRAPPER_TO_JSON_NODE_CONVERTER))
				.isTrue();

		assertThat(child
				.containsBean(IntegrationContextUtils.JSON_NODE_WRAPPER_TO_JSON_NODE_CONVERTER))
				.isTrue();

		Object converterRegistrar = parent.getBean(IntegrationContextUtils.CONVERTER_REGISTRAR_BEAN_NAME);
		assertThat(converterRegistrar).isNotNull();
		Set<?> converters = TestUtils.getPropertyValue(converterRegistrar, "converters", Set.class);
		boolean jsonNodeWrapperToJsonNodeConverterPresent = false;
		for (Object converter : converters) {
			if ("JsonNodeWrapperToJsonNodeConverter".equals(converter.getClass().getSimpleName())) {
				jsonNodeWrapperToJsonNodeConverterPresent = true;
				break;
			}
		}

		assertThat(jsonNodeWrapperToJsonNodeConverterPresent).isTrue();

		MessageChannel input = parent.getBean("testJsonNodeToStringConverterInputChannel", MessageChannel.class);
		PollableChannel output = parent.getBean("testJsonNodeToStringConverterOutputChannel", PollableChannel.class);

		TestPerson person = new TestPerson();
		person.setFirstName("John");

		input.send(new GenericMessage<Object>(person));

		Message<?> result = output.receive(1000);
		assertThat(result).isNotNull();
		assertThat(result.getPayload()).isEqualTo("JOHN");

		child.close();
		parent.close();
	}

	public static class Foo implements BeanFactoryAware, InitializingBean {

		private BeanFactory beanFactory;

		@Override
		public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
			this.beanFactory = beanFactory;
		}

		@Override
		public void afterPropertiesSet() {
			evalContexts.add(ExpressionUtils.createStandardEvaluationContext(this.beanFactory));
		}

	}

	public static class Bar {

		public static Object bar(Object o) {
			return o;
		}

		public String testJsonNodeToStringConverter(String payload) {
			return payload.toUpperCase();
		}

	}

}
