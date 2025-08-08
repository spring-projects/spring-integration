/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.channel.interceptor;

import java.util.Arrays;
import java.util.List;

import org.springframework.integration.core.MessageSelector;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.support.ChannelInterceptor;

/**
 * A {@link org.springframework.messaging.support.ChannelInterceptor ChannelInterceptor} that
 * delegates to a list of {@link MessageSelector MessageSelectors} to decide
 * whether a {@link Message} should be accepted on the {@link MessageChannel}.
 *
 * @author Mark Fisher
 * @author Gary Russell
 */
public class MessageSelectingInterceptor implements ChannelInterceptor {

	private final List<MessageSelector> selectors;

	public MessageSelectingInterceptor(MessageSelector... selectors) {
		this.selectors = Arrays.asList(selectors);
	}

	@Override
	public Message<?> preSend(Message<?> message, MessageChannel channel) {
		for (MessageSelector selector : this.selectors) {
			if (!selector.accept(message)) {
				throw new MessageDeliveryException(message,
						"selector '" + selector + "' did not accept message");
			}
		}
		return message;
	}

}
