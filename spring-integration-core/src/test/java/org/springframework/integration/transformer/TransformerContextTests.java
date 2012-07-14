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

package org.springframework.integration.transformer;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.core.PollableChannel;
import org.springframework.integration.handler.AbstractRequestHandlerAdvice;
import org.springframework.integration.message.GenericMessage;

/**
 * @author Mark Fisher
 * @author Gary Russell
 */
public class TransformerContextTests {

	private static volatile int adviceCalled;

	@Test
	public void methodInvokingTransformer() {
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"transformerContextTests.xml", this.getClass());
		MessageChannel input = (MessageChannel) context.getBean("input");
		PollableChannel output = (PollableChannel) context.getBean("output");
		input.send(new GenericMessage<String>("foo"));
		Message<?> reply = output.receive(0);
		assertEquals("FOO", reply.getPayload());
		assertEquals(1, adviceCalled);
	}

	public static class FooAdvice extends AbstractRequestHandlerAdvice {

		@Override
		protected Object doInvoke(ExecutionCallback callback, Object target, Message<?> message) throws Throwable {
			adviceCalled++;
			return callback.execute();
		}

	}
}