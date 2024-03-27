/*
 * Copyright 2002-2024 the original author or authors.
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

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.handler.ServiceActivatingHandler;
import org.springframework.integration.handler.advice.AbstractRequestHandlerAdvice;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.0
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class ServiceActivatorParserTests {

	@Autowired
	private MessageChannel literalExpressionInput;

	@Autowired
	private MessageChannel beanAsTargetInput;

	@Autowired
	private MessageChannel beanAsArgumentInput;

	@Autowired
	private MessageChannel beanInvocationResultInput;

	@Autowired
	private MessageChannel multipleLiteralArgsInput;

	@Autowired
	private MessageChannel multipleArgsFromPayloadInput;

	@Autowired
	private MessageChannel advisedInput;

	@SuppressWarnings("unused") // testing auto wiring only
	@Autowired
	@Qualifier("org.springframework.integration.config.ServiceActivatorFactoryBean#0")
	private ServiceActivatingHandler testAliasByGeneratedName;

	@SuppressWarnings("unused") // testing auto wiring only
	@Autowired
	@Qualifier("testAlias.handler")
	private ServiceActivatingHandler testAlias;

	@Autowired
	@Qualifier("testAlias")
	private EventDrivenConsumer testAliasEndpoint;

	@Test
	public void literalExpression() {
		Object result = this.sendAndReceive(literalExpressionInput, "hello");
		assertThat(result).isEqualTo("foo");
	}

	@Test
	public void beanAsTarget() {
		Object result = this.sendAndReceive(beanAsTargetInput, "hello");
		assertThat(result).isEqualTo("HELLO");
	}

	@Test
	public void beanAsArgument() {
		Object result = this.sendAndReceive(beanAsArgumentInput, new TestPayload());
		assertThat(result).isEqualTo("TestBean");
	}

	@Test
	public void beanInvocationResult() {
		Object result = this.sendAndReceive(beanInvocationResultInput, "hello");
		assertThat(result).isEqualTo("helloFOO");
	}

	@Test
	public void multipleLiteralArgs() {
		Object result = this.sendAndReceive(multipleLiteralArgsInput, "hello");
		assertThat(result).isEqualTo("foobar");
	}

	@Test
	public void multipleArgsFromPayload() {
		Object result = this.sendAndReceive(multipleArgsFromPayloadInput, new TestPerson("John", "Doe"));
		assertThat(result).isEqualTo("JohnDoe");
	}

	@Test
	public void advised() {
		Object result = this.sendAndReceive(advisedInput, "hello");
		assertThat(result).isEqualTo("bar");
	}

	@Test
	public void failRefAndExpression() {
		try {
			new ClassPathXmlApplicationContext(this.getClass().getSimpleName() + "-fail-ref-and-expression-context.xml",
					this.getClass()).close();
			fail("Expected exception");
		}
		catch (BeanDefinitionParsingException e) {
			assertThat(e.getMessage()
					.startsWith("Configuration problem: Only one of 'ref' or 'expression' is permitted, not both, " +
							"on element 'service-activator' with id='test'.")).isTrue();
		}
	}

	@Test
	public void failRefAndBean() {
		try {
			new ClassPathXmlApplicationContext(this.getClass().getSimpleName() + "-fail-ref-and-bean-context.xml",
					this.getClass()).close();
			fail("Expected exception");
		}
		catch (BeanDefinitionParsingException e) {
			assertThat(e.getMessage().startsWith("Configuration problem: Ambiguous definition. " +
					"Inner bean org.springframework.integration.config.xml.ServiceActivatorParserTests$TestBean " +
					"declaration and \"ref\" testBean are not allowed together on element " +
					"'service-activator' with id='test'.")).isTrue();
		}
	}

	@Test
	public void failExpressionAndBean() {
		try {
			new ClassPathXmlApplicationContext(this.getClass().getSimpleName() + "-fail-expression-and-bean-context.xml",
					this.getClass()).close();
			fail("Expected exception");
		}
		catch (BeanDefinitionParsingException e) {
			assertThat(e.getMessage().startsWith("Configuration problem: Neither 'ref' nor 'expression' " +
					"are permitted when an inner bean (<bean/>) is configured on element " +
					"'service-activator' with id='test'.")).isTrue();
		}
	}

	@Test
	public void failNoService() {
		try {
			new ClassPathXmlApplicationContext(this.getClass().getSimpleName() + "-fail-no-service-context.xml",
					this.getClass()).close();
			fail("Expected exception");
		}
		catch (BeanDefinitionParsingException e) {
			assertThat(e.getMessage().startsWith("Configuration problem: Exactly one of the 'ref' " +
					"attribute, 'expression' attribute, or inner bean (<bean/>) definition " +
					"is required for element 'service-activator' with id='test'.")).isTrue();
		}
	}

	@Test
	public void failExpressionAndExpression() {
		try {
			new ClassPathXmlApplicationContext(this.getClass().getSimpleName() + "-fail-expression-and-expression-element-context.xml",
					this.getClass()).close();
			fail("Expected exception");
		}
		catch (BeanDefinitionParsingException e) {
			assertThat(e.getMessage()
					.startsWith("Configuration problem: Neither 'ref' nor 'expression' are permitted when " +
							"an inner 'expression' element is configured on element " +
							"'service-activator' with id='test'.")).isTrue();
		}
	}

	@Test
	public void failMethodAndExpressionElement() {
		try {
			new ClassPathXmlApplicationContext(this.getClass().getSimpleName() + "-fail-method-and-expression-element-context.xml",
					this.getClass()).close();
			fail("Expected exception");
		}
		catch (BeanDefinitionParsingException e) {
			assertThat(e.getMessage()
					.startsWith("Configuration problem: A 'method' attribute is not permitted when configuring " +
							"an 'expression' on element 'service-activator' with id='test'.")).isTrue();
		}
	}

	@Test
	public void testConsumerEndpointFactoryBeanDefaultPhase() {
		assertThat(this.testAliasEndpoint.getPhase()).isEqualTo(Integer.MIN_VALUE);
	}

	private Object sendAndReceive(MessageChannel channel, Object payload) {
		MessagingTemplate template = new MessagingTemplate();
		template.setDefaultDestination(channel);

		return template.convertSendAndReceive(payload, Object.class);
	}

	@SuppressWarnings("unused")
	private static class TestBean {

		TestBean() {
			super();
		}

		public String caps(String s) {
			return s.toUpperCase();
		}

		public String concat(String s1, String s2) {
			return s1 + s2;
		}

	}

	@SuppressWarnings("unused")
	private static class TestPayload {

		TestPayload() {
			super();
		}

		public String getSimpleClassName(Object o) {
			return o.getClass().getSimpleName();
		}

	}

	@SuppressWarnings("unused")
	private static class TestPerson {

		private final String firstName;

		private final String lastName;

		TestPerson(String firstName, String lastName) {
			this.firstName = firstName;
			this.lastName = lastName;
		}

		public String getFirstName() {
			return firstName;
		}

		public String getLastName() {
			return lastName;
		}

	}

	public static class BarAdvice extends AbstractRequestHandlerAdvice {

		@Override
		protected Object doInvoke(ExecutionCallback callback, Object target, Message<?> message) {
			callback.execute();
			return "bar";
		}

	}

}
