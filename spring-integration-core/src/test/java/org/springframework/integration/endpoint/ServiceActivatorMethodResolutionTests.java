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

package org.springframework.integration.endpoint;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertSame;

import java.util.Date;

import org.junit.Test;

import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.gateway.RequestReplyExchanger;
import org.springframework.integration.handler.ServiceActivatingHandler;
import org.springframework.messaging.Message;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;

/**
 * @author Mark Fisher
 */
public class ServiceActivatorMethodResolutionTests {

	@Test
	public void singleAnnotationMatches() {
		SingleAnnotationTestBean testBean = new SingleAnnotationTestBean();
		ServiceActivatingHandler serviceActivator = new ServiceActivatingHandler(testBean);
		QueueChannel outputChannel = new QueueChannel();
		serviceActivator.setOutputChannel(outputChannel);
		serviceActivator.handleMessage(new GenericMessage<String>("foo"));
		Message<?> result = outputChannel.receive(0);
		assertEquals("FOO", result.getPayload());
	}

	@Test(expected = IllegalArgumentException.class)
	public void multipleAnnotationFails() {
		MultipleAnnotationTestBean testBean = new MultipleAnnotationTestBean();
		new ServiceActivatingHandler(testBean);
	}

	@Test
	public void singlePublicMethodMatches() {
		SinglePublicMethodTestBean testBean = new SinglePublicMethodTestBean();
		ServiceActivatingHandler serviceActivator = new ServiceActivatingHandler(testBean);
		QueueChannel outputChannel = new QueueChannel();
		serviceActivator.setOutputChannel(outputChannel);
		serviceActivator.handleMessage(new GenericMessage<String>("foo"));
		Message<?> result = outputChannel.receive(0);
		assertEquals("FOO", result.getPayload());
	}

	@Test(expected = IllegalArgumentException.class)
	public void multiplePublicMethodFails() {
		MultiplePublicMethodTestBean testBean = new MultiplePublicMethodTestBean();
		new ServiceActivatingHandler(testBean);
	}


	@Test
	public void testRequestReplyExchanger() {
		RequestReplyExchanger testBean = new RequestReplyExchanger() {

			@Override
			public Message<?> exchange(Message<?> request) {
				return request;
			}
		};

		final Message<?> test = new GenericMessage<Object>("foo");

		ServiceActivatingHandler serviceActivator = new ServiceActivatingHandler(testBean) {

			@Override
			protected Object handleRequestMessage(Message<?> message) {
				Object o = super.handleRequestMessage(message);
				assertSame(test, o);
				return null;
			}
		};

		serviceActivator.handleMessage(test);
	}

	@Test
	/*
	 * A handler and message handler fallback (RRE); don't force RRE
	 */
	public void testRequestReplyExchangerSeveralMethods() {
		RequestReplyExchanger testBean = new RequestReplyExchanger() {

			@Override
			public Message<?> exchange(Message<?> request) {
				return request;
			}

			@SuppressWarnings("unused")
			public String foo(String request) {
				return request.toUpperCase();
			}

		};
		ServiceActivatingHandler serviceActivator = new ServiceActivatingHandler(testBean);
		PollableChannel outputChannel = new QueueChannel();
		serviceActivator.setOutputChannel(outputChannel);

		Message<?> test = new GenericMessage<Object>(new Date());
		serviceActivator.handleMessage(test);
		assertEquals(test, outputChannel.receive(10));

		test = new GenericMessage<Object>("foo");
		serviceActivator.handleMessage(test);
		assertEquals("FOO", outputChannel.receive(10).getPayload());
	}

	@Test
	/*
	 * No handler fallback methods; don't force RRE
	 */
	public void testRequestReplyExchangerWithGenericMessageMethod() {
		RequestReplyExchanger testBean = new RequestReplyExchanger() {

			@Override
			public Message<?> exchange(Message<?> request) {
				return request;
			}

			@SuppressWarnings("unused")
			public String foo(Message<String> request) {
				return request.getPayload().toUpperCase();
			}

		};
		ServiceActivatingHandler serviceActivator = new ServiceActivatingHandler(testBean);
		PollableChannel outputChannel = new QueueChannel();
		serviceActivator.setOutputChannel(outputChannel);

		Message<?> test = new GenericMessage<Object>(new Date());
		serviceActivator.handleMessage(test);
		assertEquals(test, outputChannel.receive(10));

		test = new GenericMessage<Object>("foo");
		serviceActivator.handleMessage(test);
		assertEquals("FOO", outputChannel.receive(10).getPayload());
	}

	@Test
	/*
	 * No handler fallback methods; ambiguous message handler fallbacks; force RRE
	 */
	public void testRequestReplyExchangerWithAmbiguousGenericMessageMethod() {
		RequestReplyExchanger testBean = new RequestReplyExchanger() {

			@Override
			public Message<?> exchange(Message<?> request) {
				return request;
			}

			@SuppressWarnings("unused")
			public String foo(Message<String> request) {
				return request.getPayload().toUpperCase();
			}

			@SuppressWarnings("unused")
			public String bar(Message<String> request) {
				return request.getPayload().toUpperCase();
			}

		};
		ServiceActivatingHandler serviceActivator = new ServiceActivatingHandler(testBean);
		PollableChannel outputChannel = new QueueChannel();
		serviceActivator.setOutputChannel(outputChannel);

		Message<?> test = new GenericMessage<Object>(new Date());
		serviceActivator.handleMessage(test);
		assertEquals(test, outputChannel.receive(10));

		test = new GenericMessage<Object>("foo");
		serviceActivator.handleMessage(test);
		assertNotEquals("FOO", outputChannel.receive(10).getPayload());
	}

	@Test
	/*
	 * One message handler fallback method (RRE); ambiguous handler fallbacks; force RRE
	 */
	public void testRequestReplyExchangerWithAmbiguousMethod() {
		RequestReplyExchanger testBean = new RequestReplyExchanger() {

			@Override
			public Message<?> exchange(Message<?> request) {
				return request;
			}

			@SuppressWarnings("unused")
			public String foo(String request) {
				return request.toUpperCase();
			}

			@SuppressWarnings("unused")
			public String bar(String request) {
				return request.toUpperCase();
			}

		};
		ServiceActivatingHandler serviceActivator = new ServiceActivatingHandler(testBean);
		PollableChannel outputChannel = new QueueChannel();
		serviceActivator.setOutputChannel(outputChannel);

		Message<?> test = new GenericMessage<Object>(new Date());
		serviceActivator.handleMessage(test);
		assertEquals(test, outputChannel.receive(10));

		test = new GenericMessage<Object>("foo");
		serviceActivator.handleMessage(test);
		assertNotEquals("FOO", outputChannel.receive(10).getPayload());
	}


	@SuppressWarnings("unused")
	private static class SingleAnnotationTestBean {

		@ServiceActivator
		public String upperCase(String s) {
			return s.toUpperCase();
		}

		public String lowerCase(String s) {
			return s.toLowerCase();
		}
	}


	@SuppressWarnings("unused")
	private static class MultipleAnnotationTestBean {

		@ServiceActivator
		public String upperCase(String s) {
			return s.toUpperCase();
		}

		@ServiceActivator
		public String lowerCase(String s) {
			return s.toLowerCase();
		}
	}


	@SuppressWarnings("unused")
	private static class SinglePublicMethodTestBean {

		public String upperCase(String s) {
			return s.toUpperCase();
		}

		String lowerCase(String s) {
			return s.toLowerCase();
		}
	}


	@SuppressWarnings("unused")
	private static class MultiplePublicMethodTestBean {

		public String upperCase(String s) {
			return s.toUpperCase();
		}

		public String lowerCase(String s) {
			return s.toLowerCase();
		}
	}

}
