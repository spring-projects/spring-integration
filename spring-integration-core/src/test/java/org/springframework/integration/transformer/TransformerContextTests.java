/*
 * Copyright 2002-2017 the original author or authors.
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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.endpoint.AbstractEndpoint;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.handler.advice.AbstractRequestHandlerAdvice;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Also in JMX - changes here should be reflected there.
 *
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class TransformerContextTests {

	private static volatile int adviceCalled;

	@Autowired
	private MessageChannel input;

	@Autowired
	private MessageChannel direct;

	@Autowired
	private MessageChannel directRef;

	@Autowired
	private PollableChannel output;

	@Autowired
	private AbstractEndpoint pojoTransformer;

	@Autowired
	private TestBean testBean;

	@Test
	public void methodInvokingTransformer() {
		this.input.send(new GenericMessage<String>("foo"));
		Message<?> reply = this.output.receive(0);
		assertEquals("FOO", reply.getPayload());
		assertEquals(1, adviceCalled);

		this.direct.send(new GenericMessage<String>("foo"));
		reply = this.output.receive(0);
		assertEquals("FOO", reply.getPayload());
		StackTraceElement[] st = (StackTraceElement[]) reply.getHeaders().get("callStack");
		assertEquals("doSend", st[6].getMethodName()); // no MethodInvokerHelper

		this.directRef.send(new GenericMessage<String>("foo"));
		reply = this.output.receive(0);
		assertEquals("FOO", reply.getPayload());
		st = (StackTraceElement[]) reply.getHeaders().get("callStack");
		assertEquals("doSend", st[6].getMethodName()); // no MethodInvokerHelper

		assertTrue(this.testBean.isRunning());
		this.pojoTransformer.stop();
		assertFalse(this.testBean.isRunning());
		this.pojoTransformer.start();
		assertTrue(this.testBean.isRunning());

		this.directRef.send(new GenericMessage<String>("bar"));
		assertNull(this.output.receive(0));
	}

	public static class FooAdvice extends AbstractRequestHandlerAdvice {

		@Override
		protected Object doInvoke(ExecutionCallback callback, Object target, Message<?> message) throws Exception {
			adviceCalled++;
			return callback.execute();
		}

	}

	public static class Bar extends AbstractReplyProducingMessageHandler {

		@Override
		protected Object handleRequestMessage(Message<?> requestMessage) {
			if ("bar".equals(requestMessage.getPayload())) {
				return null;
			}
			Exception e = new RuntimeException();
			StackTraceElement[] st = e.getStackTrace();
			return MessageBuilder.withPayload(requestMessage.getPayload().toString().toUpperCase())
					.setHeader("callStack", st);
		}

	}
}
