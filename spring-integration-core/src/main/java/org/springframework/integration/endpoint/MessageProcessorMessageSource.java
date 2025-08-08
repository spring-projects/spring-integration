/*
 * Copyright © 2016 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2016-present the original author or authors.
 */

package org.springframework.integration.endpoint;

import org.springframework.integration.handler.MessageProcessor;

/**
 * The {@link org.springframework.integration.core.MessageSource} strategy implementation
 * to produce a {@link org.springframework.messaging.Message} from underlying
 * {@linkplain #messageProcessor} for polling endpoints.
 *
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 5.0
 */
public class MessageProcessorMessageSource extends AbstractMessageSource<Object> {

	private final MessageProcessor<?> messageProcessor;

	public MessageProcessorMessageSource(MessageProcessor<?> messageProcessor) {
		this.messageProcessor = messageProcessor;
	}

	@Override
	public String getComponentType() {
		return "inbound-channel-adapter";
	}

	@Override
	protected Object doReceive() {
		return this.messageProcessor.processMessage(null);
	}

}
