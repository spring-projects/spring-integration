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

import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.core.task.TaskExecutor;
import org.springframework.integration.core.Message;
import org.springframework.integration.gateway.SimpleMessagingGateway;
import org.springframework.jms.listener.AbstractMessageListenerContainer;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.jms.listener.SessionAwareMessageListener;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.Assert;

/**
 * A message-driven adapter for receiving JMS messages and sending to a channel.
 * 
 * @author Mark Fisher
 */
public class JmsInboundGateway extends SimpleMessagingGateway implements DisposableBean {

	private volatile AbstractMessageListenerContainer container;

	private volatile ConnectionFactory connectionFactory;

	private volatile Destination destination;

	private volatile String destinationName;

	private volatile MessageConverter messageConverter;

	private volatile JmsHeaderMapper headerMapper;

	private volatile boolean extractRequestPayload = true;

	private volatile boolean extractReplyPayload = true;

	private volatile TaskExecutor taskExecutor;

	private volatile PlatformTransactionManager transactionManager;

	private volatile boolean sessionTransacted;

	private volatile int sessionAcknowledgeMode = Session.AUTO_ACKNOWLEDGE;

	private volatile int concurrentConsumers = 1;

	private volatile int maxConcurrentConsumers = 1;

	private volatile int maxMessagesPerTask = Integer.MIN_VALUE;

	private volatile int idleTaskExecutionLimit = 1;


	public void setContainer(AbstractMessageListenerContainer container) {
		this.container = container;
	}

	public void setConnectionFactory(ConnectionFactory connectionFactory) {
		this.connectionFactory = connectionFactory;
	}

	public void setDestination(Destination destination) {
		this.destination = destination;
	}

	public void setDestinationName(String destinationName) {
		this.destinationName = destinationName;
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

	public void setExtractRequestPayload(boolean extractRequestPayload) {
		this.extractRequestPayload = extractRequestPayload;
	}

	public void setExtractReplyPayload(boolean extractReplyPayload) {
		this.extractReplyPayload = extractReplyPayload;
	}

	public void setTaskExecutor(TaskExecutor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	public void setSessionTransacted(boolean sessionTransacted) {
		this.sessionTransacted = sessionTransacted;
	}

	public void setSessionAcknowledgeMode(int sessionAcknowledgeMode) {
		this.sessionAcknowledgeMode = sessionAcknowledgeMode;
	}

	public void setConcurrentConsumers(int concurrentConsumers) {
		this.concurrentConsumers = concurrentConsumers;
	}

	public void setMaxConcurrentConsumers(int maxConcurrentConsumers) {
		this.maxConcurrentConsumers = maxConcurrentConsumers;
	}

	public void setMaxMessagesPerTask(int maxMessagesPerTask) {
		this.maxMessagesPerTask = maxMessagesPerTask;
	}

	public void setIdleTaskExecutionLimit(int idleTaskExecutionLimit) {
		this.idleTaskExecutionLimit = idleTaskExecutionLimit;
	}


	private void initialize() {
		if (this.container == null) {
			this.container = createDefaultContainer();
		}
		if (this.messageConverter == null) {
			HeaderMappingMessageConverter hmmc = new HeaderMappingMessageConverter(null, this.headerMapper);
			hmmc.setExtractRequestPayload(this.extractRequestPayload);
			hmmc.setExtractReplyPayload(this.extractReplyPayload);
			this.messageConverter = hmmc;
		}
		this.container.setMessageListener(new GatewayInvokingMessageListener());
		if (!this.container.isActive()) {
			this.container.afterPropertiesSet();
		}
	}

	private AbstractMessageListenerContainer createDefaultContainer() {
		Assert.isTrue(this.connectionFactory != null
				&& (this.destination != null || this.destinationName != null),
				"If a 'container' reference is not provided, then 'connectionFactory'"
				+ " and 'destination' (or 'destinationName') are required.");
		DefaultMessageListenerContainer dmlc = new DefaultMessageListenerContainer();
		dmlc.setConcurrentConsumers(this.concurrentConsumers);
		dmlc.setMaxConcurrentConsumers(this.maxConcurrentConsumers);
		dmlc.setMaxMessagesPerTask(this.maxMessagesPerTask);
		dmlc.setIdleTaskExecutionLimit(this.idleTaskExecutionLimit);
		dmlc.setConnectionFactory(this.connectionFactory);
		if (this.destination != null) {
			dmlc.setDestination(this.destination);
		}
		if (this.destinationName != null) {
			dmlc.setDestinationName(this.destinationName);
		}
		dmlc.setTransactionManager(this.transactionManager);
		dmlc.setSessionTransacted(this.sessionTransacted);
		dmlc.setSessionAcknowledgeMode(this.sessionAcknowledgeMode);
		dmlc.setAutoStartup(false);
		if (this.taskExecutor != null) {
			dmlc.setTaskExecutor(this.taskExecutor);
		}
		return dmlc;
	}

	// Lifecycle implementation

	@Override
	protected void doStart() {
		this.initialize();
		this.container.start();
		super.doStart();
	}

	@Override
	protected void doStop() {
		if (this.container != null) {
			this.container.stop();
		}
		super.doStop();
	}

	// DisposableBean implementation

	public void destroy() {
		if (this.container != null) {
			this.container.destroy();
		}
	}


	private class GatewayInvokingMessageListener implements SessionAwareMessageListener {

		public void onMessage(javax.jms.Message jmsMessage, Session session) throws JMSException {
			Object object = messageConverter.fromMessage(jmsMessage);
			Message<?> replyMessage = JmsInboundGateway.this.sendAndReceiveMessage(object);
			if (replyMessage != null) {
				javax.jms.Message jmsReply = messageConverter.toMessage(replyMessage, session);
				if (jmsReply.getJMSCorrelationID() == null) {
					jmsReply.setJMSCorrelationID(jmsMessage.getJMSMessageID());
				}
				MessageProducer producer = session.createProducer(jmsMessage.getJMSReplyTo());
				producer.send(jmsMessage.getJMSReplyTo(), jmsReply);
			}
		}
	}

}
