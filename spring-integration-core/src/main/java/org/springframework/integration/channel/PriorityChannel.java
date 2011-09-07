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

package org.springframework.integration.channel;

import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.PriorityBlockingQueue;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.integration.Message;
import org.springframework.integration.MessageHeaders;
import org.springframework.integration.util.UpperBound;

/**
 * A message channel that prioritizes messages based on a {@link Comparator}.
 * The default comparator is based upon the message header's 'priority'.
 * 
 * @author Mark Fisher
 */
public class PriorityChannel extends QueueChannel {

	private final UpperBound upperBound;
	
	private volatile long lastMessageSendTime;


	/**
	 * Create a channel with the specified queue capacity. If the capacity
	 * is a non-positive value, the queue will be unbounded. Message priority
	 * will be determined by the provided {@link Comparator}. If the comparator
	 * is <code>null</code>, the priority will be based upon the value of
	 * {@link MessageHeaders#getPriority()}.
	 */
	public PriorityChannel(int capacity, Comparator<Message<?>> comparator) {
		super(new PriorityBlockingQueue<Message<?>>(11,
				(comparator != null) ? comparator : new MessagePriorityComparator()));
		this.upperBound = new UpperBound(capacity);
	}

	/**
	 * Create a channel with the specified queue capacity. Message priority
	 * will be based upon the value of {@link MessageHeaders#getPriority()}.
	 */
	public PriorityChannel(int capacity) {
		this(capacity, null);
	}

	/**
	 * Create a channel with an unbounded queue. Message priority will be
	 * determined by the provided {@link Comparator}. If the comparator
	 * is <code>null</code>, the priority will be based upon the value of
	 * {@link MessageHeaders#getPriority()}.
	 */
	public PriorityChannel(Comparator<Message<?>> comparator) {
		this(0, comparator);
	}

	/**
	 * Create a channel with an unbounded queue. Message priority will be
	 * based on the value of {@link MessageHeaders#getPriority()}.
	 */
	public PriorityChannel() {
		this(0, null);
	}


	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	protected boolean doSend(Message<?> message, long timeout) {
		if (!upperBound.tryAcquire(timeout)) {
			return false;
		}
		if (message.getHeaders().getPriority() == null){
			
			Map innerMap = (Map) new DirectFieldAccessor(message.getHeaders()).getPropertyValue("headers");
			innerMap.put(MessageHeaders.PRIORITY, this.calculateDefaultPriorityHeader());
		}
		return super.doSend(message, 0);
	}

	@Override
	protected Message<?> doReceive(long timeout) {
		Message<?> message = super.doReceive(timeout);
		if (message != null) {
			upperBound.release();
			return message;
		}
		return null;
	}

	private long calculateDefaultPriorityHeader(){
		long currentTime = System.currentTimeMillis();
		synchronized (this) {
			if (currentTime > this.lastMessageSendTime){
				this.lastMessageSendTime = currentTime;
			}
			else if (currentTime == this.lastMessageSendTime){
				this.lastMessageSendTime = currentTime+1;
			}
			else {
				this.lastMessageSendTime += 1;
			}
			long priorityHeadrValue = this.lastMessageSendTime;
			return priorityHeadrValue * (-1);
		}
	}
	
	private static class MessagePriorityComparator implements Comparator<Message<?>> {

		public int compare(Message<?> message1, Message<?> message2) {
			Long priority1 = message1.getHeaders().getPriority();
			Long priority2 = message2.getHeaders().getPriority();
			
			priority1 = priority1 != null ? priority1 : 0;
			priority2 = priority2 != null ? priority2 : 0;
			return priority2.compareTo(priority1);
		}
	}

}
