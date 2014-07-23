/*
 * Copyright 2002-2013 the original author or authors.
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
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.TypeMismatchException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.expression.Expression;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.handler.advice.AbstractRequestHandlerAdvice;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.transformer.ContentEnricher;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Mark Fisher
 * @author Gunnar Hillert
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.1
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class EnricherParserTests {

	@Autowired
	private ApplicationContext context;

	private static volatile int adviceCalled;

	@Test
	@SuppressWarnings("unchecked")
	public void configurationCheck() {
		Object endpoint = context.getBean("enricher");
		assertEquals(EventDrivenConsumer.class, endpoint.getClass());
		Object handler = TestUtils.getPropertyValue(endpoint, "handler");
		assertEquals(ContentEnricher.class, handler.getClass());
		ContentEnricher enricher = (ContentEnricher) handler;
		assertEquals(99, enricher.getOrder());
		DirectFieldAccessor accessor = new DirectFieldAccessor(enricher);
		assertEquals(context.getBean("output"), accessor.getPropertyValue("outputChannel"));
		assertEquals(true, accessor.getPropertyValue("shouldClonePayload"));
		assertNull(accessor.getPropertyValue("requestPayloadExpression"));
		assertNotNull(TestUtils.getPropertyValue(enricher, "gateway.beanFactory"));

		Map<Expression, Expression> propertyExpressions = (Map<Expression, Expression>) accessor.getPropertyValue("propertyExpressions");
		for (Map.Entry<Expression, Expression> e : propertyExpressions.entrySet()) {
			if ("name".equals(e.getKey().getExpressionString())) {
				assertEquals("payload.sourceName", e.getValue().getExpressionString());
			}
			else if ("age".equals(e.getKey().getExpressionString())) {
				assertEquals("42", e.getValue().getExpressionString());
			}
			else if ("gender".equals(e.getKey().getExpressionString())) {
				assertEquals(Gender.MALE.name(), e.getValue().getExpressionString());
			}
			else if ("married".equals(e.getKey().getExpressionString())) {
				assertEquals(Boolean.TRUE.toString(), e.getValue().getExpressionString());
			}
			else {
				throw new IllegalStateException("expected 'name', 'age', 'gender' and married only, not: "
						+ e.getKey().getExpressionString());
			}
		}

	}

	@Test
	public void configurationCheckTimeoutParameters() {

		Object endpoint = context.getBean("enricher");

		Long requestTimeout = TestUtils.getPropertyValue(endpoint, "handler.requestTimeout", Long.class);
		Long replyTimeout   = TestUtils.getPropertyValue(endpoint, "handler.replyTimeout", Long.class);

		assertEquals(Long.valueOf(1234L), requestTimeout);
		assertEquals(Long.valueOf(9876L), replyTimeout);

	}

	@Test
	public void configurationCheckRequiresReply() {

		Object endpoint = context.getBean("enricher");

		boolean requiresReply = TestUtils.getPropertyValue(endpoint, "handler.requiresReply", Boolean.class);

		assertTrue("Was expecting requiresReply to be 'false'", requiresReply);

	}

	@Test
	public void integrationTest() {
		SubscribableChannel requests = context.getBean("requests", SubscribableChannel.class);

		class Foo extends AbstractReplyProducingMessageHandler {

			@Override
			protected Object handleRequestMessage(Message<?> requestMessage) {
				return new Source("foo");
			}
		}

		Foo foo = new Foo();
		foo.setOutputChannel(context.getBean("replies", MessageChannel.class));
		requests.subscribe(foo);
		Target original = new Target();
		Message<?> request = MessageBuilder.withPayload(original)
				.setHeader("sourceName", "test")
				.setHeader("notOverwrite", "test")
				.build();
		context.getBean("input", MessageChannel.class).send(request);
		Message<?> reply = context.getBean("output", PollableChannel.class).receive(0);
		Target enriched = (Target) reply.getPayload();
		assertEquals("foo", enriched.getName());
		assertEquals(42, enriched.getAge());
		assertEquals(Gender.MALE, enriched.getGender());
		assertTrue(enriched.isMarried());
		assertNotSame(original, enriched);
		assertEquals(1, adviceCalled);

		MessageHeaders headers = reply.getHeaders();
		assertEquals("bar", headers.get("foo"));
		assertEquals(Gender.MALE, headers.get("testBean"));
		assertEquals("foo", headers.get("sourceName"));
		assertEquals("test", headers.get("notOverwrite"));
		requests.unsubscribe(foo);
		adviceCalled--;
	}

	@Test
	public void testInt3027WrongHeaderType() {
		MessageChannel input = context.getBean("input2", MessageChannel.class);
		try {
			input.send(new GenericMessage<Object>("test"));
		}
		catch (Exception e) {
			assertThat(e, Matchers.instanceOf(MessageHandlingException.class));
			assertThat(e.getCause(), Matchers.instanceOf(TypeMismatchException.class));
			assertThat(e.getCause().getMessage(),
					Matchers.startsWith("Failed to convert value of type 'java.util.Date' to required type 'int'"));
		}
	}

	private static class Source {

		private final String sourceName;

		Source(String sourceName) {
			this.sourceName = sourceName;
		}

		@SuppressWarnings("unused")
		public String getSourceName() {
			return sourceName;
		}
	}

	public static class Target implements Cloneable {

		private volatile String name;

		private volatile int age;

		private volatile Gender gender;

		private volatile boolean married;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public int getAge() {
			return age;
		}

		public void setAge(int age) {
			this.age = age;
		}

		public Gender getGender() {
			return gender;
		}

		public void setGender(Gender gender) {
			this.gender = gender;
		}

		public boolean isMarried() {
			return married;
		}

		public void setMarried(boolean married) {
			this.married = married;
		}

		@Override
		public Object clone() {
			Target copy = new Target();
			copy.setName(this.name);
			copy.setAge(this.age);
			copy.setGender(this.gender);
			copy.setMarried(this.married);
			return copy;
		}
	}

	public static enum Gender {
		MALE, FEMALE
	}

	public static class FooAdvice extends AbstractRequestHandlerAdvice {

		@Override
		protected Object doInvoke(ExecutionCallback callback, Object target, Message<?> message) throws Exception {
			adviceCalled++;
			return callback.execute();
		}

	}
}
