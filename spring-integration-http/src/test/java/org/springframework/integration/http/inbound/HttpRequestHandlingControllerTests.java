/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.integration.http.inbound;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.Message;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.http.AbstractHttpInboundTests;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.validation.Errors;
import org.springframework.validation.ObjectError;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;

/**
 * @author Mark Fisher
 * @author Gary Russell
 * @author Gunnar Hillert
 * @author Biju Kunjummen
 * @since 2.0
 */
public class HttpRequestHandlingControllerTests extends AbstractHttpInboundTests {

	@Test
	public void sendOnly() throws Exception {
		QueueChannel requestChannel = new QueueChannel();
		HttpRequestHandlingController controller = new HttpRequestHandlingController(false);
		controller.setBeanFactory(mock(BeanFactory.class));
		controller.setRequestChannel(requestChannel);
		controller.setViewName("foo");
		controller.afterPropertiesSet();
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("POST");
		request.setContent("hello".getBytes());

		//request.setContentType("text/plain"); //Works in Spring 3.1.2.RELEASE but NOT in 3.0.7.RELEASE
		//Instead do:
		request.addHeader("Content-Type", "text/plain");

		MockHttpServletResponse response = new MockHttpServletResponse();
		ModelAndView modelAndView = controller.handleRequest(request, response);
		assertEquals("foo", modelAndView.getViewName());
		assertEquals(0, modelAndView.getModel().size());
		Message<?> requestMessage = requestChannel.receive(0);
		assertNotNull(requestMessage);
		assertEquals("hello", requestMessage.getPayload());
	}

	@Test
	public void sendOnlyViewExpression() throws Exception {
		QueueChannel requestChannel = new QueueChannel();
		HttpRequestHandlingController controller = new HttpRequestHandlingController(false);
		controller.setBeanFactory(mock(BeanFactory.class));
		controller.setRequestChannel(requestChannel);
		Expression viewExpression = new SpelExpressionParser().parseExpression("'baz'");
		controller.setViewExpression(viewExpression);
		controller.afterPropertiesSet();
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("POST");
		request.setContent("hello".getBytes());

		//request.setContentType("text/plain"); //Works in Spring 3.1.2.RELEASE but NOT in 3.0.7.RELEASE
		//Instead do:
		request.addHeader("Content-Type", "text/plain");

		MockHttpServletResponse response = new MockHttpServletResponse();
		ModelAndView modelAndView = controller.handleRequest(request, response);
		assertEquals("baz", modelAndView.getViewName());
		assertEquals(0, modelAndView.getModel().size());
		Message<?> requestMessage = requestChannel.receive(0);
		assertNotNull(requestMessage);
		assertEquals("hello", requestMessage.getPayload());
	}

	@Test
	public void requestReply() throws Exception {
		DirectChannel requestChannel = new DirectChannel();
		AbstractReplyProducingMessageHandler handler = new AbstractReplyProducingMessageHandler() {
			@Override
			protected Object handleRequestMessage(Message<?> requestMessage) {
				return requestMessage.getPayload().toString().toUpperCase();
			}
		};
		requestChannel.subscribe(handler);
		HttpRequestHandlingController controller = new HttpRequestHandlingController(true);
		controller.setBeanFactory(mock(BeanFactory.class));
		controller.setRequestChannel(requestChannel);
		controller.setViewName("foo");
		controller.afterPropertiesSet();
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("POST");

		//request.setContentType("text/plain"); //Works in Spring 3.1.2.RELEASE but NOT in 3.0.7.RELEASE
		//Instead do:
		request.addHeader("Content-Type", "text/plain");
		request.setContent("hello".getBytes()); //For Spring 3.0.7.RELEASE the Content must be set,

		MockHttpServletResponse response = new MockHttpServletResponse();
		ModelAndView modelAndView = controller.handleRequest(request, response);
		assertEquals("foo", modelAndView.getViewName());
		assertEquals(1, modelAndView.getModel().size());
		Object reply = modelAndView.getModel().get("reply");
		assertNotNull(reply);
		assertEquals("HELLO", reply);
	}

	@Test
	public void requestReplyViewExpressionString() throws Exception {
		DirectChannel requestChannel = new DirectChannel();
		AbstractReplyProducingMessageHandler handler = new AbstractReplyProducingMessageHandler() {
			@Override
			protected Message<String> handleRequestMessage(Message<?> requestMessage) {
				return MessageBuilder.withPayload("foo")
						.setHeader("bar", "baz").build();
			}
		};
		requestChannel.subscribe(handler);
		HttpRequestHandlingController controller = new HttpRequestHandlingController(true);
		controller.setBeanFactory(mock(BeanFactory.class));
		controller.setRequestChannel(requestChannel);
		Expression viewExpression = new SpelExpressionParser().parseExpression("headers['bar']");
		controller.setViewExpression(viewExpression);
		controller.afterPropertiesSet();
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("POST");
		request.setContent("hello".getBytes());
		request.setContentType("text/plain");
		MockHttpServletResponse response = new MockHttpServletResponse();
		ModelAndView modelAndView = controller.handleRequest(request, response);
		assertEquals("baz", modelAndView.getViewName());
		assertEquals(1, modelAndView.getModel().size());
		Object reply = modelAndView.getModel().get("reply");
		assertNotNull(reply);
		assertEquals("foo", reply);
	}

	@Test
	public void requestReplyViewExpressionView() throws Exception {
		final View view = mock(View.class);
		DirectChannel requestChannel = new DirectChannel();
		AbstractReplyProducingMessageHandler handler = new AbstractReplyProducingMessageHandler() {
			@Override
			protected Message<String> handleRequestMessage(Message<?> requestMessage) {
				return MessageBuilder.withPayload("foo")
						.setHeader("bar", view).build();
			}
		};
		requestChannel.subscribe(handler);
		HttpRequestHandlingController controller = new HttpRequestHandlingController(true);
		controller.setBeanFactory(mock(BeanFactory.class));
		controller.setRequestChannel(requestChannel);
		Expression viewExpression = new SpelExpressionParser().parseExpression("headers['bar']");
		controller.setViewExpression(viewExpression);
		controller.afterPropertiesSet();
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("POST");
		request.setContent("hello".getBytes());
		request.setContentType("text/plain");
		MockHttpServletResponse response = new MockHttpServletResponse();
		ModelAndView modelAndView = controller.handleRequest(request, response);
		assertSame(view, modelAndView.getView());
		assertEquals(1, modelAndView.getModel().size());
		Object reply = modelAndView.getModel().get("reply");
		assertNotNull(reply);
		assertEquals("foo", reply);
	}

	@Test
	public void requestReplyWithCustomReplyKey() throws Exception {
		DirectChannel requestChannel = new DirectChannel();
		AbstractReplyProducingMessageHandler handler = new AbstractReplyProducingMessageHandler() {
			@Override
			protected Object handleRequestMessage(Message<?> requestMessage) {
				return requestMessage.getPayload().toString().toUpperCase();
			}
		};
		requestChannel.subscribe(handler);
		HttpRequestHandlingController controller = new HttpRequestHandlingController(true);
		controller.setBeanFactory(mock(BeanFactory.class));
		controller.setRequestChannel(requestChannel);
		controller.setViewName("foo");
		controller.setReplyKey("myReply");
		controller.afterPropertiesSet();
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("POST");
		request.setContent("howdy".getBytes());

		//request.setContentType("text/plain"); //Works in Spring 3.1.2.RELEASE but NOT in 3.0.7.RELEASE
		//Instead do:
		request.addHeader("Content-Type", "text/plain");

		MockHttpServletResponse response = new MockHttpServletResponse();
		ModelAndView modelAndView = controller.handleRequest(request, response);
		assertEquals("foo", modelAndView.getViewName());
		assertEquals(1, modelAndView.getModel().size());
		assertNull(modelAndView.getModel().get("reply"));
		Object reply = modelAndView.getModel().get("myReply");
		assertEquals("HOWDY", reply);
	}

	@Test
	public void requestReplyWithFullMessageInModel() throws Exception {
		DirectChannel requestChannel = new DirectChannel();
		AbstractReplyProducingMessageHandler handler = new AbstractReplyProducingMessageHandler() {
			@Override
			protected Object handleRequestMessage(Message<?> requestMessage) {
				return requestMessage.getPayload().toString().toUpperCase();
			}
		};
		requestChannel.subscribe(handler);
		HttpRequestHandlingController controller = new HttpRequestHandlingController(true);
		controller.setBeanFactory(mock(BeanFactory.class));
		controller.setRequestChannel(requestChannel);
		controller.setViewName("foo");
		controller.setExtractReplyPayload(false);
		controller.afterPropertiesSet();
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("POST");
		request.setContent("abc".getBytes());

		//request.setContentType("text/plain"); //Works in Spring 3.1.2.RELEASE but NOT in 3.0.7.RELEASE
		//Instead do:
		request.addHeader("Content-Type", "text/plain");

		MockHttpServletResponse response = new MockHttpServletResponse();
		ModelAndView modelAndView = controller.handleRequest(request, response);
		assertEquals("foo", modelAndView.getViewName());
		assertEquals(1, modelAndView.getModel().size());
		Object reply = modelAndView.getModel().get("reply");
		assertNotNull(reply);
		assertTrue(reply instanceof Message<?>);
		assertEquals("ABC", ((Message<?>) reply).getPayload());
	}

	@Test
	public void testSendWithError() throws Exception {
		QueueChannel requestChannel = new QueueChannel() {
			@Override
			protected boolean doSend(Message<?> message, long timeout) {
				throw new RuntimeException("Planned");
			}
		};
		HttpRequestHandlingController controller = new HttpRequestHandlingController(false);
		controller.setBeanFactory(mock(BeanFactory.class));
		controller.setRequestChannel(requestChannel);
		controller.afterPropertiesSet();
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("POST");
		request.setContent("hello".getBytes());
		request.setContentType("text/plain");
		MockHttpServletResponse response = new MockHttpServletResponse();
		ModelAndView modelAndView = controller.handleRequest(request, response);
		assertEquals(1, modelAndView.getModel().size());
		Errors errors = (Errors) modelAndView.getModel().get("errors");
		assertEquals(1, errors.getErrorCount());
		ObjectError error = errors.getAllErrors().get(0);
		assertEquals(3, error.getArguments().length);
		assertTrue("Wrong message: "+error, ((String)error.getArguments()[1]).startsWith("failed to send Message"));
	}

	@Test
	public void shutDown() throws Exception {
		DirectChannel requestChannel = new DirectChannel();
		final CountDownLatch latch1 = new CountDownLatch(1);
		final CountDownLatch latch2 = new CountDownLatch(1);
		AbstractReplyProducingMessageHandler handler = new AbstractReplyProducingMessageHandler() {
			@Override
			protected Object handleRequestMessage(Message<?> requestMessage) {
				try {
					latch2.countDown();
					// hold up an active thread so we can verify the count and that it completes ok
					latch1.await(10, TimeUnit.SECONDS);
				}
				catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
				return requestMessage.getPayload().toString().toUpperCase();
			}
		};
		requestChannel.subscribe(handler);
		final HttpRequestHandlingController controller = new HttpRequestHandlingController(true);
		controller.setBeanFactory(mock(BeanFactory.class));
		controller.setRequestChannel(requestChannel);
		controller.setViewName("foo");
		controller.afterPropertiesSet();
		final MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("POST");
		request.setContent("hello".getBytes());

		//request.setContentType("text/plain"); //Works in Spring 3.1.2.RELEASE but NOT in 3.0.7.RELEASE
		//Instead do:
		request.addHeader("Content-Type", "text/plain");

		MockHttpServletResponse response = new MockHttpServletResponse();
		final AtomicInteger active = new AtomicInteger();
		final AtomicBoolean expected503 = new AtomicBoolean();
		Executors.newSingleThreadExecutor().execute(new Runnable() {
			public void run() {
				try {
					// wait for the active thread
					latch2.await(10, TimeUnit.SECONDS);
				}
				catch (InterruptedException e1) {
					Thread.currentThread().interrupt();
				}
				// start the shutdown
				active.set(controller.beforeShutdown());
				try {
					MockHttpServletResponse response = new MockHttpServletResponse();
					controller.handleRequest(request, response);
					expected503.set(response.getStatus() == HttpStatus.SERVICE_UNAVAILABLE.value());
					latch1.countDown();
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		ModelAndView modelAndView = controller.handleRequest(request, response);
		// verify we get a 503 after shutdown starts
		assertEquals(1, active.get());
		assertTrue(expected503.get());
		// verify the active request still processed ok
		assertEquals("foo", modelAndView.getViewName());
		assertEquals(1, modelAndView.getModel().size());
		Object reply = modelAndView.getModel().get("reply");
		assertNotNull(reply);
		assertEquals("HELLO", reply);
	}


}
