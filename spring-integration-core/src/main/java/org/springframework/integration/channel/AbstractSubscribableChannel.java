/*
 * Copyright 2002-2013 the original author or authors.
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

import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.integration.MessageDispatchingException;
import org.springframework.integration.dispatcher.AbstractDispatcher;
import org.springframework.integration.dispatcher.MessageDispatcher;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.util.Assert;

/**
 * Base implementation of {@link MessageChannel} that invokes the subscribed
 * {@link MessageHandler handler(s)} by delegating to a {@link MessageDispatcher}.
 *
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gary Russell
 */
public abstract class AbstractSubscribableChannel extends AbstractMessageChannel
		implements SubscribableChannel {

	private final AtomicInteger handlerCounter = new AtomicInteger();

	public boolean subscribe(MessageHandler handler) {
		MessageDispatcher dispatcher = this.getRequiredDispatcher();
		boolean added = dispatcher.addHandler(handler);
		this.adjustCounterIfNecessary(dispatcher, added ? 1 : 0);
		return added;
	}

	public boolean unsubscribe(MessageHandler handle) {
		MessageDispatcher dispatcher = this.getRequiredDispatcher();
		boolean removed = dispatcher.removeHandler(handle);
		this.adjustCounterIfNecessary(dispatcher, removed ? -1 : 0);
		return removed;
	}

	private void adjustCounterIfNecessary(MessageDispatcher dispatcher, int delta) {
		if (delta != 0) {
			int counter = 0;
			if (dispatcher instanceof AbstractDispatcher) {
				counter = ((AbstractDispatcher) dispatcher).getHandlerCount();
			}
			else {
				// some other dispatcher - hand-roll the counter
				counter = handlerCounter.addAndGet(delta);
			}
			if (logger.isInfoEnabled()) {
				logger.info("Channel '" + this.getFullChannelName() + "' has " + counter + " subscriber(s).");
			}
		}
	}

	@Override
	protected boolean doSend(Message<?> message, long timeout) {
		try {
			return this.getRequiredDispatcher().dispatch(message);
		}
		catch (MessageDispatchingException e) {
			String description = e.getMessage() + " for channel '" + this.getFullChannelName() + "'.";
			throw new MessageDeliveryException(message, description, e);
		}
	}

	private MessageDispatcher getRequiredDispatcher() {
		MessageDispatcher dispatcher = this.getDispatcher();
		Assert.state(dispatcher != null, "'dispatcher' must not be null");
		return dispatcher;
	}

	protected abstract MessageDispatcher getDispatcher();

}
