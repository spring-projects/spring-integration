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

import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.ConversionServiceFactory;
import org.springframework.integration.Message;
import org.springframework.integration.MessagingException;
import org.springframework.integration.core.ChannelResolutionException;
import org.springframework.integration.core.ChannelResolver;
import org.springframework.integration.core.MessageChannel;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * A base class for router implementations that return only the channel name(s)
 * rather than {@link MessageChannel} instances.
 * 
 * @author Mark Fisher
 * @author Jonas Partner
 */
public abstract class AbstractChannelNameResolvingMessageRouter extends AbstractMessageRouter {

	private volatile String prefix;

	private volatile String suffix;

	private volatile boolean ignoreChannelNameResolutionFailures;


	/**
	 * Specify the {@link ChannelResolver} strategy to use.
	 * The default is a BeanFactoryChannelResolver.
	 */
	public void setChannelResolver(ChannelResolver channelResolver) {
		super.setChannelResolver(channelResolver);
	}

	/**
	 * Specify a prefix to be added to each channel name prior to resolution.
	 */
	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}

	/**
	 * Specify a suffix to be added to each channel name prior to resolution.
	 */
	public void setSuffix(String suffix) {
		this.suffix = suffix;
	}

	/**
	 * Specify whether this router should ignore any failure to resolve a channel name to
	 * an actual MessageChannel instance when delegating to the ChannelResolver strategy.
	 */
	public void setIgnoreChannelNameResolutionFailures(boolean ignoreChannelNameResolutionFailures) {
		this.ignoreChannelNameResolutionFailures = ignoreChannelNameResolutionFailures;
	}

	@Override
	public void onInit() {
		Assert.notNull(this.getChannelResolver(),
				"either a ChannelResolver or BeanFactory is required");
	}

	private MessageChannel resolveChannelForName(String channelName, Message<?> message) {
		Assert.state(this.getChannelResolver() != null,
				"unable to resolve channel names, no ChannelResolver available");

		MessageChannel channel = null;
		try {
			channel = this.getChannelResolver().resolveChannelName(channelName);
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

	@SuppressWarnings("unchecked")
	private void addToCollection(Collection<MessageChannel> channels, Collection<?> channelIndicators, Message<?> message) {
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
			else if (this.getRequiredConversionService().canConvert(channelIndicator.getClass(), String.class)) {
				addChannelFromString(channels,
						this.getConversionService().convert(channelIndicator, String.class), message);
			}
			else {
				throw new MessagingException(
						"unsupported return type for router [" + channelIndicator.getClass() + "]");
			}
		}
	}

	private void addChannelFromString(Collection<MessageChannel> channels, String channelName, Message<?> message) {
		if (channelName.indexOf(',') != -1) {
			for (String name : StringUtils.commaDelimitedListToStringArray(channelName)) {
				addChannelFromString(channels, name, message);
			}
			return;
		}
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

	private ConversionService getRequiredConversionService() {
		if (this.getConversionService() == null) {
			this.setConversionService(ConversionServiceFactory.createDefaultConversionService());
		}
		return this.getConversionService();
	}

	/**
	 * Subclasses must implement this method to return the channel indicators.
	 */
	protected abstract List<Object> getChannelIndicatorList(Message<?> message);

}
