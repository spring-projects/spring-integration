/*
 * Copyright 2002-2007 the original author or authors.
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

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.integration.dispatcher.DispatcherPolicy;
import org.springframework.integration.message.Message;

/**
 * Simple implementation of a message channel. Each {@link Message} is
 * placed in a queue whose capacity may be specified upon construction. If no
 * capacity is specified, the {@link #DEFAULT_CAPACITY} will be used.
 * 
 * @author Mark Fisher
 */
public class SimpleChannel implements MessageChannel, BeanNameAware {

	private static final int DEFAULT_CAPACITY = 25;

	private String name;

	private final DispatcherPolicy dispatcherPolicy;

	private BlockingQueue<Message<?>> queue;


	/**
	 * Create a channel with the specified queue capacity and dispatcher policy.
	 */
	public SimpleChannel(int capacity, DispatcherPolicy dispatcherPolicy) {
		this.queue = new LinkedBlockingQueue<Message<?>>(capacity);
		this.dispatcherPolicy = (dispatcherPolicy != null) ? dispatcherPolicy : new DispatcherPolicy();
	}

	/**
	 * Create a channel with the specified queue capacity.
	 */
	public SimpleChannel(int capacity) {
		this(capacity, null);
	}

	/**
	 * Create a channel with the default queue capacity and the specified dispatcher policy.
	 */
	public SimpleChannel(DispatcherPolicy dispatcherPolicy) {
		this(DEFAULT_CAPACITY, dispatcherPolicy);
	}

	/**
	 * Create a channel with the default queue capacity.
	 */
	public SimpleChannel() {
		this(DEFAULT_CAPACITY, null);
	}


	/**
	 * Set the name of this channel.
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Return the name of this channel.
	 */
	public String getName() {
		return this.name;
	}

	public DispatcherPolicy getDispatcherPolicy() {
		return this.dispatcherPolicy;
	}

	/**
	 * Set the name of this channel to its bean name. This will be invoked
	 * automatically whenever the channel is configured explicitly with a bean
	 * definition.
	 */
	public void setBeanName(String beanName) {
		this.setName(beanName);
	}

	/**
	 * Send a message on this channel. If the queue is full, this method will
	 * block until either space becomes available or the sending thread is
	 * interrupted.
	 * 
	 * @param message the Message to send
	 * 
	 * @return <code>true</code> if the message is sent successfully or
	 * <code>false</code> if the sending thread is interrupted.
	 */
	public boolean send(Message message) {
		try {
			queue.put(message);
			return true;
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return false;
		}
	}

	/**
	 * Send a message on this channel. If the queue is full, this method will
	 * block until either the timeout occurs or the sending thread is
	 * interrupted. If the specified timeout is less than 1, the method will
	 * return immediately.
	 * 
	 * @param message the Message to send
	 * @param timeout the timeout in milliseconds
	 * 
	 * @return <code>true</code> if the message is sent successfully,
	 * <code>false</code> if the message cannot be sent within the allotted
	 * time or the sending thread is interrupted.
	 */
	public boolean send(Message message, long timeout) {
		try {
			if (timeout > 0) {
				return this.queue.offer(message, timeout, TimeUnit.MILLISECONDS);
			}
			return this.queue.offer(message);
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return false;
		}
	}

	/**
	 * Receive the message at the head of the queue. If the queue is empty, this
	 * method will block.
	 * 
	 * @return the Message at the head of the queue or <code>null</code> if
	 * the receiving thread is interrupted.
	 */
	public Message receive() {
		try {
			return queue.take();
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return null;
		}
	}

	/**
	 * Receive the message at the head of the queue. If the queue is empty, this
	 * method will block until the allotted timeout elapses. If the specified
	 * timeout is less than 1, the method will return immediately.
	 * 
	 * @param timeout the timeout in milliseconds
	 * 
	 * @return the message at the head of the queue or <code>null</code> in
	 * case no message is available within the allotted time or the receiving
	 * thread is interrupted.
	 */
	public Message receive(long timeout) {
		try {
			if (timeout > 0) {
				return queue.poll(timeout, TimeUnit.MILLISECONDS);
			}
			return queue.poll();
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return null;
		}
	}

}
