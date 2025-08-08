/*
 * Copyright © 2016 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2016-present the original author or authors.
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
