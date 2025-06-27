/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.integration.aggregator;

import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.jspecify.annotations.Nullable;

import org.springframework.core.log.LogMessage;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.integration.store.MessageGroup;
import org.springframework.integration.store.MessageGroupStore;
import org.springframework.integration.store.SimpleMessageStore;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * This Endpoint serves as a barrier for messages that should not be processed yet.
 * The decision when a message can be processed is delegated to a {@link ReleaseStrategy}.
 * When a message can be processed it is up to the client to take care of the locking
 * (potentially from the {@link ReleaseStrategy#canRelease(MessageGroup)} method).
 * <p>
 * This class differs from AbstractCorrelatingMessageHandler in that it completely decouples
 * the receiver and the sender.
 * It can be applied in scenarios where completion of a message group is not well-defined
 * but only a certain amount of messages for any given correlation key may be processed at a time.
 * <p>
 * The messages will be stored in a {@link MessageGroupStore} for each correlation key.
 *
 * @author Iwein Fuld
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 * @author Trung Pham
 * @author Glenn Renfro
 *
 * @see AbstractCorrelatingMessageHandler
 */
public class CorrelatingMessageBarrier extends AbstractMessageHandler implements MessageSource<Object> {

	private final ConcurrentMap<Object, Object> correlationLocks = new ConcurrentHashMap<>();

	private final MessageGroupStore store;

	@SuppressWarnings("NullAway.Init")
	private CorrelationStrategy correlationStrategy;

	@SuppressWarnings("NullAway.Init")
	private ReleaseStrategy releaseStrategy;

	public CorrelatingMessageBarrier() {
		this(new SimpleMessageStore(0));
	}

	public CorrelatingMessageBarrier(MessageGroupStore store) {
		this.store = store;
	}

	/**
	 * Set the CorrelationStrategy to be used to determine the correlation key for incoming messages.
	 * @param correlationStrategy The correlation strategy.
	 */
	public void setCorrelationStrategy(CorrelationStrategy correlationStrategy) {
		this.correlationStrategy = correlationStrategy;
	}

	/**
	 * Set the ReleaseStrategy that should be used when deciding if a group in this barrier may be released.
	 * @param releaseStrategy The release strategy.
	 */
	public void setReleaseStrategy(ReleaseStrategy releaseStrategy) {
		this.releaseStrategy = releaseStrategy;
	}

	@Override
	protected void onInit() {
		super.onInit();
		Assert.notNull(this.releaseStrategy, "'releaseStrategy' must not be null");
		Assert.notNull(this.correlationStrategy, "'correlationStrategy' must not be null");
	}

	@Override
	protected void handleMessageInternal(Message<?> message) {
		Object correlationKey = this.correlationStrategy.getCorrelationKey(message);
		Assert.notNull(correlationKey, "The correlation key is required");
		Object lock = getLock(correlationKey);
		synchronized (lock) {
			this.store.addMessagesToGroup(correlationKey, message);
		}
		logger.debug(LogMessage.format("Handled message for key [%s]: %s.", correlationKey, message));
	}

	private Object getLock(Object correlationKey) {
		Object existingLock = this.correlationLocks.putIfAbsent(correlationKey, correlationKey);
		return existingLock == null ? correlationKey : existingLock;
	}

	@SuppressWarnings("unchecked")
	@Override
	public @Nullable Message<Object> receive() {
		for (Object key : this.correlationLocks.keySet()) {
			Object lock = getLock(key);
			synchronized (lock) {
				MessageGroup group = this.store.getMessageGroup(key);
				//group might be removed by another thread
				if (group != null && this.releaseStrategy.canRelease(group)) {
					Message<?> nextMessage = null;
					Iterator<Message<?>> messages = group.getMessages().iterator();
					if (messages.hasNext()) {
						nextMessage = messages.next();
						this.store.removeMessagesFromGroup(key, nextMessage);
						logger.debug(LogMessage.format("Released message for key [%s]: %s.", key, nextMessage));
					}
					else {
						remove(key);
					}
					return (Message<Object>) nextMessage;
				}
			}
		}
		return null;
	}

	private void remove(Object key) {
		this.correlationLocks.remove(key);
		this.store.removeMessageGroup(key);
	}

}
