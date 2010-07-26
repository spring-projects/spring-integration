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

package org.springframework.integration.jms;

import java.util.concurrent.Executor;

import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.ExceptionListener;
import javax.jms.Session;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.jms.listener.AbstractMessageListenerContainer;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.jms.listener.SimpleMessageListenerContainer;
import org.springframework.jms.support.destination.DestinationResolver;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.ErrorHandler;

/**
 * A base class for managing configurable properties of a MessageListenerContainer.
 * 
 * @author Mark Fisher
 * @since 2.0
 */
abstract class MessageListenerContainerConfigurationSupport implements InitializingBean {

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

	private volatile boolean initialized;

	private final Object initializationMonitor = new Object();


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
	}

	public void setPubSubNoLocal(boolean pubSubNoLocal) {
		this.pubSubNoLocal = pubSubNoLocal;
	}

	public void setReceiveTimeout(long receiveTimeout) {
		this.receiveTimeout = receiveTimeout;
	}

	public void setRecoveryInterval(long recoveryInterval) {
		this.recoveryInterval = recoveryInterval;
	}

	public void setSessionAcknowledgeMode(int sessionAcknowledgeMode) {
		this.sessionAcknowledgeMode = sessionAcknowledgeMode;
	}

	public void setSessionTransacted(boolean sessionTransacted) {
		this.sessionTransacted = sessionTransacted;
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

	AbstractMessageListenerContainer getListenerContainer() {
		if (!this.initialized) {
			try {
				this.initialize();
			}
			catch (Exception e) {
				throw new IllegalStateException("failed to initialize listener container", e);
			}
		}
		return this.container;
	}

	public void afterPropertiesSet() throws Exception {
		this.initialize();
	}

	private void initialize() throws Exception {
		synchronized (this.initializationMonitor) {
			if (this.initialized) {
				return;
			}
			if (this.containerType == null) {
				this.containerType = DefaultMessageListenerContainer.class;
			}
			this.container = this.containerType.newInstance();
			this.container.setAcceptMessagesWhileStopping(this.acceptMessagesWhileStopping);
			this.container.setAutoStartup(this.autoStartup);
			this.container.setClientId(this.clientId);
			this.container.setConnectionFactory(this.connectionFactory);
			if (this.destination != null) {
				this.container.setDestination(this.destination);
			}
			if (this.destinationName != null) {
				this.container.setDestinationName(this.destinationName);
			}
			if (this.destinationResolver != null) {
				this.container.setDestinationResolver(this.destinationResolver);
			}
			this.container.setDurableSubscriptionName(this.durableSubscriptionName);
			this.container.setErrorHandler(this.errorHandler);
			this.container.setExceptionListener(this.exceptionListener);
			if (this.exposeListenerSession != null) {
				this.container.setExposeListenerSession(this.exposeListenerSession);
			}
			this.container.setMessageSelector(this.messageSelector);
			if (this.phase != null) {
				this.container.setPhase(this.phase);
			}
			if (this.pubSubDomain != null) {
				this.container.setPubSubDomain(this.pubSubDomain);
			}
			this.container.setSessionAcknowledgeMode(this.sessionAcknowledgeMode);
			this.container.setSessionTransacted(this.sessionTransacted);
			this.container.setSubscriptionDurable(this.subscriptionDurable);
			if (this.container instanceof DefaultMessageListenerContainer) {
				DefaultMessageListenerContainer dmlc = (DefaultMessageListenerContainer) this.container;
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
			else if (this.container instanceof SimpleMessageListenerContainer) {
				SimpleMessageListenerContainer smlc = (SimpleMessageListenerContainer) this.container;
				if (this.concurrentConsumers != null) {
					smlc.setConcurrentConsumers(this.concurrentConsumers);
				}
				smlc.setPubSubNoLocal(this.pubSubNoLocal);
				smlc.setTaskExecutor(this.taskExecutor);
			}
			this.initialized = true;
		}
	}

	// SmartLifecycle implementation (delegates to the MessageListener container)

	public int getPhase() {
		return this.getListenerContainer().getPhase();
	}

	public boolean isAutoStartup() {
		return this.autoStartup;
	}

	public boolean isRunning() {
		return this.initialized && this.container.isRunning();
	}

	/**
	 * Blocks until the listener container has subscribed; if the container does not support
	 * this test, or the caching mode is incompatible, true is returned. Otherwise blocks
	 * until timeout milliseconds have passed, or the consumer has registered.
	 * @see DefaultMessageListenerContainer.isRegisteredWithDestination()
	 * @param timeout Timeout in milliseconds.
	 * @return True if a subscriber has connected or the container/attributes does not support
	 * the test. False if a valid container does not have a registered consumer within 
	 * timeout milliseconds.
	 */
	public boolean waitRegisteredWithDestination(long timeout) {
		AbstractMessageListenerContainer container = this.getListenerContainer();
		if (container instanceof DefaultMessageListenerContainer) {
			DefaultMessageListenerContainer listenerContainer = 
				(DefaultMessageListenerContainer) container;
			if (listenerContainer.getCacheLevel() != DefaultMessageListenerContainer.CACHE_CONSUMER) {
				return true;
			}
			while (timeout > 0) {
				if (listenerContainer.isRegisteredWithDestination()) {
					return true;
				}
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) { }
				timeout -= 100;
			}
			return false;
		}
		return true;
	}
	
	public void start() {
		this.getListenerContainer().start();
	}

	public void stop() {
		if (this.isRunning()) {
			this.container.stop();
		}
	}

	public void stop(Runnable callback) {
		if (this.isRunning()) {
			this.container.stop(callback);
		}
		else {
			callback.run();
		}
	}

}
