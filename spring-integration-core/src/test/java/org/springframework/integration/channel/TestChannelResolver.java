/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.channel;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.core.DestinationResolver;
import org.springframework.util.Assert;

/**
 * @author Mark Fisher
 */
public class TestChannelResolver implements DestinationResolver<MessageChannel> {

	private volatile Map<String, MessageChannel> channels = new ConcurrentHashMap<String, MessageChannel>();

	public MessageChannel resolveDestination(String channelName) {
		return this.channels.get(channelName);
	}

	@Autowired
	public void setChannels(Map<String, MessageChannel> channels) {
		this.channels = channels;
	}

	public void addChannel(String name, MessageChannel channel) {
		Assert.notNull(name, "name must not be null");
		Assert.notNull(channel, "channel must not be null");
		this.channels.put(name, channel);
	}

}
