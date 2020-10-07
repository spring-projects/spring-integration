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

package org.springframework.integration.router;

import java.util.Collection;
import java.util.UUID;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.integration.IntegrationPatternType;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.integration.support.management.IntegrationManagedResource;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.MessagingException;
import org.springframework.util.Assert;

/**
 * Base class for all Message Routers.
 *
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gunnar Hillert
 * @author Soby Chacko
 * @author Stefan Ferstl
 * @author Artem Bilan
 */
@ManagedResource
@IntegrationManagedResource
public abstract class AbstractMessageRouter extends AbstractMessageHandler implements MessageRouter {

	private volatile MessageChannel defaultOutputChannel;

	private volatile String defaultOutputChannelName;

	private volatile boolean ignoreSendFailures;

	private volatile boolean applySequence;

	private final MessagingTemplate messagingTemplate = new MessagingTemplate();


	/**
	 * Set the default channel where Messages should be sent if channel resolution
	 * fails to return any channels. If no default channel is provided and channel
	 * resolution fails to return any channels, the router will throw an
	 * {@link MessageDeliveryException}.
	 * <p> If messages shall be ignored (dropped) instead, please provide a
	 * {@link org.springframework.integration.channel.NullChannel}.
	 * @param defaultOutputChannel The default output channel.
	 */
	public void setDefaultOutputChannel(MessageChannel defaultOutputChannel) {
		this.defaultOutputChannel = defaultOutputChannel;
	}

	/**
	 * Get the default output channel.
	 * @return the channel.
	 * @since 4.3
	 */
	@Override
	public MessageChannel getDefaultOutputChannel() {
		if (this.defaultOutputChannelName != null) {
			synchronized (this) {
				if (this.defaultOutputChannelName != null) {
					this.defaultOutputChannel = getChannelResolver().resolveDestination(this.defaultOutputChannelName);
					this.defaultOutputChannelName = null;
				}
			}
		}
		return this.defaultOutputChannel;
	}

	public void setDefaultOutputChannelName(String defaultOutputChannelName) {
		Assert.hasText(defaultOutputChannelName, "'defaultOutputChannelName' must not be empty");
		this.defaultOutputChannelName = defaultOutputChannelName;
	}

	/**
	 * Set the timeout for sending a message to the resolved channel.
	 * By default, there is no timeout, meaning the send will block indefinitely.
	 * @param timeout The timeout.
	 * @since 4.3
	 */
	public void setSendTimeout(long timeout) {
		this.messagingTemplate.setSendTimeout(timeout);
	}

	/**
	 * Specify whether send failures for one or more of the recipients should be ignored. By default this is
	 * <code>false</code> meaning that an Exception will be thrown whenever a send fails. To override this and suppress
	 * Exceptions, set the value to <code>true</code>.
	 * @param ignoreSendFailures true to ignore send failures.
	 */
	public void setIgnoreSendFailures(boolean ignoreSendFailures) {
		this.ignoreSendFailures = ignoreSendFailures;
	}

	/**
	 * Specify whether to apply the sequence number and size headers to the messages prior to sending to the recipient
	 * channels. By default, this value is <code>false</code> meaning that sequence headers will <em>not</em> be
	 * applied. If planning to use an Aggregator downstream with the default correlation and completion strategies, you
	 * should set this flag to <code>true</code>.
	 * @param applySequence true to apply sequence information.
	 */
	public void setApplySequence(boolean applySequence) {
		this.applySequence = applySequence;
	}

	@Override
	public String getComponentType() {
		return "router";
	}

	@Override
	public IntegrationPatternType getIntegrationPatternType() {
		return IntegrationPatternType.router;
	}

	/**
	 * Provides {@link MessagingTemplate} access for subclasses
	 * @return The messaging template.
	 */
	protected MessagingTemplate getMessagingTemplate() {
		return this.messagingTemplate;
	}

	protected ConversionService getRequiredConversionService() {
		ConversionService conversionService = getConversionService();
		if (conversionService == null) {
			conversionService = DefaultConversionService.getSharedInstance();
			setConversionService(conversionService);
		}
		return conversionService;
	}

	@Override
	protected void onInit() {
		super.onInit();
		Assert.state(!(this.defaultOutputChannelName != null && this.defaultOutputChannel != null),
				"'defaultOutputChannelName' and 'defaultOutputChannel' are mutually exclusive.");
		BeanFactory beanFactory = getBeanFactory();
		if (beanFactory != null) {
			this.messagingTemplate.setBeanFactory(beanFactory);
		}
	}

	/**
	 * Subclasses must implement this method to return a Collection of zero or more
	 * MessageChannels to which the given Message should be routed.
	 * @param message The message.
	 * @return The collection of message channels.
	 */
	protected abstract Collection<MessageChannel> determineTargetChannels(Message<?> message);

	@Override
	protected void handleMessageInternal(Message<?> message) {
		boolean sent = false;
		Collection<MessageChannel> results = determineTargetChannels(message);
		if (results != null) {
			int sequenceSize = results.size();
			int sequenceNumber = 1;
			for (MessageChannel channel : results) {
				final Message<?> messageToSend;
				if (!this.applySequence) {
					messageToSend = message;
				}
				else {
					UUID id = message.getHeaders().getId();
					messageToSend = getMessageBuilderFactory()
							.fromMessage(message)
							.pushSequenceDetails(id == null ? generateId() : id,
									sequenceNumber++, sequenceSize)
							.build();
				}
				if (channel != null) {
					sent |= doSend(channel, messageToSend);
				}
			}
		}
		if (!sent) {
			getDefaultOutputChannel();
			if (this.defaultOutputChannel != null) {
				this.messagingTemplate.send(this.defaultOutputChannel, message);
			}
			else {
				throw new MessageDeliveryException(message, "No channel resolved by router '" + this
						+ "' and no 'defaultOutputChannel' defined.");
			}
		}
	}

	private boolean doSend(MessageChannel channel, final Message<?> messageToSend) {
		try {
			this.messagingTemplate.send(channel, messageToSend);
			return true;
		}
		catch (MessagingException ex) {
			if (!this.ignoreSendFailures) {
				throw ex;
			}
			this.logger.debug(ex, "Send failure ignored");
			return false;
		}
	}

}
