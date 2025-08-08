/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.gateway;

import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.ChannelInterceptor;

/**
 * @author Mark Fisher
 * @author Gary Russell
 */
public class TestChannelInterceptor implements ChannelInterceptor {

	private final AtomicInteger sentCount = new AtomicInteger();

	private final AtomicInteger receivedCount = new AtomicInteger();

	public int getSentCount() {
		return this.sentCount.get();
	}

	public int getReceivedCount() {
		return this.receivedCount.get();
	}

	@Override
	public void postSend(Message<?> message, MessageChannel channel, boolean sent) {
		if (sent) {
			this.sentCount.incrementAndGet();
		}
	}

	@Override
	public Message<?> postReceive(Message<?> message, MessageChannel channel) {
		if (message != null) {
			this.receivedCount.incrementAndGet();
		}
		return message;
	}

}
