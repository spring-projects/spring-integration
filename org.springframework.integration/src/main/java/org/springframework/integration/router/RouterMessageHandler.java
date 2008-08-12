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

package org.springframework.integration.router;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.springframework.integration.ConfigurationException;
import org.springframework.integration.annotation.Router;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.integration.message.CompositeMessage;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageBuilder;
import org.springframework.integration.message.MessageTarget;

/**
 * MessageHandler adapter for methods annotated with {@link Router @Router}.
 * 
 * @author Mark Fisher
 */
public class RouterMessageHandler extends AbstractMessageHandler {

	private volatile MessageChannel defaultChannel;


	public RouterMessageHandler(Object object, Method method) {
		super(object, method);
	}

	public RouterMessageHandler(Object object, String methodName) {
		super(object, methodName);
	}

	public RouterMessageHandler() {
	}


	public void setDefaultChannel(MessageChannel defaultChannel) {
		this.defaultChannel = defaultChannel;
	}

	@Override
	protected Message<?> createReplyMessage(Object result, Message<?> requestMessage) {
		final List<Object> channels = new ArrayList<Object>();
		if (result != null) {
			if (result instanceof Collection) {
				channels.addAll((Collection<?>) result);
			}
			else if (result instanceof MessageChannel[]) {
				channels.addAll(Arrays.asList((MessageChannel[]) result));
			}
			else if (result instanceof String[]) {
				channels.addAll(Arrays.asList((String[]) result));
			}
			else if (result instanceof MessageChannel) {
				channels.add((MessageChannel) result);
			}
			else if (result instanceof String) {
				channels.add(result);
			}
			else {
				throw new ConfigurationException(
						"router method must return type 'MessageChannel' or 'String'");
			}
		}
		if (channels.size() == 0) {
			if (this.defaultChannel != null) {
				return MessageBuilder.fromMessage(requestMessage).setNextTarget(this.defaultChannel).build();
			}
			return null;
		}
		List<Message<?>> replies = new ArrayList<Message<?>>();
		for (Object channel : channels) {
			MessageBuilder<?> builder = MessageBuilder.fromMessage(requestMessage);
			if (channel instanceof MessageTarget) {
				builder.setNextTarget((MessageTarget) channel);
			}
			else if (channel instanceof String) {
				builder.setNextTarget((String) channel);
			}
			replies.add(builder.build());
		}
		return new CompositeMessage(replies);
	}

}
