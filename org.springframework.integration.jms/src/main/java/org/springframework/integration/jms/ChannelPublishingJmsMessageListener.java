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

package org.springframework.integration.jms;

import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.integration.channel.MessageChannelTemplate;
import org.springframework.integration.core.Message;
import org.springframework.integration.core.MessageChannel;
import org.springframework.integration.message.MessageBuilder;
import org.springframework.integration.message.MessageDeliveryException;
import org.springframework.jms.listener.SessionAwareMessageListener;
import org.springframework.jms.support.converter.MessageConverter;

/**
 * JMS MessageListener that converts a JMS Message into a Spring Integration
 * Message and sends that Message to a channel. If the 'expectReply' value is
 * <code>true</code>, it will also wait for a Spring Integration reply Message
 * and convert that into a JMS reply.
 * 
 * @author Mark Fisher
 */
public class ChannelPublishingJmsMessageListener implements SessionAwareMessageListener, InitializingBean {

	private volatile boolean expectReply;

	private volatile MessageConverter messageConverter;

	private volatile boolean extractRequestPayload = true;

	private volatile boolean extractReplyPayload = true;

	private volatile JmsHeaderMapper headerMapper;

	private final MessageChannelTemplate channelTemplate = new MessageChannelTemplate();


	/**
	 * Specify the channel to which request Messages should be sent.
	 */
	public void setRequestChannel(MessageChannel requestChannel) {
		this.channelTemplate.setDefaultChannel(requestChannel);
	}

	/**
	 * Specify whether a JMS reply Message is expected.
	 */
	public void setExpectReply(boolean expectReply) {
		this.expectReply = expectReply;
	}

	/**
	 * Specify the maximum time to wait when sending a request message to the
	 * request channel. The default value will be that of
	 * {@link MessageChannelTemplate}.
	 */
	public void setRequestTimeout(long requestTimeout) {
		this.channelTemplate.setSendTimeout(requestTimeout);
	}

	/**
	 * Specify the maximum time to wait for reply Messages. This value is only
	 * relevant if {@link #expectReply} is <code>true</code>. The default
	 * value will be that of {@link MessageChannelTemplate}.
	 */
	public void setReplyTimeout(long replyTimeout) {
		this.channelTemplate.setReceiveTimeout(replyTimeout);
	}

	/**
	 * Provide a {@link MessageConverter} implementation to use when
	 * converting between JMS Messages and Spring Integration Messages.
	 * If none is provided, a {@link HeaderMappingMessageConverter} will
	 * be used and the {@link JmsHeaderMapper} instance provided to the
	 * {@link #setHeaderMapper(JmsHeaderMapper)} method will be included
	 * in the conversion process.
	 */
	public void setMessageConverter(MessageConverter messageConverter) {
		this.messageConverter = messageConverter;
	}

	/**
	 * Provide a {@link JmsHeaderMapper} implementation to use when
	 * converting between JMS Messages and Spring Integration Messages.
	 * If none is provided, a {@link DefaultJmsHeaderMapper} will be used.
	 * 
	 * <p>This property will be ignored if a {@link MessageConverter} is
	 * provided to the {@link #setMessageConverter(MessageConverter)} method.
	 * However, you may provide your own implementation of the delegating
	 * {@link HeaderMappingMessageConverter} implementation.
	 */
	public void setHeaderMapper(JmsHeaderMapper headerMapper) {
		this.headerMapper = headerMapper;
	}

	/**
	 * Specify whether the JMS request Message's body should be extracted prior
	 * to converting into a Spring Integration Message. This value is set to
	 * <code>true</code> by default. To send the JMS Message itself as a
	 * Spring Integration Message payload, set this to <code>false</code>.
	 */
	public void setExtractRequestPayload(boolean extractRequestPayload) {
		this.extractRequestPayload = extractRequestPayload;
	}

	/**
	 * Specify whether the Spring Integration reply Message's payload should be
	 * extracted prior to converting into a JMS Message. This value is set to
	 * <code>true</code> by default. To send the Spring Integration Message
	 * itself as the JMS Message's body, set this to <code>false</code>.
	 */
	public void setExtractReplyPayload(boolean extractReplyPayload) {
		this.extractReplyPayload = extractReplyPayload;
	}

	public final void afterPropertiesSet() {
		if (this.messageConverter == null) {
			HeaderMappingMessageConverter hmmc = new HeaderMappingMessageConverter(null, this.headerMapper);
			hmmc.setExtractJmsMessageBody(this.extractRequestPayload);
			hmmc.setExtractIntegrationMessagePayload(this.extractReplyPayload);
			this.messageConverter = hmmc;
		}
	}

	public void onMessage(javax.jms.Message jmsMessage, Session session) throws JMSException {
		Object object = this.messageConverter.fromMessage(jmsMessage);
		Message<?> requestMessage = (object instanceof Message) ?
				(Message<?>) object : MessageBuilder.withPayload(object).build();
		if (!this.expectReply) {
			boolean sent = this.channelTemplate.send(requestMessage);
			if (!sent) {
				throw new MessageDeliveryException(requestMessage, "failed to send Message to request channel");
			}
		}
		else {
			Message<?> replyMessage = this.channelTemplate.sendAndReceive(requestMessage);
			if (replyMessage != null) {
				javax.jms.Message jmsReply = this.messageConverter.toMessage(replyMessage, session);
				if (jmsReply.getJMSCorrelationID() == null) {
					jmsReply.setJMSCorrelationID(jmsMessage.getJMSMessageID());
				}
				MessageProducer producer = session.createProducer(jmsMessage.getJMSReplyTo());
				producer.send(jmsMessage.getJMSReplyTo(), jmsReply);
			}
		}
	}

}
