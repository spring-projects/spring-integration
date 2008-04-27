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
import java.util.Map;

import org.springframework.integration.ConfigurationException;
import org.springframework.integration.annotation.Splitter;
import org.springframework.integration.channel.ChannelRegistry;
import org.springframework.integration.channel.ChannelRegistryAware;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.handler.AbstractMessageHandlerAdapter;
import org.springframework.integration.handler.HandlerMethodInvoker;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageHeader;
import org.springframework.util.Assert;

/**
 * MessageHandler adapter for methods annotated with {@link Splitter @Splitter}.
 * 
 * @author Mark Fisher
 * @author Marius Bogoevici
 */
public class SplitterMessageHandlerAdapter<T> extends AbstractMessageHandlerAdapter<T> implements ChannelRegistryAware {

	private final Method method;

	private final String outputChannelName;

	private volatile ChannelRegistry channelRegistry;

	private volatile long sendTimeout = -1;


	public SplitterMessageHandlerAdapter(T object, Method method, Map<String, ?> attributes) {
		Assert.notNull(object, "'object' must not be null");
		Assert.notNull(method, "'method' must not be null");
		Assert.isTrue(attributes != null && attributes.get(OUTPUT_CHANNEL_NAME_KEY) != null,
				"The '" + OUTPUT_CHANNEL_NAME_KEY + "' attribute is required.");
		this.setObject(object);
		this.setMethodName(method.getName());
		this.method = method;
		this.outputChannelName = (String) attributes.get(OUTPUT_CHANNEL_NAME_KEY);
	}

	public void setChannelRegistry(ChannelRegistry channelRegistry) {
		this.channelRegistry = channelRegistry;
	}

	public void setSendTimeout(long sendTimeout) {
		this.sendTimeout = sendTimeout;
	}

	@Override
	protected final Object doHandle(Message<?> message, HandlerMethodInvoker<T> invoker) {
		final MessageHeader originalMessageHeader = message.getHeader();
		if (method.getParameterTypes().length != 1) {
			throw new ConfigurationException(
					"Splitter method must accept exactly one parameter");
		}
		Object retval = null;
		Class<?> type = method.getParameterTypes()[0];
		if (type.equals(Message.class)) {
			retval = invoker.invokeMethod(message);
		}
		else {
			retval = invoker.invokeMethod(message.getPayload());
		}
		if (retval == null) {
			if (logger.isWarnEnabled()) {
				logger.warn("Splitter method '" + this.method.getName() + "' returned null");
			}
			return null;
		}
		if (retval instanceof Collection) {
			Collection<?> items = (Collection<?>) retval;
			int sequenceNumber = 0;
			int sequenceSize = items.size();
			for (Object item : items) {
				Message<?> splitMessage = (item instanceof Message<?>) ? (Message<?>) item :
						this.createReplyMessage(item, originalMessageHeader);
				this.prepareMessage(splitMessage, message.getId(), ++sequenceNumber, sequenceSize);
				this.sendMessage(splitMessage, this.outputChannelName);
			}
		}
		else if (retval.getClass().isArray()) {
			Object[] array = (Object[]) retval;
			int sequenceNumber = 0;
			int sequenceSize = array.length;
			for (Object item : array) {
				Message<?> splitMessage = (item instanceof Message<?>) ? (Message<?>) item :
						this.createReplyMessage(item, originalMessageHeader);
				this.prepareMessage(splitMessage, message.getId(), ++sequenceNumber, sequenceSize);
				this.sendMessage(splitMessage, this.outputChannelName);
			}
		}
		else {
			throw new ConfigurationException(
					"splitter method must return either a Collection or array");
		}
		return null;
	}

	private void prepareMessage(Message<?> message, Object correlationId, int sequenceNumber, int sequenceSize) {
		message.getHeader().setCorrelationId(correlationId);
		message.getHeader().setSequenceNumber(sequenceNumber);
		message.getHeader().setSequenceSize(sequenceSize);
	}

	private boolean sendMessage(Message<?> message, String channelName) {
		if (this.channelRegistry == null) {
			throw new IllegalStateException(this.getClass().getSimpleName() + " requires a ChannelRegistry reference.");
		}
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
		return (this.sendTimeout < 0) ? channel.send(message) : channel.send(message, this.sendTimeout);
	}

}
