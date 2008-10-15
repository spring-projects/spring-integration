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

import java.util.ArrayList;
import java.util.Collection;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.integration.channel.BeanFactoryChannelResolver;
import org.springframework.integration.channel.ChannelResolver;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.core.Message;
import org.springframework.integration.message.MessagingException;
import org.springframework.util.Assert;

/**
 * A base class for router implementations that return only
 * the channel name(s) rather than {@link MessageChannel} instances.
 * 
 * @author Mark Fisher
 */
public abstract class AbstractChannelNameResolvingMessageRouter extends AbstractMessageRouter implements BeanFactoryAware, InitializingBean {

	private volatile ChannelResolver channelResolver;

	private volatile String prefix;

	private volatile String suffix;

	private volatile BeanFactory beanFactory;


	public void setChannelResolver(ChannelResolver channelResolver) {
		this.channelResolver = channelResolver;
	}

	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}

	public void setSuffix(String suffix) {
		this.suffix = suffix;
	}

	public void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}

	public void afterPropertiesSet() {
		if (this.channelResolver == null) {
			Assert.notNull(beanFactory, "either a ChannelResolver or BeanFactory is required");
			this.channelResolver = new BeanFactoryChannelResolver(this.beanFactory);
		}
	}

	@Override
	protected final Collection<MessageChannel> determineTargetChannels(Message<?> message) {
		this.afterPropertiesSet();
		Collection<MessageChannel> channels = new ArrayList<MessageChannel>();
		String[] channelNames = this.determineTargetChannelNames(message);
		if (channelNames == null) {
			return null;
		}
		for (String channelName : channelNames) {
			if (channelName != null) {
				Assert.state(this.channelResolver != null,
						"unable to resolve channel names, no ChannelResolver available");
				if (this.prefix != null) {
					channelName = this.prefix + channelName;
				}
				if (this.suffix != null) {
					channelName = channelName + suffix;
				}
				MessageChannel channel = this.channelResolver.resolveChannelName(channelName);
				if (channel == null) {
					throw new MessagingException(message,
							"failed to resolve channel name '" + channelName + "'");
				}
				channels.add(channel);
			}
		}
		return channels;
	}

	/**
	 * Subclasses must implement this method to return the channel name(s).
	 */
	protected abstract String[] determineTargetChannelNames(Message<?> message);

}
