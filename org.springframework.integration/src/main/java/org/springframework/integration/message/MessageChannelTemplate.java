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

package org.springframework.integration.message;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.integration.channel.BlockingChannel;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.channel.PollableChannel;
import org.springframework.integration.message.selector.MessageSelector;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.Assert;

/**
 * This is the central class for invoking message exchange operations across
 * {@link MessageChannel}s. It supports one-way send and receive calls as well
 * as request/reply.
 * <p>
 * To enable transactions, configure the 'transactionManager' property with a
 * reference to an instance of Spring's {@link PlatformTransactionManager}
 * strategy and optionally provide the other transactional attributes
 * (e.g. 'propagationBehaviorName').
 * 
 * @author Mark Fisher
 */
public class MessageChannelTemplate implements InitializingBean {

	protected final Log logger = LogFactory.getLog(this.getClass());

	private volatile long sendTimeout = -1;

	private volatile long receiveTimeout = -1;

	private volatile PlatformTransactionManager transactionManager;

	private volatile TransactionTemplate transactionTemplate;

	private volatile String propagationBehaviorName = "PROPAGATION_REQUIRED";

	private volatile String isolationLevelName = "ISOLATION_DEFAULT";

	private volatile int transactionTimeout = -1;

	private volatile boolean readOnly = false;

	private volatile boolean initialized;

	private final Object initializationMonitor = new Object();


	/**
	 * Specify the timeout value to use for send operations.
	 * 
	 * @param sendTimeout the send timeout in milliseconds
	 */
	public void setSendTimeout(long sendTimeout) {
		this.sendTimeout = sendTimeout;
	}

	/**
	 * Specify the timeout value to use for receive operations.
	 *  
	 * @param receiveTimeout the receive timeout in milliseconds
	 */
	public void setReceiveTimeout(long receiveTimeout) {
		this.receiveTimeout = receiveTimeout;
	}

	/**
	 * Specify a transaction manager to use for all exchange operations.
	 * If none is provided, then the operations will occur without any
	 * transactional behavior (i.e. there is no default transaction manager).
	 */
	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	public void setPropagationBehaviorName(String propagationBehaviorName) {
		this.propagationBehaviorName = propagationBehaviorName;
	}

	public void setIsolationLevelName(String isolationLevelName) {
		this.isolationLevelName = isolationLevelName;
	}

	public void setTransactionTimeout(int transactionTimeout) {
		this.transactionTimeout = transactionTimeout;
	}

	public void setTransactionReadOnly(boolean readOnly) {
		this.readOnly = readOnly;
	}

	private TransactionTemplate getTransactionTemplate() {
		if (!this.initialized) {
			this.afterPropertiesSet();
		}
		return this.transactionTemplate;
	}

	public void afterPropertiesSet() {
		synchronized (this.initializationMonitor) {
			if (this.initialized) {
				return;
			}
			if (this.transactionManager != null) {
				TransactionTemplate template = new TransactionTemplate(this.transactionManager);
				template.setPropagationBehaviorName(this.propagationBehaviorName);
				template.setIsolationLevelName(this.isolationLevelName);
				template.setTimeout(this.transactionTimeout);
				template.setReadOnly(this.readOnly);
				this.transactionTemplate = template;
			}
			this.initialized = true;
		}
	}

	public boolean send(final Message<?> message, final MessageChannel channel) {
		TransactionTemplate txTemplate = this.getTransactionTemplate();
		if (txTemplate != null) {
			return (Boolean) txTemplate.execute(new TransactionCallback() {
				public Object doInTransaction(TransactionStatus status) {
					return doSend(message, channel);
				}
			});
		}
		return this.doSend(message, channel);
	}

	public Message<?> receive(final PollableChannel channel) {
		TransactionTemplate txTemplate = this.getTransactionTemplate();
		if (txTemplate != null) {
			return (Message<?>) txTemplate.execute(new TransactionCallback() {
				public Object doInTransaction(TransactionStatus status) {
					return doReceive(channel);
				}
			});
		}
		return this.doReceive(channel);
	}

	public Message<?> sendAndReceive(final Message<?> request, final MessageChannel channel) {
		TransactionTemplate txTemplate = this.getTransactionTemplate();
		if (txTemplate != null) {
			return (Message<?>) txTemplate.execute(new TransactionCallback() {
				public Object doInTransaction(TransactionStatus status) {
					return doSendAndReceive(request, channel);
				}
			});
		}
		return this.doSendAndReceive(request, channel);
	}

	private boolean doSend(Message<?> message, MessageChannel channel) {
		Assert.notNull(channel, "channel must not be null");
		long timeout = this.sendTimeout;
		boolean sent = (timeout >= 0 && channel instanceof BlockingChannel)
				? ((BlockingChannel) channel).send(message, timeout)
				: channel.send(message);
		if (!sent && this.logger.isTraceEnabled()) {
			this.logger.trace("failed to send message to channel '" + channel + "' within timeout: " + timeout);
		}
		return sent;
	}

	private Message<?> doReceive(PollableChannel channel) {
		Assert.notNull(channel, "channel must not be null");
		long timeout = this.receiveTimeout;
		Message<?> message = (timeout >= 0)
				? channel.receive(timeout)
				: channel.receive();
		if (message == null && this.logger.isTraceEnabled()) {
			this.logger.trace("failed to receive message from channel '" + channel + "' within timeout: " + timeout);
		}
		return message;
	}

	private Message<?> doSendAndReceive(Message<?> request, MessageChannel channel) {
		TemporaryReturnAddress returnAddress = new TemporaryReturnAddress(this.receiveTimeout);
		request = MessageBuilder.fromMessage(request).setReturnAddress(returnAddress).build();
		if (!this.doSend(request, channel)) {
			return null;
		}
		return this.doReceive(returnAddress);
	}


	@SuppressWarnings("unchecked")
	private static class TemporaryReturnAddress implements PollableChannel {

		private volatile Message<?> message;

		private final long receiveTimeout;

		private final CountDownLatch latch = new CountDownLatch(1);


		public TemporaryReturnAddress(long receiveTimeout) {
			this.receiveTimeout = receiveTimeout;
		}


		public String getName() {
			return "temporaryReplyChannel";
		}

		public Message receive() {
			return this.receive(-1);
		}

		public Message receive(long timeout) {
			try {
				if (this.receiveTimeout < 0) {
					this.latch.await();
				}
				else {
					this.latch.await(this.receiveTimeout, TimeUnit.MILLISECONDS);
				}
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			return this.message;
		}

		public boolean send(Message<?> message) {
			this.message = message;
			this.latch.countDown();
			return true;
		}

		public List<Message<?>> clear() {
			return null;
		}

		public List<Message<?>> purge(MessageSelector selector) {
			return null;
		}
	}

}
