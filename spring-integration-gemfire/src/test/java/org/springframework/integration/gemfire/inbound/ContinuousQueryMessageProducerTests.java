/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.integration.gemfire.inbound;

import org.apache.geode.cache.Operation;
import org.apache.geode.cache.query.CqEvent;
import org.apache.geode.cache.query.CqQuery;
import org.apache.geode.cache.query.cq.internal.ServerCQImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.data.gemfire.listener.ContinuousQueryListenerContainer;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author David Turanski
 * @author Artem Bilan
 *
 * @since 2.1
 */
public class ContinuousQueryMessageProducerTests {

	private static final SpelExpressionParser PARSER = new SpelExpressionParser();

	private ContinuousQueryMessageProducer cqMessageProducer;

	private CqMessageHandler handler;

	@BeforeEach
	void setUp() {
		ContinuousQueryListenerContainer queryListenerContainer = mock(ContinuousQueryListenerContainer.class);
		this.cqMessageProducer = new ContinuousQueryMessageProducer(queryListenerContainer, "foo");
		DirectChannel outputChannel = new DirectChannel();
		this.cqMessageProducer.setOutputChannel(outputChannel);
		this.cqMessageProducer.setBeanFactory(mock(BeanFactory.class));
		this.handler = new CqMessageHandler();
		outputChannel.subscribe(this.handler);
	}

	@Test
	void testMessageProduced() {
		CqEvent cqEvent = event(Operation.CREATE, "hello");
		this.cqMessageProducer.onEvent(cqEvent);
		assertThat(this.handler.count).isEqualTo(1);
		assertThat(this.handler.payload).isEqualTo(cqEvent);
	}

	@Test
	void testMessageNotProducedForUnsupportedEventType() {
		CqEvent cqEvent = event(Operation.DESTROY, "hello");
		this.cqMessageProducer.onEvent(cqEvent);
		assertThat(this.handler.count).isEqualTo(0);
	}

	@Test
	void testMessageProducedForAddedEventType() {
		CqEvent cqEvent = event(Operation.DESTROY, null);
		this.cqMessageProducer.setSupportedEventTypes(CqEventType.DESTROYED);
		this.cqMessageProducer.onEvent(cqEvent);
		assertThat(this.handler.count).isEqualTo(1);
		assertThat(this.handler.payload).isEqualTo(cqEvent);
	}

	@Test
	void testPayloadExpression() {
		CqEvent cqEvent = event(Operation.CREATE, "hello");
		this.cqMessageProducer.setPayloadExpression(PARSER.parseExpression("newValue.toUpperCase() + ', WORLD'"));
		this.cqMessageProducer.afterPropertiesSet();
		this.cqMessageProducer.onEvent(cqEvent);
		assertThat(this.handler.count).isEqualTo(1);
		assertThat(this.handler.payload).isEqualTo("HELLO, WORLD");
	}

	CqEvent event(final Operation operation, final Object value) {
		return new CqEvent() {

			final CqQuery cq = new ServerCQImpl();

			final byte[] ba = new byte[0];

			final Object key = new Object();

			final Exception ex = new Exception();

			public Operation getBaseOperation() {
				return operation;
			}

			public CqQuery getCq() {
				return this.cq;
			}

			public byte[] getDeltaValue() {
				return this.ba;
			}

			public Object getKey() {
				return this.key;
			}

			public Object getNewValue() {
				return value;
			}

			public Operation getQueryOperation() {
				return operation;
			}

			public Throwable getThrowable() {
				return this.ex;
			}

		};
	}

	private static class CqMessageHandler implements MessageHandler {

		int count;

		Object payload;

		public void handleMessage(Message<?> message) throws MessagingException {
			this.count++;
			this.payload = message.getPayload();
		}

	}

}
