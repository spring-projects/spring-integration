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

package org.springframework.integration.history;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Properties;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.message.AdviceMessage;
import org.springframework.integration.support.DefaultMessageBuilderFactory;
import org.springframework.integration.support.MessageBuilderFactory;
import org.springframework.integration.support.MutableMessage;
import org.springframework.integration.support.MutableMessageBuilderFactory;
import org.springframework.integration.support.context.NamedComponent;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.util.Assert;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 2.0
 */
@SuppressWarnings("serial")
public final class MessageHistory implements List<Properties>, Serializable {

	private static final Log LOGGER = LogFactory.getLog(MessageHistory.class);

	private static final UnsupportedOperationException UNSUPPORTED_OPERATION_EXCEPTION_IMMUTABLE =
			new UnsupportedOperationException("MessageHistory is immutable.");

	public static final String HEADER_NAME = "history";

	public static final String NAME_PROPERTY = "name";

	public static final String TYPE_PROPERTY = "type";

	public static final String TIMESTAMP_PROPERTY = "timestamp";

	private static final MessageBuilderFactory MESSAGE_BUILDER_FACTORY = new DefaultMessageBuilderFactory();


	private final List<Properties> components;

	@Nullable
	public static MessageHistory read(@Nullable Message<?> message) {
		return message != null
				? message.getHeaders().get(HEADER_NAME, MessageHistory.class)
				: null;
	}

	public static <T> Message<T> write(Message<T> message, NamedComponent component) {
		return write(message, component, MESSAGE_BUILDER_FACTORY);
	}

	@SuppressWarnings("unchecked")
	public static <T> Message<T> write(Message<T> messageArg, NamedComponent component,
			MessageBuilderFactory messageBuilderFactory) {

		Message<T> message = messageArg;
		Assert.notNull(message, "Message must not be null");
		Assert.notNull(component, "Component must not be null");
		Properties metadata = extractMetadata(component);
		if (!metadata.isEmpty()) {
			MessageHistory previousHistory = message.getHeaders().get(HEADER_NAME, MessageHistory.class);
			List<Properties> components =
					previousHistory != null
							? new ArrayList<>(previousHistory)
							: new ArrayList<>();
			components.add(metadata);
			MessageHistory history = new MessageHistory(components);

			if (message instanceof MutableMessage) {
				message.getHeaders().put(HEADER_NAME, history);
			}
			else if (message instanceof ErrorMessage) {
				IntegrationMessageHeaderAccessor headerAccessor = new IntegrationMessageHeaderAccessor(message);
				headerAccessor.setHeader(HEADER_NAME, history);
				Throwable payload = ((ErrorMessage) message).getPayload();
				ErrorMessage errorMessage = new ErrorMessage(payload, headerAccessor.toMessageHeaders());
				message = (Message<T>) errorMessage;
			}
			else if (message instanceof AdviceMessage) {
				IntegrationMessageHeaderAccessor headerAccessor = new IntegrationMessageHeaderAccessor(message);
				headerAccessor.setHeader(HEADER_NAME, history);
				message = new AdviceMessage<T>(message.getPayload(), headerAccessor.toMessageHeaders(),
						((AdviceMessage<?>) message).getInputMessage());
			}
			else {
				if (!(message instanceof GenericMessage) &&
						(messageBuilderFactory instanceof DefaultMessageBuilderFactory ||
								messageBuilderFactory instanceof MutableMessageBuilderFactory)
						&& LOGGER.isWarnEnabled()) {

					LOGGER.warn("MessageHistory rebuilds the message and produces the result of the [" +
							messageBuilderFactory + "], not an instance of the provided type [" +
							message.getClass() + "]. Consider to supply a custom MessageBuilderFactory " +
							"to retain custom messages during MessageHistory tracking.");
				}
				message = messageBuilderFactory.fromMessage(message)
						.setHeader(HEADER_NAME, history)
						.build();
			}
		}
		return message;
	}


	private MessageHistory(List<Properties> components) {
		Assert.notEmpty(components, "component list must not be empty");
		this.components = components;
	}


	@Override
	public int size() {
		return this.components.size();
	}

	@Override
	public boolean isEmpty() {
		return this.components.isEmpty();
	}

	@Override
	public boolean contains(Object o) {
		return this.components.contains(o);
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		return this.components.containsAll(c);
	}

	@Override
	public Properties get(int index) {
		return this.components.get(index);
	}

	@Override
	public Iterator<Properties> iterator() {
		return Collections.unmodifiableList(this.components).iterator();
	}

	@Override
	public ListIterator<Properties> listIterator() {
		return Collections.unmodifiableList(this.components).listIterator();
	}

	@Override
	public ListIterator<Properties> listIterator(int index) {
		return Collections.unmodifiableList(this.components).listIterator(index);
	}

	@Override
	public List<Properties> subList(int fromIndex, int toIndex) {
		return Collections.unmodifiableList(this.components).subList(fromIndex, toIndex);
	}

	@Override
	public Object[] toArray() {
		return this.components.toArray();
	}

	@Override
	public <T> T[] toArray(T[] a) {
		return this.components.toArray(a);
	}

	@Override
	public int indexOf(Object o) {
		return this.components.indexOf(o);
	}

	@Override
	public int lastIndexOf(Object o) {
		return this.components.lastIndexOf(o);
	}

	@Override
	public String toString() {
		return this.components
				.stream()
				.map((props) -> props.getProperty(NAME_PROPERTY))
				.collect(Collectors.joining(","));
	}


	/*
	 * Unsupported Operations
	 */

	@Override
	public boolean add(Properties e) {
		throw UNSUPPORTED_OPERATION_EXCEPTION_IMMUTABLE;
	}

	@Override
	public void add(int index, Properties element) {
		throw UNSUPPORTED_OPERATION_EXCEPTION_IMMUTABLE;
	}

	@Override
	public boolean addAll(Collection<? extends Properties> c) {
		throw UNSUPPORTED_OPERATION_EXCEPTION_IMMUTABLE;
	}

	@Override
	public boolean addAll(int index, Collection<? extends Properties> c) {
		throw UNSUPPORTED_OPERATION_EXCEPTION_IMMUTABLE;
	}

	@Override
	public Properties set(int index, Properties element) {
		throw UNSUPPORTED_OPERATION_EXCEPTION_IMMUTABLE;
	}

	@Override
	public Properties remove(int index) {
		throw UNSUPPORTED_OPERATION_EXCEPTION_IMMUTABLE;
	}

	@Override
	public boolean remove(Object o) {
		throw UNSUPPORTED_OPERATION_EXCEPTION_IMMUTABLE;
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		throw UNSUPPORTED_OPERATION_EXCEPTION_IMMUTABLE;
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		throw UNSUPPORTED_OPERATION_EXCEPTION_IMMUTABLE;
	}

	@Override
	public void clear() {
		throw UNSUPPORTED_OPERATION_EXCEPTION_IMMUTABLE;
	}


	private static Properties extractMetadata(NamedComponent component) {
		Entry entry = new Entry();
		String name = component.getComponentName();
		String type = component.getComponentType();
		if (name != null && !name.startsWith("org.springframework.integration")) {
			entry.setName(name);
			if (type != null) {
				entry.setType(type);
			}
		}
		if (!entry.isEmpty()) {
			entry.setTimestamp(Long.toString(System.currentTimeMillis()));
		}
		return entry;
	}


	/**
	 * Inner class for each Entry in the history.
	 */
	public static class Entry extends Properties {

		public String getName() {
			return this.getProperty(NAME_PROPERTY);
		}

		private void setName(String name) {
			this.setProperty(NAME_PROPERTY, name);
		}

		public String getType() {
			return this.getProperty(TYPE_PROPERTY);
		}

		private void setType(String type) {
			this.setProperty(TYPE_PROPERTY, type);
		}

		public String getTimestamp() {
			return this.getProperty(TIMESTAMP_PROPERTY);
		}

		private void setTimestamp(String timestamp) {
			this.setProperty(TIMESTAMP_PROPERTY, timestamp);
		}

	}

}
