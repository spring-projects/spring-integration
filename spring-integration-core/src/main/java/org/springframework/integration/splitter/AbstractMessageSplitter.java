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

package org.springframework.integration.splitter;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;

import reactor.function.Function;

import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.support.AbstractIntegrationMessageBuilder;
import org.springframework.integration.util.FunctionIterator;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

/**
 * Base class for Message-splitting handlers.
 *
 * @author Mark Fisher
 * @author Dave Syer
 * @author Artem Bilan
 */
public abstract class AbstractMessageSplitter extends AbstractReplyProducingMessageHandler {

	private boolean applySequence = true;

	/**
	 * Set the applySequence flag to the specified value. Defaults to true.
	 * @param applySequence true to apply sequence information.
	 */
	public void setApplySequence(boolean applySequence) {
		this.applySequence = applySequence;
	}

	@Override
	@SuppressWarnings("unchecked")
	protected final Object handleRequestMessage(Message<?> message) {
		Object result = this.splitMessage(message);
		// return null if 'null'
		if (result == null) {
			return null;
		}

		Iterator<Object> iterator;
		final int sequenceSize;
		if (result instanceof Collection) {
			Collection<Object> items = (Collection<Object>) result;
			sequenceSize = items.size();
			iterator = items.iterator();
		}
		else if (result.getClass().isArray()) {
			Object[] items = (Object[]) result;
			sequenceSize = items.length;
			iterator = Arrays.asList(items).iterator();
		}
		else if (result instanceof Iterable<?>) {
			sequenceSize = 0;
			iterator = ((Iterable<Object>) result).iterator();
		}
		else if (result instanceof Iterator<?>) {
			sequenceSize = 0;
			iterator = (Iterator<Object>) result;
		}
		else {
			sequenceSize = 1;
			iterator = Collections.singleton(result).iterator();
		}

		if (!iterator.hasNext()) {
			return null;
		}

		final MessageHeaders headers = message.getHeaders();
		final Object correlationId = headers.getId();
		final AtomicInteger sequenceNumber = new AtomicInteger(1);

		return new FunctionIterator<Object, AbstractIntegrationMessageBuilder<?>>(iterator,
				new Function<Object, AbstractIntegrationMessageBuilder<?>>() {
					@Override
					public AbstractIntegrationMessageBuilder<?> apply(Object object) {
						return createBuilder(object, headers, correlationId, sequenceNumber.getAndIncrement(),
								sequenceSize);
					}
				});
	}

	@SuppressWarnings( { "unchecked", "rawtypes" })
	private AbstractIntegrationMessageBuilder createBuilder(Object item, MessageHeaders headers, Object correlationId,
			int sequenceNumber, int sequenceSize) {
		AbstractIntegrationMessageBuilder builder;
		if (item instanceof Message) {
			builder = this.getMessageBuilderFactory().fromMessage((Message) item);
		}
		else {
			builder = this.getMessageBuilderFactory().withPayload(item);
			builder.copyHeaders(headers);
		}
		if (this.applySequence) {
			builder.pushSequenceDetails(correlationId, sequenceNumber, sequenceSize);
		}
		return builder;
	}

	@Override
	protected void produceReply(Object result, MessageHeaders requestHeaders) {
		Iterator<?> iterator = (Iterator<?>) result;
		while (iterator.hasNext()) {
			super.produceReply(iterator.next(), requestHeaders);

		}
	}

	@Override
	public String getComponentType() {
		return "splitter";
	}

	/**
	 * Subclasses must override this method to split the received Message. The return value may be a Collection or
	 * Array. The individual elements may be Messages, but it is not necessary. If the elements are not Messages, each
	 * will be provided as the payload of a Message. It is also acceptable to return a single Object or Message. In that
	 * case, a single reply Message will be produced.
	 * @param message The message.
	 * @return The result of splitting the message.
	 */
	protected abstract Object splitMessage(Message<?> message);

}
