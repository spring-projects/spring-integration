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

package org.springframework.integration.router;

import java.util.Collection;

import org.springframework.integration.channel.MessageChannelTemplate;
import org.springframework.integration.consumer.AbstractMessageConsumer;
import org.springframework.integration.core.Message;
import org.springframework.integration.core.MessageChannel;
import org.springframework.integration.message.MessageDeliveryException;

/**
 * Base class for Message Routers.
 * 
 * @author Mark Fisher
 */
public abstract class AbstractMessageRouter extends AbstractMessageConsumer {

	private volatile MessageChannel defaultOutputChannel;

	private volatile boolean resolutionRequired;

	private final MessageChannelTemplate channelTemplate = new MessageChannelTemplate();


	/**
	 * Set the default channel where Messages should be sent if channel
	 * resolution fails to return any channels. If no default channel is
	 * provided, the router will either drop the Message or throw an Exception
	 * depending on the value of {@link #resolutionRequired}. 
	 */
	public void setDefaultOutputChannel(MessageChannel defaultOutputChannel) {
		this.defaultOutputChannel = defaultOutputChannel;
	}

	/**
	 * Set the timeout for sending a message to the resolved channel. By
	 * default, there is no timeout, meaning the send will block indefinitely.
	 */
	public void setTimeout(long timeout) {
		this.channelTemplate.setSendTimeout(timeout);
	}

	/**
	 * Set whether this router should always be required to resolve at least one
	 * channel. The default is 'false'. To trigger an exception whenever the
	 * resolver returns null or an empty channel list, and this endpoint has 
	 * no 'defaultOutputChannel' configured, set this value to 'true'.
	 */
	public void setResolutionRequired(boolean resolutionRequired) {
		this.resolutionRequired = resolutionRequired;
	}

	@Override
	protected void onMessageInternal(Message<?> message) {
		boolean sent = false;
		Collection<MessageChannel> results = this.determineTargetChannels(message);
		if (results != null) {
			for (MessageChannel channel : results) {
				if (channel != null) {
					if (this.channelTemplate.send(message, channel)) {
						sent = true;
					}
				}
			}
		}
		if (!sent) {
			if (this.defaultOutputChannel != null) {
				sent = this.channelTemplate.send(message, this.defaultOutputChannel);
			}
			else if (this.resolutionRequired) {
				throw new MessageDeliveryException(message,
						"no channel resolved by router and no default output channel defined");
			}
		}
	}

	/**
	 * Subclasses must implement this method to return the target channels for
	 * a given Message.
	 */
	protected abstract Collection<MessageChannel> determineTargetChannels(Message<?> message);

}
