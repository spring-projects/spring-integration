/*
 * Copyright 2002-2008 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.endpoint;

import org.springframework.integration.channel.PollableChannel;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageConsumer;
import org.springframework.util.Assert;

/**
 * @author Mark Fisher
 */
public class PollingConsumerEndpoint extends AbstractPollingEndpoint {

	private final MessageConsumer consumer;

	private final PollableChannel inputChannel;

	private volatile long receiveTimeout = 1000;


	public PollingConsumerEndpoint(MessageConsumer consumer, PollableChannel inputChannel) {
		Assert.notNull(consumer, "consumer must not be null");
		Assert.notNull(inputChannel, "inputChannel must not be null");
		this.consumer = consumer;
		this.inputChannel = inputChannel;
	}


	public void setReceiveTimeout(long receiveTimeout) {
		this.receiveTimeout = receiveTimeout;
	}

	@Override
	protected boolean doPoll() {
		Message<?> message = (this.receiveTimeout >= 0)
				? this.inputChannel.receive(this.receiveTimeout)
				: this.inputChannel.receive();
		if (message == null) {
			return false;
		}
		this.consumer.onMessage(message);
		return true;
	}

}
