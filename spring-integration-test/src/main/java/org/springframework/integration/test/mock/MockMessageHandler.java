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

import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.messaging.Message;

/**
 * @author Artem Bilan
 *
 * @since 5.0
 */
public class MockMessageHandler extends AbstractReplyProducingMessageHandler {

	protected final Map<Matcher<Message<?>>, Function<Message<?>, ?>> matchers = new LinkedHashMap<>();

	protected Matcher<Message<?>> lastKey;

	protected Function<Message<?>, ?> lastReplyFunction;

	protected MockMessageHandler() {
	}

	public MockMessageHandlerWithReply expect(Matcher<Message<?>> messageMatcher) {
		this.matchers.put(messageMatcher, null);
		this.lastKey = messageMatcher;
		return (MockMessageHandlerWithReply) this;
	}

	@Override
	protected Object handleRequestMessage(Message<?> requestMessage) {
		Matcher<Message<?>> matcher = this.lastKey;
		Function<Message<?>, ?> replyFunction = this.lastReplyFunction;

		synchronized (this) {
			Iterator<Map.Entry<Matcher<Message<?>>, Function<Message<?>, ?>>> entryIterator = matchers.entrySet().iterator();
			if (entryIterator.hasNext()) {
				Map.Entry<Matcher<Message<?>>, Function<Message<?>, ?>> matcherFunctionEntry = entryIterator.next();
				matcher = matcherFunctionEntry.getKey();
				replyFunction = matcherFunctionEntry.getValue();
				entryIterator.remove();
			}
		}
		assertThat(requestMessage, matcher);

		if (replyFunction != null) {
			return replyFunction.apply(requestMessage);
		}

		return null;
	}

	static class MockMessageHandlerWithReply extends MockMessageHandler {

		MockMessageHandlerWithReply(Matcher<Message<?>> messageMatcher) {
			expect(messageMatcher);
		}

		@Override
		public MockMessageHandlerWithReply expect(Matcher<Message<?>> messageMatcher) {
			return super.expect(messageMatcher);
		}

		public MockMessageHandler andReply() {
			return andReply(Function.identity());
		}

		public MockMessageHandler andReply(Object reply) {
			return andReply(m -> reply);
		}

		public MockMessageHandler andReply(Function<Message<?>, ?> replyFunction) {
			this.matchers.put(this.lastKey, replyFunction);
			this.lastReplyFunction = replyFunction;
			return this;
		}

	}

}
