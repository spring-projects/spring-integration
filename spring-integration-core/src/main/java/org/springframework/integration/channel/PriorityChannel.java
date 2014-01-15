/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.integration.channel;

import java.util.Comparator;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.util.UpperBound;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

/**
 * A message channel that prioritizes messages based on a {@link Comparator}.
 * The default comparator is based upon the message header's 'priority'.
 *
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 */
public class PriorityChannel extends QueueChannel {

	private final UpperBound upperBound;

	private final AtomicLong sequenceCounter = new AtomicLong();

	/**
	 * Create a channel with the specified queue capacity. If the capacity
	 * is a non-positive value, the queue will be unbounded. Message priority
	 * will be determined by the provided {@link Comparator}. If the comparator
	 * is <code>null</code>, the priority will be based upon the value of
	 * {@link IntegrationMessageHeaderAccessor#getPriority()}.
	 *
	 * @param capacity The capacity.
	 * @param comparator The comparator.
	 */
	public PriorityChannel(int capacity, Comparator<Message<?>> comparator) {
		super(new PriorityBlockingQueue<Message<?>>(11, new SequenceFallbackComparator(comparator)));
		this.upperBound = new UpperBound(capacity);
	}

	/**
	 * Create a channel with the specified queue capacity. Message priority
	 * will be based upon the value of {@link IntegrationMessageHeaderAccessor#getPriority()}.
	 *
	 * @param capacity The queue capacity.
	 */
	public PriorityChannel(int capacity) {
		this(capacity, null);
	}

	/**
	 * Create a channel with an unbounded queue. Message priority will be
	 * determined by the provided {@link Comparator}. If the comparator
	 * is <code>null</code>, the priority will be based upon the value of
	 * {@link IntegrationMessageHeaderAccessor#getPriority()}.
	 *
	 * @param comparator The comparator.
	 */
	public PriorityChannel(Comparator<Message<?>> comparator) {
		this(0, comparator);
	}

	/**
	 * Create a channel with an unbounded queue. Message priority will be
	 * based on the value of {@link IntegrationMessageHeaderAccessor#getPriority()}.
	 */
	public PriorityChannel() {
		this(0, null);
	}

	@Override
	protected boolean doSend(Message<?> message, long timeout) {
		if (!upperBound.tryAcquire(timeout)) {
			return false;
		}
		message = new MessageWrapper(message);
		return super.doSend(message, 0);
	}

	@Override
	protected Message<?> doReceive(long timeout) {
		Message<?> message = super.doReceive(timeout);
		if (message != null) {
			message = ((MessageWrapper)message).getRootMessage();
			upperBound.release();
		}
		return message;
	}

	private static class SequenceFallbackComparator implements Comparator<Message<?>> {

		private final Comparator<Message<?>> targetComparator;

		public SequenceFallbackComparator(Comparator<Message<?>> targetComparator){
			this.targetComparator = targetComparator;
		}

		@Override
		public int compare(Message<?> message1, Message<?> message2) {
			int compareResult = 0;
			if (this.targetComparator != null){
				compareResult = this.targetComparator.compare(message1, message2);
			}
			else {
				Integer priority1 = new IntegrationMessageHeaderAccessor(message1).getPriority();
				Integer priority2 = new IntegrationMessageHeaderAccessor(message2).getPriority();

				priority1 = priority1 != null ? priority1 : 0;
				priority2 = priority2 != null ? priority2 : 0;
				compareResult = priority2.compareTo(priority1);
			}

			if (compareResult == 0){
				Long sequence1 = ((MessageWrapper) message1).getSequence();
				Long sequence2 = ((MessageWrapper) message2).getSequence();
				compareResult = sequence1.compareTo(sequence2);
			}
			return compareResult;
		}
	}

	//we need this because of INT-2508
	private class MessageWrapper implements Message<Object>{
		private final Message<?> rootMessage;
		private final long sequence;

		public MessageWrapper(Message<?> rootMessage){
			this.rootMessage = rootMessage;
			this.sequence = sequenceCounter.incrementAndGet();
		}

		public Message<?> getRootMessage(){
			return this.rootMessage;
		}

		@Override
		public MessageHeaders getHeaders() {
			return this.rootMessage.getHeaders();
		}

		@Override
		public Object getPayload() {
			return rootMessage.getPayload();
		}

		long getSequence(){
			return this.sequence;
		}
	}
}
