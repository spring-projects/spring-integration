/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.integration.store;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.integration.core.Message;
import org.springframework.util.Assert;

/**
 * Map-based implementation of {@link MessageStore} that enforces a maximum capacity.
 *
 * @author Iwein Fuld
 * @author Mark Fisher
 * @since 2.0
 */
public class SimpleMessageStore implements MessageStore {

	private final Map<Object, Message<?>> map;


	public SimpleMessageStore(int capacity) {
		this.map = new ConcurrentHashMap<Object, Message<?>>(capacity);
	}


	@SuppressWarnings("unchecked")
	public <T> Message<T> put(Message<T> message) {
		return (Message<T>) this.map.put(message.getHeaders().getId(), message);
	}

	public Message<?> get(Object key) {
		return (key != null) ? this.map.get(key) : null;
	}

	public List<Message<?>> list() {
		return new ArrayList<Message<?>>(this.map.values());
	}

	public Message<?> delete(Object key) {
		return (key != null) ? this.map.remove(key) : null;
	}

	public int size() {
		return this.map.size();
	}


	public List<Message<?>> list(Object correlationKey) {
        Assert.notNull(correlationKey, "'correlationKey' must not be null");
		List<Message<?>> matched = new ArrayList<Message<?>>();
		Collection<Message<?>> values = map.values();
		for (Message<?> message : values) {
			if(message.getHeaders().getCorrelationId().equals(correlationKey)){
				matched.add(message);
			}
		}
		return matched;
	}

}
