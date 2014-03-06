/*
 * Copyright 2002-2014 the original author or authors.
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

import org.junit.Test;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.handler.advice.AbstractRequestHandlerAdvice;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;

/**
 * @author Mark Fisher
 * @author Gary Russell
 */
public class TransformerContextTests {

	private static volatile int adviceCalled;

	@Test
	public void methodInvokingTransformer() {
		ConfigurableApplicationContext context = new ClassPathXmlApplicationContext(
				"transformerContextTests.xml", this.getClass());
		MessageChannel input = context.getBean("input", MessageChannel.class);
		PollableChannel output = context.getBean("output", PollableChannel.class);
		input.send(new GenericMessage<String>("foo"));
		Message<?> reply = output.receive(0);
		assertEquals("FOO", reply.getPayload());
		assertEquals(1, adviceCalled);

		input = context.getBean("direct", MessageChannel.class);
		input.send(new GenericMessage<String>("foo"));
		reply = output.receive(0);
		assertEquals("FOO", reply.getPayload());
		StackTraceElement[] st = (StackTraceElement[]) reply.getHeaders().get("callStack");
		assertEquals("doSend", st[6].getMethodName()); // no MethodInvokerHelper

		input = context.getBean("directRef", MessageChannel.class);
		input.send(new GenericMessage<String>("foo"));
		reply = output.receive(0);
		assertEquals("FOO", reply.getPayload());
		st = (StackTraceElement[]) reply.getHeaders().get("callStack");
		assertEquals("doSend", st[6].getMethodName()); // no MethodInvokerHelper
		context.close();
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
			Exception e = new RuntimeException();
			StackTraceElement[] st = e.getStackTrace();
			return MessageBuilder.withPayload(requestMessage.getPayload().toString().toUpperCase())
					.setHeader("callStack", st);
		}

	}
}
