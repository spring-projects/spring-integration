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
package org.springframework.integration.handler.advice;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.aopalliance.aop.Advice;
import org.junit.Test;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.Message;
import org.springframework.integration.MessageHandlingException;
import org.springframework.integration.MessageHeaders;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.core.PollableChannel;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.message.GenericMessage;
import org.springframework.retry.RecoveryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryState;
import org.springframework.retry.support.DefaultRetryState;

/**
 * @author Gary Russell
 * @since 2.2
 *
 */
public class AdvisedMessageHandlerTests {

	@Test
	public void successFailureAdvice() {
		final AtomicBoolean doFail = new AtomicBoolean();
		AbstractReplyProducingMessageHandler handler = new AbstractReplyProducingMessageHandler() {
			@Override
			protected Object handleRequestMessage(Message<?> requestMessage) {
				if (doFail.get()) {
					throw new RuntimeException("qux");
				}
				return "baz";
			}
		};
		QueueChannel replies = new QueueChannel();
		handler.setOutputChannel(replies);
		Message<String> message = new GenericMessage<String>("Hello, world!");

		// no advice
		handler.handleMessage(message);
		Message<?> reply = replies.receive(1000);
		assertNotNull(reply);
		assertEquals("baz", reply.getPayload());

		PollableChannel successChannel = new QueueChannel();
		PollableChannel failureChannel = new QueueChannel();
		ExpressionEvaluatingRequestHandlerAdvice advice = new ExpressionEvaluatingRequestHandlerAdvice(
				"'foo'", successChannel,
				"'bar'", failureChannel);

		List<Advice> adviceChain = new ArrayList<Advice>();
		adviceChain.add(advice);
		handler.setAdviceChain(adviceChain);
		handler.afterPropertiesSet();

		// advice with success
		handler.handleMessage(message);
		reply = replies.receive(1000);
		assertNotNull(reply);
		assertEquals("baz", reply.getPayload());

		Message<?> success = successChannel.receive(1000);
		assertNotNull(success);
		assertEquals("Hello, world!", success.getPayload());
		assertEquals("foo", success.getHeaders().get(MessageHeaders.POSTPROCESS_RESULT));

		// advice with failure, not trapped
		doFail.set(true);
		try {
			handler.handleMessage(message);
			fail("Expected exception");
		}
		catch (Exception e) {
			assertEquals("qux", e.getCause().getMessage());
		}

		Message<?> failure = failureChannel.receive(1000);
		assertNotNull(failure);
		assertEquals("Hello, world!", failure.getPayload());
		assertEquals("bar", failure.getHeaders().get(MessageHeaders.POSTPROCESS_RESULT));

		// advice with failure, trapped
		advice.setTrapException(true);
		handler.handleMessage(message);
		failure = failureChannel.receive(1000);
		assertNotNull(failure);
		assertEquals("Hello, world!", failure.getPayload());
		assertEquals("bar", failure.getHeaders().get(MessageHeaders.POSTPROCESS_RESULT));
		assertNull(replies.receive(1));

		// advice with failure, eval is result
		advice.setReturnFailureExpressionResult(true);
		handler.handleMessage(message);
		failure = failureChannel.receive(1000);
		assertNotNull(failure);
		assertEquals("Hello, world!", failure.getPayload());
		assertEquals("bar", failure.getHeaders().get(MessageHeaders.POSTPROCESS_RESULT));

		reply = replies.receive(1000);
		assertNotNull(reply);
		assertEquals("bar", reply.getPayload());

	}

	@Test
	public void propagateOnSuccessExpressionFailures() {
		final AtomicBoolean doFail = new AtomicBoolean();
		AbstractReplyProducingMessageHandler handler = new AbstractReplyProducingMessageHandler() {
			@Override
			protected Object handleRequestMessage(Message<?> requestMessage) {
				if (doFail.get()) {
					throw new RuntimeException("qux");
				}
				return "baz";
			}
		};
		QueueChannel replies = new QueueChannel();
		handler.setOutputChannel(replies);
		Message<String> message = new GenericMessage<String>("Hello, world!");

		PollableChannel successChannel = new QueueChannel();
		PollableChannel failureChannel = new QueueChannel();
		ExpressionEvaluatingRequestHandlerAdvice advice = new ExpressionEvaluatingRequestHandlerAdvice(
				new SpelExpressionParser().parseExpression("1/0"), successChannel,
				new SpelExpressionParser().parseExpression("1/0"), failureChannel);

		List<Advice> adviceChain = new ArrayList<Advice>();
		adviceChain.add(advice);
		handler.setAdviceChain(adviceChain);
		handler.afterPropertiesSet();

		// failing advice with success
		handler.handleMessage(message);
		Message<?> reply = replies.receive(1000);
		assertNotNull(reply);
		assertEquals("baz", reply.getPayload());

		Message<?> success = successChannel.receive(1000);
		assertNotNull(success);
		assertEquals("Hello, world!", success.getPayload());
		assertEquals(MessageHandlingException.class, success.getHeaders().get(MessageHeaders.POSTPROCESS_RESULT).getClass());
		assertEquals("Expression evaluation failed: 1/0", ((Exception) success.getHeaders().get(MessageHeaders.POSTPROCESS_RESULT)).getMessage());

		// propagate failing advice with success
		advice.setPropagateEvaluationFailures(true);
		try {
			handler.handleMessage(message);
			fail("Expected Exception");
		}
		catch (MessageHandlingException e) {
			assertEquals("Expression evaluation failed: 1/0", e.getMessage());
		}
		reply = replies.receive(1);
		assertNull(reply);

		success = successChannel.receive(1000);
		assertNotNull(success);
		assertEquals("Hello, world!", success.getPayload());
		assertEquals(MessageHandlingException.class, success.getHeaders().get(MessageHeaders.POSTPROCESS_RESULT).getClass());
		assertEquals("Expression evaluation failed: 1/0", ((Exception) success.getHeaders().get(MessageHeaders.POSTPROCESS_RESULT)).getMessage());

	}

	@Test
	public void propagateOnFailureExpressionFailures() {
		final AtomicBoolean doFail = new AtomicBoolean(true);
		AbstractReplyProducingMessageHandler handler = new AbstractReplyProducingMessageHandler() {
			@Override
			protected Object handleRequestMessage(Message<?> requestMessage) {
				if (doFail.get()) {
					throw new RuntimeException("qux");
				}
				return "baz";
			}
		};
		QueueChannel replies = new QueueChannel();
		handler.setOutputChannel(replies);
		Message<String> message = new GenericMessage<String>("Hello, world!");

		PollableChannel successChannel = new QueueChannel();
		PollableChannel failureChannel = new QueueChannel();
		ExpressionEvaluatingRequestHandlerAdvice advice = new ExpressionEvaluatingRequestHandlerAdvice(
				new SpelExpressionParser().parseExpression("1/0"), successChannel,
				new SpelExpressionParser().parseExpression("1/0"), failureChannel);

		List<Advice> adviceChain = new ArrayList<Advice>();
		adviceChain.add(advice);
		handler.setAdviceChain(adviceChain);
		handler.afterPropertiesSet();

		// failing advice with failure
		try {
			handler.handleMessage(message);
			fail("Expected exception");
		}
		catch (Exception e) {
			assertEquals("qux", e.getCause().getMessage());
		}
		Message<?> reply = replies.receive(1);
		assertNull(reply);

		Message<?> failure = failureChannel.receive(1000);
		assertNotNull(failure);
		assertEquals("Hello, world!", failure.getPayload());
		assertEquals(MessageHandlingException.class, failure.getHeaders().get(MessageHeaders.POSTPROCESS_RESULT).getClass());
		assertEquals("Expression evaluation failed: 1/0", ((Exception) failure.getHeaders().get(MessageHeaders.POSTPROCESS_RESULT)).getMessage());

		// propagate failing advice with failure; expect original exception
		advice.setPropagateEvaluationFailures(true);
		try {
			handler.handleMessage(message);
			fail("Expected Exception");
		}
		catch (MessageHandlingException e) {
			assertEquals("qux", e.getCause().getMessage());
		}
		reply = replies.receive(1);
		assertNull(reply);

		failure = failureChannel.receive(1000);
		assertNotNull(failure);
		assertEquals("Hello, world!", failure.getPayload());
		assertEquals(MessageHandlingException.class, failure.getHeaders().get(MessageHeaders.POSTPROCESS_RESULT).getClass());
		assertEquals("Expression evaluation failed: 1/0", ((Exception) failure.getHeaders().get(MessageHeaders.POSTPROCESS_RESULT)).getMessage());

	}

	@Test
	public void circuitBreakerTests() throws Exception {
		final AtomicBoolean doFail = new AtomicBoolean();
		AbstractReplyProducingMessageHandler handler = new AbstractReplyProducingMessageHandler() {

			@Override
			protected Object handleRequestMessage(Message<?> requestMessage) {
				if (doFail.get()) {
					throw new RuntimeException("foo");
				}
				return "bar";
			}

		};
		handler.setBeanName("baz");
		handler.setOutputChannel(new QueueChannel());
		RequestHandlerCircuitBreakerAdvice advice = new RequestHandlerCircuitBreakerAdvice();
		/*
		 * Circuit breaker opens after 2 failures; allows a new attempt after 100ms and
		 * immediately opens again if that attempt fails. After a successful attempt,
		 * we reset the failure counter.
		 */
		advice.setThreshold(2);
		advice.setHalfOpenAfter(100);

		List<Advice> adviceChain = new ArrayList<Advice>();
		adviceChain.add(advice);
		handler.setAdviceChain(adviceChain);
		handler.afterPropertiesSet();

		doFail.set(true);
		Message<String> message = new GenericMessage<String>("Hello, world!");
		try {
			handler.handleMessage(message);
			fail("Expected failure");
		}
		catch (Exception e) {
			assertEquals("foo", e.getCause().getMessage());
		}
		try {
			handler.handleMessage(message);
			fail("Expected failure");
		}
		catch (Exception e) {
			assertEquals("foo", e.getCause().getMessage());
		}
		try {
			handler.handleMessage(message);
			fail("Expected failure");
		}
		catch (Exception e) {
			assertEquals("Circuit Breaker is Open for baz", e.getMessage());
		}
		Thread.sleep(100);
		try {
			handler.handleMessage(message);
			fail("Expected failure");
		}
		catch (Exception e) {
			assertEquals("foo", e.getCause().getMessage());
		}
		try {
			handler.handleMessage(message);
			fail("Expected failure");
		}
		catch (Exception e) {
			assertEquals("Circuit Breaker is Open for baz", e.getMessage());
		}
		Thread.sleep(100);
		doFail.set(false);
		handler.handleMessage(message);
		doFail.set(true);
		try {
			handler.handleMessage(message);
			fail("Expected failure");
		}
		catch (Exception e) {
			assertEquals("foo", e.getCause().getMessage());
		}
		try {
			handler.handleMessage(message);
			fail("Expected failure");
		}
		catch (Exception e) {
			assertEquals("foo", e.getCause().getMessage());
		}
		try {
			handler.handleMessage(message);
			fail("Expected failure");
		}
		catch (Exception e) {
			assertEquals("Circuit Breaker is Open for baz", e.getMessage());
		}
	}

	@Test
	public void defaultRetrySucceedonThirdTry() {
		final AtomicInteger counter = new AtomicInteger(2);
		AbstractReplyProducingMessageHandler handler = new AbstractReplyProducingMessageHandler() {

			@Override
			protected Object handleRequestMessage(Message<?> requestMessage) {
				if (counter.getAndDecrement() > 0) {
					throw new RuntimeException("foo");
				}
				return "bar";
			}
		};
		QueueChannel replies = new QueueChannel();
		handler.setOutputChannel(replies);
		RequestHandlerRetryAdvice advice = new RequestHandlerRetryAdvice();

		List<Advice> adviceChain = new ArrayList<Advice>();
		adviceChain.add(advice);
		handler.setAdviceChain(adviceChain);
		handler.afterPropertiesSet();

		Message<String> message = new GenericMessage<String>("Hello, world!");
		handler.handleMessage(message);
		assertTrue(counter.get() == -1);
		Message<?> reply = replies.receive(1000);
		assertNotNull(reply);
		assertEquals("bar", reply.getPayload());

	}

	@Test
	public void defaultStatefulRetrySucceedonThirdTry() {
		final AtomicInteger counter = new AtomicInteger(2);
		AbstractReplyProducingMessageHandler handler = new AbstractReplyProducingMessageHandler() {

			@Override
			protected Object handleRequestMessage(Message<?> requestMessage) {
				if (counter.getAndDecrement() > 0) {
					throw new RuntimeException("foo");
				}
				return "bar";
			}
		};
		QueueChannel replies = new QueueChannel();
		handler.setOutputChannel(replies);
		RequestHandlerRetryAdvice advice = new RequestHandlerRetryAdvice();

		advice.setRetryStateGenerator(new RetryStateGenerator() {
			public RetryState determineRetryState(Message<?> message) {
				return new DefaultRetryState(message.getHeaders().getId());
			}
		});

		List<Advice> adviceChain = new ArrayList<Advice>();
		adviceChain.add(advice);
		handler.setAdviceChain(adviceChain);
		handler.afterPropertiesSet();

		Message<String> message = new GenericMessage<String>("Hello, world!");
		for (int i = 0; i < 3; i++) {
			try {
				handler.handleMessage(message);
			}
			catch (Exception e) {
				assertTrue(i < 2);
			}
		}
		assertTrue(counter.get() == -1);
		Message<?> reply = replies.receive(1000);
		assertNotNull(reply);
		assertEquals("bar", reply.getPayload());

	}

	@Test
	public void defaultStatefulRetryRecoverAfterThirdTry() {
		final AtomicInteger counter = new AtomicInteger(3);
		AbstractReplyProducingMessageHandler handler = new AbstractReplyProducingMessageHandler() {

			@Override
			protected Object handleRequestMessage(Message<?> requestMessage) {
				if (counter.getAndDecrement() > 0) {
					throw new RuntimeException("foo");
				}
				return "bar";
			}
		};
		QueueChannel replies = new QueueChannel();
		handler.setOutputChannel(replies);
		RequestHandlerRetryAdvice advice = new RequestHandlerRetryAdvice();

		advice.setRetryStateGenerator(new RetryStateGenerator() {
			public RetryState determineRetryState(Message<?> message) {
				return new DefaultRetryState(message.getHeaders().getId());
			}
		});

		defaultStatefulRetryRecoverAfterThirdTryGuts(counter, handler, replies, advice);

	}

	@Test
	public void defaultStatefulRetryRecoverAfterThirdTrySpelState() {
		final AtomicInteger counter = new AtomicInteger(3);
		AbstractReplyProducingMessageHandler handler = new AbstractReplyProducingMessageHandler() {

			@Override
			protected Object handleRequestMessage(Message<?> requestMessage) {
				if (counter.getAndDecrement() > 0) {
					throw new RuntimeException("foo");
				}
				return "bar";
			}
		};
		QueueChannel replies = new QueueChannel();
		handler.setOutputChannel(replies);
		RequestHandlerRetryAdvice advice = new RequestHandlerRetryAdvice();

		advice.setRetryStateGenerator(new SpelExpressionRetryStateGenerator("headers['id']"));

		defaultStatefulRetryRecoverAfterThirdTryGuts(counter, handler, replies, advice);

	}

	private void defaultStatefulRetryRecoverAfterThirdTryGuts(final AtomicInteger counter,
			AbstractReplyProducingMessageHandler handler, QueueChannel replies, RequestHandlerRetryAdvice advice) {
		advice.setRecoveryCallback(new RecoveryCallback<Object>() {

			public Object recover(RetryContext context) throws Exception {
				return "baz";
			}
		});

		List<Advice> adviceChain = new ArrayList<Advice>();
		adviceChain.add(advice);
		handler.setAdviceChain(adviceChain);
		handler.afterPropertiesSet();

		Message<String> message = new GenericMessage<String>("Hello, world!");
		for (int i = 0; i < 4; i++) {
			try {
				handler.handleMessage(message);
			}
			catch (Exception e) {
			}
		}
		assertTrue(counter.get() == 0);
		Message<?> reply = replies.receive(1000);
		assertNotNull(reply);
		assertEquals("baz", reply.getPayload());
	}
}
