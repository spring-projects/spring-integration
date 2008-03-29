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

package org.springframework.integration.adapter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.integration.MessagingConfigurationException;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.channel.RequestReplyTemplate;
import org.springframework.integration.handler.MessageHandler;
import org.springframework.integration.message.Message;
import org.springframework.util.Assert;

/**
 * A source adapter that implements the {@link MessageHandler} interface. It may
 * be used as a base class for source adapters with request-reply behavior.
 * 
 * @author Mark Fisher
 */
public class MessageHandlingSourceAdapter implements SourceAdapter, MessageHandler, InitializingBean {

	private final Log logger = LogFactory.getLog(this.getClass());

	private volatile MessageChannel channel;

	private volatile RequestReplyTemplate requestReplyTemplate;

	private volatile boolean expectReply = true;

	private volatile long sendTimeout = -1;

	private volatile long receiveTimeout = -1;

	protected final Object lifecycleMonitor = new Object();

	private volatile boolean initialized;


	/**
	 * Create an adapter that sends to the provided channel.
	 * 
	 * @param channel the channel where messages will be sent, must not be
	 * <code>null</code>.
	 */
	public MessageHandlingSourceAdapter(MessageChannel channel) {
		Assert.notNull(channel, "'channel' must not be null");
		this.channel = channel;
	}

	/**
	 * No-arg constructor for configuration via setters. Note that upon
	 * initialization, this adapter will throw an exception if a
	 * {@link MessageChannel} has not been provided.
	 * 
	 * @see #setChannel(MessageChannel)
	 */
	public MessageHandlingSourceAdapter() {
	}


	public void setChannel(MessageChannel channel) {
		this.channel = channel;
	}

	protected MessageChannel getChannel() {
		return this.channel;
	}

	/**
	 * Specify whether the handle method should be expected to return a reply.
	 * The default is '<code>true</code>'.
	 */
	public void setExpectReply(boolean expectReply) {
		this.expectReply = expectReply;
	}

	public void setSendTimeout(long sendTimeout) {
		this.sendTimeout = sendTimeout;
	}

	public void setReceiveTimeout(long receiveTimeout) {
		this.receiveTimeout = receiveTimeout;
	}

	public final void afterPropertiesSet() throws Exception {
		if (this.channel == null) {
			throw new MessagingConfigurationException("The 'channel' property of '" + this.getClass().getName()
					+ "' must not be null.");
		}
		synchronized (this.lifecycleMonitor) {
			if (this.initialized) {
				return;
			}
			if (this.requestReplyTemplate == null) {
				this.requestReplyTemplate = this.createRequestReplyTemplate();
			}
		}
		this.initialize();
		this.initialized = true;
	}

	/**
	 * Subclasses may override this method for initialization.
	 */
	protected void initialize() throws Exception {
	}

	private RequestReplyTemplate createRequestReplyTemplate() {
		RequestReplyTemplate template = new RequestReplyTemplate(this.channel);
		template.setDefaultSendTimeout(this.sendTimeout);
		template.setDefaultReceiveTimeout(this.receiveTimeout);
		return template;
	}

	public final Message<?> handle(Message<?> message) {
		if (!this.initialized) {
			try {
				this.afterPropertiesSet();
			}
			catch (Exception e) {
				throw new MessagingConfigurationException("unable to initialize " + this.getClass().getName(), e);
			}
		}
		if (!this.expectReply) {
			if (!this.channel.send(message, this.sendTimeout) && logger.isWarnEnabled()) {
				logger.warn("failed to send message to channel '" + this.channel + "' within timeout of "
						+ this.sendTimeout + " milliseconds");
			}
			return null;
		}
		return this.requestReplyTemplate.request(message);
	}

}
