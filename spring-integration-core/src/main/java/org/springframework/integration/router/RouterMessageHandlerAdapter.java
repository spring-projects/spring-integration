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
import java.util.Collection;

import org.springframework.integration.ConfigurationException;
import org.springframework.integration.annotation.Router;
import org.springframework.integration.channel.ChannelRegistry;
import org.springframework.integration.channel.ChannelRegistryAware;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.handler.AbstractMessageHandlerAdapter;
import org.springframework.integration.handler.annotation.AnnotationMethodMessageMapper;
import org.springframework.integration.message.Message;

/**
 * MessageHandler adapter for methods annotated with {@link Router @Router}.
 * 
 * @author Mark Fisher
 */
public class RouterMessageHandlerAdapter extends AbstractMessageHandlerAdapter implements ChannelRegistryAware {

	private volatile ChannelRegistry channelRegistry;


	public RouterMessageHandlerAdapter(Object object, Method method) {
		this.setObject(object);
		this.setMethod(method);
		if (method.getParameterTypes().length < 1) {
			throw new ConfigurationException("The router method must accept at least one argument.");
		}
		if (method.getParameterTypes()[0].equals(Message.class)) {
			this.setMethodExpectsMessage(true);
		}
	}

	public RouterMessageHandlerAdapter(Object object, String methodName) {
		this.setObject(object);
		this.setMethodName(methodName);
	}


	public void setChannelRegistry(ChannelRegistry channelRegistry) {
		this.channelRegistry = channelRegistry;
	}

	@Override
	protected void initialize() {
		Object target = this.getObject();
		if (target != null && this.channelRegistry != null && (target instanceof ChannelRegistryAware)) {
			((ChannelRegistryAware) target).setChannelRegistry(this.channelRegistry);
		}
		Method method = this.getMethod();
		if (method != null) {
			this.setMessageMapper(new AnnotationMethodMessageMapper(method));
		}
	}

	@Override
	protected Message<?> handleReturnValue(Object returnValue, Message<?> originalMessage) {
		if (returnValue != null) {
			if (returnValue instanceof Collection) {
				Collection<?> channels = (Collection<?>) returnValue;
				for (Object channel : channels) {
					if (channel instanceof MessageChannel) {
						this.sendMessage(originalMessage, (MessageChannel) channel);
					}
					else if (channel instanceof String) {
						this.sendMessage(originalMessage, (String) channel);
					}
					else {
						throw new ConfigurationException(
								"router method must return type 'MessageChannel' or 'String'");
					}
				}
			}
			else if (returnValue instanceof MessageChannel[]) {
				for (MessageChannel channel : (MessageChannel[]) returnValue) {
					this.sendMessage(originalMessage, channel);
				}
			}
			else if (returnValue instanceof String[]) {
				for (String channelName : (String[]) returnValue) {
					this.sendMessage(originalMessage, channelName);
				}
			}
			else if (returnValue instanceof MessageChannel) {
				this.sendMessage(originalMessage, (MessageChannel) returnValue);
			}
			else if (returnValue instanceof String) {
				this.sendMessage(originalMessage, (String) returnValue);
			}
			else {
				throw new ConfigurationException(
						"router method must return type 'MessageChannel' or 'String'");
			}
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
		return this.sendMessage(message, channel);
	}

	private boolean sendMessage(Message<?> message, MessageChannel channel) {
		if (logger.isDebugEnabled()) {
			logger.debug("sending message to channel '" + channel + "'");
		}
		return channel.send(message);
	}

}
