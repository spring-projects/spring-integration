/*
 * Copyright 2016-2024 the original author or authors.
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

package org.springframework.integration.dsl;

import java.util.Queue;
import java.util.concurrent.locks.Lock;

import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.store.ChannelMessageStore;
import org.springframework.integration.store.MessageGroupQueue;
import org.springframework.messaging.Message;

/**
 * @author Artem Bilan
 *
 * @since 5.0
 */
public class QueueChannelSpec extends MessageChannelSpec<QueueChannelSpec, QueueChannel> {

	protected Queue<Message<?>> queue; // NOSONAR

	protected Integer capacity; // NOSONAR

	protected QueueChannelSpec() {
	}

	protected QueueChannelSpec(Queue<Message<?>> queue) {
		this.queue = queue;
	}

	protected QueueChannelSpec(Integer capacity) {
		this.capacity = capacity;
	}

	@Override
	protected QueueChannel doGet() {
		if (this.queue != null) {
			this.channel = new QueueChannel(this.queue);
		}
		else if (this.capacity != null) {
			this.channel = new QueueChannel(this.capacity);
		}
		else {
			this.channel = new QueueChannel();
		}
		return super.doGet();
	}

	/**
	 * The {@link ChannelMessageStore}-specific {@link QueueChannelSpec} extension.
	 */
	public static class MessageStoreSpec extends QueueChannelSpec {

		private final ChannelMessageStore messageGroupStore;

		private final Object groupId;

		private Lock storeLock;

		protected MessageStoreSpec(ChannelMessageStore messageGroupStore, Object groupId) {
			this.messageGroupStore = messageGroupStore;
			this.groupId = groupId;
		}

		@Override
		protected MessageStoreSpec id(String id) {
			return (MessageStoreSpec) super.id(id);
		}

		public MessageStoreSpec capacity(Integer capacityToSet) {
			this.capacity = capacityToSet;
			return this;
		}

		public MessageStoreSpec storeLock(Lock storeLockToSet) {
			this.storeLock = storeLockToSet;
			return this;
		}

		@Override
		protected QueueChannel doGet() {
			if (this.capacity != null) {
				if (this.storeLock != null) {
					this.queue = new MessageGroupQueue(this.messageGroupStore, this.groupId, this.capacity,
							this.storeLock);
				}
				else {
					this.queue = new MessageGroupQueue(this.messageGroupStore, this.groupId, this.capacity);
				}
			}
			else if (this.storeLock != null) {
				this.queue = new MessageGroupQueue(this.messageGroupStore, this.groupId, this.storeLock);
			}
			else {
				this.queue = new MessageGroupQueue(this.messageGroupStore, this.groupId);
			}

			return super.doGet();
		}

	}

}
