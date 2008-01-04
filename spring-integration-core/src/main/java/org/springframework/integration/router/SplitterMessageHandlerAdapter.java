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

package org.springframework.integration.router;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;

import org.springframework.integration.MessagingConfigurationException;
import org.springframework.integration.annotation.Splitter;
import org.springframework.integration.channel.ChannelRegistry;
import org.springframework.integration.channel.ChannelRegistryAware;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.handler.AbstractMessageHandlerAdapter;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.message.Message;
import org.springframework.integration.util.SimpleMethodInvoker;

/**
 * MessageHandler adapter for methods annotated with {@link Splitter @Splitter}.
 * 
 * @author Mark Fisher
 */
public class SplitterMessageHandlerAdapter extends AbstractMessageHandlerAdapter implements ChannelRegistryAware {

	private static final String CHANNEL_KEY = "channel";

	private Map<String, ?> attributes;

	private Method method;

	private ChannelRegistry channelRegistry;


	public SplitterMessageHandlerAdapter(Object object, Method method, Map<String, ?> attributes) {
		this.setObject(object);
		this.method = method;
		this.attributes = attributes;
	}

	public void setChannelRegistry(ChannelRegistry channelRegistry) {
		this.channelRegistry = channelRegistry;
	}

	@Override
	protected Object doHandle(Message message, SimpleMethodInvoker invoker) {
		if (method.getParameterTypes().length != 1) {
			throw new MessagingConfigurationException(
					"method must accept exactly one parameter");
		}
		String channelName = (String) attributes.get(CHANNEL_KEY);
		Object retval = null;
		Class<?> type = method.getParameterTypes()[0];
		if (type.equals(Message.class)) {
			retval = invoker.invokeMethod(message);
		}
		else if (type.equals(message.getPayload().getClass())) {
			retval = invoker.invokeMethod(message.getPayload());
		}
		else {
			if (logger.isWarnEnabled()) {
				logger.warn("Message payload type did not match expected type of Splitter method '" + this.method.getName() + "'");
			}
			return message;
		}
		if (retval == null) {
			if (logger.isWarnEnabled()) {
				logger.warn("Splitter method '" + this.method.getName() + "' returned null");
			}
			return null;
		}
		if (retval instanceof Collection) {
			Collection<?> items = (Collection<?>) retval;
			int counter = 0;
			for (Object item : items) {
				if (item instanceof Message) {
					Message splitMessage = (Message) item;
					splitMessage.getHeader().setCorrelationId(message.getId());
					this.sendMessage(splitMessage, channelName);
				}
				else {
					Message splitMessage = new GenericMessage(message.getId() + "#" + (counter++), item);
					splitMessage.getHeader().setCorrelationId(message.getId());
					this.sendMessage(splitMessage, channelName);
				}
			}
		}
		else if (retval.getClass().isArray()) {
			int counter = 0;
			for (Object item : (Object[]) retval) {
				if (item instanceof Message) {
					Message splitMessage = (Message) item;
					splitMessage.getHeader().setCorrelationId(message.getId());
					this.sendMessage(splitMessage, channelName);
				}
				else {
					Message splitMessage = new GenericMessage(message.getId() + "#" + (counter++), item);
					splitMessage.getHeader().setCorrelationId(message.getId());
					this.sendMessage(splitMessage, channelName);
				}
			}
		}
		else {
			throw new MessagingConfigurationException(
					"splitter method must return either a Collection or array");
		}
		return null;
	}

	private boolean sendMessage(Message<?> message, String channelName) {
		MessageChannel channel = this.channelRegistry.lookupChannel(channelName);
		if (channel == null) {
			if (logger.isWarnEnabled()) {
				logger.warn("unable to resolve channel for name '" + channelName + "'");
			}
			return false;
		}
		if (logger.isDebugEnabled()) {
			logger.debug("sending message to channel '" + channelName + "'");
		}
		return channel.send(message);
	}

}
