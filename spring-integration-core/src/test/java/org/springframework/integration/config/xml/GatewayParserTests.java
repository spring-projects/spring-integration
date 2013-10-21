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

import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.integration.gateway.TestService;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.scheduling.annotation.AsyncResult;

/**
 * @author Mark Fisher
 */
public class GatewayParserTests {

	@Test
	public void testOneWay() {
		ApplicationContext context = new ClassPathXmlApplicationContext("gatewayParserTests.xml", this.getClass());
		TestService service = (TestService) context.getBean("oneWay");
		service.oneWay("foo");
		PollableChannel channel = (PollableChannel) context.getBean("requestChannel");
		Message<?> result = channel.receive(1000);
		assertEquals("foo", result.getPayload());
	}

	@Test
	public void testSolicitResponse() {
		ApplicationContext context = new ClassPathXmlApplicationContext("gatewayParserTests.xml", this.getClass());
		PollableChannel channel = (PollableChannel) context.getBean("replyChannel");
		channel.send(new GenericMessage<String>("foo"));
		TestService service = (TestService) context.getBean("solicitResponse");
		String result = service.solicitResponse();
		assertEquals("foo", result);
	}

	@Test
	public void testRequestReply() {
		ApplicationContext context = new ClassPathXmlApplicationContext("gatewayParserTests.xml", this.getClass());
		PollableChannel requestChannel = (PollableChannel) context.getBean("requestChannel");
		MessageChannel replyChannel = (MessageChannel) context.getBean("replyChannel");
		this.startResponder(requestChannel, replyChannel);
		TestService service = (TestService) context.getBean("requestReply");
		String result = service.requestReply("foo");
		assertEquals("foo", result);		
	}

	@Test
	public void testAsyncGateway() throws Exception {
		ApplicationContext context = new ClassPathXmlApplicationContext("gatewayParserTests.xml", this.getClass());
		PollableChannel requestChannel = (PollableChannel) context.getBean("requestChannel");
		MessageChannel replyChannel = (MessageChannel) context.getBean("replyChannel");
		this.startResponder(requestChannel, replyChannel);
		TestService service = context.getBean("async", TestService.class);
		Future<Message<?>> result = service.async("foo");
		Message<?> reply = result.get(1, TimeUnit.SECONDS);
		assertEquals("foo", reply.getPayload());
		assertEquals("testExecutor", reply.getHeaders().get("executor"));
	}


	private void startResponder(final PollableChannel requestChannel, final MessageChannel replyChannel) {
		Executors.newSingleThreadExecutor().execute(new Runnable() {
			public void run() {
				Message<?> request = requestChannel.receive();
				Message<?> reply = MessageBuilder.fromMessage(request)
						.setCorrelationId(request.getHeaders().getId()).build();
				replyChannel.send(reply);
			}
		});
	}


	@SuppressWarnings("unused")
	private static class TestExecutor extends SimpleAsyncTaskExecutor implements BeanNameAware {

		private static final long serialVersionUID = 1L;

		private volatile String beanName;

		public void setBeanName(String beanName) {
			this.beanName = beanName;
		}

		@Override
		@SuppressWarnings({"rawtypes", "unchecked"})
		public <T> Future<T> submit(Callable<T> task) {
			try {
				Future<?> result = super.submit(task);
				Message<?> message = (Message<?>) result.get(1, TimeUnit.SECONDS);
				Message<?> modifiedMessage = MessageBuilder.fromMessage(message)
						.setHeader("executor", this.beanName).build();
				return new AsyncResult(modifiedMessage);
			}
			catch (Exception e) {
				throw new IllegalStateException("unexpected exception in testExecutor", e);
			}
		}
	}

}
