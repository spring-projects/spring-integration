/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.config;

import java.util.concurrent.CountDownLatch;

import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 */
public class TestHandler {

	private String messageString;

	private CountDownLatch latch;

	private String replyMessageText = null;

	public TestHandler() {
		this(1);
	}

	public TestHandler(int countdown) {
		this.latch = new CountDownLatch(countdown);
	}

	public void setReplyMessageText(String replyMessageText) {
		this.replyMessageText = replyMessageText;
	}

	@ServiceActivator
	public String handle(Message<?> message) {
		this.messageString = message.getPayload().toString();
		this.latch.countDown();
		return this.replyMessageText;
	}

	public String getMessageString() {
		return this.messageString;
	}

	public CountDownLatch getLatch() {
		return this.latch;
	}

}
