/*
 * Copyright 2002-2018 the original author or authors.
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

import java.io.Closeable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Stream;

import org.reactivestreams.Publisher;

import org.springframework.integration.channel.ReactiveStreamsSubscribableChannel;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.support.AbstractIntegrationMessageBuilder;
import org.springframework.integration.util.FunctionIterator;
import org.springframework.messaging.Message;

import reactor.core.publisher.Flux;

/**
 * Base class for Message-splitting handlers.
 *
 * @author Mark Fisher
 * @author Dave Syer
 * @author Artem Bilan
 * @author Ruslan Stelmachenko
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

		boolean reactive = getOutputChannel() instanceof ReactiveStreamsSubscribableChannel;
		setAsync(reactive);

		Iterator<Object> iterator = null;
		Flux<Object> flux = null;

		final int sequenceSize;

		if (result instanceof Iterable<?>) {
			Iterable<Object> iterable = (Iterable<Object>) result;
			sequenceSize = obtainSizeIfPossible(iterable);
			if (reactive) {
				flux = Flux.fromIterable(iterable);
			}
			else {
				iterator = iterable.iterator();
			}
		}
		else if (result.getClass().isArray()) {
			Object[] items = (Object[]) result;
			sequenceSize = items.length;
			if (reactive) {
				flux = Flux.fromArray(items);
			}
			else {
				iterator = Arrays.asList(items).iterator();
			}
		}
		else if (result instanceof Iterator<?>) {
			Iterator<Object> iter = (Iterator<Object>) result;
			sequenceSize = obtainSizeIfPossible(iter);
			if (reactive) {
				flux = Flux.fromIterable(() -> iter);
			}
			else {
				iterator = iter;
			}
		}
		else if (result instanceof Stream<?>) {
			Stream<Object> stream = ((Stream<Object>) result);
			sequenceSize = 0;
			if (reactive) {
				flux = Flux.fromStream(stream);
			}
			else {
				iterator = stream.iterator();
			}
		}
		else if (result instanceof Publisher<?>) {
			Publisher<Object> publisher = (Publisher<Object>) result;
			sequenceSize = 0;
			if (reactive) {
				flux = Flux.from(publisher);
			}
			else {
				iterator = Flux.from((Publisher<Object>) result).toIterable().iterator();
			}
		}
		else {
			sequenceSize = 1;
			if (reactive) {
				flux = Flux.just(result);
			}
			else {
				iterator = Collections.singleton(result).iterator();
			}
		}

		if (iterator != null && !iterator.hasNext()) {
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

		Function<Object, AbstractIntegrationMessageBuilder<?>> messageBuilderFunction =
				object -> createBuilder(object, headers, correlationId, sequenceNumber.getAndIncrement(), sequenceSize);

		if (reactive) {
			return flux.map(messageBuilderFunction);
		}
		else {
			return new FunctionIterator<>(iterator, messageBuilderFunction);
		}
	}

	/**
	 * Obtain a size of the provided {@link Iterable}. Default implementation returns
	 * {@link Collection#size()} if the iterable is a collection, or {@code 0} otherwise.
	 * @param iterable the {@link Iterable} to obtain the size
	 * @return the size of the {@link Iterable}
	 * @since 5.0
	 */
	protected int obtainSizeIfPossible(Iterable<?> iterable) {
		return iterable instanceof Collection ? ((Collection<?>) iterable).size() : 0;
	}

	/**
	 * Obtain a size of the provided {@link Iterator}.
	 * Default implementation returns {@code 0}.
	 * @param iterator the {@link Iterator} to obtain the size
	 * @return the size of the {@link Iterator}
	 * @since 5.0
	 */
	protected int obtainSizeIfPossible(Iterator<?> iterator) {
		return 0;
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
		if (result instanceof Iterator<?>) {
			Iterator<?> iterator = (Iterator<?>) result;
			try {
				while (iterator.hasNext()) {
					super.produceOutput(iterator.next(), requestMessage);
				}
			}
			finally {
				if (iterator instanceof Closeable) {
					try {
						((Closeable) iterator).close();
					}
					catch (Exception e) {
						// ignored
					}
				}
			}
		}
		else {
			super.produceOutput(result, requestMessage);
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
