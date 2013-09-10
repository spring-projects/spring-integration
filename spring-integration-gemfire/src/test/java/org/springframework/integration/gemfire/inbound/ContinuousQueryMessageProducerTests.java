/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.springframework.integration.gemfire.inbound;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import org.junit.Before;
import org.junit.Test;

import org.springframework.data.gemfire.listener.ContinuousQueryListenerContainer;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.expression.ExpressionUtils;
import org.springframework.messaging.MessageHandler;

import com.gemstone.gemfire.cache.Operation;
import com.gemstone.gemfire.cache.query.CqEvent;
import com.gemstone.gemfire.cache.query.CqQuery;
import com.gemstone.gemfire.cache.query.internal.CqQueryImpl;

/**
 * @author David Turanski
 * @author Artem Bilan
 * @since 2.1
 */
public class ContinuousQueryMessageProducerTests {
	ContinuousQueryListenerContainer queryListenerContainer;

	ContinuousQueryMessageProducer cqMessageProducer;

	CqMessageHandler handler;

	@Before
	public void setUp() {
		queryListenerContainer = mock(ContinuousQueryListenerContainer.class);
		cqMessageProducer = new ContinuousQueryMessageProducer(queryListenerContainer, "");
		DirectChannel outputChannel = new DirectChannel();
		cqMessageProducer.setOutputChannel(outputChannel);
		cqMessageProducer.setIntegrationEvaluationContext(ExpressionUtils.createStandardEvaluationContext());
		handler = new CqMessageHandler();
		outputChannel.subscribe(handler);
	}

	@Test
	public void testMessageProduced() {
		CqEvent cqEvent = event(Operation.CREATE, "hello");

		cqMessageProducer.onEvent(cqEvent);

		assertEquals(1, handler.count);
		assertEquals(cqEvent, handler.payload);
	}

	@Test
	public void testMessageNotProducedForUnsupportedEventType() {
		CqEvent cqEvent = event(Operation.DESTROY, "hello");

		cqMessageProducer.onEvent(cqEvent);

		assertEquals(0, handler.count);
	}

	@Test
	public void testMessageProducedForAddedEventType() {

		CqEvent cqEvent = event(Operation.DESTROY, null);

		cqMessageProducer.setSupportedEventTypes(CqEventType.DESTROYED);
		cqMessageProducer.onEvent(cqEvent);

		assertEquals(1, handler.count);
		assertEquals(cqEvent, handler.payload);
	}

	@Test
	public void testPayloadExpression() {
		CqEvent cqEvent = event(Operation.CREATE, "hello");
		cqMessageProducer.setPayloadExpression("newValue.toUpperCase() + ', WORLD'");
		cqMessageProducer.onEvent(cqEvent);
		assertEquals(1, handler.count);
		assertEquals("HELLO, WORLD", handler.payload);
	}

	CqEvent event(final Operation operation, final Object value) {

		CqEvent event = new CqEvent() {

			final CqQuery cq = new CqQueryImpl();

			final byte[] ba = new byte[0];

			final Object key = new Object();

			final Exception ex = new Exception();

			public Operation getBaseOperation() {
				return operation;
			}

			public CqQuery getCq() {
				return cq;
			}

			public byte[] getDeltaValue() {
				return ba;
			}

			public Object getKey() {
				return key;
			}

			public Object getNewValue() {
				return value;
			}

			public Operation getQueryOperation() {
				return operation;
			}

			public Throwable getThrowable() {
				return ex;
			}
		};

		return event;
	}

	private static class CqMessageHandler implements MessageHandler {
		public int count;

		public Object payload;

		/*
		 * (non-Javadoc)
		 *
		 * @see
		 * org.springframework.messaging.MessageHandler#handleMessage
		 * (org.springframework.messaging.Message)
		 */
		public void handleMessage(Message<?> message) throws MessagingException {
			count++;
			payload = message.getPayload();
		}
	}
}
