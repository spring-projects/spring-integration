/*
 * Copyright 2002-2010 the original author or authors.
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
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.ConversionServiceFactory;
import org.springframework.integration.channel.BeanFactoryChannelResolver;
import org.springframework.integration.channel.ChannelResolutionException;
import org.springframework.integration.channel.ChannelResolver;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.core.Message;
import org.springframework.integration.core.MessageChannel;
import org.springframework.integration.core.MessagingException;
import org.springframework.util.Assert;

/**
 * A base class for router implementations that return only the channel name(s)
 * rather than {@link MessageChannel} instances.
 * 
 * @author Mark Fisher
 * @author Jonas Partner
 */
public abstract class AbstractChannelNameResolvingMessageRouter extends AbstractMessageRouter
		implements BeanFactoryAware, InitializingBean {

	private volatile ChannelResolver channelResolver;

	private volatile ConversionService conversionService;

	private volatile String prefix;

	private volatile String suffix;

	private volatile BeanFactory beanFactory;

	private volatile boolean ignoreChannelNameResolutionFailures;


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

	public void setIgnoreChannelNameResolutionFailures(boolean ignoreChannelNameResolutionFailures) {
		this.ignoreChannelNameResolutionFailures = ignoreChannelNameResolutionFailures;
	}

	public void afterPropertiesSet() {
		if (this.channelResolver == null) {
			Assert.notNull(beanFactory,
					"either a ChannelResolver or BeanFactory is required");
			this.channelResolver = new BeanFactoryChannelResolver(this.beanFactory);
		}
	}

	protected MessageChannel resolveChannelForName(String channelName, Message<?> message) {
		Assert.state(this.channelResolver != null,
				"unable to resolve channel names, no ChannelResolver available");

		MessageChannel channel = null;
		try {
			channel = this.channelResolver.resolveChannelName(channelName);
		}
		catch (ChannelResolutionException e) {
			if (!ignoreChannelNameResolutionFailures)
				throw new MessagingException(message,
						"failed to resolve channel name '" + channelName + "'", e);
		}
		if (channel == null && !ignoreChannelNameResolutionFailures) {
			throw new MessagingException(message,
					"failed to resolve channel name '" + channelName + "'");
		}
		return channel;
	}

	@Override
	protected Collection<MessageChannel> determineTargetChannels(Message<?> message) {
		this.afterPropertiesSet();
		Collection<MessageChannel> channels = new ArrayList<MessageChannel>();
		Collection<Object> channelsReturned = this.getChannelIndicatorList(message);
		addToCollection(channels, channelsReturned, message);
		return channels;
	}


	protected void addToCollection(Collection<MessageChannel> channels, Collection<?> channelIndicators, Message<?> message) {
		if (channelIndicators == null) {
			return;
		}
		for (Object channelIndicator : channelIndicators) {
			if (channelIndicator == null) {
				continue;
			}
			else if (channelIndicator instanceof MessageChannel) {
				channels.add((MessageChannel) channelIndicator);
			}
			else if (channelIndicator instanceof MessageChannel[]) {
				channels.addAll(Arrays.asList((MessageChannel[]) channelIndicator));
			}
			else if (channelIndicator instanceof String) {
				addChannelFromString(channels, (String) channelIndicator, message);
			}
			else if (channelIndicator instanceof String[]) {
				for (String indicatorName : (String[]) channelIndicator) {
					addChannelFromString(channels, indicatorName, message);
				}
			}
			else if (channelIndicator instanceof Collection) {
				addToCollection(channels, (Collection<?>) channelIndicator, message);
			}
			else if (this.getConversionService().canConvert(channelIndicator.getClass(), String.class)) {
				addChannelFromString(channels,
						this.getConversionService().convert(channelIndicator, String.class), message);
			}
			else {
				throw new MessagingException(
						"unsupported return type for router [" + channelIndicator.getClass() + "]");
			}
		}
	}

	protected void addChannelFromString(Collection<MessageChannel> channels, String channelName, Message<?> message) {
		if (this.prefix != null) {
			channelName = this.prefix + channelName;
		}
		if (this.suffix != null) {
			channelName = channelName + suffix;
		}
		MessageChannel channel = resolveChannelForName(channelName, message);
		if (channel != null) {
			channels.add(channel);
		}
	}

	private ConversionService getConversionService() {
		if (this.conversionService == null) {
			if (this.beanFactory != null) {
				this.conversionService = IntegrationContextUtils.getConversionService(this.beanFactory);
			}
			if (this.conversionService == null) {
				this.conversionService = ConversionServiceFactory.createDefaultConversionService();
			}
		}
		return this.conversionService;
	}

	/**
	 * Subclasses must implement this method to return the channel indicators.
	 */
	protected abstract List<Object> getChannelIndicatorList(Message<?> message);

}
