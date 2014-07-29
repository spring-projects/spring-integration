/*
 * Copyright 2013 the original author or authors
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 */
package org.springframework.integration.redis.inbound;

import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.BoundListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.integration.channel.MessagePublishingErrorHandler;
import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.integration.redis.event.RedisExceptionEvent;
import org.springframework.integration.support.channel.BeanFactoryChannelResolver;
import org.springframework.integration.util.ErrorHandlingTaskExecutor;
import org.springframework.jmx.export.annotation.ManagedMetric;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.util.Assert;

/**
 * @author Mark Fisher
 * @author Gunnar Hillert
 * @author Artem Bilan
 * @since 3.0
 */
@ManagedResource
public class RedisQueueMessageDrivenEndpoint extends MessageProducerSupport implements ApplicationEventPublisherAware {

	public static final long DEFAULT_RECEIVE_TIMEOUT = 1000;

	public static final long DEFAULT_RECOVERY_INTERVAL = 5000;

	private final BoundListOperations<String, byte[]> boundListOperations;

	private volatile ApplicationEventPublisher applicationEventPublisher;

	private volatile MessageChannel errorChannel;

	private volatile Executor taskExecutor;

	private volatile RedisSerializer<?> serializer = new JdkSerializationRedisSerializer();

	private volatile boolean expectMessage = false;

	private volatile long receiveTimeout = DEFAULT_RECEIVE_TIMEOUT;

	private volatile long recoveryInterval = DEFAULT_RECOVERY_INTERVAL;

	private volatile long stopTimeout = DEFAULT_RECEIVE_TIMEOUT;

	private volatile boolean active;

	private volatile boolean listening;

	/**
	 * @param queueName         Must not be an empty String
	 * @param connectionFactory Must not be null
	 */
	public RedisQueueMessageDrivenEndpoint(String queueName, RedisConnectionFactory connectionFactory) {
		Assert.hasText(queueName, "'queueName' is required");
		Assert.notNull(connectionFactory, "'connectionFactory' must not be null");
		RedisTemplate<String, byte[]> template = new RedisTemplate<String, byte[]>();
		template.setConnectionFactory(connectionFactory);
		template.setEnableDefaultSerializer(false);
		template.setKeySerializer(new StringRedisSerializer());
		template.afterPropertiesSet();
		this.boundListOperations = template.boundListOps(queueName);
	}

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
		this.applicationEventPublisher = applicationEventPublisher;
	}

	public void setSerializer(RedisSerializer<?> serializer) {
		this.serializer = serializer;
	}

	/**
	 * When data is retrieved from the Redis queue, does the returned data represent
	 * just the payload for a Message, or does the data represent a serialized
	 * {@link Message}?. {@code expectMessage} defaults to false. This means
	 * the retrieved data will be used as the payload for a new Spring Integration
	 * Message. Otherwise, the data is deserialized as Spring Integration
	 * Message.
	 * @param expectMessage Defaults to false
	 */
	public void setExpectMessage(boolean expectMessage) {
		this.expectMessage = expectMessage;
	}

	/**
	 * This timeout (milliseconds) is used when retrieving elements from the queue
	 * specified by {@link #boundListOperations}.
	 * <p> If the queue does contain elements, the data is retrieved immediately. However,
	 * if the queue is empty, the Redis connection is blocked until either an element
	 * can be retrieved from the queue or until the specified timeout passes.
	 * <p> A timeout of zero can be used to block indefinitely. If not set explicitly
	 * the timeout value will default to {@code 1000}
	 * <p> See also: http://redis.io/commands/brpop
	 * @param receiveTimeout Must be non-negative. Specified in milliseconds.
	 */
	public void setReceiveTimeout(long receiveTimeout) {
		Assert.isTrue(receiveTimeout > 0, "'receiveTimeout' must be > 0.");
		this.receiveTimeout = receiveTimeout;
	}

	/**
	 * @param stopTimeout the timeout to block {@link #doStop()} until the last message will be processed
	 * or this timeout is reached. Should be less then or equal to {@link #receiveTimeout}
	 * @since 4.0.3
	 */
	public void setStopTimeout(long stopTimeout) {
		this.stopTimeout = stopTimeout;
	}

	public void setTaskExecutor(Executor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

	@Override
	public void setErrorChannel(MessageChannel errorChannel) {
		super.setErrorChannel(errorChannel);
		this.errorChannel = errorChannel;
	}

	public void setRecoveryInterval(long recoveryInterval) {
		this.recoveryInterval = recoveryInterval;
	}

	@Override
	protected void onInit() {
		super.onInit();
		if (this.expectMessage) {
			Assert.notNull(this.serializer, "'serializer' has to be provided where 'expectMessage == true'.");
		}
		if (this.taskExecutor == null) {
			String beanName = this.getComponentName();
			this.taskExecutor = new SimpleAsyncTaskExecutor((beanName == null ? "" : beanName + "-")
					+ this.getComponentType());
		}
		if (!(this.taskExecutor instanceof ErrorHandlingTaskExecutor) && this.getBeanFactory() != null) {
			MessagePublishingErrorHandler errorHandler =
					new MessagePublishingErrorHandler(new BeanFactoryChannelResolver(this.getBeanFactory()));
			errorHandler.setDefaultErrorChannel(this.errorChannel);
			this.taskExecutor = new ErrorHandlingTaskExecutor(this.taskExecutor, errorHandler);
		}
	}

	@Override
	public String getComponentType() {
		return "redis:queue-inbound-channel-adapter";
	}

	@SuppressWarnings("unchecked")
	private void popMessageAndSend() {
		Message<Object> message = null;

		byte[] value = null;
		try {
			value = this.boundListOperations.rightPop(this.receiveTimeout, TimeUnit.MILLISECONDS);
		}
		catch (Exception e) {
			this.listening = false;
			if (this.active) {
				logger.error("Failed to execute listening task. Will attempt to resubmit in " + this.recoveryInterval
						+ " milliseconds.", e);
				this.publishException(e);
				this.sleepBeforeRecoveryAttempt();
			}
			else {
				logger.debug("Failed to execute listening task. " + e.getClass() + ": " + e.getMessage());
			}
			return;
		}

		if (value != null) {
			if (this.expectMessage) {
				try {
					message = (Message<Object>) this.serializer.deserialize(value);
				}
				catch (Exception e) {
					throw new MessagingException("Deserialization of Message failed.", e);
				}
			}
			else {
				Object payload = value;
				if (this.serializer != null) {
					payload = this.serializer.deserialize(value);
				}
				message = this.getMessageBuilderFactory().withPayload(payload).build();
			}
		}

		if (message != null) {
			if (this.listening) {
				this.sendMessage(message);
			}
			else {
				this.boundListOperations.rightPush(value);
			}
		}
	}

	@Override
	protected void doStart() {
		if (!this.active) {
			this.active = true;
			this.restart();
		}
	}

	/**
	 * Sleep according to the specified recovery interval.
	 * Called between recovery attempts.
	 */
	private void sleepBeforeRecoveryAttempt() {
		if (this.recoveryInterval > 0) {
			try {
				Thread.sleep(this.recoveryInterval);
			}
			catch (InterruptedException e) {
				logger.debug("Thread interrupted while sleeping the recovery interval");
				Thread.currentThread().interrupt();
			}
		}
	}

	private void publishException(Exception e) {
		if (this.applicationEventPublisher != null) {
			this.applicationEventPublisher.publishEvent(new RedisExceptionEvent(this, e));
		}
		else {
			if (logger.isDebugEnabled()) {
				logger.debug("No application event publisher for exception: " + e.getMessage());
			}
		}
	}

	private void restart() {
		this.taskExecutor.execute(new ListenerTask());
	}

	@Override
	protected void doStop() {
		try {
			this.active = false;
			this.lifecycleCondition.await(Math.min(this.stopTimeout, this.receiveTimeout), TimeUnit.MICROSECONDS);
		}
		catch (InterruptedException e) {
			logger.debug("Thread interrupted while stopping the endpoint");
			Thread.currentThread().interrupt();
		}
		finally {
			this.listening = false;
		}
	}

	public boolean isListening() {
		return listening;
	}

	/**
	 * Returns the size of the Queue specified by {@link #boundListOperations}. The queue is
	 * represented by a Redis list. If the queue does not exist <code>0</code>
	 * is returned. See also http://redis.io/commands/llen
	 *
	 * @return Size of the queue. Never negative.
	 */
	@ManagedMetric
	public long getQueueSize() {
		return this.boundListOperations.size();
	}

	/**
	 * Clear the Redis Queue specified by {@link #boundListOperations}.
	 */
	@ManagedOperation
	public void clearQueue() {
		this.boundListOperations.getOperations().delete(this.boundListOperations.getKey());
	}


	private class ListenerTask implements Runnable {

		@Override
		public void run() {
			try {
				while (RedisQueueMessageDrivenEndpoint.this.active) {
					RedisQueueMessageDrivenEndpoint.this.listening = true;
					RedisQueueMessageDrivenEndpoint.this.popMessageAndSend();
				}
			}
			finally {
				if (RedisQueueMessageDrivenEndpoint.this.active) {
					RedisQueueMessageDrivenEndpoint.this.restart();
				}
				else {
					RedisQueueMessageDrivenEndpoint.this.lifecycleLock.lock();
					try {
						RedisQueueMessageDrivenEndpoint.this.lifecycleCondition.signalAll();
					}
					finally {
						RedisQueueMessageDrivenEndpoint.this.lifecycleLock.unlock();
					}
				}
			}
		}

	}

}
