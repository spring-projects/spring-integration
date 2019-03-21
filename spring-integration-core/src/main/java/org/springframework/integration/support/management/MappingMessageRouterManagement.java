/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.integration.support.management;

import java.util.Collection;
import java.util.Map;
import java.util.Properties;

import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;

/**
 * Exposes channel mapping operations when the router is proxied.
 * {@link #setChannelMappings(Map)} is also exposed. This cannot
 * be used with a control-bus, but it can be used programmatically
 * or over JMX.
 *
 * @author Gary Russell
 * @since 2.1
 *
 */
public interface MappingMessageRouterManagement {

	/**
	 * Add a channel mapping from the provided key to channel name.
	 * @param key The key.
	 * @param channelName The channel name.
	 */
	@ManagedOperation
	void setChannelMapping(String key, String channelName);

	/**
	 * Remove a channel mapping for the given key if present.
	 * @param key The key.
	 */
	@ManagedOperation
	void removeChannelMapping(String key);

	/**
	 * Provide mappings from channel keys to channel names.
	 * @param channelMappings The channel mappings.
	 * @since 4.0
	 */
	@ManagedOperation
	void replaceChannelMappings(Properties channelMappings);

	/**
	 * @return an unmodifiable map of channel mappings.
	 * @since 4.0
	 */
	@ManagedAttribute
	Map<String, String> getChannelMappings();

	/**
	 * Provide mappings from channel keys to channel names.
	 * Channel names will be resolved by the
	 * {@link org.springframework.messaging.core.DestinationResolver}.
	 * @param channelMappings The channel mappings.
	 * @since 4.0
	 */
	@ManagedAttribute
	void setChannelMappings(Map<String, String> channelMappings);

	/**
	 * Provide a collection of channel names to which
	 * we have routed messages where the channel was not explicitly mapped.
	 * <p> Implementations may choose to return only the most recent channel names.
	 * @return a collection of channel names to which
	 * we have routed messages where the channel was not explicitly mapped.
	 * @since 4.3
	 */
	@ManagedAttribute
	Collection<String> getDynamicChannelNames();

}
