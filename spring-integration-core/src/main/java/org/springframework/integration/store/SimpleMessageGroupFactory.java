/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.springframework.integration.store;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.concurrent.LinkedBlockingQueue;

import org.springframework.messaging.Message;

/**
 * @author Artem Bilan
 * @since 4.3
 */
public class SimpleMessageGroupFactory {

	private final GroupType type;

	public SimpleMessageGroupFactory() {
		this(GroupType.HASH_SET);
	}

	public SimpleMessageGroupFactory(GroupType type) {
		this.type = type;
	}

	public SimpleMessageGroup create(Object groupId) {
		return create(Collections.<Message<?>> emptyList(), groupId);
	}

	public SimpleMessageGroup create(Collection<? extends Message<?>> messages, Object groupId) {
		return create(messages, groupId, System.currentTimeMillis(), false);
	}

	public SimpleMessageGroup create(Collection<? extends Message<?>> messages, Object groupId, long timestamp,
	                                 boolean complete) {
		return new SimpleMessageGroup(this.type.get(), messages, groupId, timestamp, complete);
	}

	public enum GroupType {

		BLOCKING_QUEUE {

			@Override
			Collection<Message<?>> get() {
				return new LinkedBlockingQueue<Message<?>>();
			}

		},
		HASH_SET {

			@Override
			Collection<Message<?>> get() {
				return new LinkedHashSet<Message<?>>();
			}

		},
		SYNCHRONISED_SET {

			@Override
			Collection<Message<?>> get() {
				return Collections.<Message<?>>synchronizedSet(new LinkedHashSet<Message<?>>());
			}

		};

		abstract Collection<Message<?>> get();

	}

}
