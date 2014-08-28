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

package org.springframework.integration.config.xml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.integration.gateway.TestService;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import reactor.core.composable.Promise;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class GatewayParserTests {

	@Autowired
	private ApplicationContext context;

	@Test
	public void testOneWay() {
		TestService service = (TestService) context.getBean("oneWay");
		service.oneWay("foo");
		PollableChannel channel = (PollableChannel) context.getBean("requestChannel");
		Message<?> result = channel.receive(1000);
		assertEquals("foo", result.getPayload());
	}

	@Test
	public void testSolicitResponse() {
		PollableChannel channel = (PollableChannel) context.getBean("replyChannel");
		channel.send(new GenericMessage<String>("foo"));
		TestService service = (TestService) context.getBean("solicitResponse");
		String result = service.solicitResponse();
		assertEquals("foo", result);
	}

	@Test
	public void testRequestReply() {
		PollableChannel requestChannel = (PollableChannel) context.getBean("requestChannel");
		MessageChannel replyChannel = (MessageChannel) context.getBean("replyChannel");
		this.startResponder(requestChannel, replyChannel);
		TestService service = (TestService) context.getBean("requestReply");
		String result = service.requestReply("foo");
		assertEquals("foo", result);
	}

	@Test
	public void testAsyncGateway() throws Exception {
		PollableChannel requestChannel = (PollableChannel) context.getBean("requestChannel");
		MessageChannel replyChannel = (MessageChannel) context.getBean("replyChannel");
		this.startResponder(requestChannel, replyChannel);
		TestService service = context.getBean("async", TestService.class);
		Future<Message<?>> result = service.async("foo");
		Message<?> reply = result.get(1, TimeUnit.SECONDS);
		assertEquals("foo", reply.getPayload());
		assertEquals("testExecutor", reply.getHeaders().get("executor"));
		assertNotNull(TestUtils.getPropertyValue(context.getBean("&async"), "asyncExecutor"));
	}

	@Test
	public void testAsyncDisabledGateway() throws Exception {
		Object service = context.getBean("&asyncOff");
		assertNull(TestUtils.getPropertyValue(service, "asyncExecutor"));
	}

	@Test
	public void testPromiseGateway() throws Exception {
		PollableChannel requestChannel = context.getBean("requestChannel", PollableChannel.class);
		MessageChannel replyChannel = context.getBean("replyChannel", MessageChannel.class);
		this.startResponder(requestChannel, replyChannel);
		TestService service = context.getBean("promise", TestService.class);
		Promise<Message<?>> result = service.promise("foo");
		Message<?> reply = result.await(1, TimeUnit.SECONDS);
		assertEquals("foo", reply.getPayload());
		assertNotNull(TestUtils.getPropertyValue(context.getBean("&promise"), "asyncExecutor"));
	}

	private void startResponder(final PollableChannel requestChannel, final MessageChannel replyChannel) {
		Executors.newSingleThreadExecutor().execute(new Runnable() {
			@Override
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

		@Override
		public void setBeanName(String beanName) {
			this.beanName = beanName;
		}

		@Override
		@SuppressWarnings({"rawtypes", "unchecked"})
		public <T> Future<T> submit(Callable<T> task) {
			try {
				Future<?> result = super.submit(task);
				Message<?> message = (Message<?>) result.get(1, TimeUnit.SECONDS);
				Message<?> modifiedMessage;
				if (message == null) {
					modifiedMessage = MessageBuilder.withPayload("foo")
							.setHeader("executor", this.beanName).build();
				}
				else {
					modifiedMessage = MessageBuilder.fromMessage(message)
						.setHeader("executor", this.beanName).build();
				}
				return new AsyncResult(modifiedMessage);
			}
			catch (Exception e) {
				throw new IllegalStateException("unexpected exception in testExecutor", e);
			}
		}

	}

}
