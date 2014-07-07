/*
 * Copyright 2002-2014 the original author or authors.
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
import org.springframework.integration.jms.AbstractJmsChannel;
import org.springframework.integration.jms.DynamicJmsTemplate;
import org.springframework.integration.jms.PollableJmsChannel;
import org.springframework.integration.jms.SubscribableJmsChannel;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.listener.AbstractMessageListenerContainer;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.jms.listener.SimpleMessageListenerContainer;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.destination.DestinationResolver;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ErrorHandler;
import org.springframework.util.StringUtils;

/**
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 * @since 2.0
 */
public class JmsChannelFactoryBean extends AbstractFactoryBean<AbstractJmsChannel>
		implements SmartLifecycle, DisposableBean, BeanNameAware {

	private volatile AbstractJmsChannel channel;

	private volatile List<ChannelInterceptor> interceptors;

	private final boolean messageDriven;

	private final JmsTemplate jmsTemplate = new DynamicJmsTemplate();

	private volatile AbstractMessageListenerContainer container;

	private volatile Class<? extends AbstractMessageListenerContainer> containerType;

	private volatile boolean acceptMessagesWhileStopping;

	private volatile boolean autoStartup = true;

	private volatile String cacheLevelName;

	private volatile Integer cacheLevel;

	private volatile String clientId;

	private volatile String concurrency;

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

	private volatile int maxSubscribers = Integer.MAX_VALUE;


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
		Assert.isTrue(this.messageDriven,
				"'acceptMessagesWhileStopping' is allowed only in case of 'messageDriven = true'");
		this.acceptMessagesWhileStopping = acceptMessagesWhileStopping;
	}

	public void setAutoStartup(boolean autoStartup) {
		this.autoStartup = autoStartup;
	}

	public void setCacheLevelName(String cacheLevelName) {
		Assert.isTrue(this.messageDriven, "'cacheLevelName' is allowed only in case of 'messageDriven = true'");
		Assert.state(this.cacheLevel == null,
				"'cacheLevelName' and 'cacheLevel' are mutually exclusive");
		this.cacheLevelName = cacheLevelName;
	}

	public void setCacheLevel(Integer cacheLevel) {
		Assert.isTrue(this.messageDriven, "'cacheLevel' is allowed only in case of 'messageDriven = true'");
		Assert.state(!StringUtils.hasText(this.cacheLevelName),
				"'cacheLevelName' and 'cacheLevel' are mutually exclusive");
		this.cacheLevel = cacheLevel;
	}

	public void setClientId(String clientId) {
		Assert.isTrue(this.messageDriven, "'clientId' is allowed only in case of 'messageDriven = true'");
		this.clientId = clientId;
	}

	public void setConcurrency(String concurrency) {
		Assert.isTrue(this.messageDriven, "'concurrency' is allowed only in case of 'messageDriven = true'");
		this.concurrency = concurrency;
	}

	public void setConcurrentConsumers(int concurrentConsumers) {
		Assert.isTrue(this.messageDriven, "'concurrentConsumers' is allowed only in case of 'messageDriven = true'");
		this.concurrentConsumers = concurrentConsumers;
	}

	public void setConnectionFactory(ConnectionFactory connectionFactory) {
		this.connectionFactory = connectionFactory;
		this.jmsTemplate.setConnectionFactory(this.connectionFactory);
	}

	public void setContainerType(Class<? extends AbstractMessageListenerContainer> containerType) {
		Assert.isTrue(this.messageDriven, "'containerType' is allowed only in case of 'messageDriven = true'");
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
		Assert.isTrue(this.messageDriven, "'durableSubscriptionName' is allowed only in case of 'messageDriven = true'");
		this.durableSubscriptionName = durableSubscriptionName;
	}

	public void setErrorHandler(ErrorHandler errorHandler) {
		Assert.isTrue(this.messageDriven, "'errorHandler' is allowed only in case of 'messageDriven = true'");
		this.errorHandler = errorHandler;
	}

	public void setExceptionListener(ExceptionListener exceptionListener) {
		Assert.isTrue(this.messageDriven, "'exceptionListener' is allowed only in case of 'messageDriven = true'");
		this.exceptionListener = exceptionListener;
	}

	public void setExposeListenerSession(boolean exposeListenerSession) {
		Assert.isTrue(this.messageDriven, "'exposeListenerSession' is allowed only in case of 'messageDriven = true'");
		this.exposeListenerSession = exposeListenerSession;
	}

	public void setIdleTaskExecutionLimit(int idleTaskExecutionLimit) {
		Assert.isTrue(this.messageDriven, "'idleTaskExecutionLimit' is allowed only in case of 'messageDriven = true'");
		this.idleTaskExecutionLimit = idleTaskExecutionLimit;
	}

	public void setMaxConcurrentConsumers(int maxConcurrentConsumers) {
		Assert.isTrue(this.messageDriven, "'maxConcurrentConsumers' is allowed only in case of 'messageDriven = true'");
		this.maxConcurrentConsumers = maxConcurrentConsumers;
	}

	public void setMaxMessagesPerTask(int maxMessagesPerTask) {
		Assert.isTrue(this.messageDriven, "'maxMessagesPerTask' is allowed only in case of 'messageDriven = true'");
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
		Assert.isTrue(this.messageDriven, "'recoveryInterval' is allowed only in case of 'messageDriven = true'");
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
		Assert.isTrue(this.messageDriven, "'subscriptionDurable' is allowed only in case of 'messageDriven = true'");
		this.subscriptionDurable = subscriptionDurable;
	}

	public void setTaskExecutor(Executor taskExecutor) {
		Assert.isTrue(this.messageDriven, "'taskExecutor' is allowed only in case of 'messageDriven = true'");
		this.taskExecutor = taskExecutor;
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		Assert.isTrue(this.messageDriven, "'transactionManager' is allowed only in case of 'messageDriven = true'");
		this.transactionManager = transactionManager;
	}

	public void setTransactionName(String transactionName) {
		Assert.isTrue(this.messageDriven, "'transactionName' is allowed only in case of 'messageDriven = true'");
		this.transactionName = transactionName;
	}

	public void setTransactionTimeout(int transactionTimeout) {
		Assert.isTrue(this.messageDriven, "'transactionTimeout' is allowed only in case of 'messageDriven = true'");
		this.transactionTimeout = transactionTimeout;
	}

	public void setMaxSubscribers(int maxSubscribers) {
		Assert.isTrue(this.messageDriven, "'maxSubscribers' is allowed only in case of 'messageDriven = true'");
		this.maxSubscribers = maxSubscribers;
	}

	@Override
	public void setBeanName(String name) {
		this.beanName = name;
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
			SubscribableJmsChannel subscribableJmsChannel = new SubscribableJmsChannel(this.container, this.jmsTemplate);
			subscribableJmsChannel.setMaxSubscribers(this.maxSubscribers);
			this.channel = subscribableJmsChannel;
		}
		else {
			Assert.isTrue(!Boolean.TRUE.equals(this.pubSubDomain),
					"A JMS Topic-backed 'publish-subscribe-channel' must be message-driven.");
			PollableJmsChannel pollableJmschannel = new PollableJmsChannel(this.jmsTemplate);
			if (this.messageSelector != null) {
				pollableJmschannel.setMessageSelector(this.messageSelector);
			}
			this.channel = pollableJmschannel;
		}
		if (!CollectionUtils.isEmpty(this.interceptors)) {
			this.channel.setInterceptors(this.interceptors);
		}
		this.channel.setBeanName(this.beanName);
		if (this.getBeanFactory() != null) {
			this.channel.setBeanFactory(this.getBeanFactory());
		}
		this.channel.afterPropertiesSet();
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

			if (this.cacheLevel != null) {
				dmlc.setCacheLevel(this.cacheLevel);
			}

			if (StringUtils.hasText(this.concurrency)){
				dmlc.setConcurrency(this.concurrency);
			}

			if (this.concurrentConsumers != null){
				dmlc.setConcurrentConsumers(this.concurrentConsumers);
			}

			if (this.maxConcurrentConsumers != null){
				dmlc.setMaxConcurrentConsumers(this.maxConcurrentConsumers);
			}

			if (this.idleTaskExecutionLimit != null) {
				dmlc.setIdleTaskExecutionLimit(this.idleTaskExecutionLimit);
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
			if (StringUtils.hasText(this.concurrency)){
				smlc.setConcurrency(this.concurrency);
			}

			if (this.concurrentConsumers != null){
				smlc.setConcurrentConsumers(this.concurrentConsumers);
			}

			smlc.setPubSubNoLocal(this.pubSubNoLocal);
			smlc.setTaskExecutor(this.taskExecutor);
		}
		return container;
	}

	/*
	 * SmartLifecycle implementation (delegates to the created channel if message-driven)
	 */

	@Override
	public boolean isAutoStartup() {
		return this.channel instanceof SubscribableJmsChannel && ((SubscribableJmsChannel) this.channel).isAutoStartup();
	}

	@Override
	public int getPhase() {
		return (this.channel instanceof SubscribableJmsChannel) ?
				((SubscribableJmsChannel) this.channel).getPhase() : 0;
	}

	@Override
	public boolean isRunning() {
		return this.channel instanceof SubscribableJmsChannel && ((SubscribableJmsChannel) this.channel).isRunning();
	}

	@Override
	public void start() {
		if (this.channel instanceof SubscribableJmsChannel) {
			((SubscribableJmsChannel) this.channel).start();
		}
	}

	@Override
	public void stop() {
		if (this.channel instanceof SubscribableJmsChannel) {
			((SubscribableJmsChannel) this.channel).stop();
		}
	}

	@Override
	public void stop(Runnable callback) {
		if (this.channel instanceof SubscribableJmsChannel) {
			((SubscribableJmsChannel) this.channel).stop(callback);
		}
	}

	@Override
	protected void destroyInstance(AbstractJmsChannel instance) throws Exception {
		if (instance instanceof SubscribableJmsChannel) {
			((SubscribableJmsChannel) this.channel).destroy();
		}
	}

}
