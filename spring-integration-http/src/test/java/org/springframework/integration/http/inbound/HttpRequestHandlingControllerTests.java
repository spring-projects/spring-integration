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

package org.springframework.integration.http.inbound;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.servlet.http.Cookie;
import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.http.HttpStatus;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.http.AbstractHttpInboundTests;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.validation.Errors;
import org.springframework.validation.ObjectError;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author Mark Fisher
 * @author Gary Russell
 * @author Gunnar Hillert
 * @author Biju Kunjummen
 * @author Artem Bilan
 * @author Anthony Schweigard
 *
 * @since 2.0
 */
public class HttpRequestHandlingControllerTests extends AbstractHttpInboundTests {

	@Test
	public void sendOnly() {
		QueueChannel requestChannel = new QueueChannel();
		HttpRequestHandlingController controller = new HttpRequestHandlingController(false);
		controller.setBeanFactory(mock(BeanFactory.class));
		controller.setRequestChannel(requestChannel);
		controller.setViewName("foo");
		controller.afterPropertiesSet();
		controller.start();

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("POST");
		request.setContent("hello".getBytes());

		//request.setContentType("text/plain"); //Works in Spring 3.1.2.RELEASE but NOT in 3.0.7.RELEASE
		//Instead do:
		request.addHeader("Content-Type", "text/plain");

		MockHttpServletResponse response = new MockHttpServletResponse();
		ModelAndView modelAndView = controller.handleRequest(request, response);
		assertThat(modelAndView.getViewName()).isEqualTo("foo");
		assertThat(modelAndView.getModel().size()).isEqualTo(0);
		Message<?> requestMessage = requestChannel.receive(0);
		assertThat(requestMessage).isNotNull();
		assertThat(requestMessage.getPayload()).isEqualTo("hello");
	}

	@Test
	public void sendOnlyViewExpression() {
		QueueChannel requestChannel = new QueueChannel();
		HttpRequestHandlingController controller = new HttpRequestHandlingController(false);
		controller.setBeanFactory(mock(BeanFactory.class));
		controller.setRequestChannel(requestChannel);
		Expression viewExpression = new SpelExpressionParser().parseExpression("'baz'");
		controller.setViewExpression(viewExpression);
		controller.afterPropertiesSet();
		controller.start();

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("POST");
		request.setContent("hello".getBytes());

		//request.setContentType("text/plain"); //Works in Spring 3.1.2.RELEASE but NOT in 3.0.7.RELEASE
		//Instead do:
		request.addHeader("Content-Type", "text/plain");

		MockHttpServletResponse response = new MockHttpServletResponse();
		ModelAndView modelAndView = controller.handleRequest(request, response);
		assertThat(modelAndView.getViewName()).isEqualTo("baz");
		assertThat(modelAndView.getModel().size()).isEqualTo(0);
		Message<?> requestMessage = requestChannel.receive(0);
		assertThat(requestMessage).isNotNull();
		assertThat(requestMessage.getPayload()).isEqualTo("hello");
	}

	@Test
	public void requestReply() {
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
		controller.start();

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("POST");

		//request.setContentType("text/plain"); //Works in Spring 3.1.2.RELEASE but NOT in 3.0.7.RELEASE
		//Instead do:
		request.addHeader("Content-Type", "text/plain");
		request.setContent("hello".getBytes()); //For Spring 3.0.7.RELEASE the Content must be set,

		MockHttpServletResponse response = new MockHttpServletResponse();
		ModelAndView modelAndView = controller.handleRequest(request, response);
		assertThat(modelAndView.getViewName()).isEqualTo("foo");
		assertThat(modelAndView.getModel().size()).isEqualTo(1);
		Object reply = modelAndView.getModel().get("reply");
		assertThat(reply).isNotNull();
		assertThat(reply).isEqualTo("HELLO");
	}

	@Test
	public void requestReplyViewExpressionString() {
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
		controller.start();

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("POST");
		request.setContent("hello".getBytes());
		request.setContentType("text/plain");
		MockHttpServletResponse response = new MockHttpServletResponse();
		ModelAndView modelAndView = controller.handleRequest(request, response);
		assertThat(modelAndView.getViewName()).isEqualTo("baz");
		assertThat(modelAndView.getModel().size()).isEqualTo(1);
		Object reply = modelAndView.getModel().get("reply");
		assertThat(reply).isNotNull();
		assertThat(reply).isEqualTo("foo");
	}

	@Test
	public void requestReplyViewExpressionView() {
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
		controller.start();

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("POST");
		request.setContent("hello".getBytes());
		request.setContentType("text/plain");
		MockHttpServletResponse response = new MockHttpServletResponse();
		ModelAndView modelAndView = controller.handleRequest(request, response);
		assertThat(modelAndView.getView()).isSameAs(view);
		assertThat(modelAndView.getModel().size()).isEqualTo(1);
		Object reply = modelAndView.getModel().get("reply");
		assertThat(reply).isNotNull();
		assertThat(reply).isEqualTo("foo");
	}

	@Test
	public void requestReplyWithCustomReplyKey() {
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
		controller.start();

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("POST");
		request.setContent("howdy".getBytes());

		//request.setContentType("text/plain"); //Works in Spring 3.1.2.RELEASE but NOT in 3.0.7.RELEASE
		//Instead do:
		request.addHeader("Content-Type", "text/plain");

		MockHttpServletResponse response = new MockHttpServletResponse();
		ModelAndView modelAndView = controller.handleRequest(request, response);
		assertThat(modelAndView.getViewName()).isEqualTo("foo");
		assertThat(modelAndView.getModel().size()).isEqualTo(1);
		assertThat(modelAndView.getModel().get("reply")).isNull();
		Object reply = modelAndView.getModel().get("myReply");
		assertThat(reply).isEqualTo("HOWDY");
	}

	@Test
	public void requestReplyWithFullMessageInModel() {
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
		controller.start();

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("POST");
		request.setContent("abc".getBytes());

		//request.setContentType("text/plain"); //Works in Spring 3.1.2.RELEASE but NOT in 3.0.7.RELEASE
		//Instead do:
		request.addHeader("Content-Type", "text/plain");

		MockHttpServletResponse response = new MockHttpServletResponse();
		ModelAndView modelAndView = controller.handleRequest(request, response);
		assertThat(modelAndView.getViewName()).isEqualTo("foo");
		assertThat(modelAndView.getModel().size()).isEqualTo(1);
		Object reply = modelAndView.getModel().get("reply");
		assertThat(reply).isNotNull();
		assertThat(reply instanceof Message<?>).isTrue();
		assertThat(((Message<?>) reply).getPayload()).isEqualTo("ABC");
	}

	@Test
	public void testSendWithError() {
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
		controller.start();

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("POST");
		request.setContent("hello".getBytes());
		request.setContentType("text/plain");
		MockHttpServletResponse response = new MockHttpServletResponse();
		ModelAndView modelAndView = controller.handleRequest(request, response);
		assertThat(modelAndView.getModel().size()).isEqualTo(1);
		Errors errors = (Errors) modelAndView.getModel().get("errors");
		assertThat(errors.getErrorCount()).isEqualTo(1);
		ObjectError error = errors.getAllErrors().get(0);
		assertThat(error.getArguments().length).isEqualTo(3);
		assertThat(((String) error.getArguments()[1]).startsWith("failed to send Message"))
				.as("Wrong message: " + error).isTrue();
	}

	@Test
	public void shutDown() {
		DirectChannel requestChannel = new DirectChannel();
		final CountDownLatch latch1 = new CountDownLatch(1);
		final CountDownLatch latch2 = new CountDownLatch(1);
		AbstractReplyProducingMessageHandler handler = new AbstractReplyProducingMessageHandler() {

			@Override
			protected Object handleRequestMessage(Message<?> requestMessage) {
				try {
					latch2.countDown();
					// hold up an active thread, so we can verify the count and that it completes ok
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
		controller.start();

		final MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("POST");
		request.setContent("hello".getBytes());

		//request.setContentType("text/plain"); //Works in Spring 3.1.2.RELEASE but NOT in 3.0.7.RELEASE
		//Instead do:
		request.addHeader("Content-Type", "text/plain");

		MockHttpServletResponse response = new MockHttpServletResponse();
		final AtomicInteger active = new AtomicInteger();
		final AtomicBoolean expected503 = new AtomicBoolean();
		ExecutorService executorService = Executors.newSingleThreadExecutor();
		executorService.execute(() -> {
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
				MockHttpServletResponse response1 = new MockHttpServletResponse();
				controller.handleRequest(request, response1);
				expected503.set(response1.getStatus() == HttpStatus.SERVICE_UNAVAILABLE.value());
				latch1.countDown();
			}
			catch (Exception e) {
				LogFactory.getLog(getClass()).error("Async handleRequest failed", e);
			}
		});
		ModelAndView modelAndView = controller.handleRequest(request, response);
		// verify we get a 503 after shutdown starts
		assertThat(active.get()).isEqualTo(1);
		assertThat(expected503.get()).isTrue();
		// verify the active request still processed ok
		assertThat(modelAndView.getViewName()).isEqualTo("foo");
		assertThat(modelAndView.getModel().size()).isEqualTo(1);
		Object reply = modelAndView.getModel().get("reply");
		assertThat(reply).isNotNull();
		assertThat(reply).isEqualTo("HELLO");
		executorService.shutdown();
	}

	@Test
	public void handleRequestDuplicateCookies() {
		DirectChannel requestChannel = new DirectChannel();
		requestChannel.subscribe(new AbstractReplyProducingMessageHandler() {

			@Override
			protected Object handleRequestMessage(Message<?> requestMessage) {
				return requestMessage.getPayload().toString();
			}
		});

		HttpRequestHandlingController controller = new HttpRequestHandlingController(true);
		controller.setErrorsKey("errors");
		controller.setRequestChannel(requestChannel);
		controller.setViewName("foo");
		controller.setReplyKey("cookiesReply");
		controller.setExtractReplyPayload(true);
		controller.setPayloadExpression(new SpelExpressionParser().parseExpression("#cookies['c1'][0].value"));
		controller.setBeanFactory(mock(BeanFactory.class));
		controller.afterPropertiesSet();
		controller.start();

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("POST");
		request.setContent("hello".getBytes());
		request.addHeader("Content-Type", "text/plain");
		request.setCookies(new Cookie("c1", "first"), new Cookie("c1", "last"));

		MockHttpServletResponse response = new MockHttpServletResponse();

		ModelAndView modelAndView = controller.handleRequest(request, response);
		assertThat(modelAndView.getModelMap()).doesNotContainKey("errors");
		assertThat(modelAndView.getModelMap()).containsKey("cookiesReply");
		assertThat(modelAndView.getModelMap()).containsEntry("cookiesReply", "first");
	}

}
