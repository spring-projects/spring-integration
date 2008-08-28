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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.integration.channel.ChannelRegistry;
import org.springframework.integration.channel.ChannelRegistryAware;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageDeliveryException;
import org.springframework.integration.message.MessageExchangeTemplate;
import org.springframework.integration.message.MessageTarget;
import org.springframework.integration.message.MessagingException;

/**
 * Base class for message router implementations.
 * 
 * @author Mark Fisher
 */
public abstract class AbstractRouter implements Router, ChannelRegistryAware {

	protected final Log logger = LogFactory.getLog(this.getClass());

	private volatile ChannelRegistry channelRegistry;

	private final MessageExchangeTemplate messageExchangeTemplate = new MessageExchangeTemplate();


	public void setChannelRegistry(ChannelRegistry channelRegistry) {
		this.channelRegistry = channelRegistry;
	}

	protected ChannelRegistry getChannelRegistry() {
		return this.channelRegistry;
	}

	public final boolean route(Message<?> message) {
		Collection<?> results = this.resolveChannels(message);
		if (results == null || results.isEmpty()) {
			return false;
		}
		boolean sent = false;
		for (Object channelOrName : results) {
			MessageTarget target = null;
			if (channelOrName == null) {
				continue;
			}
			if (channelOrName instanceof MessageTarget) {
				target = (MessageTarget) channelOrName;
			}
			else if (channelOrName instanceof String) {
				if (this.channelRegistry == null) {
					throw new MessagingException(message, "router has no ChannelRegistry");
				}
				target = this.channelRegistry.lookupChannel((String) channelOrName);
			}
			else {
				throw new MessagingException(message, "unsupported return type for router [" + channelOrName.getClass() + "]");
			}
			if (target == null) {
				throw new MessageDeliveryException(message, "unable to resolve channel for '" + channelOrName + "'");
			}
			this.messageExchangeTemplate.send(message, target);
			sent = true;
		}
		return sent;
	}

	/**
	 * Subclasses must implement this method to return 0 or more MessageChannel
	 * instances or channel names to which the given Message should be routed.
	 */
	protected abstract Collection<?> resolveChannels(Message<?> message);

}
