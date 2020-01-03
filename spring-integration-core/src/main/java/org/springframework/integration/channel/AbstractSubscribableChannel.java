/*
 * Copyright 2002-2020 the original author or authors.
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

import org.springframework.integration.MessageDispatchingException;
import org.springframework.integration.dispatcher.MessageDispatcher;
import org.springframework.integration.support.management.SubscribableChannelManagement;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.util.Assert;

/**
 * Base implementation of {@link org.springframework.messaging.MessageChannel} that
 * invokes the subscribed {@link MessageHandler handler(s)} by delegating to a
 * {@link MessageDispatcher}.
 *
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artme Bilan
 */
public abstract class AbstractSubscribableChannel extends AbstractMessageChannel
		implements SubscribableChannel, SubscribableChannelManagement {

	@Override
	public int getSubscriberCount() {
		return getRequiredDispatcher().getHandlerCount();
	}

	@Override
	public boolean subscribe(MessageHandler handler) {
		MessageDispatcher dispatcher = getRequiredDispatcher();
		boolean added = dispatcher.addHandler(handler);
		adjustCounterIfNecessary(dispatcher, added ? 1 : 0);
		return added;
	}

	@Override
	public boolean unsubscribe(MessageHandler handle) {
		MessageDispatcher dispatcher = getRequiredDispatcher();
		boolean removed = dispatcher.removeHandler(handle);
		this.adjustCounterIfNecessary(dispatcher, removed ? -1 : 0);
		return removed;
	}

	private void adjustCounterIfNecessary(MessageDispatcher dispatcher, int delta) {
		if (delta != 0 && logger.isInfoEnabled()) {
			logger.info("Channel '" + getFullChannelName() + "' has " + dispatcher.getHandlerCount()
					+ " subscriber(s).");
		}
	}

	@Override
	protected boolean doSend(Message<?> message, long timeout) {
		try {
			return getRequiredDispatcher().dispatch(message);
		}
		catch (MessageDispatchingException ex) {
			String description = ex.getMessage() + " for channel '" + getFullChannelName() + "'.";
			throw new MessageDeliveryException(message, description, ex);
		}
	}

	private MessageDispatcher getRequiredDispatcher() {
		MessageDispatcher dispatcher = getDispatcher();
		Assert.state(dispatcher != null, "'dispatcher' must not be null");
		return dispatcher;
	}

	protected abstract MessageDispatcher getDispatcher();

}
