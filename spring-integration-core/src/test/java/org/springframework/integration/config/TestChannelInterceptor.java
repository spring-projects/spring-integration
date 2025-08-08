/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.config;

import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.ChannelInterceptor;

/**
 * @author Mark Fisher
 * @author Gary Russell
 */
public class TestChannelInterceptor implements ChannelInterceptor {

	private final AtomicInteger sendCount = new AtomicInteger();

	private final AtomicInteger receiveCount = new AtomicInteger();

	@Override
	public Message<?> preSend(Message<?> message, MessageChannel channel) {
		sendCount.incrementAndGet();
		return message;
	}

	@Override
	public Message<?> postReceive(Message<?> message, MessageChannel channel) {
		receiveCount.incrementAndGet();
		return message;
	}

	public int getSendCount() {
		return this.sendCount.get();
	}

	public int getReceiveCount() {
		return this.receiveCount.get();
	}

}
