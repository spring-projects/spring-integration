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
package org.springframework.integration.handler.advice;

import static org.hamcrest.Matchers.endsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.aopalliance.aop.Advice;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.logging.Log;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.core.MessageSelector;
import org.springframework.integration.endpoint.PollingConsumer;
import org.springframework.integration.filter.MessageFilter;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.handler.advice.ExpressionEvaluatingRequestHandlerAdvice.MessageHandlingExpressionEvaluatingAdviceException;
import org.springframework.integration.message.AdviceMessage;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.util.ErrorHandlingTaskExecutor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.retry.RecoveryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryState;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.DefaultRetryState;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.ErrorHandler;

/**
 * @author Gary Russell
 * @author Artem Bilan
 * @since 2.2
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext
public class AdvisedMessageHandlerTests {

	@Autowired
	private MessageChannel input;

	@Test
	public void circuitBreakerExceptionText() {
		GenericMessage<String> message = new GenericMessage<String>("foo");
		try {
			input.send(message);
			fail("expected exception");
		}
		catch (MessageHandlingException e) {
			assertThat(e.getCause(), Matchers.instanceOf(ArithmeticException.class));
		}
		try {
			input.send(message);
			fail("expected exception");
		}
		catch (RuntimeException e) {
			assertThat(e.getMessage(), endsWith("(myService)]"));
		}
	}

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
		ExpressionEvaluatingRequestHandlerAdvice advice = new ExpressionEvaluatingRequestHandlerAdvice();
		advice.setBeanFactory(mock(BeanFactory.class));
		advice.setSuccessChannel(successChannel);
		advice.setFailureChannel(failureChannel);
		advice.setOnSuccessExpression("'foo'");
		advice.setOnFailureExpression("'bar:' + #exception.message");

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
		assertEquals("Hello, world!", ((AdviceMessage) success).getInputMessage().getPayload());
		assertEquals("foo", success.getPayload());

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
		assertEquals("Hello, world!", ((MessagingException) failure.getPayload()).getFailedMessage().getPayload());
		assertEquals("bar:qux", ((MessageHandlingExpressionEvaluatingAdviceException) failure.getPayload()).getEvaluationResult());

		// advice with failure, trapped
		advice.setTrapException(true);
		handler.handleMessage(message);
		failure = failureChannel.receive(1000);
		assertNotNull(failure);
		assertEquals("Hello, world!", ((MessagingException) failure.getPayload()).getFailedMessage().getPayload());
		assertEquals("bar:qux", ((MessageHandlingExpressionEvaluatingAdviceException) failure.getPayload()).getEvaluationResult());
		assertNull(replies.receive(1));

		// advice with failure, eval is result
		advice.setReturnFailureExpressionResult(true);
		handler.handleMessage(message);
		failure = failureChannel.receive(1000);
		assertNotNull(failure);
		assertEquals("Hello, world!", ((MessagingException) failure.getPayload()).getFailedMessage().getPayload());
		assertEquals("bar:qux", ((MessageHandlingExpressionEvaluatingAdviceException) failure.getPayload()).getEvaluationResult());

		reply = replies.receive(1000);
		assertNotNull(reply);
		assertEquals("bar:qux", reply.getPayload());

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
		ExpressionEvaluatingRequestHandlerAdvice advice = new ExpressionEvaluatingRequestHandlerAdvice();
		advice.setBeanFactory(mock(BeanFactory.class));
		advice.setSuccessChannel(successChannel);
		advice.setFailureChannel(failureChannel);
		advice.setOnSuccessExpression("1/0");
		advice.setOnFailureExpression("1/0");

		List<Advice> adviceChain = new ArrayList<Advice>();
		adviceChain.add(advice);
		handler.setAdviceChain(adviceChain);
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.afterPropertiesSet();

		// failing advice with success
		handler.handleMessage(message);
		Message<?> reply = replies.receive(1000);
		assertNotNull(reply);
		assertEquals("baz", reply.getPayload());

		Message<?> success = successChannel.receive(1000);
		assertNotNull(success);
		assertEquals("Hello, world!", ((AdviceMessage) success).getInputMessage().getPayload());
		assertEquals(ArithmeticException.class, success.getPayload().getClass());
		assertEquals("/ by zero", ((Exception) success.getPayload()).getMessage());

		// propagate failing advice with success
		advice.setPropagateEvaluationFailures(true);
		try {
			handler.handleMessage(message);
			fail("Expected Exception");
		}
		catch (MessageHandlingException e) {
			assertEquals("/ by zero", e.getCause().getMessage());
		}
		reply = replies.receive(1);
		assertNull(reply);

		success = successChannel.receive(1000);
		assertNotNull(success);
		assertEquals("Hello, world!", ((AdviceMessage) success).getInputMessage().getPayload());
		assertEquals(ArithmeticException.class, success.getPayload().getClass());
		assertEquals("/ by zero", ((Exception) success.getPayload()).getMessage());

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
		ExpressionEvaluatingRequestHandlerAdvice advice = new ExpressionEvaluatingRequestHandlerAdvice();
		advice.setBeanFactory(mock(BeanFactory.class));
		advice.setSuccessChannel(successChannel);
		advice.setFailureChannel(failureChannel);
		advice.setOnSuccessExpression("1/0");
		advice.setOnFailureExpression("1/0");

		List<Advice> adviceChain = new ArrayList<Advice>();
		adviceChain.add(advice);
		handler.setAdviceChain(adviceChain);
		handler.setBeanFactory(mock(BeanFactory.class));
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
		assertEquals("Hello, world!", ((MessagingException) failure.getPayload()).getFailedMessage().getPayload());
		assertEquals(MessageHandlingExpressionEvaluatingAdviceException.class, failure.getPayload().getClass());
		assertEquals("qux", ((Exception) failure.getPayload()).getCause().getMessage());

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
		assertEquals("Hello, world!", ((MessagingException) failure.getPayload()).getFailedMessage().getPayload());
		assertEquals(MessageHandlingExpressionEvaluatingAdviceException.class, failure.getPayload().getClass());
		assertEquals("qux", ((Exception) failure.getPayload()).getCause().getMessage());

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
		handler.setBeanFactory(mock(BeanFactory.class));
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
			assertEquals("Circuit Breaker is Open for baz", e.getCause().getMessage());
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
			assertEquals("Circuit Breaker is Open for baz", e.getCause().getMessage());
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
			assertEquals("Circuit Breaker is Open for baz", e.getCause().getMessage());
		}
	}

	@Test
	public void defaultRetrySucceedOnThirdTry() {
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
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.afterPropertiesSet();

		Message<String> message = new GenericMessage<String>("Hello, world!");
		handler.handleMessage(message);
		assertTrue(counter.get() == -1);
		Message<?> reply = replies.receive(1000);
		assertNotNull(reply);
		assertEquals("bar", reply.getPayload());

	}

	@Test
	public void defaultStatefulRetrySucceedOnThirdTry() {
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
			@Override
			public RetryState determineRetryState(Message<?> message) {
				return new DefaultRetryState(message.getHeaders().getId());
			}
		});

		List<Advice> adviceChain = new ArrayList<Advice>();
		adviceChain.add(advice);
		handler.setAdviceChain(adviceChain);
		handler.setBeanFactory(mock(BeanFactory.class));
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
			@Override
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

			@Override
			public Object recover(RetryContext context) throws Exception {
				return "baz";
			}
		});

		List<Advice> adviceChain = new ArrayList<Advice>();
		adviceChain.add(advice);
		handler.setAdviceChain(adviceChain);
		handler.setBeanFactory(mock(BeanFactory.class));
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

	@Test
	public void errorMessageSendingRecovererTests() {
		AbstractReplyProducingMessageHandler handler = new AbstractReplyProducingMessageHandler() {

			@Override
			protected Object handleRequestMessage(Message<?> requestMessage) {
				throw new RuntimeException("fooException");
			}
		};
		QueueChannel errors = new QueueChannel();
		RequestHandlerRetryAdvice advice = new RequestHandlerRetryAdvice();
		ErrorMessageSendingRecoverer recoverer = new ErrorMessageSendingRecoverer(errors);
		advice.setRecoveryCallback(recoverer);

		List<Advice> adviceChain = new ArrayList<Advice>();
		adviceChain.add(advice);
		handler.setAdviceChain(adviceChain);
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.afterPropertiesSet();

		Message<String> message = new GenericMessage<String>("Hello, world!");
		handler.handleMessage(message);
		Message<?> error = errors.receive(1000);
		assertNotNull(error);
		assertEquals("fooException", ((Exception) error.getPayload()).getCause().getMessage());

	}

	@Test
	public void errorMessageSendingRecovererTestsNoThrowable() {
		AbstractReplyProducingMessageHandler handler = new AbstractReplyProducingMessageHandler() {

			@Override
			protected Object handleRequestMessage(Message<?> requestMessage) {
				throw new RuntimeException("fooException");
			}
		};
		QueueChannel errors = new QueueChannel();
		RequestHandlerRetryAdvice advice = new RequestHandlerRetryAdvice();
		ErrorMessageSendingRecoverer recoverer = new ErrorMessageSendingRecoverer(errors);
		advice.setRecoveryCallback(recoverer);
		RetryTemplate retryTemplate = new RetryTemplate();
		retryTemplate.setRetryPolicy(new SimpleRetryPolicy() {

			@Override
			public boolean canRetry(RetryContext context) {
				return false;
			}
		});
		advice.setRetryTemplate(retryTemplate);
		advice.setBeanFactory(mock(BeanFactory.class));
		advice.afterPropertiesSet();

		List<Advice> adviceChain = new ArrayList<Advice>();
		adviceChain.add(advice);
		handler.setAdviceChain(adviceChain);
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.afterPropertiesSet();

		Message<String> message = new GenericMessage<String>("Hello, world!");
		handler.handleMessage(message);
		Message<?> error = errors.receive(1000);
		assertNotNull(error);
		assertTrue(error.getPayload() instanceof ErrorMessageSendingRecoverer.RetryExceptionNotAvailableException);
		assertNotNull(((MessagingException) error.getPayload()).getFailedMessage());
		assertSame(message, ((MessagingException) error.getPayload()).getFailedMessage());
	}

	@Test
	public void testINT2858RetryAdviceAsFirstInAdviceChain() {
		final AtomicInteger counter = new AtomicInteger(3);

		AbstractReplyProducingMessageHandler handler = new AbstractReplyProducingMessageHandler() {
			@Override
			protected Object handleRequestMessage(Message<?> requestMessage) {
				return "foo";
			}
		};

		List<Advice> adviceChain = new ArrayList<Advice>();

		adviceChain.add(new RequestHandlerRetryAdvice());
		adviceChain.add(new MethodInterceptor() {
			@Override
			public Object invoke(MethodInvocation invocation) throws Throwable {
				counter.getAndDecrement();
				throw new RuntimeException("intentional");
			}
		});

		handler.setBeanFactory(mock(BeanFactory.class));
		handler.setAdviceChain(adviceChain);
		handler.afterPropertiesSet();

		try {
			handler.handleMessage(new GenericMessage<String>("test"));
		}
		catch (Exception e) {
			Throwable cause = e.getCause();
			assertEquals(RuntimeException.class, cause.getClass());
			assertEquals("intentional", cause.getMessage());
		}

		assertTrue(counter.get() == 0);
	}

	@Test
	public void testINT2858RetryAdviceAsNestedInAdviceChain() {
		final AtomicInteger counter = new AtomicInteger(0);

		AbstractReplyProducingMessageHandler handler = new AbstractReplyProducingMessageHandler() {
			@Override
			protected Object handleRequestMessage(Message<?> requestMessage) {
				return "foo";
			}
		};

		QueueChannel replies = new QueueChannel();
		handler.setOutputChannel(replies);

		List<Advice> adviceChain = new ArrayList<Advice>();

		ExpressionEvaluatingRequestHandlerAdvice expressionAdvice = new ExpressionEvaluatingRequestHandlerAdvice();
		expressionAdvice.setBeanFactory(mock(BeanFactory.class));
//		MessagingException / RuntimeException
		expressionAdvice.setOnFailureExpression("#exception.cause.message");
		expressionAdvice.setReturnFailureExpressionResult(true);
		final AtomicInteger outerCounter = new AtomicInteger();
		adviceChain.add(new AbstractRequestHandlerAdvice() {

			@Override
			protected Object doInvoke(ExecutionCallback callback, Object target, Message<?> message) throws Exception {
				outerCounter.incrementAndGet();
				return callback.execute();
			}
		});
		adviceChain.add(expressionAdvice);
		adviceChain.add(new RequestHandlerRetryAdvice());
		adviceChain.add(new MethodInterceptor() {
			@Override
			public Object invoke(MethodInvocation invocation) throws Throwable {
				throw new RuntimeException("intentional: " + counter.incrementAndGet());
			}
		});

		handler.setAdviceChain(adviceChain);
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.afterPropertiesSet();

		handler.handleMessage(new GenericMessage<String>("test"));
		Message<?> receive = replies.receive(1000);
		assertNotNull(receive);
		assertEquals("intentional: 3", receive.getPayload());
		assertEquals(1, outerCounter.get());
	}


	@Test
	public void testINT2858ExpressionAdviceWithSendFailureOnEachRetry() {
		final AtomicInteger counter = new AtomicInteger(0);

		AbstractReplyProducingMessageHandler handler = new AbstractReplyProducingMessageHandler() {
			@Override
			protected Object handleRequestMessage(Message<?> requestMessage) {
				return "foo";
			}
		};

		QueueChannel errors = new QueueChannel();

		List<Advice> adviceChain = new ArrayList<Advice>();

		ExpressionEvaluatingRequestHandlerAdvice expressionAdvice = new ExpressionEvaluatingRequestHandlerAdvice();
		expressionAdvice.setBeanFactory(mock(BeanFactory.class));
		expressionAdvice.setOnFailureExpression("#exception.message");
		expressionAdvice.setFailureChannel(errors);

		adviceChain.add(new RequestHandlerRetryAdvice());
		adviceChain.add(expressionAdvice);
		adviceChain.add(new MethodInterceptor() {
			@Override
			public Object invoke(MethodInvocation invocation) throws Throwable {
				throw new RuntimeException("intentional: " + counter.incrementAndGet());
			}
		});

		handler.setAdviceChain(adviceChain);
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.afterPropertiesSet();

		try {
			handler.handleMessage(new GenericMessage<String>("test"));
		}
		catch (Exception e) {
			assertEquals("intentional: 3", e.getCause().getMessage());
		}

		for (int i = 1; i <= 3; i++) {
			Message<?> receive = errors.receive(1000);
			assertNotNull(receive);
			assertEquals("intentional: " + i, ((MessageHandlingExpressionEvaluatingAdviceException) receive.getPayload()).getEvaluationResult());
		}

		assertNull(errors.receive(1));

	}

	/**
	 * Verify that Errors such as OOM are properly propagated.
	 */
	@Test
	public void throwableProperlyPropagated() throws Exception {
		AbstractRequestHandlerAdvice advice = new AbstractRequestHandlerAdvice() {

			@Override
			protected Object doInvoke(ExecutionCallback callback, Object target, Message<?> message) throws Exception {
				Object result;
				try {
					result = callback.execute();
				}
				catch (Exception e) {
					// should not be unwrapped because the cause is a Throwable
					throw this.unwrapExceptionIfNecessary(e);
				}
				return result;
			}
		};
		final Throwable theThrowable = new Throwable("foo");
		MethodInvocation methodInvocation = mock(MethodInvocation.class);

		Method method = AbstractReplyProducingMessageHandler.class.getDeclaredMethod("handleRequestMessage", Message.class);
		when(methodInvocation.getMethod()).thenReturn(method);
		when(methodInvocation.getArguments()).thenReturn(new Object[] {new GenericMessage<String>("foo")});
		try {
			doAnswer(new Answer<Object>() {
				@Override
				public Object answer(InvocationOnMock invocation) throws Throwable {
					throw theThrowable;
				}
			}).when(methodInvocation).proceed();
			advice.invoke(methodInvocation);
			fail("Expected throwable");
		}
		catch (Throwable t) {
			assertSame(theThrowable, t);
		}
	}

	/**
	 * Verify that Errors such as OOM are properly propagated and we suppress the
	 * ThrowableHolderException from the output message.
	 */
	@Test
	public void throwableProperlyPropagatedAndReported() throws Exception {
		QueueChannel errors = new QueueChannel();

		ExpressionEvaluatingRequestHandlerAdvice expressionAdvice = new ExpressionEvaluatingRequestHandlerAdvice();
		expressionAdvice.setBeanFactory(mock(BeanFactory.class));
		expressionAdvice.setOnFailureExpression("'foo'");
		expressionAdvice.setFailureChannel(errors);

		Throwable theThrowable = new Throwable("foo");
		ProxyFactory proxyFactory = new ProxyFactory(new Foo(theThrowable));
		proxyFactory.addAdvice(expressionAdvice);

		Bar fooHandler = (Bar) proxyFactory.getProxy();

		try {
			fooHandler.handleRequestMessage(new GenericMessage<String>("foo"));
			fail("Expected throwable");
		}
		catch (Throwable t) {
			assertSame(theThrowable, t);
			ErrorMessage error = (ErrorMessage) errors.receive(1000);
			assertNotNull(error);
			assertSame(theThrowable, error.getPayload().getCause());
		}
	}

	@Test
	public void testInappropriateAdvice() throws Exception {
		final AtomicBoolean called = new AtomicBoolean(false);
		Advice advice = new AbstractRequestHandlerAdvice() {
			@Override
			protected Object doInvoke(ExecutionCallback callback, Object target, Message<?> message) throws Exception {
				called.set(true);
				return callback.execute();
			}
		};
		PollableChannel inputChannel = new QueueChannel();
		PollingConsumer consumer = new PollingConsumer(inputChannel, new MessageHandler() {
			@Override
			public void handleMessage(Message<?> message) throws MessagingException {
			}
		});
		consumer.setAdviceChain(Collections.singletonList(advice));
		consumer.setTaskExecutor(new ErrorHandlingTaskExecutor(
				Executors.newSingleThreadExecutor(),
				new ErrorHandler() {
					@Override
					public void handleError(Throwable t) {
					}
				}));
		consumer.setBeanFactory(mock(BeanFactory.class));
		consumer.afterPropertiesSet();

		Callable<?> pollingTask = TestUtils.getPropertyValue(consumer, "poller.pollingTask", Callable.class);
		assertTrue(AopUtils.isAopProxy(pollingTask));
		Log logger = TestUtils.getPropertyValue(advice, "logger", Log.class);
		logger = spy(logger);
		when(logger.isWarnEnabled()).thenReturn(Boolean.TRUE);
		final AtomicReference<String> logMessage = new AtomicReference<String>();
		doAnswer(new Answer<Object>() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				logMessage.set((String) invocation.getArguments()[0]);
				return null;
			}
		}).when(logger).warn(Mockito.anyString());
		DirectFieldAccessor accessor = new DirectFieldAccessor(advice);
		accessor.setPropertyValue("logger", logger);

		pollingTask.call();
		assertFalse(called.get());
		assertNotNull(logMessage.get());
		assertTrue(logMessage.get().endsWith("can only be used for MessageHandlers; " +
				"an attempt to advise method 'call' in " +
				"'org.springframework.integration.endpoint.AbstractPollingEndpoint$1' is ignored"));
	}

	public void filterDiscardNoAdvice() {
		MessageFilter filter = new MessageFilter(new MessageSelector() {
			@Override
			public boolean accept(Message<?> message) {
				return false;
			}
		});
		QueueChannel discardChannel = new QueueChannel();
		filter.setDiscardChannel(discardChannel);
		filter.handleMessage(new GenericMessage<String>("foo"));
		assertNotNull(discardChannel.receive(0));
	}

	@Test
	public void filterDiscardWithinAdvice() {
		MessageFilter filter = new MessageFilter(new MessageSelector() {
			@Override
			public boolean accept(Message<?> message) {
				return false;
			}
		});
		final QueueChannel discardChannel = new QueueChannel();
		filter.setDiscardChannel(discardChannel);
		List<Advice> adviceChain = new ArrayList<Advice>();
		final AtomicReference<Message<?>> discardedWithinAdvice = new AtomicReference<Message<?>>();
		adviceChain.add(new AbstractRequestHandlerAdvice() {
			@Override
			protected Object doInvoke(ExecutionCallback callback, Object target, Message<?> message) throws Exception {
				Object result = callback.execute();
				discardedWithinAdvice.set(discardChannel.receive(0));
				return result;
			}
		});
		filter.setAdviceChain(adviceChain);
		filter.setBeanFactory(mock(BeanFactory.class));
		filter.afterPropertiesSet();
		filter.handleMessage(new GenericMessage<String>("foo"));
		assertNotNull(discardedWithinAdvice.get());
		assertNull(discardChannel.receive(0));
	}

	@Test
	public void filterDiscardOutsideAdvice() {
		MessageFilter filter = new MessageFilter(new MessageSelector() {
			@Override
			public boolean accept(Message<?> message) {
				return false;
			}
		});
		final QueueChannel discardChannel = new QueueChannel();
		filter.setDiscardChannel(discardChannel);
		List<Advice> adviceChain = new ArrayList<Advice>();
		final AtomicReference<Message<?>> discardedWithinAdvice = new AtomicReference<Message<?>>();
		final AtomicBoolean adviceCalled = new AtomicBoolean();
		adviceChain.add(new AbstractRequestHandlerAdvice() {
			@Override
			protected Object doInvoke(ExecutionCallback callback, Object target, Message<?> message) throws Exception {
				Object result = callback.execute();
				discardedWithinAdvice.set(discardChannel.receive(0));
				adviceCalled.set(true);
				return result;
			}
		});
		filter.setAdviceChain(adviceChain);
		filter.setDiscardWithinAdvice(false);
		filter.setBeanFactory(mock(BeanFactory.class));
		filter.afterPropertiesSet();
		filter.handleMessage(new GenericMessage<String>("foo"));
		assertTrue(adviceCalled.get());
		assertNull(discardedWithinAdvice.get());
		assertNotNull(discardChannel.receive(0));
	}

	@Test
	public void testInt2943RetryWithExceptionClassifierFalse() {
		testInt2943RetryWithExceptionClassifier(false, 1);
	}

	@Test
	public void testInt2943RetryWithExceptionClassifierTrue() {
		testInt2943RetryWithExceptionClassifier(true, 3);
	}

	private void testInt2943RetryWithExceptionClassifier(boolean retryForMyException, int expected) {
		final AtomicInteger counter = new AtomicInteger(0);

		@SuppressWarnings("serial")
		class MyException extends RuntimeException {

		}

		AbstractReplyProducingMessageHandler handler = new AbstractReplyProducingMessageHandler() {

			@Override
			protected Object handleRequestMessage(Message<?> requestMessage) {
				counter.incrementAndGet();
				throw new MyException();

			}
		};
		QueueChannel replies = new QueueChannel();
		handler.setOutputChannel(replies);
		RequestHandlerRetryAdvice advice = new RequestHandlerRetryAdvice();

		RetryTemplate retryTemplate = new RetryTemplate();

		Map<Class<? extends Throwable>, Boolean> retryableExceptions = new HashMap<Class<? extends Throwable>, Boolean>();
		retryableExceptions.put(MyException.class, retryForMyException);
		retryableExceptions.put(MessagingException.class, true);

		retryTemplate.setRetryPolicy(new SimpleRetryPolicy(3, retryableExceptions));

		advice.setRetryTemplate(retryTemplate);

		List<Advice> adviceChain = new ArrayList<Advice>();
		adviceChain.add(advice);
		handler.setAdviceChain(adviceChain);
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.afterPropertiesSet();

		Message<String> message = new GenericMessage<String>("Hello, world!");
		try {
			handler.handleMessage(message);
			fail("MessagingException expected.");
		}
		catch (Exception e) {
			assertThat(e, Matchers.instanceOf(MessagingException.class));
			assertThat(e.getCause(), Matchers.instanceOf(MyException.class));
		}

		assertEquals(expected, counter.get());

	}

	private interface Bar {
		Object handleRequestMessage(Message<?> message) throws Throwable;
	}

	private class Foo implements Bar {

		public final Throwable throwable;

		public Foo(Throwable throwable) {
			this.throwable = throwable;
		}

		@Override
		public Object handleRequestMessage(Message<?> message) throws Throwable {
			throw this.throwable;
		}
	}
}
