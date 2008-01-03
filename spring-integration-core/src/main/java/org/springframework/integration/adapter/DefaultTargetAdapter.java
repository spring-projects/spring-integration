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

import org.springframework.integration.bus.ConsumerPolicy;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageMapper;
import org.springframework.integration.message.SimplePayloadMessageMapper;
import org.springframework.util.Assert;

/**
 * Target adapter implementation that delegates to a {@link MessageMapper}
 * and then passes the resulting object to the provided {@link Target}.
 * 
 * @author Mark Fisher
 */
public class DefaultTargetAdapter implements TargetAdapter {

	private String name;

	private MessageChannel channel;

	private Target target;

	private MessageMapper mapper = new SimplePayloadMessageMapper();

	private ConsumerPolicy policy = ConsumerPolicy.newPollingPolicy(5);


	public DefaultTargetAdapter(Target target) {
		Assert.notNull(target, "'target' must not be null");
		this.target = target;
	}

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

	public void setMessageMapper(MessageMapper mapper) {
		Assert.notNull(mapper, "'mapper' must not be null");
		this.mapper = mapper;
	}

	protected MessageMapper getMessageMapper() {
		return this.mapper;
	}

	public void setConsumerPolicy(ConsumerPolicy policy) {
		Assert.notNull(policy, "'policy' must not be null");
		this.policy = policy;
	}

	public ConsumerPolicy getConsumerPolicy() {
		return this.policy;
	}

	public void messageReceived(Message<?> message) {
		this.target.send(this.mapper.fromMessage(message));
	}

}
