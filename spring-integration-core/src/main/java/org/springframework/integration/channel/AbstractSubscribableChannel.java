/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
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
