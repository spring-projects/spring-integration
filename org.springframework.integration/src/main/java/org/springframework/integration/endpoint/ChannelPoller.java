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
import org.springframework.integration.dispatcher.SimpleDispatcher;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageConsumer;
import org.springframework.integration.message.Subscribable;
import org.springframework.integration.scheduling.Trigger;
import org.springframework.util.Assert;

/**
 * @author Mark Fisher
 */
public class ChannelPoller extends AbstractPoller implements Subscribable {

	private final PollableChannel channel;

	private volatile long receiveTimeout = 1000;

	private final SimpleDispatcher dispatcher = new SimpleDispatcher();


	public ChannelPoller(PollableChannel channel, Trigger trigger) {
		super(trigger);
		Assert.notNull(channel, "channel must not be null");
		this.channel = channel;
	}

	/**
	 * Specify the timeout to use when receiving from the channel (in milliseconds).
	 * A negative value indicates that receive calls should block indefinitely.
	 * The default value is 1000 (1 second).
	 */
	public void setReceiveTimeout(long receiveTimeout) {
		this.receiveTimeout = receiveTimeout;
	}

	public boolean subscribe(MessageConsumer consumer) {
		return this.dispatcher.subscribe(consumer);
	}

	public boolean unsubscribe(MessageConsumer consumer) {
		return this.dispatcher.unsubscribe(consumer);
	}

	@Override
	protected boolean doPoll() {
		Message<?> message = (this.receiveTimeout >= 0)
				? this.channel.receive(this.receiveTimeout)
				: this.channel.receive();
		if (message == null) {
			return false;
		}
		return this.dispatcher.dispatch(message);
	}

}
