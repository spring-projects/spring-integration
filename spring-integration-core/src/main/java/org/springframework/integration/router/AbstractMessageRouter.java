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

package org.springframework.integration.router;

import java.util.Collection;
import java.util.UUID;

import org.springframework.integration.Message;
import org.springframework.integration.MessageDeliveryException;
import org.springframework.integration.MessageHeaders;
import org.springframework.integration.MessagingException;
import org.springframework.integration.core.MessageBuilder;
import org.springframework.integration.core.MessageChannel;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.handler.AbstractMessageHandler;

/**
 * Base class for Message Routers.
 * 
 * @author Mark Fisher
 */
public abstract class AbstractMessageRouter extends AbstractMessageHandler {

	private volatile MessageChannel defaultOutputChannel;

	private volatile boolean resolutionRequired;

	private volatile boolean ignoreSendFailures;

	private volatile boolean applySequence;

	private final MessagingTemplate messagingTemplate = new MessagingTemplate();


	/**
	 * Set the default channel where Messages should be sent if channel
	 * resolution fails to return any channels. If no default channel is
	 * provided, the router will either drop the Message or throw an Exception
	 * depending on the value of {@link #resolutionRequired}. 
	 */
	public void setDefaultOutputChannel(MessageChannel defaultOutputChannel) {
		this.defaultOutputChannel = defaultOutputChannel;
	}

	/**
	 * Set the timeout for sending a message to the resolved channel. By
	 * default, there is no timeout, meaning the send will block indefinitely.
	 */
	public void setTimeout(long timeout) {
		this.messagingTemplate.setSendTimeout(timeout);
	}

	/**
	 * Set whether this router should always be required to resolve at least one
	 * channel. The default is 'false'. To trigger an exception whenever the
	 * resolver returns null or an empty channel list, and this endpoint has 
	 * no 'defaultOutputChannel' configured, set this value to 'true'.
	 */
	public void setResolutionRequired(boolean resolutionRequired) {
		this.resolutionRequired = resolutionRequired;
	}

	/**
	 * Specify whether send failures for one or more of the recipients should be
	 * ignored. By default this is <code>false</code> meaning that an Exception
	 * will be thrown whenever a send fails. To override this and suppress
	 * Exceptions, set the value to <code>true</code>.
	 */
	public void setIgnoreSendFailures(boolean ignoreSendFailures) {
		this.ignoreSendFailures = ignoreSendFailures;
	}

	/**
	 * Specify whether to apply the sequence number and size headers to the
	 * messages prior to sending to the recipient channels. By default, this
	 * value is <code>false</code> meaning that sequence headers will
	 * <em>not</em> be applied. If planning to use an Aggregator downstream with
	 * the default correlation and completion strategies, you should set this
	 * flag to <code>true</code>.
	 */
	public void setApplySequence(boolean applySequence) {
		this.applySequence = applySequence;
	}

	@Override
	public String getComponentType() {
		return "router";
	}

	@Override
	protected void handleMessageInternal(Message<?> message) {
		boolean sent = false;
		Collection<MessageChannel> results = this.determineTargetChannels(message);
		if (results != null) {
			int sequenceSize = results.size();
			int sequenceNumber = 1;
			for (MessageChannel channel : results) {
				final Message<?> messageToSend = (!this.applySequence) ? message
						: MessageBuilder.fromMessage(message)
								.setSequenceNumber(sequenceNumber++)
								.setSequenceSize(sequenceSize)
								.setCorrelationId(message.getHeaders().getId())
								.setHeader(MessageHeaders.ID, UUID.randomUUID())
								.build();
				if (channel != null) {
					try {
						this.messagingTemplate.send(channel, messageToSend);
						sent = true;
					}
					catch (MessagingException e) {
						if (!this.ignoreSendFailures) {
							throw e;
						}
						else if (this.logger.isDebugEnabled()) {
							this.logger.debug(e);
						}
					}
				}
			}
		}
		if (!sent) {
			if (this.defaultOutputChannel != null) {
				this.messagingTemplate.send(this.defaultOutputChannel, message);
			}
			else if (this.resolutionRequired) {
				throw new MessageDeliveryException(message,
						"no channel resolved by router and no default output channel defined");
			}
		}
	}

	/**
	 * Subclasses must implement this method to return the target channels for
	 * a given Message.
	 */
	protected abstract Collection<MessageChannel> determineTargetChannels(Message<?> message);

}
