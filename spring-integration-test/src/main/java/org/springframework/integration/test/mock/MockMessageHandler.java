/*
 * Copyright 2017 the original author or authors.
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

package org.springframework.integration.test.mock;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

import org.hamcrest.Matcher;

import org.springframework.integration.handler.AbstractMessageProducingHandler;
import org.springframework.integration.test.matcher.PayloadAndHeaderMatcher;
import org.springframework.integration.test.matcher.PayloadMatcher;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * The {@link AbstractMessageProducingHandler} extension for the assertion purpose in tests.
 * <p>
 * The provided {@link Matcher}s are applied to the incoming messages one at a time
 * until the last {@link Matcher}, which is applied for all subsequent messages -
 * the similar behavior exists in the
 * {@code Mockito.doReturn(Object toBeReturned, Object... toBeReturnedNext)}.
 * <p>
 * Typically is used as a chain of assertions and optional replies for them:
 * <pre class="code">
 * {@code
 *      MockIntegration.mockMessageHandler(hasHeader("bar", "BAR"))
 *               .thenReply()
 *               .assertNext(new GenericMessage<>("foo", Collections.singletonMap("key", "value")))
 *               .assertNext(hasPayload("foo"))
 *               .thenReply(m -> m.getPayload("X"));
 * }
 * </pre>
 *
 * @author Artem Bilan
 *
 * @since 5.0
 */
public class MockMessageHandler extends AbstractMessageProducingHandler {

	protected final Map<Matcher<Message<?>>, Function<Message<?>, ?>> matchers = new LinkedHashMap<>();

	protected Matcher<Message<?>> lastKey;

	protected Function<Message<?>, ?> lastReplyFunction;

	protected boolean hasReplies;

	protected MockMessageHandler() {
	}

	public MockMessageHandlerWithReply assertNext(Object payload) {
		return assertNext(PayloadMatcher.hasPayload(payload));
	}

	public MockMessageHandlerWithReply assertNext(Message<?> message) {
		return assertNext(PayloadAndHeaderMatcher.sameExceptIgnorableHeaders(message));
	}

	public MockMessageHandlerWithReply assertNext(Message<?> message, String... headersToIgnore) {
		return assertNext(PayloadAndHeaderMatcher.sameExceptIgnorableHeaders(message, headersToIgnore));
	}

	public MockMessageHandlerWithReply assertNext(Matcher<Message<?>> nextMessageMatcher) {
		Assert.notNull(nextMessageMatcher, "'nextMessageMatcher' must not be null");
		this.matchers.put(nextMessageMatcher, null);
		this.lastKey = nextMessageMatcher;
		this.lastReplyFunction = null;
		return (MockMessageHandlerWithReply) this;
	}

	@Override
	protected void handleMessageInternal(Message<?> message) throws Exception {
		Matcher<Message<?>> matcher = this.lastKey;
		Function<Message<?>, ?> replyFunction = this.lastReplyFunction;

		synchronized (this) {
			Iterator<Map.Entry<Matcher<Message<?>>, Function<Message<?>, ?>>> entryIterator =
					this.matchers.entrySet().iterator();
			if (entryIterator.hasNext()) {
				Map.Entry<Matcher<Message<?>>, Function<Message<?>, ?>> matcherFunctionEntry = entryIterator.next();
				matcher = matcherFunctionEntry.getKey();
				replyFunction = matcherFunctionEntry.getValue();
				entryIterator.remove();
			}
		}

		org.junit.Assert.assertThat(message, matcher);

		if (replyFunction != null) {
			sendOutputs(replyFunction.apply(message), message);
		}
	}

	static class MockMessageHandlerWithReply extends MockMessageHandler {

		MockMessageHandlerWithReply(Matcher<Message<?>> nextMessageMatcher) {
			assertNext(nextMessageMatcher);
		}

		/**
		 * Add the {@link Function#identity()} reply to the current assertion.
		 * Produces the {@code requestMessage} as a reply.
		 * @return this
		 */
		public MockMessageHandler thenReply() {
			return thenReply(Function.identity());
		}

		/**
		 * Add the {@link Function} for static payload reply to the current assertion.
		 * @param reply the object which becomes as a payload for the reply.
		 * @return this
		 */
		public MockMessageHandler thenReply(Object reply) {
			return thenReply(m -> reply);
		}

		/**
		 * Add the {@link Function} for the reply based on the {@code requestMessage}
		 * to the current assertion.
		 * @param replyFunction the function to build reply based on the requestMessage
		 * @return this
		 */
		public MockMessageHandler thenReply(Function<Message<?>, ?> replyFunction) {
			this.matchers.put(this.lastKey, replyFunction);
			this.lastReplyFunction = replyFunction;
			this.hasReplies = this.lastReplyFunction != null;
			return this;
		}

	}

}
