/*
 * Copyright 2002-2007 the original author or authors.
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

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.integration.MessageHandlingException;
import org.springframework.integration.MessagingConfigurationException;
import org.springframework.integration.bus.ConsumerPolicy;
import org.springframework.integration.channel.ChannelRegistry;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.handler.MessageHandler;
import org.springframework.integration.message.Message;

/**
 * A generic endpoint implementation designed to accommodate a variety of
 * strategies including:
 * <ul>
 *   <li><i>source channel-adapter:</i> source adapter + target channel</li>
 *   <li><i>target channel-adapter:</i> source channel + target adapter</li>
 *   <li><i>one-way:</i> source + handler that returns null and no target</li>
 *   <li><i>request-reply:</i> source + handler and either a reply channel
 *   specified on the request message or a default target on the endpoint</i>
 * </ul>
 * 
 * @author Mark Fisher
 */
public class GenericMessageEndpoint<T> implements MessageEndpoint<T>, BeanNameAware {

	private String name;

	private String inputChannelName;

	private String defaultOutputChannelName;

	private MessageHandler handler;

	private ChannelRegistry channelRegistry;

	private ConsumerPolicy consumerPolicy = new ConsumerPolicy();


	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setBeanName(String beanName) {
		this.setName(beanName);
	}

	/**
	 * Set the name of the channel from which this endpoint receives messages.
	 */
	public void setInputChannelName(String inputChannelName) {
		this.inputChannelName = inputChannelName;
	}

	/**
	 * Return the name of the channel from which this endpoint receives messages.
	 */
	public String getInputChannelName() {
		return this.inputChannelName;
	}

	public void setConsumerPolicy(ConsumerPolicy consumerPolicy) {
		this.consumerPolicy = consumerPolicy;
	}

	public ConsumerPolicy getConsumerPolicy() {
		return this.consumerPolicy;
	}

	/**
	 * Set the name of the channel to which this endpoint can send reply messages by default.
	 */
	public void setDefaultOutputChannelName(String defaultOutputChannelName) {
		this.defaultOutputChannelName = defaultOutputChannelName;
	}

	public String getDefaultOutputChannelName() {
		return this.defaultOutputChannelName;
	}

	/**
	 * Set a handler to be invoked for each consumed message.
	 */
	public void setHandler(MessageHandler handler) {
		this.handler = handler;
	}

	/**
	 * Set the channel registry to use for looking up channels by name.
	 */
	public void setChannelRegistry(ChannelRegistry channelRegistry) {
		this.channelRegistry = channelRegistry;
	}


	public void messageReceived(Message<T> message) {
		if (this.handler == null) {
			if (this.defaultOutputChannelName == null) {
				throw new MessagingConfigurationException(
						"endpoint must have either a 'handler' or 'defaultOutputChannelName'");
			}
			MessageChannel replyChannel = this.channelRegistry.lookupChannel(this.defaultOutputChannelName);
			replyChannel.send(message);
			return;
		}
		Message<?> replyMessage = handler.handle(message);
		if (replyMessage != null) {
			MessageChannel replyChannel = this.resolveReplyChannel(message);
			if (replyChannel == null) {
				throw new MessageHandlingException("Unable to determine reply channel for message. "
						+ "Provide a 'replyChannelName' in the message header or a 'defaultOutputChannelName' "
						+ "on the message endpoint.");
			}
			replyChannel.send(replyMessage);
		}
	}

	private MessageChannel resolveReplyChannel(Message<?> message) {
		if (this.channelRegistry == null) {
			return null;
		}
		String replyChannelName = message.getHeader().getReplyChannelName();
		if (replyChannelName != null && replyChannelName.trim().length() > 0) {
			return this.channelRegistry.lookupChannel(replyChannelName);
		}
		if (this.defaultOutputChannelName != null) {
			return this.channelRegistry.lookupChannel(this.defaultOutputChannelName);
		}
		return null;
	}

}
