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
import org.springframework.integration.annotation.Splitter;
import org.springframework.integration.channel.ChannelRegistry;
import org.springframework.integration.channel.ChannelRegistryAware;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.handler.AbstractMessageHandlerAdapter;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageBuilder;

/**
 * MessageHandler adapter for methods annotated with {@link Splitter @Splitter}.
 * 
 * @author Mark Fisher
 * @author Marius Bogoevici
 */
public class SplitterMessageHandlerAdapter extends AbstractMessageHandlerAdapter implements ChannelRegistryAware {

	private volatile String outputChannelName;

	private volatile long sendTimeout = -1;


	public SplitterMessageHandlerAdapter(Object object, Method method) {
		this.setObject(object);
		this.setMethod(method);
		if (method.getParameterTypes().length < 1) {
			throw new ConfigurationException("The splitter method must accept at least one argument.");
		}
		if (method.getParameterTypes()[0].equals(Message.class)) {
			this.setMethodExpectsMessage(true);
		}
	}

	public SplitterMessageHandlerAdapter(Object object, String methodName) {
		this.setObject(object);
		this.setMethodName(methodName);
	}

	public SplitterMessageHandlerAdapter() {
	}


	public void setOutputChannelName(String outputChannelName) {
		this.outputChannelName = outputChannelName;
	}

	public void setSendTimeout(long sendTimeout) {
		this.sendTimeout = sendTimeout;
	}

	@Override
	protected final Message<?> handleReturnValue(Object returnValue, Message<?> originalMessage) {
		if (returnValue == null) {
			if (logger.isWarnEnabled()) {
				logger.warn("Splitter method returned null.");
			}
			return null;
		}
		if (returnValue instanceof Collection) {
			Collection<?> items = (Collection<?>) returnValue;
			int sequenceNumber = 0;
			int sequenceSize = items.size();
			for (Object item : items) {
				Message<?> splitMessage = (item instanceof Message<?>) ?
						(Message<?>) item : this.createReplyMessage(item, originalMessage);
				splitMessage = MessageBuilder.fromMessage(splitMessage)
						.setCorrelationId(originalMessage.getHeaders().getId())
						.setSequenceNumber(++sequenceNumber)
						.setSequenceSize(sequenceSize).build();
				this.sendMessage(splitMessage, this.outputChannelName);
			}
		}
		else if (returnValue.getClass().isArray()) {
			Object[] array = (Object[]) returnValue;
			int sequenceNumber = 0;
			int sequenceSize = array.length;
			for (Object item : array) {
				Message<?> splitMessage = (item instanceof Message<?>) ?
						(Message<?>) item : this.createReplyMessage(item, originalMessage);
				splitMessage = MessageBuilder.fromMessage(splitMessage)
						.setCorrelationId(originalMessage.getHeaders().getId())
						.setSequenceNumber(++sequenceNumber)
						.setSequenceSize(sequenceSize).build();	
				this.sendMessage(splitMessage, this.outputChannelName);
			}
		}
		else {
			throw new ConfigurationException(
					"splitter method must return either a Collection or array");
		}
		return null;
	}

	private boolean sendMessage(Message<?> message, String channelName) {
		ChannelRegistry channelRegistry = this.getChannelRegistry();
		if (channelRegistry == null) {
			throw new IllegalStateException(this.getClass().getSimpleName() + " requires a ChannelRegistry reference.");
		}
		MessageChannel channel = channelRegistry.lookupChannel(channelName);
		if (channel == null) {
			if (logger.isWarnEnabled()) {
				logger.warn("unable to resolve channel for name '" + channelName + "'");
			}
			return false;
		}
		if (logger.isDebugEnabled()) {
			logger.debug("sending message to channel '" + channelName + "'");
		}
		return (this.sendTimeout < 0) ? channel.send(message) : channel.send(message, this.sendTimeout);
	}

}
