/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.integration.aggregator;

import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.integration.core.MessageSource;
import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.integration.store.MessageGroup;
import org.springframework.integration.store.MessageGroupStore;
import org.springframework.integration.store.SimpleMessageStore;
import org.springframework.messaging.Message;

/**
 * This Endpoint serves as a barrier for messages that should not be processed yet. The decision when a message can be
 * processed is delegated to a {@link org.springframework.integration.aggregator.ReleaseStrategy ReleaseStrategy}.
 * When a message can be processed it is up to the client to take care of the locking (potentially from the ReleaseStrategy's
 * {@link org.springframework.integration.aggregator.ReleaseStrategy#canRelease(org.springframework.integration.store.MessageGroup) canRelease(..)}
 * method).
 * <p>
 * This class differs from AbstractCorrelatingMessageHandler in that it completely decouples the receiver and the sender. It can
 * be applied in scenarios where completion of a message group is not well defined but only a certain amount of messages
 * for any given correlation key may be processed at a time.
 * <p>
 * The messages will be stored in a {@link org.springframework.integration.store.MessageGroupStore MessageStore}
 * for each correlation key.
 *
 * @author Iwein Fuld
 * @author Oleg Zhurakousky
 *
 * @see AbstractCorrelatingMessageHandler
 */
public class CorrelatingMessageBarrier extends AbstractMessageHandler implements MessageSource<Object> {

	private static final Log log = LogFactory.getLog(CorrelatingMessageBarrier.class);

	private volatile CorrelationStrategy correlationStrategy;

	private volatile ReleaseStrategy releaseStrategy;

	private final ConcurrentMap<Object, Object> correlationLocks = new ConcurrentHashMap<Object, Object>();

	private final MessageGroupStore store;


	public CorrelatingMessageBarrier(MessageGroupStore store) {
		this.store = store;
	}

	public CorrelatingMessageBarrier() {
		this(new SimpleMessageStore(0));
	}


	/**
	 * Set the CorrelationStrategy to be used to determine the correlation key for incoming messages
	 *
	 * @param correlationStrategy The correlation strategy.
	 */
	public void setCorrelationStrategy(CorrelationStrategy correlationStrategy) {
		this.correlationStrategy = correlationStrategy;
	}

	/**
	 * Set the ReleaseStrategy that should be used when deciding if a group in this barrier may be released.
	 *
	 * @param releaseStrategy The release strategy.
	 */
	public void setReleaseStrategy(ReleaseStrategy releaseStrategy) {
		this.releaseStrategy = releaseStrategy;
	}

	@Override
	protected void handleMessageInternal(Message<?> message) throws Exception {
		Object correlationKey = correlationStrategy.getCorrelationKey(message);
		Object lock = getLock(correlationKey);
		synchronized (lock) {
			store.addMessageToGroup(correlationKey, message);
		}
		if (log.isDebugEnabled()) {
			log.debug(String.format("Handled message for key [%s]: %s.", correlationKey, message));
		}
	}

	private Object getLock(Object correlationKey) {
		Object existingLock = correlationLocks.putIfAbsent(correlationKey, correlationKey);
		return existingLock == null ? correlationKey : existingLock;
	}


	@Override
	public Message<Object> receive() {
		for (Object key : correlationLocks.keySet()) {
			Object lock = getLock(key);
			synchronized (lock) {
				MessageGroup group = store.getMessageGroup(key);
				//group might be removed by another thread
				if (group != null) {
					if (releaseStrategy.canRelease(group)) {
						Message<?> nextMessage = null;

						Iterator<Message<?>> messages = group.getMessages().iterator();
						if (messages.hasNext()) {
							nextMessage = messages.next();
							store.removeMessageFromGroup(key, nextMessage);
							if (log.isDebugEnabled()) {
								log.debug(String.format("Released message for key [%s]: %s.", key, nextMessage));
							}
						} else {
							remove(key);
						}
						@SuppressWarnings("unchecked")
						Message<Object> result = (Message<Object>) nextMessage;
						return result;
					}
				}
			}
		}
		return null;
	}

	private void remove(Object key) {
		correlationLocks.remove(key);
		store.removeMessageGroup(key);
	}

}
