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

package org.springframework.integration.jms.config;

import java.util.List;
import java.util.concurrent.Executor;

import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.ExceptionListener;
import javax.jms.Session;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.context.SmartLifecycle;
import org.springframework.integration.channel.ChannelInterceptor;
import org.springframework.integration.jms.AbstractJmsChannel;
import org.springframework.integration.jms.PollableJmsChannel;
import org.springframework.integration.jms.SubscribableJmsChannel;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.listener.AbstractMessageListenerContainer;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.jms.listener.SimpleMessageListenerContainer;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.destination.DestinationResolver;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ErrorHandler;

/**
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @since 2.0
 */
public class JmsChannelFactoryBean extends AbstractFactoryBean<AbstractJmsChannel> implements SmartLifecycle, DisposableBean, BeanNameAware {

	private volatile AbstractJmsChannel channel;

	private volatile List<ChannelInterceptor> interceptors;

	private final boolean messageDriven;

	private final JmsTemplate jmsTemplate = new JmsTemplate();

	private volatile AbstractMessageListenerContainer container;

	private volatile Class<? extends AbstractMessageListenerContainer> containerType;

	private volatile boolean acceptMessagesWhileStopping;

	private volatile boolean autoStartup = true;

	private volatile String cacheLevelName;

	private volatile String clientId;

	private volatile Integer concurrentConsumers;

	private volatile ConnectionFactory connectionFactory;

	private volatile Destination destination;

	private volatile String destinationName;

	private volatile DestinationResolver destinationResolver;

	private volatile String durableSubscriptionName;

	private volatile ErrorHandler errorHandler;

	private volatile ExceptionListener exceptionListener;

	private volatile Boolean exposeListenerSession;

	private volatile Integer idleTaskExecutionLimit;

	private volatile Integer maxConcurrentConsumers;

	private volatile Integer maxMessagesPerTask;

	private volatile String messageSelector;

	private volatile Integer phase;

	private volatile Boolean pubSubDomain;

	private volatile boolean pubSubNoLocal;

	private volatile Long receiveTimeout;

	private volatile Long recoveryInterval;
	
	private volatile String beanName;

	/**
	 * This value differs from the container implementations' default (which is AUTO_ACKNOWLEDGE)
	 */
	private volatile int sessionAcknowledgeMode = Session.SESSION_TRANSACTED;

	/**
	 * This value differs from the container implementations' default (which is false).
	 */
	private volatile boolean sessionTransacted = true;

	private volatile boolean subscriptionDurable;

	private volatile Executor taskExecutor;

	private volatile PlatformTransactionManager transactionManager;

	private volatile String transactionName;

	private volatile Integer transactionTimeout;


	public JmsChannelFactoryBean() {
		this(true);
	}

	public JmsChannelFactoryBean(boolean messageDriven) {
		this.messageDriven = messageDriven;
	}


	public void setInterceptors(List<ChannelInterceptor> interceptors) {
		this.interceptors = interceptors;
	}

	/*
	 * Template properties
	 */

	public void setDeliveryPersistent(boolean deliveryPersistent) {
		this.jmsTemplate.setDeliveryPersistent(deliveryPersistent);
	}

	public void setExplicitQosEnabled(boolean explicitQosEnabled) {
		this.jmsTemplate.setExplicitQosEnabled(explicitQosEnabled);
	}

	public void setMessageConverter(MessageConverter messageConverter) {
		this.jmsTemplate.setMessageConverter(messageConverter);
	}

	public void setMessageIdEnabled(boolean messageIdEnabled) {
		this.jmsTemplate.setMessageIdEnabled(messageIdEnabled);
	}

	public void setMessageTimestampEnabled(boolean messageTimestampEnabled) {
		this.jmsTemplate.setMessageTimestampEnabled(messageTimestampEnabled);
	}

	public void setPriority(int priority) {
		this.jmsTemplate.setPriority(priority);
	}

	public void setTimeToLive(long timeToLive) {
		this.jmsTemplate.setTimeToLive(timeToLive);
	}

	/*
	 * Container properties
	 */

	public void setAcceptMessagesWhileStopping(boolean acceptMessagesWhileStopping) {
		this.acceptMessagesWhileStopping = acceptMessagesWhileStopping;
	}

	public void setAutoStartup(boolean autoStartup) {
		this.autoStartup = autoStartup;
	}

	public void setCacheLevelName(String cacheLevelName) {
		this.cacheLevelName = cacheLevelName;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	public void setConcurrentConsumers(int concurrentConsumers) {
		this.concurrentConsumers = concurrentConsumers;
	}

	public void setConnectionFactory(ConnectionFactory connectionFactory) {
		this.connectionFactory = connectionFactory;
		this.jmsTemplate.setConnectionFactory(this.connectionFactory);
	}

	public void setContainerType(Class<? extends AbstractMessageListenerContainer> containerType) {
		this.containerType = containerType;
	}

	public void setDestination(Destination destination) {
		this.destination = destination;
	}

	public void setDestinationName(String destinationName) {
		this.destinationName = destinationName;
	}

	public void setDestinationResolver(DestinationResolver destinationResolver) {
		this.destinationResolver = destinationResolver;
		this.jmsTemplate.setDestinationResolver(destinationResolver);
	}

	public void setDurableSubscriptionName(String durableSubscriptionName) {
		this.durableSubscriptionName = durableSubscriptionName;
	}

	public void setErrorHandler(ErrorHandler errorHandler) {
		this.errorHandler = errorHandler;
	}

	public void setExceptionListener(ExceptionListener exceptionListener) {
		this.exceptionListener = exceptionListener;
	}

	public void setExposeListenerSession(boolean exposeListenerSession) {
		this.exposeListenerSession = exposeListenerSession;
	}

	public void setIdleTaskExecutionLimit(int idleTaskExecutionLimit) {
		this.idleTaskExecutionLimit = idleTaskExecutionLimit;
	}

	public void setMaxConcurrentConsumers(int maxConcurrentConsumers) {
		this.maxConcurrentConsumers = maxConcurrentConsumers;
	}

	public void setMaxMessagesPerTask(int maxMessagesPerTask) {
		this.maxMessagesPerTask = maxMessagesPerTask;
	}

	public void setMessageSelector(String messageSelector) {
		this.messageSelector = messageSelector;
	}

	public void setPhase(int phase) {
		this.phase = phase;
	}

	public void setPubSubDomain(boolean pubSubDomain) {
		this.pubSubDomain = pubSubDomain;
		this.jmsTemplate.setPubSubDomain(pubSubDomain);
	}

	public void setPubSubNoLocal(boolean pubSubNoLocal) {
		this.pubSubNoLocal = pubSubNoLocal;
		this.jmsTemplate.setPubSubNoLocal(pubSubNoLocal);
	}

	public void setReceiveTimeout(long receiveTimeout) {
		this.receiveTimeout = receiveTimeout;
		this.jmsTemplate.setReceiveTimeout(receiveTimeout);
	}

	public void setRecoveryInterval(long recoveryInterval) {
		this.recoveryInterval = recoveryInterval;
	}

	public void setSessionAcknowledgeMode(int sessionAcknowledgeMode) {
		this.sessionAcknowledgeMode = sessionAcknowledgeMode;
		this.jmsTemplate.setSessionAcknowledgeMode(sessionAcknowledgeMode);
	}

	public void setSessionTransacted(boolean sessionTransacted) {
		this.sessionTransacted = sessionTransacted;
		this.jmsTemplate.setSessionTransacted(sessionTransacted);
	}

	public void setSubscriptionDurable(boolean subscriptionDurable) {
		this.subscriptionDurable = subscriptionDurable;
	}

	public void setTaskExecutor(Executor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	public void setTransactionName(String transactionName) {
		this.transactionName = transactionName;
	}

	public void setTransactionTimeout(int transactionTimeout) {
		this.transactionTimeout = transactionTimeout;
	}
	
	@Override
	public Class<?> getObjectType() {
		return (this.channel != null) ? this.channel.getClass() : AbstractJmsChannel.class;
	}

	@Override
	protected AbstractJmsChannel createInstance() throws Exception {
		this.initializeJmsTemplate();
		if (this.messageDriven) {
			this.container = this.createContainer();
			this.channel = new SubscribableJmsChannel(this.container, this.jmsTemplate);
		}
		else {
			Assert.isTrue(!Boolean.TRUE.equals(this.pubSubDomain),
					"A JMS Topic-backed 'publish-subscribe-channel' must be message-driven.");
			this.channel = new PollableJmsChannel(this.jmsTemplate);
		}
		if (!CollectionUtils.isEmpty(this.interceptors)) {
			this.channel.setInterceptors(this.interceptors);
		}
		this.channel.afterPropertiesSet();
		this.channel.setBeanName(this.beanName);
		return this.channel;
	}

	private void initializeJmsTemplate() {
		Assert.isTrue(this.destination != null ^ this.destinationName != null,
				"Exactly one of destination or destinationName is required.");
		if (this.destination != null) {
			this.jmsTemplate.setDefaultDestination(this.destination);
		}
		if (this.destinationName != null) {
			this.jmsTemplate.setDefaultDestinationName(this.destinationName);
		}
	}

	private AbstractMessageListenerContainer createContainer() throws Exception {
		if (this.containerType == null) {
			this.containerType = DefaultMessageListenerContainer.class;
		}
		AbstractMessageListenerContainer container = this.containerType.newInstance();
		container.setAcceptMessagesWhileStopping(this.acceptMessagesWhileStopping);
		container.setAutoStartup(this.autoStartup);
		container.setClientId(this.clientId);
		container.setConnectionFactory(this.connectionFactory);
		if (this.destination != null) {
			container.setDestination(this.destination);
		}
		if (this.destinationName != null) {
			container.setDestinationName(this.destinationName);
		}
		if (this.destinationResolver != null) {
			container.setDestinationResolver(this.destinationResolver);
		}
		container.setDurableSubscriptionName(this.durableSubscriptionName);
		container.setErrorHandler(this.errorHandler);
		container.setExceptionListener(this.exceptionListener);
		if (this.exposeListenerSession != null) {
			container.setExposeListenerSession(this.exposeListenerSession);
		}
		container.setMessageSelector(this.messageSelector);
		if (this.phase != null) {
			container.setPhase(this.phase);
		}
		if (this.pubSubDomain != null) {
			container.setPubSubDomain(this.pubSubDomain);
		}
		container.setSessionAcknowledgeMode(this.sessionAcknowledgeMode);
		container.setSessionTransacted(this.sessionTransacted);
		container.setSubscriptionDurable(this.subscriptionDurable);
		if (container instanceof DefaultMessageListenerContainer) {
			DefaultMessageListenerContainer dmlc = (DefaultMessageListenerContainer) container;
			if (this.cacheLevelName != null) {
				dmlc.setCacheLevelName(this.cacheLevelName);
			}
			if (this.concurrentConsumers != null) {
				dmlc.setConcurrentConsumers(this.concurrentConsumers);
			}
			if (this.idleTaskExecutionLimit != null) {
				dmlc.setIdleTaskExecutionLimit(this.idleTaskExecutionLimit);
			}
			if (this.maxConcurrentConsumers != null) {
				dmlc.setMaxConcurrentConsumers(this.maxConcurrentConsumers);
			}
			if (this.maxMessagesPerTask != null) {
				dmlc.setMaxMessagesPerTask(this.maxMessagesPerTask);
			}
			dmlc.setPubSubNoLocal(this.pubSubNoLocal);
			if (this.receiveTimeout != null) {
				dmlc.setReceiveTimeout(this.receiveTimeout);
			}
			if (this.recoveryInterval != null) {
				dmlc.setRecoveryInterval(this.recoveryInterval);
			}
			dmlc.setTaskExecutor(this.taskExecutor);
			dmlc.setTransactionManager(this.transactionManager);
			if (this.transactionName != null) {
				dmlc.setTransactionName(this.transactionName);
			}
			if (this.transactionTimeout != null) {
				dmlc.setTransactionTimeout(this.transactionTimeout);
			}
		}
		else if (container instanceof SimpleMessageListenerContainer) {
			SimpleMessageListenerContainer smlc = (SimpleMessageListenerContainer) container;
			if (this.concurrentConsumers != null) {
				smlc.setConcurrentConsumers(this.concurrentConsumers);
			}
			smlc.setPubSubNoLocal(this.pubSubNoLocal);
			smlc.setTaskExecutor(this.taskExecutor);
		}
		return container;
	}

	public void setBeanName(String name) {
		this.beanName = name;
	}
	/*
	 * SmartLifecycle implementation (delegates to the created channel if message-driven)
	 */

	public boolean isAutoStartup() {
		return (this.channel instanceof SubscribableJmsChannel) ?
				((SubscribableJmsChannel) this.channel).isAutoStartup() : false;
	}

	public int getPhase() {
		return (this.channel instanceof SubscribableJmsChannel) ?
				((SubscribableJmsChannel) this.channel).getPhase() : 0;
	}

	public boolean isRunning() {
		return (this.channel instanceof SubscribableJmsChannel) ?
				((SubscribableJmsChannel) this.channel).isRunning() : false;
	}

	public void start() {
		if (this.channel instanceof SubscribableJmsChannel) {
			((SubscribableJmsChannel) this.channel).start();
		}
	}

	public void stop() {
		if (this.channel instanceof SubscribableJmsChannel) {
			((SubscribableJmsChannel) this.channel).stop();
		}
	}

	public void stop(Runnable callback) {
		if (this.channel instanceof SubscribableJmsChannel) {
			((SubscribableJmsChannel) this.channel).stop(callback);
		}
	}

	protected void destroyInstance(AbstractJmsChannel instance) throws Exception {
		if (instance instanceof SubscribableJmsChannel) {
			((SubscribableJmsChannel) this.channel).destroy();
		}
	}
}
