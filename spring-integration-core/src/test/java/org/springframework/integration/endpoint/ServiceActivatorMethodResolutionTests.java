/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.integration.endpoint;

import java.util.Date;

import org.junit.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.gateway.RequestReplyExchanger;
import org.springframework.integration.handler.ServiceActivatingHandler;
import org.springframework.messaging.Message;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.support.GenericMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 */
public class ServiceActivatorMethodResolutionTests {

	@Test
	public void singleAnnotationMatches() {
		SingleAnnotationTestBean testBean = new SingleAnnotationTestBean();
		ServiceActivatingHandler serviceActivator = new ServiceActivatingHandler(testBean);
		QueueChannel outputChannel = new QueueChannel();
		serviceActivator.setOutputChannel(outputChannel);
		serviceActivator.setBeanFactory(mock(BeanFactory.class));
		serviceActivator.afterPropertiesSet();

		serviceActivator.handleMessage(new GenericMessage<>("foo"));
		Message<?> result = outputChannel.receive(0);
		assertThat(result.getPayload()).isEqualTo("FOO");
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
		serviceActivator.setBeanFactory(mock(BeanFactory.class));
		serviceActivator.afterPropertiesSet();

		serviceActivator.handleMessage(new GenericMessage<>("foo"));
		Message<?> result = outputChannel.receive(0);
		assertThat(result.getPayload()).isEqualTo("FOO");
	}

	@Test(expected = IllegalArgumentException.class)
	public void multiplePublicMethodFails() {
		MultiplePublicMethodTestBean testBean = new MultiplePublicMethodTestBean();
		new ServiceActivatingHandler(testBean);
	}

	@Test
	public void testRequestReplyExchanger() {
		RequestReplyExchanger testBean = request -> request;

		final Message<?> test = new GenericMessage<Object>("foo");

		ServiceActivatingHandler serviceActivator = new ServiceActivatingHandler(testBean) {

			@Override
			protected Object handleRequestMessage(Message<?> message) {
				Object o = super.handleRequestMessage(message);
				assertThat(o).isSameAs(test);
				return null;
			}
		};
		serviceActivator.setBeanFactory(mock(BeanFactory.class));
		serviceActivator.afterPropertiesSet();

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
		serviceActivator.setBeanFactory(mock(BeanFactory.class));
		serviceActivator.afterPropertiesSet();

		Message<?> test = new GenericMessage<Object>(new Date());
		serviceActivator.handleMessage(test);
		assertThat(outputChannel.receive(10)).isEqualTo(test);

		test = new GenericMessage<Object>("foo");
		serviceActivator.handleMessage(test);
		assertThat(outputChannel.receive(10).getPayload()).isEqualTo("FOO");
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
		serviceActivator.setBeanFactory(mock(BeanFactory.class));
		serviceActivator.afterPropertiesSet();

		Message<?> test = new GenericMessage<Object>(new Date());
		serviceActivator.handleMessage(test);
		assertThat(outputChannel.receive(10)).isEqualTo(test);

		test = new GenericMessage<Object>("foo");
		serviceActivator.handleMessage(test);
		assertThat(outputChannel.receive(10).getPayload()).isEqualTo("FOO");
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
		serviceActivator.setBeanFactory(mock(BeanFactory.class));
		serviceActivator.afterPropertiesSet();

		Message<?> test = new GenericMessage<Object>(new Date());
		serviceActivator.handleMessage(test);
		assertThat(outputChannel.receive(10)).isEqualTo(test);

		test = new GenericMessage<Object>("foo");
		serviceActivator.handleMessage(test);
		assertThat(outputChannel.receive(10).getPayload()).isNotEqualTo("FOO");
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
		serviceActivator.setBeanFactory(mock(BeanFactory.class));
		serviceActivator.afterPropertiesSet();

		Message<?> test = new GenericMessage<Object>(new Date());
		serviceActivator.handleMessage(test);
		assertThat(outputChannel.receive(10)).isEqualTo(test);

		test = new GenericMessage<Object>("foo");
		serviceActivator.handleMessage(test);
		assertThat(outputChannel.receive(10).getPayload()).isNotEqualTo("FOO");
	}

	@Test
	public void nullOk() {
		NullOkTestBean testBean = new NullOkTestBean();
		ServiceActivatingHandler serviceActivator = new ServiceActivatingHandler(testBean);
		QueueChannel outputChannel = new QueueChannel();
		serviceActivator.setOutputChannel(outputChannel);
		serviceActivator.setBeanFactory(mock(BeanFactory.class));
		serviceActivator.afterPropertiesSet();

		serviceActivator.handleMessage(new GenericMessage<>(new KafkaNull()));
		Message<?> result = outputChannel.receive(0);
		assertThat(result.getPayload()).isEqualTo("gotNull");
	}

	@SuppressWarnings("unused")
	private static class SingleAnnotationTestBean {

		SingleAnnotationTestBean() {
			super();
		}

		@ServiceActivator
		public String upperCase(String s) {
			return s.toUpperCase();
		}

		public String lowerCase(String s) {
			return s.toLowerCase();
		}

	}

	private static class MultipleAnnotationTestBean {

		MultipleAnnotationTestBean() {
			super();
		}

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

		SinglePublicMethodTestBean() {
			super();
		}

		public String upperCase(String s) {
			return s.toUpperCase();
		}

		String lowerCase(String s) {
			return s.toLowerCase();
		}

	}

	@SuppressWarnings("unused")
	private static class MultiplePublicMethodTestBean {

		MultiplePublicMethodTestBean() {
			super();
		}

		public String upperCase(String s) {
			return s.toUpperCase();
		}

		public String lowerCase(String s) {
			return s.toLowerCase();
		}

	}

	@SuppressWarnings("unused")
	private static class NullOkTestBean {

		NullOkTestBean() {
			super();
		}

		@ServiceActivator
		public String nullOK(@Payload(required = false) String s) {
			if (s == null) {
				return "gotNull";
			}
			else {
				return s;
			}
		}

	}

	private static class KafkaNull {

		KafkaNull() {
			super();
		}

	}

}
