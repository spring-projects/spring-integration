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

import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.Session;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.Lifecycle;
import org.springframework.core.task.TaskExecutor;
import org.springframework.integration.MessagingConfigurationException;
import org.springframework.integration.adapter.AbstractSourceAdapter;
import org.springframework.jms.listener.AbstractJmsListeningContainer;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.SimpleMessageConverter;
import org.springframework.util.Assert;

/**
 * A message-driven adapter for receiving JMS messages and sending to a channel.
 * 
 * @author Mark Fisher
 */
public class JmsMessageDrivenSourceAdapter extends AbstractSourceAdapter<Object> implements Lifecycle, DisposableBean {

	private volatile AbstractJmsListeningContainer container;

	private volatile ConnectionFactory connectionFactory;

	private volatile Destination destination;

	private volatile String destinationName;

	private volatile MessageConverter messageConverter = new SimpleMessageConverter();

	private volatile TaskExecutor taskExecutor;

	private volatile boolean sessionTransacted;

	private volatile int sessionAcknowledgeMode = Session.AUTO_ACKNOWLEDGE;

	private volatile long receiveTimeout = 1000;

	private volatile int concurrentConsumers = 1;

	private volatile int maxConcurrentConsumers = 1;

	private volatile int maxMessagesPerTask = Integer.MIN_VALUE;

	private volatile int idleTaskExecutionLimit = 1;

	private volatile long sendTimeout = -1;


	public void setContainer(AbstractJmsListeningContainer container) {
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

	public void setMessageConverter(MessageConverter messageConverter) {
		Assert.notNull(messageConverter, "'messageConverter' must not be null");
		this.messageConverter = messageConverter;
	}

	public void setSendTimeout(long sendTimeout) {
		this.sendTimeout = sendTimeout;
	}

	public void setTaskExecutor(TaskExecutor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

	public void setSessionTransacted(boolean sessionTransacted) {
		this.sessionTransacted = sessionTransacted;
	}

	public void setSessionAcknowledgeMode(int sessionAcknowledgeMode) {
		this.sessionAcknowledgeMode = sessionAcknowledgeMode;
	}

	@Override
	public void initialize() {
		if (this.container == null) {
			initDefaultContainer();
		}
	}

	private void initDefaultContainer() {
		if (this.connectionFactory == null || (this.destination == null && this.destinationName == null)) {
			throw new MessagingConfigurationException("If a 'container' reference is not provided, then "
					+ "'connectionFactory' and 'destination' (or 'destinationName') are required.");
		}
		DefaultMessageListenerContainer dmlc = new DefaultMessageListenerContainer();
		dmlc.setConnectionFactory(this.connectionFactory);
		if (this.destination != null) {
			dmlc.setDestination(this.destination);
		}
		if (this.destinationName != null) {
			dmlc.setDestinationName(this.destinationName);
		}
		dmlc.setReceiveTimeout(this.receiveTimeout);
		dmlc.setConcurrentConsumers(this.concurrentConsumers);
		dmlc.setMaxConcurrentConsumers(this.maxConcurrentConsumers);
		dmlc.setMaxMessagesPerTask(this.maxMessagesPerTask);
		dmlc.setIdleTaskExecutionLimit(this.idleTaskExecutionLimit);
		dmlc.setSessionTransacted(this.sessionTransacted);
		dmlc.setSessionAcknowledgeMode(this.sessionAcknowledgeMode);
		dmlc.setAutoStartup(false);
		ChannelPublishingJmsListener listener = new ChannelPublishingJmsListener(
				this.getChannel(), this.messageConverter);
		listener.setTimeout(this.sendTimeout);
		dmlc.setMessageListener(listener);
		if (this.taskExecutor != null) {
			dmlc.setTaskExecutor(this.taskExecutor);
		}
		dmlc.afterPropertiesSet();
		this.container = dmlc;
	}

	public boolean isRunning() {
		return container.isRunning();
	}

	public void start() {
		container.start();
	}

	public void stop() {
		container.stop();
	}

	public void destroy() {
		container.destroy();
	}

}
