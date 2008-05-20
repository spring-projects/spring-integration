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

package org.springframework.integration.channel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.integration.message.Message;
import org.springframework.util.Assert;

/**
 * Sends to a channel and provides a configurable timeout. Convenient for either
 * subclassing or delegation from components that need to publish to a channel.
 * 
 * @author Mark Fisher
 */
public class ChannelPublisher {

	private final Log logger = LogFactory.getLog(this.getClass());

	private volatile MessageChannel channel;

	private volatile long timeout = 0;


	public ChannelPublisher() {
	}

	public ChannelPublisher(MessageChannel channel) {
		this.setChannel(channel);
	}


	public void setChannel(MessageChannel channel) {
		Assert.notNull(channel, "channel must not be null");
		this.channel = channel;		
	}

	public void setTimeout(long timeout) {
		this.timeout = timeout;
	}

	protected MessageChannel getChannel() {
		return this.channel;
	}

	public boolean publish(Message<?> message) {
		if (this.channel == null) {
			if (logger.isWarnEnabled()) {
				logger.warn("unable to send message, no channel available");
			}
			return false;
		}
		if (message == null) {
			if (logger.isWarnEnabled()) {
				logger.warn("null messages are not supported");
			}
			return false;
		}
		return (this.timeout < 0) ? this.channel.send(message) : this.channel.send(message, this.timeout);
	}

}
