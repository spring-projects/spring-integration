/*
 * Copyright 2002-2016 the original author or authors.
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.support.AbstractIntegrationMessageBuilder;
import org.springframework.integration.util.FunctionIterator;
import org.springframework.messaging.Message;

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

		Map<String, Object> messageHeaders = message.getHeaders();
		if (willAddHeaders(message)) {
			messageHeaders = new HashMap<>(messageHeaders);
			addHeaders(message, messageHeaders);
		}
		final Map<String, Object> headers = messageHeaders;
		final Object correlationId = message.getHeaders().getId();
		final AtomicInteger sequenceNumber = new AtomicInteger(1);

		return new FunctionIterator<Object, AbstractIntegrationMessageBuilder<?>>(iterator,
				object ->
						createBuilder(object, headers, correlationId, sequenceNumber.getAndIncrement(), sequenceSize));
	}

	private AbstractIntegrationMessageBuilder<?> createBuilder(Object item, Map<String, Object> headers,
			Object correlationId, int sequenceNumber, int sequenceSize) {
		AbstractIntegrationMessageBuilder<?> builder;
		if (item instanceof Message) {
			builder = getMessageBuilderFactory().fromMessage((Message<?>) item);
		}
		else if (item instanceof AbstractIntegrationMessageBuilder) {
			builder = (AbstractIntegrationMessageBuilder<?>) item;
		}
		else {
			builder = getMessageBuilderFactory().withPayload(item);
		}
		builder.copyHeadersIfAbsent(headers);
		if (this.applySequence) {
			builder.pushSequenceDetails(correlationId, sequenceNumber, sequenceSize);
		}
		return builder;
	}

	/**
	 * Return true if the subclass needs to add headers in the resulting splits.
	 * If true, {@link #addHeaders} will be called.
	 * @param message the message.
	 * @return true
	 */
	protected boolean willAddHeaders(Message<?> message) {
		return false;
	}

	/**
	 * Allows subclasses to add extra headers to the output messages. Headers may not be
	 * removed by this method.
	 *
	 * @param message the inbound message.
	 * @param headers the headers to add messages to.
	 */
	protected void addHeaders(Message<?> message, Map<String, Object> headers) {
	}

	@Override
	protected boolean shouldCopyRequestHeaders() {
		return false;
	}

	@Override
	protected void produceOutput(Object result, Message<?> requestMessage) {
		Iterator<?> iterator = (Iterator<?>) result;
		while (iterator.hasNext()) {
			super.produceOutput(iterator.next(), requestMessage);

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
