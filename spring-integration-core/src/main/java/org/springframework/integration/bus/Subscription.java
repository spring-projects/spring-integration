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

package org.springframework.integration.bus;

import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.scheduling.Schedule;

/**
 * Configuration metadata for activating a subscription.
 * 
 * @author Mark Fisher
 */
public class Subscription {

	private MessageChannel channel;

	private String channelName;

	private Schedule schedule;


	public Subscription() {
	}

	public Subscription(MessageChannel channel) {
		this.channel = channel;
	}

	public Subscription(String channelName) {
		this.channelName = channelName;
	}


	public MessageChannel getChannel() {
		return this.channel;
	}

	public void setChannel(MessageChannel channel) {
		this.channel = channel;
	}

	public String getChannelName() {
		return (this.channel != null) ? this.channel.getName() : this.channelName;
	}

	public void setChannelName(String channelName) {
		this.channelName = channelName;
	}

	public Schedule getSchedule() {
		return this.schedule;
	}

	public void setSchedule(Schedule schedule) {
		this.schedule = schedule;
	}

}
