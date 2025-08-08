/*
 * Copyright © 2013 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2013-present the original author or authors.
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
