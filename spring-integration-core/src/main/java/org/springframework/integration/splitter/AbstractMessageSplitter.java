/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.integration.splitter;

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

import org.springframework.integration.IntegrationPatternType;
import org.springframework.integration.channel.ReactiveStreamsSubscribableChannel;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.handler.DiscardingMessageHandler;
import org.springframework.integration.support.AbstractIntegrationMessageBuilder;
import org.springframework.integration.support.json.JacksonPresent;
import org.springframework.integration.util.FunctionIterator;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

import com.fasterxml.jackson.core.TreeNode;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Base class for Message-splitting handlers.
 *
 * @author Mark Fisher
 * @author Dave Syer
 * @author Artem Bilan
 * @author Ruslan Stelmachenko
 * @author Gary Russell
 */
public abstract class AbstractMessageSplitter extends AbstractReplyProducingMessageHandler
		implements DiscardingMessageHandler {

	private boolean applySequence = true;

	private MessageChannel discardChannel;

	private String discardChannelName;

	/**
	 * Set the applySequence flag to the specified value. Defaults to true.
	 * @param applySequence true to apply sequence information.
	 */
	public void setApplySequence(boolean applySequence) {
		this.applySequence = applySequence;
	}

	/**
	 * Specify a channel where rejected Messages should be sent. If the discard
	 * channel is null (the default), rejected Messages will be dropped.
	 * A "Rejected Message" means that split function has returned an empty result (but not null):
	 * no items to iterate for sending.
	 * @param discardChannel The discard channel.
	 * @since 5.2
	 */
	public void setDiscardChannel(MessageChannel discardChannel) {
		this.discardChannel = discardChannel;
	}

	/**
	 * Specify a channel bean name (resolved to {@link MessageChannel} lazily)
	 * where rejected Messages should be sent. If the discard
	 * channel is null (the default), rejected Messages will be dropped.
	 * A "Rejected Message" means that split function has returned an empty result (but not null):
	 * no items to iterate for sending.
	 * @param discardChannelName The discard channel bean name.
	 * @since 5.2
	 */
	public void setDiscardChannelName(String discardChannelName) {
		Assert.hasText(discardChannelName, "'discardChannelName' must not be empty");
		this.discardChannelName = discardChannelName;
	}

	@Override
	public MessageChannel getDiscardChannel() {
		if (this.discardChannel == null) {
			String channelName = this.discardChannelName;
			if (channelName != null) {
				this.discardChannel = getChannelResolver().resolveDestination(channelName);
				this.discardChannelName = null;
			}
		}
		return this.discardChannel;
	}

	@Override
	public IntegrationPatternType getIntegrationPatternType() {
		return IntegrationPatternType.splitter;
	}

	@Override
	protected void doInit() {
		Assert.state(!(this.discardChannelName != null && this.discardChannel != null),
				"'discardChannelName' and 'discardChannel' are mutually exclusive.");
	}

	@Override
	protected final Object handleRequestMessage(Message<?> message) {
		Object result = splitMessage(message);
		// return null if 'null'
		if (result == null) {
			return null;
		}

		boolean reactive = getOutputChannel() instanceof ReactiveStreamsSubscribableChannel;
		setAsync(reactive);


		if (reactive) {
			return prepareFluxResult(message, result);
		}
		else {
			return prepareIteratorResult(message, result);
		}
	}

	@SuppressWarnings("unchecked")
	private Flux<?> prepareFluxResult(Message<?> message, Object result) {
		int sequenceSize = 1;
		Flux<?> flux = Flux.just(result);
		if (result instanceof Iterable<?>) {
			Iterable<Object> iterable = (Iterable<Object>) result;
			sequenceSize = obtainSizeIfPossible(iterable);
			flux = Flux.fromIterable(iterable);
		}
		else if (result.getClass().isArray()) {
			Object[] items = ObjectUtils.toObjectArray(result);
			sequenceSize = items.length;
			flux = Flux.fromArray(items);
		}
		else if (result instanceof Iterator<?>) {
			Iterator<Object> iter = (Iterator<Object>) result;
			sequenceSize = obtainSizeIfPossible(iter);
			flux = Flux.fromIterable(() -> iter);
		}
		else if (result instanceof Stream<?>) {
			Stream<Object> stream = ((Stream<Object>) result);
			sequenceSize = 0;
			flux = Flux.fromStream(stream);
		}
		else if (result instanceof Publisher<?>) {
			Publisher<Object> publisher = (Publisher<Object>) result;
			sequenceSize = 0;
			flux = Flux.from(publisher);
		}

		Function<Object, ?> messageBuilderFunction = prepareMessageBuilderFunction(message, sequenceSize);

		return flux
				.map(messageBuilderFunction)
				.switchIfEmpty(
						Mono.defer(() -> {
							MessageChannel discardingChannel = getDiscardChannel();
							if (discardingChannel != null) {
								this.messagingTemplate.send(discardingChannel, message);
							}
							return Mono.empty();
						}));
	}

	@SuppressWarnings("unchecked")
	private Iterator<?> prepareIteratorResult(Message<?> message, Object result) {
		int sequenceSize = 1;
		Iterator<?> iterator = Collections.singleton(result).iterator();

		if (result instanceof Iterable<?>) {
			Iterable<Object> iterable = (Iterable<Object>) result;
			sequenceSize = obtainSizeIfPossible(iterable);
			iterator = iterable.iterator();
		}
		else if (result.getClass().isArray()) {
			Object[] items = ObjectUtils.toObjectArray(result);
			sequenceSize = items.length;
			iterator = Arrays.asList(items).iterator();
		}
		else if (result instanceof Iterator<?>) {
			Iterator<Object> iter = (Iterator<Object>) result;
			sequenceSize = obtainSizeIfPossible(iter);
			iterator = iter;
		}
		else if (result instanceof Stream<?>) {
			Stream<Object> stream = ((Stream<Object>) result);
			sequenceSize = 0;
			iterator = stream.iterator();
		}
		else if (result instanceof Publisher<?>) {
			sequenceSize = 0;
			iterator = Flux.from((Publisher<?>) result).toIterable().iterator();
		}

		if (!iterator.hasNext()) {
			MessageChannel discardingChannel = getDiscardChannel();
			if (discardingChannel != null) {
				this.messagingTemplate.send(discardingChannel, message);
			}
			return null;
		}

		Function<Object, ?> messageBuilderFunction = prepareMessageBuilderFunction(message, sequenceSize);

		return new FunctionIterator<>(
				result instanceof AutoCloseable && !result.equals(iterator) ? (AutoCloseable) result : null,
				iterator, messageBuilderFunction);
	}

	private Function<Object, ?> prepareMessageBuilderFunction(Message<?> message, int sequenceSize) {
		Map<String, Object> messageHeaders = message.getHeaders();
		if (willAddHeaders(message)) {
			messageHeaders = new HashMap<>(messageHeaders);
			addHeaders(message, messageHeaders);
		}

		Map<String, Object> headers = messageHeaders;
		Object correlationId = message.getHeaders().getId();
		AtomicInteger sequenceNumber = new AtomicInteger(1);

		return object -> createBuilder(object, headers, correlationId, sequenceNumber.getAndIncrement(), sequenceSize);
	}

	/**
	 * Obtain a size of the provided {@link Iterable}. Default implementation returns
	 * {@link Collection#size()} if the iterable is a collection, or {@code 0} otherwise.
	 * If iterable is a Jackson {@code TreeNode}, then its size is used.
	 * @param iterable the {@link Iterable} to obtain the size
	 * @return the size of the {@link Iterable}
	 * @since 5.0
	 */
	protected int obtainSizeIfPossible(Iterable<?> iterable) {
		if (iterable instanceof Collection) {
			return ((Collection<?>) iterable).size();
		}
		else if (JacksonPresent.isJackson2Present() && JacksonNodeHelper.isNode(iterable)) {
			return JacksonNodeHelper.nodeSize(iterable);
		}
		else {
			return 0;
		}
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

		AbstractIntegrationMessageBuilder<?> builder = messageBuilderForReply(item);
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
				if (iterator instanceof AutoCloseable) {
					try {
						((AutoCloseable) iterator).close();
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


	private static class JacksonNodeHelper {

		private static boolean isNode(Object object) {
			return object instanceof TreeNode;
		}

		private static int nodeSize(Object node) {
			return ((TreeNode) node).size();
		}

	}

}
