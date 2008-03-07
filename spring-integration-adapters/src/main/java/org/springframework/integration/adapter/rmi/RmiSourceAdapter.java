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

package org.springframework.integration.adapter.rmi;

import java.rmi.RemoteException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.integration.MessagingConfigurationException;
import org.springframework.integration.adapter.SourceAdapter;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.channel.RequestReplyTemplate;
import org.springframework.integration.handler.MessageHandler;
import org.springframework.integration.message.Message;
import org.springframework.remoting.rmi.RmiServiceExporter;

/**
 * A source channel adapter for RMI-based remoting.
 * 
 * @author Mark Fisher
 */
public class RmiSourceAdapter implements SourceAdapter, MessageHandler, InitializingBean {

	public static final String SERVICE_NAME_PREFIX = "internal.rmiSourceAdapter.";


	private final Log logger = LogFactory.getLog(this.getClass());

	private volatile MessageChannel channel;

	private volatile RequestReplyTemplate requestReplyTemplate;

	private volatile boolean expectReply = true;

	private volatile long sendTimeout = -1;

	private volatile long receiveTimeout = -1;


	public void setChannel(MessageChannel channel) {
		this.channel = channel;
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

	public void afterPropertiesSet() throws RemoteException {
		String channelName = this.channel.getName();
		if (channelName == null) {
			throw new MessagingConfigurationException("RmiSourceAdapter's MessageChannel must have a 'name'");
		}
		this.requestReplyTemplate = new RequestReplyTemplate(this.channel);
		this.requestReplyTemplate.setDefaultSendTimeout(this.sendTimeout);
		this.requestReplyTemplate.setDefaultReceiveTimeout(this.receiveTimeout);
		RmiServiceExporter exporter = new RmiServiceExporter();
		exporter.setService(this);
		exporter.setServiceInterface(MessageHandler.class);
		exporter.setServiceName(SERVICE_NAME_PREFIX + channelName);
		exporter.afterPropertiesSet();
	}

	public Message<?> handle(Message<?> message) {
		if (this.requestReplyTemplate == null) {
			try {
				this.afterPropertiesSet();
			}
			catch (RemoteException e) {
				throw new MessagingConfigurationException("unable to initialize RmiSourceAdapter", e);
			}
		}
		if (!this.expectReply) {
			if (!this.channel.send(message, this.sendTimeout) && logger.isWarnEnabled()) {
				logger.warn("failed to send message to channel '" + channel +
						"' within timeout of " + this.sendTimeout + " milliseconds");
			}
			return null;
		}
		return this.requestReplyTemplate.request(message);
	}

}
