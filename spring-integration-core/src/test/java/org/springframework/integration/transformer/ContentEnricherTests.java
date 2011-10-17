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

package org.springframework.integration.transformer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.Message;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.support.MessageBuilder;

/**
 * @author Mark Fisher
 * @since 2.1
 */
public class ContentEnricherTests {

	@Test
	public void simpleProperty() {
		QueueChannel replyChannel = new QueueChannel();
		DirectChannel requestChannel = new DirectChannel();
		requestChannel.subscribe(new AbstractReplyProducingMessageHandler() {
			@Override
			protected Object handleRequestMessage(Message<?> requestMessage) {
				return new Source("John", "Doe");
			}
		});
		ContentEnricher enricher = new ContentEnricher(requestChannel);
		SpelExpressionParser parser = new SpelExpressionParser();
		Map<String, Expression> propertyExpressions = new HashMap<String, Expression>();
		propertyExpressions.put("name", parser.parseExpression("payload.lastName + ', ' + payload.firstName"));
		enricher.setPropertyExpressions(propertyExpressions);
		Target target = new Target("replace me");
		Message<?> requestMessage = MessageBuilder.withPayload(target).setReplyChannel(replyChannel).build();
		enricher.handleMessage(requestMessage);
		Message<?> reply = replyChannel.receive(0);
		assertEquals("Doe, John", ((Target) reply.getPayload()).getName());
	}

	@Test
	public void nestedProperty() {
		QueueChannel replyChannel = new QueueChannel();
		DirectChannel requestChannel = new DirectChannel();
		requestChannel.subscribe(new AbstractReplyProducingMessageHandler() {
			@Override
			protected Object handleRequestMessage(Message<?> requestMessage) {
				return new Source("John", "Doe");
			}
		});
		ContentEnricher enricher = new ContentEnricher(requestChannel);
		SpelExpressionParser parser = new SpelExpressionParser();
		Map<String, Expression> propertyExpressions = new HashMap<String, Expression>();
		propertyExpressions.put("child.name", parser.parseExpression("payload.lastName + ', ' + payload.firstName"));
		enricher.setPropertyExpressions(propertyExpressions);
		Target target = new Target("test");
		Message<?> requestMessage = MessageBuilder.withPayload(target).setReplyChannel(replyChannel).build();
		enricher.handleMessage(requestMessage);
		Message<?> reply = replyChannel.receive(0);
		Target result = (Target) reply.getPayload();
		assertEquals("test", result.getName());
		assertEquals("Doe, John", result.getChild().getName());
	}

	@Test
	public void clonePayload() {
		QueueChannel replyChannel = new QueueChannel();
		DirectChannel requestChannel = new DirectChannel();
		requestChannel.subscribe(new AbstractReplyProducingMessageHandler() {
			@Override
			protected Object handleRequestMessage(Message<?> requestMessage) {
				return new Source("John", "Doe");
			}
		});
		ContentEnricher enricher = new ContentEnricher(requestChannel);
		enricher.setShouldClonePayload(true);
		SpelExpressionParser parser = new SpelExpressionParser();
		Map<String, Expression> propertyExpressions = new HashMap<String, Expression>();
		propertyExpressions.put("name", parser.parseExpression("payload.lastName + ', ' + payload.firstName"));
		enricher.setPropertyExpressions(propertyExpressions);
		Target target = new Target("replace me");
		Message<?> requestMessage = MessageBuilder.withPayload(target).setReplyChannel(replyChannel).build();
		enricher.handleMessage(requestMessage);
		Message<?> reply = replyChannel.receive(0);
		Target result = (Target) reply.getPayload();
		assertEquals("Doe, John", result.getName());
		assertNotSame(target, result);
	}


	@SuppressWarnings("unused")
	private static final class Source {

		private final String firstName, lastName;

		Source(String firstName, String lastName) {
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


	public static final class Target implements Cloneable {

		private volatile String name;

		private volatile Target child;

		public Target() {
			this.name = "default";
		}

		private Target(String name) {
			this.name = name;
		}

		public String getName() {
			return this.name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Target getChild() {
			return this.child;
		}

		public void setChild(Target child) {
			this.child = child;
		}

		public Object clone() {
			Target clone = new Target(this.name);
			clone.setChild(this.child);
			return clone;
		}
	}

}