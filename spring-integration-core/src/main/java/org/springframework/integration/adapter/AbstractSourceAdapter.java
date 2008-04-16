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

package org.springframework.integration.adapter;

import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.message.Message;
import org.springframework.util.Assert;

/**
 * @author Mark Fisher
 */
public abstract class AbstractSourceAdapter implements SourceAdapter {

	private final MessageChannel channel;

	private volatile long sendTimeout = -1;


	public AbstractSourceAdapter(MessageChannel channel) {
		Assert.notNull(channel, "channel must not be null");
		this.channel = channel;
	}


	protected MessageChannel getChannel() {
		return this.channel;
	}

	public void setSendTimeout(long sendTimeout) {
		this.sendTimeout = sendTimeout;
	}

	protected boolean sendToChannel(Message<?> message) {
		if (message == null) {
			throw new IllegalArgumentException("message must not be null");
		}
		return (this.sendTimeout < 0) ? this.channel.send(message) : this.channel.send(message, this.sendTimeout);
	}

}
