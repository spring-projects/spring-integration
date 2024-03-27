/*
 * Copyright 2002-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import org.springframework.core.convert.ConversionService;
import org.springframework.core.log.LogMessage;
import org.springframework.integration.support.management.MappingMessageRouterManagement;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.core.DestinationResolutionException;
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
 * @author Trung Pham
 *
 * @since 2.1
 */
public abstract class AbstractMappingMessageRouter extends AbstractMessageRouter
		implements MappingMessageRouterManagement {

	private static final int DEFAULT_DYNAMIC_CHANNEL_LIMIT = 100;

	private int dynamicChannelLimit = DEFAULT_DYNAMIC_CHANNEL_LIMIT;

	@SuppressWarnings("serial")
	private final Map<String, MessageChannel> dynamicChannels =
			Collections.synchronizedMap(
					new LinkedHashMap<>(DEFAULT_DYNAMIC_CHANNEL_LIMIT, 0.75f, true) {

						@Override
						protected boolean removeEldestEntry(Entry<String, MessageChannel> eldest) {
							return size() > AbstractMappingMessageRouter.this.dynamicChannelLimit;
						}

					});

	private String prefix;

	private String suffix;

	private boolean resolutionRequired = true;

	private boolean channelKeyFallback = true;

	private boolean defaultOutputChannelSet;

	private boolean channelKeyFallbackSetExplicitly;

	private volatile Map<String, String> channelMappings = new LinkedHashMap<>();

	/**
	 * Provide mappings from channel keys to channel names.
	 * Channel names will be resolved by the
	 * {@link org.springframework.messaging.core.DestinationResolver}.
	 * @param channelMappings The channel mappings.
	 */
	@Override
	@ManagedAttribute
	public void setChannelMappings(Map<String, String> channelMappings) {
		Assert.notNull(channelMappings, "'channelMappings' must not be null");
		Map<String, String> newChannelMappings = new LinkedHashMap<>(channelMappings);
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
	 * When true (default), if a resolved channel key does not exist in the channel map,
	 * the key itself is used as the channel name, which we will attempt to resolve to a
	 * channel. Set to {@code false} to disable this feature. This could be useful to prevent
	 * malicious actors from generating a message that could cause the message to be
	 * routed to an unexpected channel, such as one upstream of the router, which would
	 * cause a stack overflow.
	 * @param channelKeyFallback false to disable the fallback.
	 * @since 5.2
	 */
	public void setChannelKeyFallback(boolean channelKeyFallback) {
		this.channelKeyFallback = channelKeyFallback;
		this.channelKeyFallbackSetExplicitly = true;
	}

	/**
	 * Set the default channel where Messages should be sent if channel resolution
	 * fails to return any channels.
	 * It also sets {@link #channelKeyFallback} to {@code false} to avoid
	 * an attempt to resolve a channel from its key, but instead send the message
	 * directly to this channel.
	 * If {@link #channelKeyFallback} is set explicitly to {@code true},
	 * the logic depends on the {@link #resolutionRequired} option
	 * ({@code true} by default), and therefore a fallback to
	 * this default output channel may never happen.
	 * The configuration where a default output channel is present and
	 * {@link #setResolutionRequired resolutionRequired} and
	 * {@link #setChannelKeyFallback channelKeyFallback} are set to {@code true}
	 * is rejected since it leads to ambiguity.
	 * @param defaultOutputChannel The default output channel.
	 * @since 6.0
	 * @see #setChannelKeyFallback(boolean)
	 * @see #setResolutionRequired(boolean)
	 */
	@Override
	public void setDefaultOutputChannel(MessageChannel defaultOutputChannel) {
		super.setDefaultOutputChannel(defaultOutputChannel);
		if (!this.channelKeyFallbackSetExplicitly) {
			this.channelKeyFallback = false;
		}
		this.defaultOutputChannelSet = true;
	}

	/**
	 * Set the default channel where Messages should be sent if channel resolution
	 * fails to return any channels.
	 * It also sets {@link #channelKeyFallback} to {@code false} to avoid
	 * an attempt to resolve a channel from its key, but instead send the message
	 * directly to this channel.
	 * If {@link #channelKeyFallback} is set explicitly to {@code true},
	 * the logic depends on the {@link #resolutionRequired} option
	 * ({@code true} by default), and therefore a fallback to
	 * this default output channel may never happen.
	 * The configuration where a default output channel is present and
	 * {@link #setResolutionRequired resolutionRequired} and
	 * {@link #setChannelKeyFallback channelKeyFallback} are set to {@code true}
	 * is rejected since it leads to ambiguity.
	 * @param defaultOutputChannelName the name of the channel bean for default output.
	 * @since 6.0
	 * @see #setChannelKeyFallback(boolean)
	 * @see #setResolutionRequired(boolean)
	 */
	@Override
	public void setDefaultOutputChannelName(String defaultOutputChannelName) {
		super.setDefaultOutputChannelName(defaultOutputChannelName);
		if (!this.channelKeyFallbackSetExplicitly) {
			this.channelKeyFallback = false;
		}
		this.defaultOutputChannelSet = true;
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
		return new LinkedHashMap<>(this.channelMappings);
	}

	/**
	 * Add a channel mapping from the provided key to channel name.
	 * @param key The key.
	 * @param channelName The channel name.
	 */
	@Override
	@ManagedOperation
	public void setChannelMapping(String key, String channelName) {
		Map<String, String> newChannelMappings = new LinkedHashMap<>(this.channelMappings);
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
		Map<String, String> newChannelMappings = new LinkedHashMap<>(this.channelMappings);
		newChannelMappings.remove(key);
		this.channelMappings = newChannelMappings;
	}

	@Override
	@ManagedAttribute
	public Collection<String> getDynamicChannelNames() {
		return Collections.unmodifiableSet(this.dynamicChannels.keySet());
	}

	@Override
	protected void onInit() {
		super.onInit();
		Assert.state(!this.channelKeyFallback || !this.resolutionRequired || !this.defaultOutputChannelSet,
				"The 'defaultOutputChannel' cannot be reached " +
						"when both 'channelKeyFallback' & 'resolutionRequired' are set to true. " +
						"See their javadocs for more information.");
	}

	@Override
	protected Collection<MessageChannel> determineTargetChannels(Message<?> message) {
		Collection<MessageChannel> channels = new ArrayList<>();
		Collection<Object> channelKeys = getChannelKeys(message);
		addToCollection(channels, channelKeys, message);
		return channels;
	}

	/**
	 * Subclasses must implement this method to return the channel keys.
	 * A "key" might be present in this router's "channelMappings", or it
	 * could be the channel's name or even the Message Channel instance itself.
	 * @param message The message.
	 * @return The channel keys.
	 */
	protected abstract List<Object> getChannelKeys(Message<?> message);

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
		Map<String, String> newChannelMappings = new LinkedHashMap<>();
		Set<String> keys = channelMappings.stringPropertyNames();
		for (String key : keys) {
			newChannelMappings.put(key.trim(), channelMappings.getProperty(key).trim());
		}
		doSetChannelMappings(newChannelMappings);
	}

	private void doSetChannelMappings(Map<String, String> newChannelMappings) {
		Map<String, String> oldChannelMappings = this.channelMappings;
		this.channelMappings = newChannelMappings;
		logger.debug(LogMessage.format("Channel mappings: %s replaced with: %s", oldChannelMappings, newChannelMappings));
	}

	private MessageChannel resolveChannelForName(String channelName, Message<?> message) {
		MessageChannel channel = null;
		try {
			channel = getChannelResolver().resolveDestination(channelName);
		}
		catch (DestinationResolutionException ex) {
			if (this.resolutionRequired) {
				throw new MessagingException(message, "Failed to resolve a channel for name '" + channelName + "'", ex);
			}
			else {
				logger.debug(() -> "Failed to resolve a channel for name '" + channelName + "'. Ignored");
			}
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
		String channelName = this.channelKeyFallback ? channelKey : null;
		boolean mapped = false;
		if (this.channelMappings.containsKey(channelKey)) {
			channelName = this.channelMappings.get(channelKey);
			mapped = true;
		}
		if (channelName != null) {
			addChannel(channels, message, channelName, mapped);
		}
	}

	private void addChannel(Collection<MessageChannel> channels, Message<?> message, String channelNameArg,
			boolean mapped) {

		String channelName = channelNameArg;
		if (this.prefix != null) {
			channelName = this.prefix + channelName;
		}
		if (this.suffix != null) {
			channelName = channelName + this.suffix;
		}
		MessageChannel channel = resolveChannelForName(channelName, message);
		if (channel != null) {
			channels.add(channel);
			if (!mapped && this.dynamicChannels.get(channelName) == null) {
				this.dynamicChannels.put(channelName, channel);
			}
		}
	}

	private void addToCollection(Collection<MessageChannel> channels, Collection<?> channelKeys, Message<?> message) {
		if (channelKeys == null) {
			return;
		}
		for (Object channelKey : channelKeys) {
			if (channelKey != null) {
				addChannelKeyToCollection(channels, message, channelKey);
			}
		}
	}

	private void addChannelKeyToCollection(Collection<MessageChannel> channels, Message<?> message, Object channelKey) {
		ConversionService conversionService = getRequiredConversionService();
		if (channelKey instanceof MessageChannel) {
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
		else if (conversionService.canConvert(channelKey.getClass(), String.class)) {
			String converted = conversionService.convert(channelKey, String.class);
			if (converted != null) {
				addChannelFromString(channels, converted, message);
			}
		}
		else {
			throw new MessagingException("Unsupported return type for router [" + channelKey.getClass() + "]");
		}
	}

}
