/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
