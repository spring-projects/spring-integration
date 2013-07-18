/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.integration.history;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Properties;

import org.springframework.integration.Message;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.support.context.NamedComponent;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * @author Mark Fisher
 * @since 2.0
 */
@SuppressWarnings("serial")
public class MessageHistory implements List<Properties>, Serializable {

	public static final String HEADER_NAME = "history";

	public static final String NAME_PROPERTY = "name";

	public static final String TYPE_PROPERTY = "type";

	public static final String TIMESTAMP_PROPERTY = "timestamp";


	private final List<Properties> components;


	public static MessageHistory read(Message<?> message) {
		return (message != null) ?
				message.getHeaders().get(HEADER_NAME, MessageHistory.class) : null;
	}

	public static <T> Message<T> write(Message<T> message, NamedComponent component) {
		Assert.notNull(message, "Message must not be null");
		Assert.notNull(component, "Component must not be null");
		Properties metadata = extractMetadata(component);
		if (!metadata.isEmpty()) {
			MessageHistory previousHistory = message.getHeaders().get(HEADER_NAME, MessageHistory.class);
			List<Properties> components = (previousHistory != null) ?
					new ArrayList<Properties>(previousHistory) : new ArrayList<Properties>();
			components.add(metadata);
			MessageHistory history = new MessageHistory(components);
			message = MessageBuilder.fromMessage(message).setHeader(HEADER_NAME, history).build();
		}
		return message;
	}


	private MessageHistory(List<Properties> components) {
		Assert.notEmpty(components, "component list must not be empty");
		this.components = components;
	}


	public int size() {
		return this.components.size();
	}

	public boolean isEmpty() {
		return this.components.isEmpty();
	}

	public boolean contains(Object o) {
		return this.components.contains(o);
	}
	
	public boolean containsAll(Collection<?> c) {
		return this.components.containsAll(c);
	}

	public Properties get(int index) {
		return this.components.get(index);
	}

	public Iterator<Properties> iterator() {
		return Collections.unmodifiableList(this.components).iterator();
	}

	public ListIterator<Properties> listIterator() {
		return Collections.unmodifiableList(this.components).listIterator();
	}

	public ListIterator<Properties> listIterator(int index) {
		return Collections.unmodifiableList(this.components).listIterator(index);
	}

	public List<Properties> subList(int fromIndex, int toIndex) {
		return Collections.unmodifiableList(this.components).subList(fromIndex, toIndex);
	}

	public Object[] toArray() {
		return this.components.toArray();
	}

	public <T> T[] toArray(T[] a) {
		return this.components.toArray(a);
	}

	public int indexOf(Object o) {
		return this.components.indexOf(o);
	}

	public int lastIndexOf(Object o) {
		return this.components.lastIndexOf(o);
	}

	public String toString() {
		List<String> names = new ArrayList<String>();
		for (Properties p : this.components) {
			String name = p.getProperty(NAME_PROPERTY);
			if (name != null) {
				names.add(name);
			}
		}
		return StringUtils.collectionToCommaDelimitedString(names);
	}


	/*
	 * Unsupported Operations
	 */

	public boolean add(Properties e) {
		throw new UnsupportedOperationException("MessageHistory is immutable.");
	}

	public void add(int index, Properties element) {
		throw new UnsupportedOperationException("MessageHistory is immutable.");
	}

	public boolean addAll(Collection<? extends Properties> c) {
		throw new UnsupportedOperationException("MessageHistory is immutable.");
	}

	public boolean addAll(int index, Collection<? extends Properties> c) {
		throw new UnsupportedOperationException("MessageHistory is immutable.");
	}

	public Properties set(int index, Properties element) {
		throw new UnsupportedOperationException("MessageHistory is immutable.");
	}

	public Properties remove(int index) {
		throw new UnsupportedOperationException("MessageHistory is immutable.");
	}

	public boolean remove(Object o) {
		throw new UnsupportedOperationException("MessageHistory is immutable.");
	}

	public boolean removeAll(Collection<?> c) {
		throw new UnsupportedOperationException("MessageHistory is immutable.");
	}

	public boolean retainAll(Collection<?> c) {
		throw new UnsupportedOperationException("MessageHistory is immutable.");
	}

	public void clear() {
		throw new UnsupportedOperationException("MessageHistory is immutable.");
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
