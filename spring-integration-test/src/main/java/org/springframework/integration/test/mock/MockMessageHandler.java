/*
 * Copyright 2017-present the original author or authors.
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

package org.springframework.integration.test.mock;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Function;

import org.jspecify.annotations.Nullable;
import org.mockito.ArgumentCaptor;
import org.mockito.internal.matchers.CapturingMatcher;

import org.springframework.integration.handler.AbstractMessageProducingHandler;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * The {@link AbstractMessageProducingHandler} extension for the mocking purpose in tests.
 * <p>
 * The provided {@link Consumer}s and {@link Function}s are applied to the incoming
 * messages one at a time until the last, which is applied for all subsequent messages.
 * The similar behavior exists in the
 * {@code Mockito.doReturn(Object toBeReturned, Object... toBeReturnedNext)}.
 * <p>
 * Typically is used as a chain of stub actions:
 * <pre class="code">
 * {@code
 *      MockIntegration.mockMessageHandler()
 *               .handleNext(...)
 *               .handleNext(...)
 *               .handleNextAndReply(...)
 *               .handleNextAndReply(...)
 *               .handleNext(...)
 *               .handleNextAndReply(...);
 * }
 * </pre>
 *
 * @author Artem Bilan
 * @author Christian Tzolov
 * @author Glenn Renfro
 *
 * @since 5.0
 */
public class MockMessageHandler extends AbstractMessageProducingHandler {

	private final Lock lock = new ReentrantLock();

	protected final List<Function<Message<?>, ? extends @Nullable Object>> messageFunctions = new LinkedList<>();

	private final @Nullable CapturingMatcher<Message<?>> capturingMatcher;

	protected @Nullable Function<Message<?>, ? extends @Nullable Object> lastFunction;

	protected boolean hasReplies;

	protected MockMessageHandler(@Nullable ArgumentCaptor<Message<?>> messageArgumentCaptor) {
		if (messageArgumentCaptor != null) {
			this.capturingMatcher = TestUtils.getPropertyValue(messageArgumentCaptor, "capturingMatcher");
		}
		else {
			this.capturingMatcher = null;
		}
	}

	/**
	 * Add the {@link Consumer} to the stack to handle the next incoming message.
	 * @param nextMessageConsumer the Consumer to handle the next incoming message.
	 * @return this
	 */
	@SuppressWarnings("NullAway") // See github.com/uber/NullAway/issues/1075
	public MockMessageHandler handleNext(Consumer<Message<?>> nextMessageConsumer) {
		this.lastFunction = m -> {
			nextMessageConsumer.accept(m);
			return null;
		};
		this.messageFunctions.add(this.lastFunction);
		return this;
	}

	/**
	 * Add the {@link Function} to the stack to handle the next incoming message
	 * and produce reply for it.
	 * @param nextMessageFunction the Function to handle the next incoming message.
	 * @return this
	 */
	public MockMessageHandler handleNextAndReply(Function<Message<?>, ? extends @Nullable Object> nextMessageFunction) {
		this.lastFunction = nextMessageFunction;
		this.messageFunctions.add(this.lastFunction);
		this.hasReplies = true;
		return this;
	}

	@Override
	protected void handleMessageInternal(Message<?> message) {
		if (this.capturingMatcher != null) {
			this.capturingMatcher.captureFrom(message);
		}

		Function<Message<?>, ?> function = this.lastFunction;

		this.lock.lock();
		try {
			Iterator<Function<Message<?>, ?>> iterator = this.messageFunctions.iterator();
			if (iterator.hasNext()) {
				function = iterator.next();
				iterator.remove();
			}
		}
		finally {
			this.lock.unlock();
		}

		Assert.notNull(function, "At least one of the 'handleNext' or 'handleNextAndReply' has to be attached to this MockMessageHandler.");
		Object result = function.apply(message);

		if (result != null) {
			sendOutputs(result, message);
		}
	}

}
