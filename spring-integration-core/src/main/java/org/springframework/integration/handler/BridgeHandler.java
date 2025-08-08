/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.handler;

import org.springframework.integration.IntegrationPatternType;
import org.springframework.messaging.Message;

/**
 * A simple MessageHandler implementation that passes the request Message
 * directly to the output channel without modifying it. The main purpose of this
 * handler is to bridge a PollableChannel to a SubscribableChannel or
 * vice-versa.
 * <p>
 * The BridgeHandler can be used as a stopper at the end of an assembly line of
 * channels. In this setup the output channel doesn't have to be set, but if the
 * output channel is omitted the {@code REPLY_CHANNEL} MUST be set on the
 * message. Otherwise, a MessagingException will be thrown at runtime.
 *
 * @author Mark Fisher
 * @author Iwein Fuld
 * @author Marius Bogoevici
 */
public class BridgeHandler extends AbstractReplyProducingMessageHandler {

	@Override
	public String getComponentType() {
		return "bridge";
	}

	@Override
	public IntegrationPatternType getIntegrationPatternType() {
		return IntegrationPatternType.bridge;
	}

	@Override
	protected Object handleRequestMessage(Message<?> requestMessage) {
		return requestMessage;
	}

	@Override
	protected boolean shouldCopyRequestHeaders() {
		return false;
	}

}
