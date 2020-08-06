/*
 * Copyright 2002-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.channel.interceptor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.integration.core.MessageSelector;
import org.springframework.integration.support.channel.ChannelResolverUtils;
import org.springframework.integration.support.management.ManageableLifecycle;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.InterceptableChannel;
import org.springframework.util.Assert;

/**
 * A {@link ChannelInterceptor} that publishes a copy of the intercepted message
 * to a secondary target while still sending the original message to the main channel.
 *
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 */
@ManagedResource
public class WireTap implements ChannelInterceptor, ManageableLifecycle, VetoCapableInterceptor, BeanFactoryAware {

	private static final Log LOGGER = LogFactory.getLog(WireTap.class);

	private final MessageSelector selector;

	private MessageChannel channel;

	private String channelName;

	private long timeout = 0;

	private BeanFactory beanFactory;

	private volatile boolean running = true;


	/**
	 * Create a new wire tap with <em>no</em> {@link MessageSelector}.
	 * @param channel the MessageChannel to which intercepted messages will be sent
	 */
	public WireTap(MessageChannel channel) {
		this(channel, null);
	}

	/**
	 * Create a new wire tap with the provided {@link MessageSelector}.
	 * @param channel the channel to which intercepted messages will be sent
	 * @param selector the selector that must accept a message for it to be
	 * sent to the intercepting channel
	 */
	public WireTap(MessageChannel channel, MessageSelector selector) {
		Assert.notNull(channel, "channel must not be null");
		this.channel = channel;
		this.selector = selector;
	}

	/**
	 * Create a new wire tap based on the MessageChannel name and
	 * with <em>no</em> {@link MessageSelector}.
	 * @param channelName the name of the target MessageChannel
	 * to which intercepted messages will be sent
	 * @since 4.3
	 */
	public WireTap(String channelName) {
		this(channelName, null);
	}

	/**
	 * Create a new wire tap with the provided {@link MessageSelector}.
	 * @param channelName the name of the target MessageChannel
	 * to which intercepted messages will be sent.
	 * @param selector the selector that must accept a message for it to be
	 * sent to the intercepting channel
	 * @since 4.3
	 */
	public WireTap(String channelName, MessageSelector selector) {
		Assert.hasText(channelName, "channelName must not be empty");
		this.channelName = channelName;
		this.selector = selector;
	}

	/**
	 * Specify the timeout value for sending to the intercepting target.
	 * @param timeout the timeout in milliseconds
	 */
	public void setTimeout(long timeout) {
		this.timeout = timeout;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		if (this.beanFactory == null) {
			this.beanFactory = beanFactory;
		}
	}

	/**
	 * Check whether the wire tap is currently running.
	 */
	@Override
	@ManagedAttribute
	public boolean isRunning() {
		return this.running;
	}

	/**
	 * Restart the wire tap if it has been stopped. It is running by default.
	 */
	@Override
	@ManagedOperation
	public void start() {
		this.running = true;
	}

	/**
	 * Stop the wire tap. To restart, invoke {@link #start()}.
	 */
	@Override
	@ManagedOperation
	public void stop() {
		this.running = false;
	}

	/**
	 * Intercept the Message and, <em>if accepted</em> by the {@link MessageSelector},
	 * send it to the secondary target. If this wire tap's {@link MessageSelector} is
	 * <code>null</code>, it will accept all messages.
	 */
	@Override
	public Message<?> preSend(Message<?> message, MessageChannel channel) {
		MessageChannel wireTapChannel = getChannel();
		if (wireTapChannel.equals(channel)) {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("WireTap is refusing to intercept its own channel '" + wireTapChannel + "'");
			}
			return message;
		}
		if (this.running && (this.selector == null || this.selector.accept(message))) {
			boolean sent = (this.timeout >= 0)
					? wireTapChannel.send(message, this.timeout)
					: wireTapChannel.send(message);
			if (!sent && LOGGER.isWarnEnabled()) {
				LOGGER.warn("failed to send message to WireTap channel '" + wireTapChannel + "'");
			}
		}
		return message;
	}

	@Override
	public boolean shouldIntercept(String beanName, InterceptableChannel channel) {

		return !getChannel().equals(channel);
	}

	private MessageChannel getChannel() {
		String channelNameToUse = this.channelName;
		if (channelNameToUse != null) {
			this.channel =
					ChannelResolverUtils.getChannelResolver(this.beanFactory)
							.resolveDestination(channelNameToUse);
			this.channelName = null;
		}
		return this.channel;
	}

}
