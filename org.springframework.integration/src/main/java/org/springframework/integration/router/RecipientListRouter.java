/*
 * Copyright 2002-2009 the original author or authors.
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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.integration.channel.MessageChannelTemplate;
import org.springframework.integration.core.Message;
import org.springframework.integration.core.MessageChannel;
import org.springframework.integration.core.MessageHeaders;
import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.integration.message.MessageBuilder;
import org.springframework.integration.message.MessageDeliveryException;
import org.springframework.util.Assert;

/**
 * A Message Router that sends Messages to a statically configured list of
 * recipients. The recipients are provided as a list of {@link MessageChannel}
 * instances. For dynamic recipient lists, consider instead using the @Router
 * annotation or extending {@link AbstractChannelNameResolvingMessageRouter}. 
 * 
 * @author Mark Fisher
 */
public class RecipientListRouter extends AbstractMessageHandler implements InitializingBean {

	private volatile boolean ignoreSendFailures;

	private volatile boolean applySequence;

	private volatile List<MessageChannel> channels;

	private final MessageChannelTemplate channelTemplate = new MessageChannelTemplate();


	public void setChannels(List<MessageChannel> channels) {
		this.channels = channels;
	}

	/**
	 * Set the timeout for sending a message to the resolved channel. By
	 * default, there is no timeout, meaning the send will block indefinitely.
	 */
	public void setTimeout(long timeout) {
		this.channelTemplate.setSendTimeout(timeout);
	}

	/**
	 * Specify whether send failures for one or more of the recipients
	 * should be ignored. By default this is <code>false</code> meaning
	 * that an Exception will be thrown whenever a send fails. To override
	 * this and suppress Exceptions, set the value to <code>true</code>.
	 */
	public void setIgnoreSendFailures(boolean ignoreSendFailures) {
		this.ignoreSendFailures = ignoreSendFailures;
	}

	/**
	 * Specify whether to apply the sequence number and size headers to the
	 * messages prior to sending to the recipient channels. By default, this
	 * value is <code>false</code> meaning that sequence headers will
	 * <em>not</em> be applied. If planning to use an Aggregator downstream
	 * with the default correlation and completion strategies, you should set
	 * this flag to <code>true</code>.
	 */
	public void setApplySequence(boolean applySequence) {
		this.applySequence = applySequence;
	}

	public void afterPropertiesSet() {
		Assert.notEmpty(this.channels, "a non-empty channel list is required");
	}

	@Override
	protected void handleMessageInternal(Message<?> message) throws Exception {
		List<MessageChannel> channelList = new ArrayList<MessageChannel>(this.channels);
		int sequenceSize = channelList.size();
		int sequenceNumber = 1;
		for (MessageChannel channel : channelList) {
			final Message<?> messageToSend = (!this.applySequence) ? message
					: MessageBuilder.fromMessage(message)
							.setSequenceNumber(sequenceNumber++)
							.setSequenceSize(sequenceSize)
							.setCorrelationId(message.getHeaders().getId())
							.setHeader(MessageHeaders.ID, UUID.randomUUID())
							.build();
			boolean sent = this.channelTemplate.send(messageToSend, channel);
			if (!sent && !this.ignoreSendFailures) {
				throw new MessageDeliveryException(message,
						"RecipientListRouter failed to send to channel: " + channel);
			}
		}
	}

}
