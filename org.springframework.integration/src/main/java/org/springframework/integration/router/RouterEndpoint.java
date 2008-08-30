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

import org.springframework.integration.channel.ChannelRegistry;
import org.springframework.integration.channel.ChannelRegistryAware;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.endpoint.AbstractEndpoint;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageDeliveryException;
import org.springframework.util.Assert;

/**
 * @author Mark Fisher
 */
public class RouterEndpoint extends AbstractEndpoint {

	private final Router router;

	private volatile MessageChannel defaultOutputChannel;

	private volatile boolean resolutionRequired;


	public RouterEndpoint(Router router) {
		Assert.notNull(router, "router must not be null");
		this.router = router;
	}


	@Override
	public void setChannelRegistry(ChannelRegistry channelRegistry) {
		super.setChannelRegistry(channelRegistry);
		if (this.router instanceof ChannelRegistryAware) {
			((ChannelRegistryAware) this.router).setChannelRegistry(channelRegistry);
		}
	}

	public void setDefaultOutputChannel(MessageChannel defaultOutputChannel) {
		this.defaultOutputChannel = defaultOutputChannel;
	}

	/**
	 * Set the timeout for sending a message to the resolved channel. By
	 * default, there is no timeout, meaning the send will block indefinitely.
	 */
	public void setTimeout(long timeout) {
		this.getMessageExchangeTemplate().setSendTimeout(timeout);
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
	protected boolean sendInternal(Message<?> message) {
		boolean sent = this.router.route(message); 
		if (!sent) {
			if (this.defaultOutputChannel != null) {
				sent = this.getMessageExchangeTemplate().send(message, this.defaultOutputChannel);
			}
			else if (this.resolutionRequired) {
				throw new MessageDeliveryException(message,
						"no target resolved by router and no default output channel defined");
			}
		}
		return sent;
	}

}
