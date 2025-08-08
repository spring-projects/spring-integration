/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.config;

import java.util.concurrent.CountDownLatch;

/**
 * @author Mark Fisher
 */
public class TestBean {

	private String message;

	private CountDownLatch latch;

	private String replyMessageText = null;

	public TestBean() {
		this(1);
	}

	public TestBean(int countdown) {
		this.latch = new CountDownLatch(countdown);
	}

	public void setReplyMessageText(String replyMessageText) {
		this.replyMessageText = replyMessageText;
	}

	public CountDownLatch getLatch() {
		return this.latch;
	}

	public String store(String message) {
		this.message = message;
		latch.countDown();
		return this.replyMessageText;
	}

	public String getMessage() {
		return this.message;
	}

}
