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

package org.springframework.integration.channel;

import java.util.Comparator;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.integration.StaticMessageHeaderAccessor;
import org.springframework.integration.store.MessageGroupQueue;
import org.springframework.integration.store.PriorityCapableChannelMessageStore;
import org.springframework.integration.util.UpperBound;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

/**
 * A message channel that prioritizes messages based on a {@link Comparator}.
 * The default comparator is based upon the message header's 'priority'.
 *
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 */
public class PriorityChannel extends QueueChannel {

	/**
	 * PriorityBlockingQueue#DEFAULT_INITIAL_CAPACITY is private.
	 */
	private static final int DEFAULT_INITIAL_CAPACITY = 11;

	private final UpperBound upperBound;

	private final AtomicLong sequenceCounter = new AtomicLong();

	private final boolean useMessageStore;

	/**
	 * Create a channel with an unbounded queue. Message priority will be
	 * based on the value of {@link StaticMessageHeaderAccessor#getPriority(Message)}.
	 */
	public PriorityChannel() {
		this(0, null);
	}

	/**
	 * Create a channel with the specified queue capacity. Message priority
	 * will be based upon the value of {@link StaticMessageHeaderAccessor#getPriority(Message)}.
	 * @param capacity The queue capacity.
	 */
	public PriorityChannel(int capacity) {
		this(capacity, null);
	}

	/**
	 * Create a channel with an unbounded queue. Message priority will be
	 * determined by the provided {@link Comparator}. If the comparator
	 * is <code>null</code>, the priority will be based upon the value of
	 * {@link StaticMessageHeaderAccessor#getPriority(Message)}.
	 * @param comparator The comparator.
	 */
	public PriorityChannel(Comparator<Message<?>> comparator) {
		this(0, comparator);
	}

	/**
	 * Create a channel with the specified queue capacity. If the capacity
	 * is a non-positive value, the queue will be unbounded. Message priority
	 * will be determined by the provided {@link Comparator}. If the comparator
	 * is <code>null</code>, the priority will be based upon the value of
	 * {@link StaticMessageHeaderAccessor#getPriority(Message)}.
	 * @param capacity The capacity.
	 * @param comparator The comparator.
	 */
	public PriorityChannel(int capacity, @Nullable Comparator<Message<?>> comparator) {
		super(new PriorityBlockingQueue<>(DEFAULT_INITIAL_CAPACITY, new SequenceFallbackComparator(comparator)));
		this.upperBound = new UpperBound(capacity);
		this.useMessageStore = false;
	}

	/**
	 * Create a channel based on the provided {@link PriorityCapableChannelMessageStore}
	 * and group id for message store operations.
	 * @param messageGroupStore the {@link PriorityCapableChannelMessageStore} to use.
	 * @param groupId to group message for this channel in the message store.
	 * @since 5.0
	 */
	public PriorityChannel(PriorityCapableChannelMessageStore messageGroupStore, Object groupId) {
		this(new MessageGroupQueue(messageGroupStore, groupId));
	}

	/**
	 * Create a channel based on the provided {@link MessageGroupQueue}.
	 * @param messageGroupQueue the {@link MessageGroupQueue} to use.
	 * @since 5.0
	 */
	public PriorityChannel(MessageGroupQueue messageGroupQueue) {
		super(messageGroupQueue);
		this.upperBound = new UpperBound(0);
		this.useMessageStore = true;
	}

	@Override
	public int getRemainingCapacity() {
		return this.upperBound.availablePermits();
	}

	@Override
	protected boolean doSend(Message<?> message, long timeout) {
		if (!this.upperBound.tryAcquire(timeout)) {
			return false;
		}
		if (!this.useMessageStore) {
			return super.doSend(new MessageWrapper(message), 0);
		}
		else {
			return super.doSend(message, 0);
		}
	}

	@Override
	protected Message<?> doReceive(long timeout) {
		Message<?> message = super.doReceive(timeout);
		if (message != null) {
			if (!this.useMessageStore) {
				message = ((MessageWrapper) message).getRootMessage();
			}
			this.upperBound.release();
		}
		return message;
	}

	private static final class SequenceFallbackComparator implements Comparator<Message<?>> {

		private final Comparator<Message<?>> targetComparator;

		SequenceFallbackComparator(@Nullable Comparator<Message<?>> targetComparator) {
			this.targetComparator = targetComparator;
		}

		@Override
		public int compare(Message<?> message1, Message<?> message2) {
			int compareResult;
			if (this.targetComparator != null) {
				compareResult = this.targetComparator.compare(message1, message2);
			}
			else {
				Integer priority1 = StaticMessageHeaderAccessor.getPriority(message1);
				Integer priority2 = StaticMessageHeaderAccessor.getPriority(message2);

				priority1 = priority1 != null ? priority1 : 0;
				priority2 = priority2 != null ? priority2 : 0;
				compareResult = priority2.compareTo(priority1);
			}

			if (compareResult == 0) {
				Long sequence1 = ((MessageWrapper) message1).getSequence();
				Long sequence2 = ((MessageWrapper) message2).getSequence();
				compareResult = sequence1.compareTo(sequence2);
			}
			return compareResult;
		}

	}

	//we need this because of INT-2508
	private final class MessageWrapper implements Message<Object> {

		private final Message<?> rootMessage;

		private final long sequence;

		MessageWrapper(Message<?> rootMessage) {
			this.rootMessage = rootMessage;
			this.sequence = PriorityChannel.this.sequenceCounter.incrementAndGet();
		}

		public Message<?> getRootMessage() {
			return this.rootMessage;
		}

		@Override
		public MessageHeaders getHeaders() {
			return this.rootMessage.getHeaders();
		}

		@Override
		public Object getPayload() {
			return this.rootMessage.getPayload();
		}

		long getSequence() {
			return this.sequence;
		}

	}

}
