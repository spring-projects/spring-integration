/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.gateway;

import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * Adapts a {@link RequestReplyExchanger} to the
 * {@link org.springframework.messaging.MessageHandler} interface.
 *
 * @author Oleg Zhurakousky
 * @author Mark Fisher
 * @author Gary Russell
 *
 * @since 2.0
 */
class RequestReplyMessageHandlerAdapter extends AbstractReplyProducingMessageHandler {

	private final RequestReplyExchanger exchanger;

	RequestReplyMessageHandlerAdapter(RequestReplyExchanger exchanger) {
		Assert.notNull(exchanger, "exchanger must not be null");
		this.exchanger = exchanger;
	}

	/**
	 * Delegates to the exchanger.
	 */
	@Override
	protected Object handleRequestMessage(Message<?> requestMessage) {
		return this.exchanger.exchange(requestMessage);
	}

}
