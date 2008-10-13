/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.integration.endpoint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageBuilder;

/**
 * @author Mark Fisher
 */
public class ReplyHolder {

	private final List<MessageBuilder<?>> builders = new ArrayList<MessageBuilder<?>>();

	private volatile Object targetChannel;


	public MessageBuilder<?> set(Object replyObject) {
		return this.createAndAddBuilder(replyObject, true);
	}

	public MessageBuilder<?> add(Object replyObject) {
		return this.createAndAddBuilder(replyObject, false);
	}

	public void setTargetChannel(MessageChannel targetChannel) {
		this.targetChannel = targetChannel;
	}

	public void setTargetChannelName(String targetChannelName) {
		this.targetChannel = targetChannelName;
	}

	protected Object getTargetChannel() {
		return this.targetChannel;
	}

	public boolean isEmpty() {
		return this.builders.isEmpty();
	}

	public List<MessageBuilder<?>> builders() {
		return Collections.unmodifiableList(this.builders);
	}

	private MessageBuilder<?> createAndAddBuilder(Object replyObject, boolean clearExistingValues) {
		MessageBuilder<?> builder = null;
		if (replyObject instanceof MessageBuilder) {
			builder = (MessageBuilder<?>) replyObject;
		}
		else if (replyObject instanceof Message) {
			builder = MessageBuilder.fromMessage((Message<?>) replyObject);
		}
		else {
			builder = MessageBuilder.withPayload(replyObject);
		}
		synchronized (this.builders) {
			if (clearExistingValues) {
				this.builders.clear();
			}
			this.builders.add(builder);
		}
		return builder;
	}

}
