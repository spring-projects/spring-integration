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

import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageMapper;
import org.springframework.integration.message.SimplePayloadMessageMapper;
import org.springframework.util.Assert;

/**
 * A base class providing common behavior for source adapters.
 * 
 * @author Mark Fisher
 */
public class AbstractSourceAdapter<T> implements SourceAdapter {

	private MessageChannel channel;

	private MessageMapper<?,T> mapper = new SimplePayloadMessageMapper<T>();

	private long sendTimeout = -1;


	public void setChannel(MessageChannel channel) {
		Assert.notNull(channel, "'channel' must not be null");
		this.channel = channel;
	}

	public void setSendTimeout(long sendTimeout) {
		this.sendTimeout = sendTimeout;
	}

	public void setMessageMapper(MessageMapper<?,T> mapper) {
		Assert.notNull(mapper, "'mapper' must not be null");
		this.mapper = mapper;
	}

	protected MessageMapper<?,T> getMessageMapper() {
		return this.mapper;
	}

	protected boolean sendToChannel(T object) {
		Message<?> message = this.mapper.toMessage(object);
		if (this.sendTimeout < 0) {
			return this.channel.send(message);
		}
		return this.channel.send(message, this.sendTimeout);
	}

}
