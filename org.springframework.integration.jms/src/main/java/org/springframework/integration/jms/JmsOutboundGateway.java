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

import java.io.Serializable;

import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.QueueRequestor;
import javax.jms.QueueSession;
import javax.jms.Session;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.integration.consumer.AbstractReplyProducingMessageConsumer;
import org.springframework.integration.consumer.ReplyMessageHolder;
import org.springframework.integration.core.Message;
import org.springframework.integration.core.MessageChannel;
import org.springframework.integration.message.MessageBuilder;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.SessionCallback;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.SimpleMessageConverter;
import org.springframework.util.Assert;

/**
 * An outbound Messaging Gateway for request/reply JMS.
 * 
 * @author Mark Fisher
 */
public class JmsOutboundGateway extends AbstractReplyProducingMessageConsumer implements InitializingBean {

	private volatile Queue jmsQueue;

	private volatile MessageConverter messageConverter;

	private final JmsTemplate jmsTemplate = new JmsTemplate();


	public void setJmsQueue(Queue jmsQueue) {
		this.jmsQueue = jmsQueue;
	}

	public void setConnectionFactory(ConnectionFactory connectionFactory) {
		this.jmsTemplate.setConnectionFactory(connectionFactory);
	}

	public void setReplyChannel(MessageChannel replyChannel) {
		this.setOutputChannel(replyChannel);
	}

	public void afterPropertiesSet() {
		this.jmsTemplate.afterPropertiesSet();
		Assert.notNull(this.jmsQueue, "jmsQueue must not be null");
		this.messageConverter = new HeaderMappingMessageConverter(new SimpleMessageConverter());
	}

	@Override
	protected void onMessage(final Message<?> message, final ReplyMessageHolder replyMessageHolder) {
		final Message<?> requestMessage = MessageBuilder.fromMessage(message).build();
		this.jmsTemplate.execute(new SessionCallback() {
			public Object doInJms(Session session) throws JMSException {
				Assert.state(session instanceof QueueSession,
						"QueueSession is required for the outbound JMS Gateway");
				javax.jms.Message jmsRequest = (messageConverter != null)
						? messageConverter.toMessage(requestMessage, session)
						: session.createObjectMessage((Serializable) requestMessage);
				QueueRequestor requestor = new QueueRequestor((QueueSession) session, jmsQueue);
				javax.jms.Message jmsReply = requestor.request(jmsRequest);
				Object result = (messageConverter != null)
						? messageConverter.fromMessage(jmsReply) : jmsReply;
				replyMessageHolder.set(result);
				return null;
			}
		}, true);
	}

}
