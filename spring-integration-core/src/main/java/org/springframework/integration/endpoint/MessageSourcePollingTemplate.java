/*
 * Copyright © 2018 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2018-present the original author or authors.
 */

package org.springframework.integration.endpoint;

import org.springframework.integration.StaticMessageHeaderAccessor;
import org.springframework.integration.acks.AckUtils;
import org.springframework.integration.acks.AcknowledgmentCallback;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.support.utils.IntegrationUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.util.Assert;

/**
 * A {@link PollingOperations} used to ad-hoc poll a {@link MessageSource}.
 * If the source supports deferred acknowledgment, it will be ACCEPTed
 * or REJECTed if necessary.
 *
 * @author Gary Russell
 *
 * @since 5.0.1
 *
 */
public class MessageSourcePollingTemplate implements PollingOperations {

	private final MessageSource<?> source;

	public MessageSourcePollingTemplate(MessageSource<?> source) {
		Assert.notNull(source, "'source' cannot be null");
		this.source = source;
	}

	@Override
	public boolean poll(MessageHandler handler) {
		Assert.notNull(handler, "'handler' cannot be null");
		Message<?> message = this.source.receive();
		if (message != null) {
			AcknowledgmentCallback ackCallback = StaticMessageHeaderAccessor.getAcknowledgmentCallback(message);
			try {
				handler.handleMessage(message);
				AckUtils.autoAck(ackCallback);
			}
			catch (Exception e) {
				AckUtils.autoNack(ackCallback);
				throw IntegrationUtils.wrapInHandlingExceptionIfNecessary(message,
						() -> "error occurred during handling message in 'MessageSourcePollingTemplate' ["
								+ this + "]", e);
			}
			return true;
		}
		return false;
	}

}
