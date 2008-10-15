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

package org.springframework.integration.channel.interceptor;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.integration.channel.BlockingChannel;
import org.springframework.integration.channel.ChannelInterceptor;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.core.Message;
import org.springframework.integration.message.MessageStore;
import org.springframework.integration.message.MessagingException;
import org.springframework.util.Assert;

/**
 * A {@link ChannelInterceptor} that delegates to a {@link MessageStore}.
 * Upon sending to the channel, the message will be added to the store,
 * and upon receiving from the channel, the message will be removed from
 * the store.
 * 
 * @author Mark Fisher
 */
public class MessageStoringInterceptor extends ChannelInterceptorAdapter {

	private final Log logger = LogFactory.getLog(this.getClass());

	private final MessageStore messageStore;

	private volatile boolean initialized;

	private final Object initializationMonitor = new Object();


	public MessageStoringInterceptor(MessageStore messageStore) {
		Assert.notNull(messageStore, "MessageStore must not be null");
		this.messageStore = messageStore;
	}


	@Override
	public Message<?> preSend(Message<?> message, MessageChannel channel) {
		if (message != null) {
			this.messageStore.put(message.getHeaders().getId(), message);
		}
		return message;
	}

	@Override
	public boolean preReceive(MessageChannel channel) {
		synchronized (this.initializationMonitor) {
			if (!this.initialized) {
				if (logger.isDebugEnabled()) {
					logger.debug("pre-loading Messages from MessageStore for channel '" + channel.getName() + "'");
				}
				List<Message<?>> storedMessages = this.messageStore.list();
				for (Message<?> message : storedMessages) {
					boolean sent = (channel instanceof BlockingChannel)
							? ((BlockingChannel) channel).send(message, 0) : channel.send(message);
					if (!sent) {
						throw new MessagingException("failed to initialize channel from MessageStore");
					}
				}
				if (logger.isDebugEnabled()) {
					logger.debug("pre-loaded " + storedMessages.size() + " Messages for channel '" + channel.getName() + "'");
				}
			}
			this.initialized = true;
		}
		return true;
	}

	@Override
	public Message<?> postReceive(Message<?> message, MessageChannel channel) {
		if (message != null) {
			this.messageStore.remove(message.getHeaders().getId());
		}
		return message;
	}

}
