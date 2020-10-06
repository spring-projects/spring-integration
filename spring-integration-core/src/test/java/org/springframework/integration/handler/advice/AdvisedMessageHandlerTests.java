/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.integration.handler.advice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.anyString;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.aopalliance.aop.Advice;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.Test;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.log.LogAccessor;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.endpoint.PollingConsumer;
import org.springframework.integration.filter.MessageFilter;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.handler.advice.ExpressionEvaluatingRequestHandlerAdvice.MessageHandlingExpressionEvaluatingAdviceException;
import org.springframework.integration.message.AdviceMessage;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.util.ErrorHandlingTaskExecutor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.retry.RetryContext;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.DefaultRetryState;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.2
 */
@SpringJUnitConfig
@DirtiesContext
public class AdvisedMessageHandlerTests {

	@Autowired
	private MessageChannel input;

	@Test
	public void circuitBreakerExceptionText() {
		GenericMessage<String> message = new GenericMessage<>("foo");
		try {
			input.send(message);
			fail("expected exception");
		}
		catch (MessageHandlingException e) {
			assertThat(e.getCause()).isInstanceOf(ArithmeticException.class);
		}
		try {
			input.send(message);
			fail("expected exception");
		}
		catch (RuntimeException e) {
			assertThat(e.getMessage()).contains("Circuit Breaker is Open for");
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
		String componentName = "testComponentName";
		handler.setComponentName(componentName);
		QueueChannel replies = new QueueChannel();
		handler.setOutputChannel(replies);
		Message<String> message = new GenericMessage<>("Hello, world!");

		// no advice
		handler.handleMessage(message);
		Message<?> reply = replies.receive(1000);
		assertThat(reply).isNotNull();
		assertThat(reply.getPayload()).isEqualTo("baz");

		PollableChannel successChannel = new QueueChannel();
		PollableChannel failureChannel = new QueueChannel();
		ExpressionEvaluatingRequestHandlerAdvice advice = new ExpressionEvaluatingRequestHandlerAdvice();
		advice.setBeanFactory(mock(BeanFactory.class));
		advice.setSuccessChannel(successChannel);
		advice.setFailureChannel(failureChannel);
		advice.setOnSuccessExpressionString("'foo'");
		advice.setOnFailureExpressionString("'bar:' + #exception.message");

		List<Advice> adviceChain = new ArrayList<Advice>();
		adviceChain.add(advice);
		final AtomicReference<String> compName = new AtomicReference<>();
		adviceChain.add(new AbstractRequestHandlerAdvice() {

			@Override
			protected Object doInvoke(ExecutionCallback callback, Object target, Message<?> message) {
				compName.set(((AbstractReplyProducingMessageHandler.RequestHandler) target).getAdvisedHandler()
						.getComponentName());
				return callback.execute();
			}

		});
		handler.setAdviceChain(adviceChain);
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.afterPropertiesSet();

		// advice with success
		handler.handleMessage(message);
		reply = replies.receive(1000);
		assertThat(reply).isNotNull();
		assertThat(reply.getPayload()).isEqualTo("baz");

		assertThat(compName.get()).isEqualTo(componentName);

		Message<?> success = successChannel.receive(1000);
		assertThat(success).isNotNull();
		assertThat(((AdviceMessage<?>) success).getInputMessage().getPayload()).isEqualTo("Hello, world!");
		assertThat(success.getPayload()).isEqualTo("foo");

		// advice with failure, not trapped
		doFail.set(true);
		try {
			handler.handleMessage(message);
			fail("Expected exception");
		}
		catch (Exception e) {
			assertThat(e.getCause().getMessage()).isEqualTo("qux");
		}

		Message<?> failure = failureChannel.receive(1000);
		assertThat(failure).isNotNull();
		assertThat(((MessagingException) failure.getPayload()).getFailedMessage().getPayload())
				.isEqualTo("Hello, world!");
		assertThat(((MessageHandlingExpressionEvaluatingAdviceException) failure.getPayload()).getEvaluationResult())
				.isEqualTo("bar:qux");

		// advice with failure, trapped
		advice.setTrapException(true);
		handler.handleMessage(message);
		failure = failureChannel.receive(1000);
		assertThat(failure).isNotNull();
		assertThat(((MessagingException) failure.getPayload()).getFailedMessage().getPayload())
				.isEqualTo("Hello, world!");
		assertThat(((MessageHandlingExpressionEvaluatingAdviceException) failure.getPayload()).getEvaluationResult())
				.isEqualTo("bar:qux");
		assertThat(replies.receive(1)).isNull();

		// advice with failure, eval is result
		advice.setReturnFailureExpressionResult(true);
		handler.handleMessage(message);
		failure = failureChannel.receive(1000);
		assertThat(failure).isNotNull();
		assertThat(((MessagingException) failure.getPayload()).getFailedMessage().getPayload())
				.isEqualTo("Hello, world!");
		assertThat(((MessageHandlingExpressionEvaluatingAdviceException) failure.getPayload()).getEvaluationResult())
				.isEqualTo("bar:qux");

		reply = replies.receive(1000);
		assertThat(reply).isNotNull();
		assertThat(reply.getPayload()).isEqualTo("bar:qux");

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
		Message<String> message = new GenericMessage<>("Hello, world!");

		PollableChannel successChannel = new QueueChannel();
		PollableChannel failureChannel = new QueueChannel();
		ExpressionEvaluatingRequestHandlerAdvice advice = new ExpressionEvaluatingRequestHandlerAdvice();
		advice.setBeanFactory(mock(BeanFactory.class));
		advice.setSuccessChannel(successChannel);
		advice.setFailureChannel(failureChannel);
		advice.setOnSuccessExpressionString("1/0");
		advice.setOnFailureExpressionString("1/0");

		List<Advice> adviceChain = new ArrayList<Advice>();
		adviceChain.add(advice);
		handler.setAdviceChain(adviceChain);
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.afterPropertiesSet();

		// failing advice with success
		handler.handleMessage(message);
		Message<?> reply = replies.receive(1000);
		assertThat(reply).isNotNull();
		assertThat(reply.getPayload()).isEqualTo("baz");

		Message<?> success = successChannel.receive(1000);
		assertThat(success).isNotNull();
		assertThat(((AdviceMessage<?>) success).getInputMessage().getPayload()).isEqualTo("Hello, world!");
		assertThat(success.getPayload().getClass()).isEqualTo(ArithmeticException.class);
		assertThat(((Exception) success.getPayload()).getMessage()).isEqualTo("/ by zero");

		// propagate failing advice with success
		advice.setPropagateEvaluationFailures(true);
		try {
			handler.handleMessage(message);
			fail("Expected Exception");
		}
		catch (MessageHandlingException e) {
			assertThat(e.getCause().getMessage()).isEqualTo("/ by zero");
		}
		reply = replies.receive(1);
		assertThat(reply).isNull();

		success = successChannel.receive(1000);
		assertThat(success).isNotNull();
		assertThat(((AdviceMessage<?>) success).getInputMessage().getPayload()).isEqualTo("Hello, world!");
		assertThat(success.getPayload().getClass()).isEqualTo(ArithmeticException.class);
		assertThat(((Exception) success.getPayload()).getMessage()).isEqualTo("/ by zero");

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
		Message<String> message = new GenericMessage<>("Hello, world!");

		PollableChannel successChannel = new QueueChannel();
		PollableChannel failureChannel = new QueueChannel();
		ExpressionEvaluatingRequestHandlerAdvice advice = new ExpressionEvaluatingRequestHandlerAdvice();
		advice.setBeanFactory(mock(BeanFactory.class));
		advice.setSuccessChannel(successChannel);
		advice.setFailureChannel(failureChannel);
		advice.setOnSuccessExpressionString("1/0");
		advice.setOnFailureExpressionString("1/0");

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
			assertThat(e.getCause().getMessage()).isEqualTo("qux");
		}
		Message<?> reply = replies.receive(1);
		assertThat(reply).isNull();

		Message<?> failure = failureChannel.receive(10000);
		assertThat(failure).isNotNull();
		assertThat(((MessagingException) failure.getPayload()).getFailedMessage().getPayload())
				.isEqualTo("Hello, world!");
		assertThat(failure.getPayload().getClass()).isEqualTo(MessageHandlingExpressionEvaluatingAdviceException.class);
		assertThat(((Exception) failure.getPayload()).getCause().getMessage()).isEqualTo("qux");

		// propagate failing advice with failure; expect original exception
		advice.setPropagateEvaluationFailures(true);
		try {
			handler.handleMessage(message);
			fail("Expected Exception");
		}
		catch (MessageHandlingException e) {
			assertThat(e.getCause().getMessage()).isEqualTo("qux");
		}
		reply = replies.receive(1);
		assertThat(reply).isNull();

		failure = failureChannel.receive(10000);
		assertThat(failure).isNotNull();
		assertThat(((MessagingException) failure.getPayload()).getFailedMessage().getPayload())
				.isEqualTo("Hello, world!");
		assertThat(failure.getPayload().getClass()).isEqualTo(MessageHandlingExpressionEvaluatingAdviceException.class);
		assertThat(((Exception) failure.getPayload()).getCause().getMessage()).isEqualTo("qux");

	}

	@Test
	@SuppressWarnings("rawtypes")
	public void circuitBreakerTests() {
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
		advice.setHalfOpenAfter(1000);

		List<Advice> adviceChain = new ArrayList<>();
		adviceChain.add(advice);
		handler.setAdviceChain(adviceChain);
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.afterPropertiesSet();

		doFail.set(true);
		Message<String> message = new GenericMessage<>("Hello, world!");
		try {
			handler.handleMessage(message);
			fail("Expected failure");
		}
		catch (Exception e) {
			assertThat(e.getCause().getMessage()).isEqualTo("foo");
		}
		try {
			handler.handleMessage(message);
			fail("Expected failure");
		}
		catch (Exception e) {
			assertThat(e.getCause().getMessage()).isEqualTo("foo");
		}
		try {
			handler.handleMessage(message);
			fail("Expected failure");
		}
		catch (Exception e) {
			assertThat(e.getMessage()).isEqualTo("Circuit Breaker is Open for bean 'baz'");
			assertThat(((MessagingException) e).getFailedMessage()).isSameAs(message);
		}

		Map metadataMap = TestUtils.getPropertyValue(advice, "metadataMap", Map.class);
		Object metadata = metadataMap.values().iterator().next();

		DirectFieldAccessor metadataDfa = new DirectFieldAccessor(metadata);

		// Simulate some timeout in between requests
		metadataDfa.setPropertyValue("lastFailure", System.currentTimeMillis() - 10000);

		try {
			handler.handleMessage(message);
			fail("Expected failure");
		}
		catch (Exception e) {
			assertThat(e.getCause().getMessage()).isEqualTo("foo");
		}
		try {
			handler.handleMessage(message);
			fail("Expected failure");
		}
		catch (Exception e) {
			assertThat(e.getMessage()).isEqualTo("Circuit Breaker is Open for bean 'baz'");
		}

		// Simulate some timeout in between requests
		metadataDfa.setPropertyValue("lastFailure", System.currentTimeMillis() - 10000);

		doFail.set(false);
		handler.handleMessage(message);
		doFail.set(true);
		try {
			handler.handleMessage(message);
			fail("Expected failure");
		}
		catch (Exception e) {
			assertThat(e.getCause().getMessage()).isEqualTo("foo");
		}
		try {
			handler.handleMessage(message);
			fail("Expected failure");
		}
		catch (Exception e) {
			assertThat(e.getCause().getMessage()).isEqualTo("foo");
		}
		try {
			handler.handleMessage(message);
			fail("Expected failure");
		}
		catch (Exception e) {
			assertThat(e.getMessage()).isEqualTo("Circuit Breaker is Open for bean 'baz'");
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

		List<Advice> adviceChain = new ArrayList<>();
		adviceChain.add(advice);
		handler.setAdviceChain(adviceChain);
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.afterPropertiesSet();

		Message<String> message = new GenericMessage<>("Hello, world!");
		handler.handleMessage(message);
		assertThat(counter.get() == -1).isTrue();
		Message<?> reply = replies.receive(10000);
		assertThat(reply).isNotNull();
		assertThat(reply.getPayload()).isEqualTo("bar");

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

		advice.setRetryStateGenerator(message -> new DefaultRetryState(message.getHeaders().getId()));

		List<Advice> adviceChain = new ArrayList<>();
		adviceChain.add(advice);
		handler.setAdviceChain(adviceChain);
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.afterPropertiesSet();

		Message<String> message = new GenericMessage<>("Hello, world!");
		for (int i = 0; i < 3; i++) {
			try {
				handler.handleMessage(message);
			}
			catch (Exception e) {
				assertThat(i < 2).isTrue();
			}
		}
		assertThat(counter.get() == -1).isTrue();
		Message<?> reply = replies.receive(10000);
		assertThat(reply).isNotNull();
		assertThat(reply.getPayload()).isEqualTo("bar");

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

		advice.setRetryStateGenerator(message -> new DefaultRetryState(message.getHeaders().getId()));

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
		advice.setRecoveryCallback(context -> "baz");

		List<Advice> adviceChain = new ArrayList<>();
		adviceChain.add(advice);
		handler.setAdviceChain(adviceChain);
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.afterPropertiesSet();

		Message<String> message = new GenericMessage<>("Hello, world!");
		for (int i = 0; i < 4; i++) {
			try {
				handler.handleMessage(message);
			}
			catch (Exception e) {
			}
		}
		assertThat(counter.get() == 0).isTrue();
		Message<?> reply = replies.receive(10000);
		assertThat(reply).isNotNull();
		assertThat(reply.getPayload()).isEqualTo("baz");
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

		List<Advice> adviceChain = new ArrayList<>();
		adviceChain.add(advice);
		handler.setAdviceChain(adviceChain);
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.afterPropertiesSet();

		Message<String> message = new GenericMessage<>("Hello, world!");
		handler.handleMessage(message);
		Message<?> error = errors.receive(10000);
		assertThat(error).isNotNull();
		assertThat(((Exception) error.getPayload()).getCause().getMessage()).isEqualTo("fooException");

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

			static final long serialVersionUID = -1;

			@Override
			public boolean canRetry(RetryContext context) {
				return false;
			}
		});
		advice.setRetryTemplate(retryTemplate);
		advice.setBeanFactory(mock(BeanFactory.class));
		advice.afterPropertiesSet();

		List<Advice> adviceChain = new ArrayList<>();
		adviceChain.add(advice);
		handler.setAdviceChain(adviceChain);
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.afterPropertiesSet();

		Message<String> message = new GenericMessage<>("Hello, world!");
		handler.handleMessage(message);
		Message<?> error = errors.receive(10000);
		assertThat(error).isNotNull();
		assertThat(error.getPayload() instanceof ErrorMessageSendingRecoverer.RetryExceptionNotAvailableException)
				.isTrue();
		assertThat(((MessagingException) error.getPayload()).getFailedMessage()).isNotNull();
		assertThat(((MessagingException) error.getPayload()).getFailedMessage()).isSameAs(message);
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

		List<Advice> adviceChain = new ArrayList<>();

		adviceChain.add(new RequestHandlerRetryAdvice());
		adviceChain.add((MethodInterceptor) invocation -> {
			counter.getAndDecrement();
			throw new RuntimeException("intentional");
		});

		handler.setBeanFactory(mock(BeanFactory.class));
		handler.setAdviceChain(adviceChain);
		handler.afterPropertiesSet();

		try {
			handler.handleMessage(new GenericMessage<>("test"));
		}
		catch (Exception e) {
			Throwable cause = e.getCause();
			assertThat(cause.getClass()).isEqualTo(RuntimeException.class);
			assertThat(cause.getMessage()).isEqualTo("intentional");
		}

		assertThat(counter.get() == 0).isTrue();
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

		List<Advice> adviceChain = new ArrayList<>();

		ExpressionEvaluatingRequestHandlerAdvice expressionAdvice = new ExpressionEvaluatingRequestHandlerAdvice();
		expressionAdvice.setBeanFactory(mock(BeanFactory.class));
//		MessagingException / RuntimeException
		expressionAdvice.setOnFailureExpressionString("#exception.cause.message");
		expressionAdvice.setReturnFailureExpressionResult(true);
		final AtomicInteger outerCounter = new AtomicInteger();
		adviceChain.add(new AbstractRequestHandlerAdvice() {

			@Override
			protected Object doInvoke(ExecutionCallback callback, Object target, Message<?> message) {
				outerCounter.incrementAndGet();
				return callback.execute();
			}
		});
		adviceChain.add(expressionAdvice);
		adviceChain.add(new RequestHandlerRetryAdvice());
		adviceChain.add((MethodInterceptor) invocation -> {
			throw new RuntimeException("intentional: " + counter.incrementAndGet());
		});

		handler.setAdviceChain(adviceChain);
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.afterPropertiesSet();

		handler.handleMessage(new GenericMessage<>("test"));
		Message<?> receive = replies.receive(10000);
		assertThat(receive).isNotNull();
		assertThat(receive.getPayload()).isEqualTo("intentional: 3");
		assertThat(outerCounter.get()).isEqualTo(1);
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

		List<Advice> adviceChain = new ArrayList<>();

		ExpressionEvaluatingRequestHandlerAdvice expressionAdvice = new ExpressionEvaluatingRequestHandlerAdvice();
		expressionAdvice.setBeanFactory(mock(BeanFactory.class));
		expressionAdvice.setOnFailureExpressionString("#exception.message");
		expressionAdvice.setFailureChannel(errors);

		adviceChain.add(new RequestHandlerRetryAdvice());
		adviceChain.add(expressionAdvice);
		adviceChain.add((MethodInterceptor) invocation -> {
			throw new RuntimeException("intentional: " + counter.incrementAndGet());
		});

		handler.setAdviceChain(adviceChain);
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.afterPropertiesSet();

		try {
			handler.handleMessage(new GenericMessage<>("test"));
		}
		catch (Exception e) {
			assertThat(e.getCause().getMessage()).isEqualTo("intentional: 3");
		}

		for (int i = 1; i <= 3; i++) {
			Message<?> receive = errors.receive(10000);
			assertThat(receive).isNotNull();
			assertThat(((MessageHandlingExpressionEvaluatingAdviceException) receive.getPayload())
					.getEvaluationResult()).isEqualTo("intentional: " + i);
		}

		assertThat(errors.receive(1)).isNull();

	}

	/**
	 * Verify that Errors such as OOM are properly propagated.
	 */
	@Test
	public void throwableProperlyPropagated() throws Exception {
		AbstractRequestHandlerAdvice advice = new AbstractRequestHandlerAdvice() {

			@Override
			protected Object doInvoke(ExecutionCallback callback, Object target, Message<?> message) {
				Object result;
				try {
					result = callback.execute();
				}
				catch (Exception e) {
					if (e instanceof ThrowableHolderException) {
						throw (ThrowableHolderException) e;
					}
					else {
						throw new ThrowableHolderException(e);
					}
				}
				return result;
			}
		};
		final Throwable theThrowable = new Throwable("foo");
		MethodInvocation methodInvocation = mock(MethodInvocation.class);

		Method method = AbstractReplyProducingMessageHandler.class.getDeclaredMethod("handleRequestMessage",
				Message.class);
		when(methodInvocation.getMethod()).thenReturn(method);
		when(methodInvocation.getArguments()).thenReturn(new Object[]{ new GenericMessage<>("foo") });
		try {
			doAnswer(invocation -> {
				throw theThrowable;
			}).when(methodInvocation).proceed();
			advice.invoke(methodInvocation);
			fail("Expected throwable");
		}
		catch (Throwable t) {
			assertThat(t).isSameAs(theThrowable);
		}
	}

	/**
	 * Verify that Errors such as OOM are properly propagated and we suppress the
	 * ThrowableHolderException from the output message.
	 */
	@Test
	public void throwableProperlyPropagatedAndReported() {
		QueueChannel errors = new QueueChannel();

		ExpressionEvaluatingRequestHandlerAdvice expressionAdvice = new ExpressionEvaluatingRequestHandlerAdvice();
		expressionAdvice.setBeanFactory(mock(BeanFactory.class));
		expressionAdvice.setOnFailureExpressionString("'foo'");
		expressionAdvice.setFailureChannel(errors);

		Throwable theThrowable = new Throwable("foo");
		ProxyFactory proxyFactory = new ProxyFactory(new Foo(theThrowable));
		proxyFactory.addAdvice(expressionAdvice);

		Bar fooHandler = (Bar) proxyFactory.getProxy();

		try {
			fooHandler.handleRequestMessage(new GenericMessage<>("foo"));
			fail("Expected throwable");
		}
		catch (Throwable t) {
			assertThat(t).isSameAs(theThrowable);
			ErrorMessage error = (ErrorMessage) errors.receive(10000);
			assertThat(error).isNotNull();
			assertThat(error.getPayload().getCause()).isSameAs(theThrowable);
		}
	}

	@Test
	public void testInappropriateAdvice() throws Exception {
		final AtomicBoolean called = new AtomicBoolean(false);
		Advice advice = new AbstractRequestHandlerAdvice() {

			@Override
			protected Object doInvoke(ExecutionCallback callback, Object target, Message<?> message) {
				called.set(true);
				return callback.execute();
			}
		};
		PollableChannel inputChannel = new QueueChannel();
		PollingConsumer consumer = new PollingConsumer(inputChannel, message -> { });
		consumer.setAdviceChain(Collections.singletonList(advice));
		ExecutorService exec = Executors.newSingleThreadExecutor();
		consumer.setTaskExecutor(new ErrorHandlingTaskExecutor(exec, t -> { }));
		consumer.setBeanFactory(mock(BeanFactory.class));
		consumer.afterPropertiesSet();
		consumer.setTaskScheduler(mock(TaskScheduler.class));
		consumer.start();

		Callable<?> pollingTask = TestUtils.getPropertyValue(consumer, "pollingTask", Callable.class);
		assertThat(AopUtils.isAopProxy(pollingTask)).isTrue();
		LogAccessor logger = spy(TestUtils.getPropertyValue(advice, "logger", LogAccessor.class));
		when(logger.isWarnEnabled()).thenReturn(Boolean.TRUE);
		final AtomicReference<String> logMessage = new AtomicReference<>();
		doAnswer(invocation -> {
			logMessage.set(invocation.getArgument(0));
			return null;
		}).when(logger).warn(anyString());
		DirectFieldAccessor accessor = new DirectFieldAccessor(advice);
		accessor.setPropertyValue("logger", logger);

		pollingTask.call();
		assertThat(called.get()).isFalse();
		assertThat(logMessage.get()).isNotNull();
		assertThat(logMessage.get()).contains("can only be used for MessageHandlers; " +
				"an attempt to advise method 'call' in " +
				"'org.springframework.integration.endpoint.AbstractPollingEndpoint");
		consumer.stop();
		exec.shutdownNow();
	}

	public void filterDiscardNoAdvice() {
		MessageFilter filter = new MessageFilter(message -> false);
		QueueChannel discardChannel = new QueueChannel();
		filter.setDiscardChannel(discardChannel);
		filter.handleMessage(new GenericMessage<>("foo"));
		assertThat(discardChannel.receive(0)).isNotNull();
	}

	@Test
	public void filterDiscardWithinAdvice() {
		MessageFilter filter = new MessageFilter(message -> false);
		final QueueChannel discardChannel = new QueueChannel();
		filter.setDiscardChannel(discardChannel);
		List<Advice> adviceChain = new ArrayList<>();
		final AtomicReference<Message<?>> discardedWithinAdvice = new AtomicReference<Message<?>>();
		adviceChain.add(new AbstractRequestHandlerAdvice() {

			@Override
			protected Object doInvoke(ExecutionCallback callback, Object target, Message<?> message) {
				Object result = callback.execute();
				discardedWithinAdvice.set(discardChannel.receive(0));
				return result;
			}
		});
		filter.setAdviceChain(adviceChain);
		filter.setBeanFactory(mock(BeanFactory.class));
		filter.afterPropertiesSet();
		filter.handleMessage(new GenericMessage<>("foo"));
		assertThat(discardedWithinAdvice.get()).isNotNull();
		assertThat(discardChannel.receive(0)).isNull();
	}

	@Test
	public void filterDiscardOutsideAdvice() {
		MessageFilter filter = new MessageFilter(message -> false);
		final QueueChannel discardChannel = new QueueChannel();
		filter.setDiscardChannel(discardChannel);
		List<Advice> adviceChain = new ArrayList<>();
		final AtomicReference<Message<?>> discardedWithinAdvice = new AtomicReference<Message<?>>();
		final AtomicBoolean adviceCalled = new AtomicBoolean();
		adviceChain.add(new AbstractRequestHandlerAdvice() {

			@Override
			protected Object doInvoke(ExecutionCallback callback, Object target, Message<?> message) {
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
		filter.handleMessage(new GenericMessage<>("foo"));
		assertThat(adviceCalled.get()).isTrue();
		assertThat(discardedWithinAdvice.get()).isNull();
		assertThat(discardChannel.receive(0)).isNotNull();
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

		Map<Class<? extends Throwable>, Boolean> retryableExceptions = new HashMap<>();
		retryableExceptions.put(MyException.class, retryForMyException);

		retryTemplate.setRetryPolicy(new SimpleRetryPolicy(3, retryableExceptions, true));

		advice.setRetryTemplate(retryTemplate);

		List<Advice> adviceChain = new ArrayList<>();
		adviceChain.add(advice);
		handler.setAdviceChain(adviceChain);
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.afterPropertiesSet();

		Message<String> message = new GenericMessage<>("Hello, world!");
		try {
			handler.handleMessage(message);
			fail("MessagingException expected.");
		}
		catch (Exception e) {
			assertThat(e).isInstanceOf(MessagingException.class);
			assertThat(e.getCause()).isInstanceOf(MyException.class);
		}

		assertThat(counter.get()).isEqualTo(expected);
	}

	@Test
	public void enhancedRecoverer() {
		QueueChannel channel = new QueueChannel();
		ErrorMessageSendingRecoverer recoverer = new ErrorMessageSendingRecoverer(channel);
		recoverer.publish(new GenericMessage<>("foo"), new GenericMessage<>("bar"), new RuntimeException("baz"));
		Message<?> error = channel.receive(0);
		assertThat(error).isInstanceOf(ErrorMessage.class);
		assertThat(error.getPayload()).isInstanceOf(MessagingException.class);
		MessagingException payload = (MessagingException) error.getPayload();
		assertThat(payload.getCause()).isInstanceOf(RuntimeException.class);
		assertThat(payload.getCause().getMessage()).isEqualTo("baz");
		assertThat(payload.getFailedMessage().getPayload()).isEqualTo("bar");
		assertThat(((ErrorMessage) error).getOriginalMessage().getPayload()).isEqualTo("foo");
	}

	private interface Bar {

		Object handleRequestMessage(Message<?> message) throws Throwable;

	}

	private class Foo implements Bar {

		public final Throwable throwable;

		Foo(Throwable throwable) {
			this.throwable = throwable;
		}

		@Override
		public Object handleRequestMessage(Message<?> message) throws Throwable {
			throw this.throwable;
		}

	}

}
