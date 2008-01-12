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

package org.springframework.integration.adapter.jms;

import javax.jms.JMSException;
import javax.jms.MessageListener;

import org.springframework.integration.MessageDeliveryException;
import org.springframework.integration.MessagingConfigurationException;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageMapper;
import org.springframework.integration.message.SimplePayloadMessageMapper;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.SimpleMessageConverter;
import org.springframework.util.Assert;

/**
 * JMS {@link MessageListener} implementation that converts the received JMS
 * message into a Spring Integration message and then sends that to a channel.
 * 
 * @author Mark Fisher
 */
public class ChannelPublishingJmsListener implements MessageListener {

	private MessageChannel channel;

	private long timeout = -1;

	private MessageConverter converter = new SimpleMessageConverter();

	private MessageMapper mapper = new SimplePayloadMessageMapper();


	public ChannelPublishingJmsListener() {
	}

	public ChannelPublishingJmsListener(MessageChannel channel) {
		Assert.notNull(channel, "'channel' must not be null");
		this.channel = channel;
	}


	public void setChannel(MessageChannel channel) {
		Assert.notNull(channel, "'channel' must not be null");
		this.channel = channel;
	}

	public void setTimeout(long timeout) {
		this.timeout = timeout;
	}

	public void setMessageConverter(MessageConverter messageConverter) {
		Assert.notNull(messageConverter, "'messageConverter' must not be null");
		this.converter = messageConverter;
	}

	public void setMessageMapper(MessageMapper messageMapper) {
		Assert.notNull(messageMapper, "'messageMapper' must not be null");
		this.mapper = messageMapper;
	}

	public void onMessage(javax.jms.Message jmsMessage) {
		if (this.channel == null) {
			throw new MessagingConfigurationException("'channel' must not be null");
		}
		try {
			Object payload = converter.fromMessage(jmsMessage);
			Message messageToSend = mapper.toMessage(payload);
			if (this.timeout < 0) {
				this.channel.send(messageToSend);
			}
			else {
				this.channel.send(messageToSend, timeout);
			}
		}
		catch (JMSException e) {
			throw new MessageDeliveryException("failed to convert JMS Message", e);
		}
	}

}
