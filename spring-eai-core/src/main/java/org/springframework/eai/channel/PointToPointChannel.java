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

package org.springframework.eai.channel;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.springframework.eai.message.Message;

/**
 * Simple implementation of a point-to-point message channel.
 * Messages are stored in a queue whose capacity may be
 * provided upon construction. If no capacity is specified,
 * the {@link #DEFAULT_CAPACITY} will be used.
 * 
 * @author Mark Fisher
 */
public class PointToPointChannel implements MessageChannel {

	private static final int DEFAULT_CAPACITY = 25;


	private BlockingQueue<Message> queue;


	public PointToPointChannel(int capacity) {
		queue = new LinkedBlockingQueue<Message>(capacity);
	}

	public PointToPointChannel() {
		this(DEFAULT_CAPACITY);
	}


	/**
	 * Send a message on this channel. If the queue is
	 * full, this method will block until either space
	 * becomes available or the sending thread is
	 * interrupted.
	 */
	public void send(Message message) {
		try {
			queue.put(message);
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	/**
	 * Receive the message at the head of the queue.
	 * If the queue is empty, this method will block.
	 * 
	 * @return the Message at the head of the queue
	 * or <code>null</code> if the receiving thread
	 * is interrupted.
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

}
