/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.integration.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.MessageRejectedException;
import org.springframework.integration.core.MessageSelector;
import org.springframework.integration.core.PollableChannel;
import org.springframework.integration.handler.AbstractRequestHandlerAdvice;
import org.springframework.integration.message.GenericMessage;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.StringUtils;

/**
 * @author Mark Fisher
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class FilterParserTests {

	@Autowired @Qualifier("adapterInput")
	MessageChannel adapterInput;

	@Autowired @Qualifier("adapterOutput")
	PollableChannel adapterOutput;

	@Autowired @Qualifier("implementationInput")
	MessageChannel implementationInput;

	@Autowired @Qualifier("implementationOutput")
	PollableChannel implementationOutput;

	@Autowired @Qualifier("exceptionInput")
	MessageChannel exceptionInput;

	@Autowired @Qualifier("discardInput")
	MessageChannel discardInput;

	@Autowired @Qualifier("discardOutput")
	PollableChannel discardOutput;

	@Autowired @Qualifier("discardAndExceptionInput")
	MessageChannel discardAndExceptionInput;

	@Autowired @Qualifier("discardAndExceptionOutput")
	PollableChannel discardAndExceptionOutput;

	private static volatile int adviceCalled;

	@Test
	public void filterWithSelectorAdapterAccepts() {
		adviceCalled = 0;
		adapterInput.send(new GenericMessage<String>("test"));
		Message<?> reply = adapterOutput.receive(0);
		assertNotNull(reply);
		assertEquals("test", reply.getPayload());
		assertEquals(1, adviceCalled);
	}

	@Test
	public void filterWithSelectorAdapterRejects() {
		adapterInput.send(new GenericMessage<String>(""));
		Message<?> reply = adapterOutput.receive(0);
		assertNull(reply);
	}

	@Test
	public void filterWithSelectorImplementationAccepts() {
		implementationInput.send(new GenericMessage<String>("test"));
		Message<?> reply = implementationOutput.receive(0);
		assertNotNull(reply);
		assertEquals("test", reply.getPayload());
	}

	@Test
	public void filterWithSelectorImplementationRejects() {
		implementationInput.send(new GenericMessage<String>(""));
		Message<?> reply = implementationOutput.receive(0);
		assertNull(reply);
	}

	@Test
	public void exceptionThrowingFilterAccepts() {
		exceptionInput.send(new GenericMessage<String>("test"));
		Message<?> reply = implementationOutput.receive(0);
		assertNotNull(reply);
	}

	@Test(expected = MessageRejectedException.class)
	public void exceptionThrowingFilterRejects() {
		exceptionInput.send(new GenericMessage<String>(""));
	}

	@Test
	public void filterWithDiscardChannel() {
		discardInput.send(new GenericMessage<String>(""));
		Message<?> discard = discardOutput.receive(0);
		assertNotNull(discard);
		assertEquals("", discard.getPayload());
		assertNull(adapterOutput.receive(0));
	}

	@Test(expected = MessageRejectedException.class)
	public void filterWithDiscardChannelAndException() throws Exception {
		Exception exception = null;
		try {
			discardAndExceptionInput.send(new GenericMessage<String>(""));
		}
		catch (Exception e) {
			exception = e;
		}
		Message<?> discard = discardAndExceptionOutput.receive(0);
		assertNotNull(discard);
		assertEquals("", discard.getPayload());
		assertNull(adapterOutput.receive(0));
		throw exception;
	}


	public static class TestSelectorBean {

		public boolean hasText(String s) {
			return StringUtils.hasText(s);
		}
	}


	public static class TestSelectorImpl implements MessageSelector {

		public boolean accept(Message<?> message) {
			if (message != null && message.getPayload() instanceof String) {
				return StringUtils.hasText((String) message.getPayload());
			}
			return false;
		}
	}

	public static class FooFilter extends AbstractRequestHandlerAdvice {

		@Override
		protected Object doInvoke(ExecutionCallback callback, Object target, Message<?> message) throws Throwable {
			adviceCalled++;
			return callback.execute();
		}

	}
}
