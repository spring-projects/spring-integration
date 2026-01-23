/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.integration.dispatcher;

import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author Iwein Fuld
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 * @author Glenn Renfro
 */
public class RoundRobinDispatcherTests {

	private final UnicastingDispatcher dispatcher = new UnicastingDispatcher();

	private final MessageHandler handler = mock();

	private final Message<?> message = mock();

	private final MessageHandler differentHandler = mock();

	@BeforeEach
	public void setupDispatcher() {
		this.dispatcher.setLoadBalancingStrategy(new RoundRobinLoadBalancingStrategy());
	}

	@Test
	public void dispatchMessageWithSingleHandler() {
		this.dispatcher.addHandler(this.handler);
		this.dispatcher.dispatch(this.message);
		verify(this.handler).handleMessage(this.message);
	}

	@Test
	public void differentHandlerInvokedOnSecondMessage() {
		this.dispatcher.addHandler(this.handler);
		this.dispatcher.addHandler(this.differentHandler);
		this.dispatcher.dispatch(this.message);
		this.dispatcher.dispatch(this.message);
		verify(this.handler).handleMessage(this.message);
		verify(this.differentHandler).handleMessage(this.message);
	}

	@Test
	public void multipleCyclesThroughHandlers() {
		this.dispatcher.addHandler(this.handler);
		this.dispatcher.addHandler(this.differentHandler);
		for (int i = 0; i < 7; i++) {
			this.dispatcher.dispatch(this.message);
		}
		verify(this.handler, times(4)).handleMessage(this.message);
		verify(this.differentHandler, times(3)).handleMessage(this.message);
	}

	@Test
	public void currentHandlerIndexOverFlow() {
		this.dispatcher.addHandler(this.handler);
		this.dispatcher.addHandler(this.differentHandler);
		TestUtils.<AtomicInteger>getPropertyValue(this.dispatcher, "loadBalancingStrategy.currentHandlerIndex")
				.set(Integer.MAX_VALUE - 5);
		for (int i = 0; i < 40; i++) {
			this.dispatcher.dispatch(this.message);
		}
		verify(this.handler, atLeast(18)).handleMessage(this.message);
		verify(this.differentHandler, atLeast(18)).handleMessage(this.message);
	}

	/**
	 * Verifies that the dispatcher adds the message to the exception if it
	 * was not attached by the handler.
	 */
	@Test
	public void testExceptionEnhancement() {
		this.dispatcher.addHandler(this.handler);
		doThrow(new MessagingException("Mock Exception"))
				.when(this.handler)
				.handleMessage(this.message);

		assertThatExceptionOfType(MessagingException.class)
				.isThrownBy(() -> this.dispatcher.dispatch(this.message))
				.satisfies(ex -> assertThat(ex.getFailedMessage()).isEqualTo(this.message));
	}

	/**
	 * Verifies that the dispatcher does not add the message to the exception if it
	 * was attached by the handler.
	 */
	@Test
	public void testNoExceptionEnhancement() {
		this.dispatcher.addHandler(this.handler);
		Message<String> dontReplaceThisMessage = MessageBuilder.withPayload("x").build();
		doThrow(new MessagingException(dontReplaceThisMessage, "Mock Exception"))
				.when(this.handler)
				.handleMessage(this.message);

		assertThatExceptionOfType(MessagingException.class)
				.isThrownBy(() -> this.dispatcher.dispatch(this.message))
				.satisfies(ex -> assertThat(ex.getFailedMessage()).isEqualTo(dontReplaceThisMessage));
	}

	@Test
	public void testFailOverAndLogging() {
		RuntimeException testException = new RuntimeException("intentional");
		doThrow(testException)
				.when(this.handler)
				.handleMessage(this.message);
		this.dispatcher.addHandler(this.handler);
		this.dispatcher.addHandler(this.differentHandler);

		DirectFieldAccessor directFieldAccessor = new DirectFieldAccessor(this.dispatcher);
		Log log = (Log) spy(directFieldAccessor.getPropertyType("logger"));
		given(log.isDebugEnabled()).willReturn(true);
		directFieldAccessor.setPropertyValue("logger", log);

		this.dispatcher.dispatch(this.message);

		verify(this.handler).handleMessage(this.message);
		verify(this.differentHandler).handleMessage(this.message);

		verify(log).debug(startsWith("An exception was thrown by '"), eq(testException));
	}

}
