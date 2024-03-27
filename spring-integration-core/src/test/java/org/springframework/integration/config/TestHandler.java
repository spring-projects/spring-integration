/*
 * Copyright 2002-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
