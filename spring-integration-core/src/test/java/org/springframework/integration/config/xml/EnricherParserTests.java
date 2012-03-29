/*
 * Copyright 2002-2011 the original author or authors.
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
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.expression.Expression;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.core.PollableChannel;
import org.springframework.integration.core.SubscribableChannel;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.transformer.ContentEnricher;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Mark Fisher
 * @author Gunnar Hillert
 * 
 * @since 2.1
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class EnricherParserTests {

	@Autowired
	private ApplicationContext context;


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

		Map<Expression, Expression> propertyExpressions = (Map<Expression, Expression>) accessor.getPropertyValue("propertyExpressions");
		for (Map.Entry<Expression, Expression> e : propertyExpressions.entrySet()) {
			if ("name".equals(e.getKey().getExpressionString())) {
				assertEquals("payload.sourceName", e.getValue().getExpressionString());
			}
			else if ("age".equals(e.getKey().getExpressionString())) {
				assertEquals("42", e.getValue().getExpressionString());
			}
			else if ("gender".equals(e.getKey().getExpressionString())) {
				assertEquals("@testBean", e.getValue().getExpressionString());
			}
			else {
				throw new IllegalStateException("expected 'name', 'age', and 'gender' only, not: " + e.getKey().getExpressionString());
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
		requests.subscribe(new AbstractReplyProducingMessageHandler() {
			@Override
			protected Object handleRequestMessage(Message<?> requestMessage) {
				return new Source("foo");
			}
		});
		Target original = new Target();
		Message<?> request = MessageBuilder.withPayload(original).build();
		context.getBean("input", MessageChannel.class).send(request);
		Message<?> reply = context.getBean("output", PollableChannel.class).receive(0);
		Target enriched = (Target) reply.getPayload();
		assertEquals("foo", enriched.getName());
		assertEquals(42, enriched.getAge());
		assertEquals("male", enriched.getGender());
		assertNotSame(original, enriched);
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

		private volatile String gender;

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

		public String getGender() {
			return gender;
		}

		public void setGender(String gender) {
			this.gender = gender;
		}

		public Object clone() {
			Target copy = new Target();
			copy.setName(this.name);
			copy.setAge(this.age);
			return copy;
		}
	}

}
