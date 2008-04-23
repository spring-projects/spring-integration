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

package org.springframework.integration.adapter.jms;

import javax.jms.MessageListener;

import org.springframework.integration.channel.ChannelPublisher;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageDeliveryException;
import org.springframework.integration.message.MessagingException;
import org.springframework.jms.support.converter.MessageConverter;

/**
 * JMS {@link MessageListener} implementation that converts the received JMS
 * message into a Spring Integration message and then sends that to a channel.
 * 
 * @author Mark Fisher
 */
public class ChannelPublishingJmsListener extends ChannelPublisher implements MessageListener {

	private final MessageConverter converter;


	public ChannelPublishingJmsListener(MessageChannel channel, MessageConverter converter) {
		super(channel);
		this.converter = (converter != null && converter instanceof HeaderMappingMessageConverter) ?
				converter : new HeaderMappingMessageConverter(converter);
	}


	public void onMessage(javax.jms.Message jmsMessage) {
		try {
			Message<?> messageToSend = (Message<?>) this.converter.fromMessage(jmsMessage);
			if (!this.publish(messageToSend)){
				throw new MessageDeliveryException(messageToSend, "failed to send Message to channel: " + this.getChannel());
			}
		}
		catch (Exception e) {
			throw new MessagingException("failed to convert and send JMS Message", e);
		}
	}

}
