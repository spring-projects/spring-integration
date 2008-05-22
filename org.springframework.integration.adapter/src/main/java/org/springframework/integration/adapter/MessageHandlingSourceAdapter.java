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
import org.springframework.integration.ConfigurationException;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.gateway.RequestReplyTemplate;
import org.springframework.integration.handler.MessageHandler;
import org.springframework.integration.message.Message;
import org.springframework.util.Assert;

/**
 * A source adapter that implements the {@link MessageHandler} interface. It may
 * be used as a base class for source adapters with request-reply behavior.
 * 
 * @author Mark Fisher
 */
public class MessageHandlingSourceAdapter implements MessageHandler, InitializingBean {

	private final Log logger = LogFactory.getLog(this.getClass());

	private final MessageChannel requestChannel;

	private final RequestReplyTemplate requestReplyTemplate = new RequestReplyTemplate();

	private volatile boolean expectReply = true;

	protected final Object lifecycleMonitor = new Object();

	private volatile boolean initialized;


	/**
	 * Create an adapter that sends to the provided channel.
	 * 
	 * @param requestChannel the channel where messages will be sent, must not be
	 * <code>null</code>.
	 */
	public MessageHandlingSourceAdapter(MessageChannel requestChannel) {
		Assert.notNull(requestChannel, "request channel must not be null");
		this.requestChannel = requestChannel;
		this.requestReplyTemplate.setRequestChannel(requestChannel);
	}


	/**
	 * Specify whether the handle method should be expected to return a reply.
	 * The default is '<code>true</code>'.
	 */
	public void setExpectReply(boolean expectReply) {
		this.expectReply = expectReply;
	}

	public void setRequestTimeout(long requestTimeout) {
		this.requestReplyTemplate.setRequestTimeout(requestTimeout);
	}

	public void setReplyTimeout(long replyTimeout) {
		this.requestReplyTemplate.setReplyTimeout(replyTimeout);
	}

	protected MessageChannel getChannel() {
		return this.requestChannel;
	}

	public final void afterPropertiesSet() throws Exception {
		synchronized (this.lifecycleMonitor) {
			if (this.initialized) {
				return;
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

	public final Message<?> handle(Message<?> message) {
		if (!this.initialized) {
			try {
				this.afterPropertiesSet();
			}
			catch (Exception e) {
				throw new ConfigurationException("unable to initialize " + this.getClass().getName(), e);
			}
		}
		if (!this.expectReply) {
			boolean sent = this.requestReplyTemplate.send(message);
			if (!sent && logger.isWarnEnabled()) {
				logger.warn("failed to send message to channel within timeout");
			}
			return null;
		}
		return this.requestReplyTemplate.request(message);
	}

}
