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

package org.springframework.integration.adapter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageMapper;
import org.springframework.integration.message.SimplePayloadMessageMapper;
import org.springframework.integration.scheduling.PollingSchedule;
import org.springframework.integration.scheduling.Schedule;
import org.springframework.util.Assert;

/**
 * Base class providing common behavior for target adapters.
 * 
 * @author Mark Fisher
 */
public abstract class AbstractTargetAdapter<T> implements TargetAdapter {

	protected Log logger = LogFactory.getLog(this.getClass());

	private String name;

	private MessageChannel channel;

	private MessageMapper<?,T> mapper = new SimplePayloadMessageMapper<T>();

	private Schedule schedule = new PollingSchedule(5);


	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return this.name;
	}

	public void setChannel(MessageChannel channel) {
		Assert.notNull(channel, "'channel' must not be null");
		this.channel = channel;
	}

	public MessageChannel getChannel() {
		return this.channel;
	}

	public void setMessageMapper(MessageMapper<?,T> mapper) {
		Assert.notNull(mapper, "'mapper' must not be null");
		this.mapper = mapper;
	}

	protected MessageMapper<?,T> getMessageMapper() {
		return this.mapper;
	}

	public void setSchedule(Schedule schedule) {
		Assert.notNull(schedule, "'schedule' must not be null");
		this.schedule = schedule;
	}

	public Schedule getSchedule() {
		return this.schedule;
	}

	public final Message handle(Message message) {
		this.sendToTarget(this.mapper.fromMessage(message));
		return null;
	}

	protected abstract boolean sendToTarget(T object);

}
