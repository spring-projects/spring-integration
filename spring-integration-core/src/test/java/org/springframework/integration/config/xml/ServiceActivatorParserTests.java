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

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.handler.ServiceActivatingHandler;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Mark Fisher
 * @author Gary Russell
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

	@SuppressWarnings("unused") // testing auto wiring only
	@Autowired
	@Qualifier("org.springframework.integration.config.ServiceActivatorFactoryBean#0")
	private ServiceActivatingHandler testAliasByGeneratedName;

	@SuppressWarnings("unused") // testing auto wiring only
	@Autowired
	@Qualifier("testAlias.handler")
	private ServiceActivatingHandler testAlias;

	@Test
	public void literalExpression() {
		Object result = this.sendAndReceive(literalExpressionInput, "hello");
		assertEquals("foo", result);
	}

	@Test
	public void beanAsTarget() {
		Object result = this.sendAndReceive(beanAsTargetInput, "hello");
		assertEquals("HELLO", result);
	}

	@Test
	public void beanAsArgument() {
		Object result = this.sendAndReceive(beanAsArgumentInput, new TestPayload());
		assertEquals("TestBean", result);
	}

	@Test
	public void beanInvocationResult() {
		Object result = this.sendAndReceive(beanInvocationResultInput, "hello");
		assertEquals("helloFOO", result);
	}

	@Test
	public void multipleLiteralArgs() {
		Object result = this.sendAndReceive(multipleLiteralArgsInput, "hello");
		assertEquals("foobar", result);
	}

	@Test
	public void multipleArgsFromPayload() {
		Object result = this.sendAndReceive(multipleArgsFromPayloadInput, new TestPerson("John", "Doe"));
		assertEquals("JohnDoe", result);
	}


	private Object sendAndReceive(MessageChannel channel, Object payload) {
		MessagingTemplate template = new MessagingTemplate(channel);
		return template.convertSendAndReceive(payload);
	}


	@SuppressWarnings("unused")
	private static class TestBean {

		public String caps(String s) {
			return s.toUpperCase();
		}

		public String concat(String s1, String s2) {
			return s1 + s2;
		}
	}


	@SuppressWarnings("unused")
	private static class TestPayload {

		public String getSimpleClassName(Object o) {
			return o.getClass().getSimpleName();
		}
	}


	@SuppressWarnings("unused")
	private static class TestPerson {

		private String firstName;

		private String lastName;

		public TestPerson(String firstName, String lastName) {
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

}
