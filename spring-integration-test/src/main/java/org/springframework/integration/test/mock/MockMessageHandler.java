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

import static org.junit.Assert.assertThat;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

import org.hamcrest.Matcher;

import org.springframework.integration.handler.AbstractMessageProducingHandler;
import org.springframework.messaging.Message;

/**
 * @author Artem Bilan
 *
 * @since 5.0
 */
@SuppressWarnings("rawtypes")
public class MockMessageHandler extends AbstractMessageProducingHandler {

	protected final Map<Matcher<Message>, Function<Message<?>, ?>> matchers = new LinkedHashMap<>();

	protected Matcher<Message> lastKey;

	protected Function<Message<?>, ?> lastReplyFunction;

	protected boolean hasReplies;

	protected MockMessageHandler() {
	}

	public MockMessageHandlerWithReply assertNext(Matcher<Message> nextMessageMatcher) {
		this.matchers.put(nextMessageMatcher, null);
		this.lastKey = nextMessageMatcher;
		this.lastReplyFunction = null;
		return (MockMessageHandlerWithReply) this;
	}

	@Override
	protected void handleMessageInternal(Message<?> message) throws Exception {
		Matcher<Message> matcher = this.lastKey;
		Function<Message<?>, ?> replyFunction = this.lastReplyFunction;

		synchronized (this) {
			Iterator<Map.Entry<Matcher<Message>, Function<Message<?>, ?>>> entryIterator =
					this.matchers.entrySet().iterator();
			if (entryIterator.hasNext()) {
				Map.Entry<Matcher<Message>, Function<Message<?>, ?>> matcherFunctionEntry = entryIterator.next();
				matcher = matcherFunctionEntry.getKey();
				replyFunction = matcherFunctionEntry.getValue();
				entryIterator.remove();
			}
		}

		assertThat(message, matcher);

		if (replyFunction != null) {
			sendOutputs(replyFunction.apply(message), message);
		}
	}

	static class MockMessageHandlerWithReply extends MockMessageHandler {

		MockMessageHandlerWithReply(Matcher<Message> nextMessageMatcher) {
			assertNext(nextMessageMatcher);
		}

		public MockMessageHandler thenReply() {
			return thenReply(Function.identity());
		}

		public MockMessageHandler thenReply(Object reply) {
			return thenReply(m -> reply);
		}

		public MockMessageHandler thenReply(Function<Message<?>, ?> replyFunction) {
			this.matchers.put(this.lastKey, replyFunction);
			this.lastReplyFunction = replyFunction;
			this.hasReplies = this.lastReplyFunction != null;
			return this;
		}

	}

}
