/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.integration.message;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

/**
 * A {@link MessageStore} implementation whose <code>get</code> and
 * <code>remove</code> methods block until a message is available.
 * <p>
 * Alternative methods that accept an explicit timeout value are also available.
 * 
 * @author Mark Fisher
 */
public class RetrievalBlockingMessageStore extends SimpleMessageStore implements MessageStore {

	private final ConcurrentMap<Object, List<SynchronousQueue<MessageHolder>>> listeners =
			new ConcurrentHashMap<Object, List<SynchronousQueue<MessageHolder>>>();

	private final Object listenerMonitor = new Object();


	public RetrievalBlockingMessageStore(int capacity) {
		super(capacity);
	}


	public Message<?> put(Object key, Message<?> message) {
		Message<?> previousMessage = super.put(key, message);
		boolean sentReply = false;
		List<SynchronousQueue<MessageHolder>> listenerList = null;
		synchronized (this.listenerMonitor) {
			listenerList = this.listeners.remove(key);
		}
		if (listenerList != null) {
			for (int i = 0; i < listenerList.size(); i++) {
				Queue<MessageHolder> queue = listenerList.get(i);
				if (!sentReply) {
					sentReply = queue.offer(new MessageHolder(message));
				}
				else {
					queue.offer(new MessageHolder(null));
				}
			}
		}
		return previousMessage;
	}

	public Message<?> get(Object key) {
		return this.get(key, -1);
	}

	public Message<?> get(Object key, long timeout) {
		Message<?> message = super.get(key);
		return (message != null) ? message : waitForMessage(key, timeout, false);
	}

	public Message<?> remove(Object key) {
		return this.remove(key, -1);
	}

	public Message<?> remove(Object key, long timeout) {
		Message<?> message = super.remove(key);
		return (message != null) ? message : waitForMessage(key, timeout, true);
	}

	private Message<?> waitForMessage(Object key, long timeout, boolean shouldRemove) {
		Message<?> message = null;
		SynchronousQueue<MessageHolder> queue = new SynchronousQueue<MessageHolder>();
		synchronized (this.listenerMonitor) {
			List<SynchronousQueue<MessageHolder>> listenerList = this.listeners.get(key);
			if (listenerList == null) {
				listenerList = new LinkedList<SynchronousQueue<MessageHolder>>();
				this.listeners.put(key, listenerList);
			}
			listenerList.add(queue);
		}
		try {
			MessageHolder holder = (timeout < 0) ? queue.take() : queue.poll(timeout, TimeUnit.MILLISECONDS);
			if (holder != null) {
				message = holder.getMessage();
				if (message != null && shouldRemove) {
					return super.remove(key);
				}
			}
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		finally {
			synchronized (this.listenerMonitor) {
				List<SynchronousQueue<MessageHolder>> listenerList = this.listeners.get(key);
				if (listenerList != null) {
					listenerList.remove(queue);
					if (listenerList.size() == 0) {
						this.listeners.remove(key);
					}
				}
			}
		}
		return message;
	}


	/**
	 * A wrapper class to enable <code>null</code> messages in the queue.
	 */
	private static class MessageHolder {

		private final Message<?> message;

		MessageHolder(Message<?> message) {
			this.message = message;
		}

		Message<?> getMessage() {
			return this.message;
		}
	}

}
