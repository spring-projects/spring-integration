/*
 * Copyright 2002-2016 the original author or authors.
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
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.integration.support.management.MappingMessageRouterManagement;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.core.DestinationResolutionException;
import org.springframework.messaging.core.DestinationResolver;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Base class for all Message Routers that support mapping from arbitrary String values
 * to Message Channel names.
 *
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gunnar Hillert
 * @author Gary Russell
 * @author Artem Bilan
 * @since 2.1
 */
public abstract class AbstractMappingMessageRouter extends AbstractMessageRouter implements MappingMessageRouterManagement {

	private static final int DEFAULT_DYNAMIC_CHANNEL_LIMIT = 100;

	private int dynamicChannelLimit = DEFAULT_DYNAMIC_CHANNEL_LIMIT;

	@SuppressWarnings("serial")
	private final Map<String, MessageChannel> dynamicChannels = Collections.<String, MessageChannel>synchronizedMap(
			new LinkedHashMap<String, MessageChannel>(DEFAULT_DYNAMIC_CHANNEL_LIMIT, 0.75f, true) {

				@Override
				protected boolean removeEldestEntry(Entry<String, MessageChannel> eldest) {
					return this.size() > AbstractMappingMessageRouter.this.dynamicChannelLimit;
				}

			});

	protected volatile Map<String, String> channelMappings = new ConcurrentHashMap<String, String>();

	private volatile String prefix;

	private volatile String suffix;

	private volatile boolean resolutionRequired = true;


	/**
	 * Provide mappings from channel keys to channel names.
	 * Channel names will be resolved by the {@link DestinationResolver}.
	 * @param channelMappings The channel mappings.
	 */
	@Override
	@ManagedAttribute
	public void setChannelMappings(Map<String, String> channelMappings) {
		Assert.notNull(channelMappings, "'channelMappings' must not be null");
		Map<String, String> newChannelMappings = new ConcurrentHashMap<String, String>(channelMappings);
		doSetChannelMappings(newChannelMappings);
	}

	/**
	 * Specify a prefix to be added to each channel name prior to resolution.
	 * @param prefix The prefix.
	 */
	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}

	/**
	 * Specify a suffix to be added to each channel name prior to resolution.
	 * @param suffix The suffix.
	 */
	public void setSuffix(String suffix) {
		this.suffix = suffix;
	}

	/**
	 * Specify whether this router should ignore any failure to resolve a channel name to
	 * an actual MessageChannel instance when delegating to the ChannelResolver strategy.
	 * @param resolutionRequired true if resolution is required.
	 */
	public void setResolutionRequired(boolean resolutionRequired) {
		this.resolutionRequired = resolutionRequired;
	}

	/**
	 * Set a limit for how many dynamic channels are retained (for reporting purposes).
	 * When the limit is exceeded, the oldest channel is discarded.
	 * <p><b>NOTE: this does not affect routing, just the reporting which dynamically
	 * resolved channels have been routed to.</b> Default {@code 100}.
	 * @param dynamicChannelLimit the limit.
	 * @see #getDynamicChannelNames()
	 */
	public void setDynamicChannelLimit(int dynamicChannelLimit) {
		this.dynamicChannelLimit = dynamicChannelLimit;
	}

	/**
	 * Returns an unmodifiable version of the channel mappings.
	 * This is intended for use by subclasses only.
	 * @return The channel mappings.
	 */
	@Override
	@ManagedAttribute
	public Map<String, String> getChannelMappings() {
		return new HashMap<String, String>(this.channelMappings);
	}

	/**
	 * Add a channel mapping from the provided key to channel name.
	 * @param key The key.
	 * @param channelName The channel name.
	 */
	@Override
	@ManagedOperation
	public void setChannelMapping(String key, String channelName) {
		Map<String, String> newChannelMappings = new ConcurrentHashMap<String, String>(this.channelMappings);
		newChannelMappings.put(key, channelName);
		this.channelMappings = newChannelMappings;
	}

	/**
	 * Remove a channel mapping for the given key if present.
	 * @param key The key.
	 */
	@Override
	@ManagedOperation
	public void removeChannelMapping(String key) {
		Map<String, String> newChannelMappings = new ConcurrentHashMap<String, String>(this.channelMappings);
		newChannelMappings.remove(key);
		this.channelMappings = newChannelMappings;
	}

	@Override
	@ManagedAttribute
	public Collection<String> getDynamicChannelNames() {
		return Collections.unmodifiableSet(this.dynamicChannels.keySet());
	}

	/**
	 * Subclasses must implement this method to return the channel keys.
	 * A "key" might be present in this router's "channelMappings", or it
	 * could be the channel's name or even the Message Channel instance itself.
	 * @param message The message.
	 * @return The channel keys.
	 */
	protected abstract List<Object> getChannelKeys(Message<?> message);


	@Override
	protected Collection<MessageChannel> determineTargetChannels(Message<?> message) {
		Collection<MessageChannel> channels = new ArrayList<MessageChannel>();
		Collection<Object> channelKeys = this.getChannelKeys(message);
		addToCollection(channels, channelKeys, message);
		return channels;
	}

	/**
	 * Convenience method allowing conversion of a list
	 * of mappings in a control-bus message.
	 * <p>This is intended to be called via a control-bus; keys and values that are not
	 * Strings will be ignored.
	 * <p>Mappings must be delimited with newlines, for example:
	 * <p>{@code "@'myRouter.handler'.replaceChannelMappings('foo=qux \n baz=bar')"}.
	 * @param channelMappings The channel mappings.
	 * @since 4.0
	 */
	@Override
	@ManagedOperation
	public void replaceChannelMappings(Properties channelMappings) {
		Assert.notNull(channelMappings, "'channelMappings' must not be null");
		Map<String, String> newChannelMappings = new ConcurrentHashMap<String, String>();
		Set<String> keys = channelMappings.stringPropertyNames();
		for (String key : keys) {
			newChannelMappings.put(key.trim(), channelMappings.getProperty(key).trim());
		}
		this.doSetChannelMappings(newChannelMappings);
	}

	private void doSetChannelMappings(Map<String, String> newChannelMappings) {
		Map<String, String> oldChannelMappings = this.channelMappings;
		this.channelMappings = newChannelMappings;
		if (logger.isDebugEnabled()) {
			logger.debug("Channel mappings: " + oldChannelMappings + " replaced with: " + newChannelMappings);
		}
	}

	private MessageChannel resolveChannelForName(String channelName, Message<?> message) {
		MessageChannel channel = null;
		try {
			channel = getChannelResolver().resolveDestination(channelName);
		}
		catch (DestinationResolutionException e) {
			if (this.resolutionRequired) {
				throw new MessagingException(message, "failed to resolve channel name '" + channelName + "'", e);
			}
		}
		if (channel == null && this.resolutionRequired) {
			throw new MessagingException(message, "failed to resolve channel name '" + channelName + "'");
		}
		return channel;
	}

	private void addChannelFromString(Collection<MessageChannel> channels, String channelKey, Message<?> message) {
		if (channelKey.indexOf(',') != -1) {
			for (String name : StringUtils.tokenizeToStringArray(channelKey, ",")) {
				addChannelFromString(channels, name, message);
			}
			return;
		}

		// if the channelMappings contains a mapping, we'll use the mapped value
		// otherwise, the String-based channelKey itself will be used as the channel name
		String channelName = channelKey;
		boolean mapped = false;
		if (this.channelMappings.containsKey(channelKey)) {
			channelName = this.channelMappings.get(channelKey);
			mapped = true;
		}
		if (this.prefix != null) {
			channelName = this.prefix + channelName;
		}
		if (this.suffix != null) {
			channelName = channelName + this.suffix;
		}
		MessageChannel channel = resolveChannelForName(channelName, message);
		if (channel != null) {
			channels.add(channel);
			if (!mapped && !(this.dynamicChannels.get(channelName) != null)) {
				this.dynamicChannels.put(channelName, channel);
			}
		}
	}

	private void addToCollection(Collection<MessageChannel> channels, Collection<?> channelKeys, Message<?> message) {
		if (channelKeys == null) {
			return;
		}
		for (Object channelKey : channelKeys) {
			if (channelKey == null) {
				continue;
			}
			else if (channelKey instanceof MessageChannel) {
				channels.add((MessageChannel) channelKey);
			}
			else if (channelKey instanceof MessageChannel[]) {
				channels.addAll(Arrays.asList((MessageChannel[]) channelKey));
			}
			else if (channelKey instanceof String) {
				addChannelFromString(channels, (String) channelKey, message);
			}
			else if (channelKey instanceof Class) {
				addChannelFromString(channels, ((Class<?>) channelKey).getName(), message);
			}
			else if (channelKey instanceof String[]) {
				for (String indicatorName : (String[]) channelKey) {
					addChannelFromString(channels, indicatorName, message);
				}
			}
			else if (channelKey instanceof Collection) {
				addToCollection(channels, (Collection<?>) channelKey, message);
			}
			else if (this.getRequiredConversionService().canConvert(channelKey.getClass(), String.class)) {
				addChannelFromString(channels, this.getConversionService().convert(channelKey, String.class), message);
			}
			else {
				throw new MessagingException("unsupported return type for router [" + channelKey.getClass() + "]");
			}
		}
	}

}
