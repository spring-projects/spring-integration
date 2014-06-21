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

package org.springframework.integration.amqp.config;

import java.util.List;
import java.util.concurrent.Executor;

import org.aopalliance.aop.Advice;

import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.support.MessagePropertiesConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.context.Lifecycle;
import org.springframework.context.SmartLifecycle;
import org.springframework.integration.amqp.channel.AbstractAmqpChannel;
import org.springframework.integration.amqp.channel.PointToPointSubscribableAmqpChannel;
import org.springframework.integration.amqp.channel.PollableAmqpChannel;
import org.springframework.integration.amqp.channel.PublishSubscribeAmqpChannel;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.interceptor.TransactionAttribute;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ErrorHandler;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * If point-to-point, we send to the default exchange with the routing key
 * equal to "[beanName]" and we declare that same Queue and register a listener
 * if message-driven or poll explicitly otherwise. If publish-subscribe, we declare
 * a FanoutExchange named "si.fanout.[beanName]" and we send to that without any
 * routing key, and on the receiving side, we create an anonymous Queue that is
 * bound to that exchange.
 *
 * @author Mark Fisher
 * @author Gary Russell
 * @since 2.1
 */
public class AmqpChannelFactoryBean extends AbstractFactoryBean<AbstractAmqpChannel> implements SmartLifecycle, DisposableBean, BeanNameAware {

	private volatile AbstractAmqpChannel channel;

	private volatile List<ChannelInterceptor> interceptors;

	private final boolean messageDriven;

	private final AmqpTemplate amqpTemplate = new RabbitTemplate();

	private volatile SimpleMessageListenerContainer container;

	private volatile AmqpAdmin amqpAdmin;

	private volatile FanoutExchange exchange;

	private volatile String queueName;

	private volatile boolean autoStartup = true;

	private volatile Advice[] adviceChain;

	private volatile Integer concurrentConsumers;

	private volatile ConnectionFactory connectionFactory;

	private volatile MessagePropertiesConverter messagePropertiesConverter;

	private volatile ErrorHandler errorHandler;

	private volatile Boolean exposeListenerChannel;

	private volatile Integer phase;

	private volatile Integer prefetchCount;

	private volatile Boolean isPubSub;

	private volatile Long receiveTimeout;

	private volatile Long recoveryInterval;

	private volatile Long shutdownTimeout;

	private volatile String beanName;

	private volatile AcknowledgeMode acknowledgeMode;

	/**
	 * This value differs from the container implementations' default (which is false).
	 */
	private volatile boolean channelTransacted = true;

	private volatile Executor taskExecutor;

	private volatile PlatformTransactionManager transactionManager;

	private volatile TransactionAttribute transactionAttribute;

	private volatile Integer txSize;

	private volatile Integer maxSubscribers;

	private volatile Boolean missingQueuesFatal;


	public AmqpChannelFactoryBean() {
		this(true);
	}

	public AmqpChannelFactoryBean(boolean messageDriven) {
		this.messageDriven = messageDriven;
	}


	@Override
	public void setBeanName(String name) {
		this.beanName = name;
	}

	public void setInterceptors(List<ChannelInterceptor> interceptors) {
		this.interceptors = interceptors;
	}

	/**
	 * This is an optional reference to an AmqpAdmin to use when
	 * declaring a Queue implicitly for a PollableAmqpChannel. It
	 * is not needed for the message-driven (Subscribable) channels
	 * since those are able to create a RabbitAdmin instance using
	 * the underlying listener container's ConnectionFactory.
	 *
	 * @param amqpAdmin The amqp admin.
	 */
	public void setAmqpAdmin(AmqpAdmin amqpAdmin) {
		this.amqpAdmin = amqpAdmin;
	}

	/**
	 * Set the FanoutExchange to use. This is only relevant for
	 * publish-subscribe-channels, and even then if not provided,
	 * a FanoutExchange will be implicitly created.
	 *
	 * @param exchange The fanout exchange.
	 */
	public void setExchange(FanoutExchange exchange) {
		this.exchange = exchange;
	}

	/**
	 * Set the Queue name to use. This is only relevant for
	 * point-to-point channels, even then if not provided,
	 * a Queue will be implicitly created.
	 *
	 * @param queueName The queue name.
	 */
	public void setQueueName(String queueName) {
		this.queueName = queueName;
	}

	/*
	 * Template-only properties
	 */

	public void setEncoding(String encoding) {
		if (this.amqpTemplate instanceof RabbitTemplate) {
			((RabbitTemplate) this.amqpTemplate).setEncoding(encoding);
		}
		else if (logger.isInfoEnabled()) {
			logger.info("AmqpTemplate is not a RabbitTemplate, so configured 'encoding' value will be ignored.");
		}
	}

	public void setMessageConverter(MessageConverter messageConverter) {
		if (this.amqpTemplate instanceof RabbitTemplate) {
			((RabbitTemplate) this.amqpTemplate).setMessageConverter(messageConverter);
		}
		else if (logger.isInfoEnabled()) {
			logger.info("AmqpTemplate is not a RabbitTemplate, so configured MessageConverter will be ignored.");
		}
	}

	/*
	 * Template and Container properties
	 */

	public void setChannelTransacted(boolean channelTransacted) {
		this.channelTransacted = channelTransacted;
		if (this.amqpTemplate instanceof RabbitTemplate) {
			((RabbitTemplate) this.amqpTemplate).setChannelTransacted(channelTransacted);
		}
	}

	public void setConnectionFactory(ConnectionFactory connectionFactory) {
		this.connectionFactory = connectionFactory;
		if (this.amqpTemplate instanceof RabbitTemplate) {
			((RabbitTemplate) this.amqpTemplate).setConnectionFactory(this.connectionFactory);
		}
	}

	public void setMessagePropertiesConverter(MessagePropertiesConverter messagePropertiesConverter) {
		this.messagePropertiesConverter = messagePropertiesConverter;
		if (this.amqpTemplate instanceof RabbitTemplate) {
			((RabbitTemplate) this.amqpTemplate).setMessagePropertiesConverter(messagePropertiesConverter);
		}
	}

	/*
	 * Container-only properties
	 */

	public void setAcknowledgeMode(AcknowledgeMode acknowledgeMode) {
		this.acknowledgeMode = acknowledgeMode;
	}

	public void setAdviceChain(Advice[] adviceChain) {
		this.adviceChain = adviceChain;
	}

	public void setAutoStartup(boolean autoStartup) {
		this.autoStartup = autoStartup;
	}

	public void setConcurrentConsumers(int concurrentConsumers) {
		this.concurrentConsumers = concurrentConsumers;
	}

	public void setErrorHandler(ErrorHandler errorHandler) {
		this.errorHandler = errorHandler;
	}

	public void setExposeListenerChannel(boolean exposeListenerChannel) {
		this.exposeListenerChannel = exposeListenerChannel;
	}

	public void setPhase(int phase) {
		this.phase = phase;
	}

	public void setPrefetchCount(int prefetchCount) {
		this.prefetchCount = prefetchCount;
	}

	public void setPubSub(boolean pubSub) {
		this.isPubSub = pubSub;
	}

	public void setReceiveTimeout(long receiveTimeout) {
		this.receiveTimeout = receiveTimeout;
	}

	public void setRecoveryInterval(long recoveryInterval) {
		this.recoveryInterval = recoveryInterval;
	}

	public void setShutdownTimeout(long shutdownTimeout) {
		this.shutdownTimeout = shutdownTimeout;
	}

	public void setTaskExecutor(Executor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

	public void setTransactionAttribute(TransactionAttribute transactionAttribute) {
		this.transactionAttribute = transactionAttribute;
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	public void setTxSize(int txSize) {
		this.txSize = txSize;
	}

	public void setMaxSubscribers(int maxSubscribers) {
		this.maxSubscribers = maxSubscribers;
	}

	public void setMissingQueuesFatal(Boolean missingQueuesFatal) {
		this.missingQueuesFatal = missingQueuesFatal;
	}

	@Override
	public Class<?> getObjectType() {
		return (this.channel != null) ? this.channel.getClass() : AbstractAmqpChannel.class;
	}

	@Override
	protected AbstractAmqpChannel createInstance() throws Exception {
		if (this.messageDriven) {
			this.container = this.createContainer();
			if (this.amqpTemplate instanceof InitializingBean) {
				((InitializingBean) this.amqpTemplate).afterPropertiesSet();
			}
			if (this.isPubSub) {
				PublishSubscribeAmqpChannel pubsub = new PublishSubscribeAmqpChannel(
						this.beanName, this.container, this.amqpTemplate);
				if (this.exchange != null) {
					pubsub.setExchange(this.exchange);
				}
				if (this.maxSubscribers != null) {
					pubsub.setMaxSubscribers(this.maxSubscribers);
				}
				this.channel = pubsub;
			}
			else {
				PointToPointSubscribableAmqpChannel p2p = new PointToPointSubscribableAmqpChannel(
						this.beanName, this.container, this.amqpTemplate);
				if (StringUtils.hasText(this.queueName)) {
					p2p.setQueueName(this.queueName);
				}
				if (this.maxSubscribers != null) {
					p2p.setMaxSubscribers(this.maxSubscribers);
				}
				this.channel = p2p;
			}
		}
		else {
			Assert.isTrue(!Boolean.TRUE.equals(this.isPubSub),
					"An AMQP 'publish-subscribe-channel' must be message-driven.");
			PollableAmqpChannel pollable = new PollableAmqpChannel(this.beanName, this.amqpTemplate);
			if (this.amqpAdmin != null) {
				pollable.setAmqpAdmin(this.amqpAdmin);
			}
			if (StringUtils.hasText(this.queueName)) {
				pollable.setQueueName(this.queueName);
			}
			this.channel = pollable;
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

	private SimpleMessageListenerContainer createContainer() throws Exception {
		//if (!messageDriven) TODO: no container attributes would apply if not message-driven
		SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
		if (this.acknowledgeMode != null) {
			container.setAcknowledgeMode(this.acknowledgeMode);
		}
		if (!ObjectUtils.isEmpty(this.adviceChain)) {
			container.setAdviceChain(this.adviceChain);
		}
		container.setAutoStartup(this.autoStartup);
		container.setChannelTransacted(this.channelTransacted);
		if (this.concurrentConsumers != null) {
			container.setConcurrentConsumers(this.concurrentConsumers);
		}
		container.setConnectionFactory(this.connectionFactory);
		if (this.errorHandler != null) {
			container.setErrorHandler(this.errorHandler);
		}
		if (this.exposeListenerChannel != null) {
			container.setExposeListenerChannel(this.exposeListenerChannel);
		}
		if (this.messagePropertiesConverter != null) {
			container.setMessagePropertiesConverter(this.messagePropertiesConverter);
		}
		if (this.phase != null) {
			container.setPhase(this.phase);
		}
		if (this.prefetchCount != null) {
			container.setPrefetchCount(this.prefetchCount);
		}
		if (this.receiveTimeout != null) {
			container.setReceiveTimeout(this.receiveTimeout);
		}
		if (this.recoveryInterval != null) {
			container.setRecoveryInterval(this.recoveryInterval);
		}
		if (this.shutdownTimeout != null) {
			container.setShutdownTimeout(this.shutdownTimeout);
		}
		if (this.taskExecutor != null) {
			container.setTaskExecutor(this.taskExecutor);
		}
		if (this.transactionAttribute != null) {
			container.setTransactionAttribute(this.transactionAttribute);
		}
		if (this.transactionManager != null) {
			container.setTransactionManager(this.transactionManager);
		}
		if (this.txSize != null) {
			container.setTxSize(this.txSize);
		}
		if (this.missingQueuesFatal != null) {
			container.setMissingQueuesFatal(this.missingQueuesFatal);
		}
		return container;
	}

	/*
	 * SmartLifecycle implementation (delegates to the created channel if message-driven)
	 */

	@Override
	public boolean isAutoStartup() {
		return (this.channel instanceof SmartLifecycle) ?
				((SmartLifecycle) this.channel).isAutoStartup() : false;
	}

	@Override
	public int getPhase() {
		return (this.channel instanceof SmartLifecycle) ?
				((SmartLifecycle) this.channel).getPhase() : 0;
	}

	@Override
	public boolean isRunning() {
		return (this.channel instanceof Lifecycle) ?
				((Lifecycle) this.channel).isRunning() : false;
	}

	@Override
	public void start() {
		if (this.channel instanceof Lifecycle) {
			((Lifecycle) this.channel).start();
		}
	}

	@Override
	public void stop() {
		if (this.channel instanceof Lifecycle) {
			((Lifecycle) this.channel).stop();
		}
	}

	@Override
	public void stop(Runnable callback) {
		if (this.channel instanceof SmartLifecycle) {
			((SmartLifecycle) this.channel).stop(callback);
		}
	}

	@Override
	protected void destroyInstance(AbstractAmqpChannel instance) throws Exception {
		if (instance instanceof DisposableBean) {
			((DisposableBean) this.channel).destroy();
		}
	}

}
