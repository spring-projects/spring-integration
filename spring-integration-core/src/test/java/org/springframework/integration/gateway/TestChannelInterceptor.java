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
