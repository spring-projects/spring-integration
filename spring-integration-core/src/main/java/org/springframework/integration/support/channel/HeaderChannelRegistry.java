/*
 * Copyright 2013-2014 the original author or authors.
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

package org.springframework.integration.support.channel;

import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.messaging.MessageChannel;

/**
 * Implementations convert a channel to a name, retaining a reference to the channel keyed by the name.
 * Allows a downstream {@link BeanFactoryChannelResolver} to find the channel by name in
 * the event that the flow serialized the message at some point.
 *
 * @author Gary Russell
 * @since 3.0
 *
 */
public interface HeaderChannelRegistry {

	/**
	 * Converts the channel to a name (String). If the channel is not a
	 * {@link MessageChannel}, it is returned unchanged.
	 * @param channel The channel.
	 * @return The channel name, or the channel if it is not a MessageChannel.
	 */
	Object channelToChannelName(Object channel);

	/**
	 * Converts the channel to a name (String). If the channel is not a
	 * {@link MessageChannel}, it is returned unchanged.
	 * @param channel The channel.
	 * @param timeToLive How long (ms) at a minimum, the channel mapping should
	 * remain in the registry.
	 * @return The channel name, or the channel if it is not a MessageChannel.
	 * @since 4.1
	 */
	Object channelToChannelName(Object channel, long timeToLive);

	/**
	 * Converts the channel name back to a {@link MessageChannel} (if it is
	 * registered).
	 * @param name The name of the channel.
	 * @return The channel, or null if there is no channel registered with the name.
	 */
	MessageChannel channelNameToChannel(String name);

	/**
	 * @return the current size of the registry
	 */
	@ManagedAttribute
	int size();

	/**
	 * Cancel the scheduled reap task and run immediately; then reschedule.
	 */
	@ManagedOperation(description = "Cancel the scheduled reap task and run immediately; then reschedule.")
	void runReaper();

}
