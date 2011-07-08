/*
 * Copyright 2002-2010 the original author or authors.
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.context.NamedComponent;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.history.MessageHistory;
import org.springframework.integration.history.TrackableComponent;
import org.springframework.util.Assert;

/**
 * A Channel Adapter implementation for connecting a
 * {@link MessageSource} to a {@link MessageChannel}.
 * 
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 */
public class SourcePollingChannelAdapter extends AbstractPollingEndpoint implements TrackableComponent {
	
	private final Log logger = LogFactory.getLog(this.getClass());

	private volatile MessageSource<?> source;

	private volatile MessageChannel outputChannel;

	private volatile boolean shouldTrack;

	private final MessagingTemplate messagingTemplate = new MessagingTemplate();

	/**
	 * Specify the source to be polled for Messages.
	 */
	public void setSource(MessageSource<?> source) {
		this.source = source;
	}

	/**
	 * Specify the {@link MessageChannel} where Messages should be sent.
	 */
	public void setOutputChannel(MessageChannel outputChannel) {
		this.outputChannel = outputChannel;
	}

	/**
	 * Specify the maximum time to wait for a Message to be sent to the
	 * output channel.
	 */
	public void setSendTimeout(long sendTimeout) {
		this.messagingTemplate.setSendTimeout(sendTimeout);
	}

	/**
	 * Specify whether this component should be tracked in the Message History.
	 */
	public void setShouldTrack(boolean shouldTrack) {
		this.shouldTrack = shouldTrack;
	}

	@Override
	public String getComponentType() {
		return (this.source instanceof NamedComponent) ?
				((NamedComponent) this.source).getComponentType() : "inbound-channel-adapter";
	}

	@Override
	protected void onInit() {
		Assert.notNull(this.source, "source must not be null");
		Assert.notNull(this.outputChannel, "outputChannel must not be null");
		super.onInit();
	}
	
	@Override
	protected boolean doPoll() {
		Message<?> message = this.source.receive();
		if (this.logger.isDebugEnabled()){
			this.logger.debug("Poll resulted in Message: " + message);
		}
		if (message != null) {
			if (this.shouldTrack) {
				message = MessageHistory.write(message, this);
			}
			this.messagingTemplate.send(this.outputChannel, message);
			return true;
		}
		if (this.logger.isDebugEnabled()){
			this.logger.debug("Received no Message during the poll, returning 'false'");
		}
		return false;
	}
}
