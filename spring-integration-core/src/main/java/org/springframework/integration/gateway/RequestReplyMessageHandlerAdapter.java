/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
