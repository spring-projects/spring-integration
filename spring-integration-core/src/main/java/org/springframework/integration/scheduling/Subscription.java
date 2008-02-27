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

package org.springframework.integration.scheduling;

import org.springframework.integration.channel.MessageChannel;
import org.springframework.util.Assert;

/**
 * Configuration metadata for activating a subscription. Immutable.
 * 
 * @author Mark Fisher
 */
public class Subscription {

	private final MessageChannel channel;

	private final String channelName;

	private final Schedule schedule;


	public Subscription(MessageChannel channel) {
		this(channel, null);
	}

	public Subscription(String channelName) {
		this(channelName, null);
	}

	public Subscription(MessageChannel channel, Schedule schedule) {
		Assert.notNull(channel, "'channel' must not be null");
		this.channel = channel;
		this.schedule = schedule;
		this.channelName = this.channel.getName();
	}

	public Subscription(String channelName, Schedule schedule) {
		Assert.notNull(channelName, "'channelName' must not be null");
		this.channelName = channelName;
		this.schedule = schedule;
		this.channel = null;
	}


	public MessageChannel getChannel() {
		return this.channel;
	}

	public String getChannelName() {
		return (this.channel != null) ? this.channel.getName() : this.channelName;
	}

	public Schedule getSchedule() {
		return this.schedule;
	}

}
