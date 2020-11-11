/*
 * Copyright 2014-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.redis.inbound;

import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.BeanClassLoaderAware;
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
import org.springframework.integration.gateway.MessagingGatewaySupport;
import org.springframework.integration.redis.event.RedisExceptionEvent;
import org.springframework.integration.support.management.IntegrationManagedResource;
import org.springframework.integration.util.ErrorHandlingTaskExecutor;
import org.springframework.jmx.export.annotation.ManagedMetric;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.scheduling.SchedulingAwareRunnable;
import org.springframework.util.Assert;

/**
 * @author David Liu
 * @author Artem Bilan
 * @author Gary Russell
 * @author Matthias Jeschke
 *
 * @since 4.1
 */
@ManagedResource
@IntegrationManagedResource
public class RedisQueueInboundGateway extends MessagingGatewaySupport
		implements ApplicationEventPublisherAware, BeanClassLoaderAware {

	private static final String QUEUE_NAME_SUFFIX = ".reply";

	public static final long DEFAULT_RECEIVE_TIMEOUT = 1000;

	public static final long DEFAULT_RECOVERY_INTERVAL = 5000;

	private final RedisTemplate<String, byte[]> template;

	private final BoundListOperations<String, byte[]> boundListOperations;

	private ApplicationEventPublisher applicationEventPublisher;

	private boolean serializerExplicitlySet;

	private Executor taskExecutor;

	private RedisSerializer<?> serializer;

	private long receiveTimeout = DEFAULT_RECEIVE_TIMEOUT;

	private long recoveryInterval = DEFAULT_RECOVERY_INTERVAL;

	private boolean extractPayload = true;

	private volatile boolean listening;

	private volatile Runnable stopCallback;

	/**
	 * @param queueName Must not be an empty String
	 * @param connectionFactory Must not be null
	 */
	public RedisQueueInboundGateway(String queueName, RedisConnectionFactory connectionFactory) {
		Assert.hasText(queueName, "'queueName' is required");
		Assert.notNull(connectionFactory, "'connectionFactory' must not be null");
		this.template = new RedisTemplate<>();
		this.template.setConnectionFactory(connectionFactory);
		this.template.setEnableDefaultSerializer(false);
		this.template.setKeySerializer(new StringRedisSerializer());
		this.template.afterPropertiesSet();
		this.boundListOperations = this.template.boundListOps(queueName);
	}

	public void setExtractPayload(boolean extractPayload) {
		this.extractPayload = extractPayload;
	}

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
		this.applicationEventPublisher = applicationEventPublisher;
	}

	@Override
	public void setBeanClassLoader(ClassLoader beanClassLoader) {
		if (!this.serializerExplicitlySet) {
			this.serializer = new JdkSerializationRedisSerializer(beanClassLoader);
		}
	}

	public void setSerializer(RedisSerializer<?> serializer) {
		this.serializer = serializer;
		this.serializerExplicitlySet = true;
	}

	/**
	 * This timeout (milliseconds) is used when retrieving elements from the queue
	 * specified by {@link #boundListOperations}.
	 * <p> If the queue does contain elements, the data is retrieved immediately. However,
	 * if the queue is empty, the Redis connection is blocked until either an element
	 * can be retrieved from the queue or until the specified timeout passes.
	 * <p> A timeout of zero can be used to block indefinitely. If not set explicitly
	 * the timeout value will default to {@code 1000}
	 * <p> See also: https://redis.io/commands/brpop
	 * @param receiveTimeout Must be non-negative. Specified in milliseconds.
	 */
	public void setReceiveTimeout(long receiveTimeout) {
		Assert.isTrue(receiveTimeout >= 0, "'receiveTimeout' must be >= 0.");
		this.receiveTimeout = receiveTimeout;
	}

	public void setTaskExecutor(Executor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

	public void setRecoveryInterval(long recoveryInterval) {
		this.recoveryInterval = recoveryInterval;
	}

	@Override
	protected void onInit() {
		super.onInit();
		if (!this.extractPayload) {
			Assert.notNull(this.serializer, "'serializer' has to be provided where 'extractPayload == false'.");
		}
		if (this.taskExecutor == null) {
			String beanName = getComponentName();
			this.taskExecutor = new SimpleAsyncTaskExecutor((beanName == null ? "" : beanName + "-")
					+ getComponentType());
		}
		if (!(this.taskExecutor instanceof ErrorHandlingTaskExecutor) && getBeanFactory() != null) {
			MessagePublishingErrorHandler errorHandler = new MessagePublishingErrorHandler();
			errorHandler.setBeanFactory(getBeanFactory());
			errorHandler.setDefaultErrorChannel(getErrorChannel());
			this.taskExecutor = new ErrorHandlingTaskExecutor(this.taskExecutor, errorHandler);
		}
	}

	@Override
	public String getComponentType() {
		return "redis:queue-inbound-gateway";
	}

	private void handlePopException(Exception e) {
		this.listening = false;
		if (isActive()) {
			logger.error(e, () ->
					"Failed to execute listening task. Will attempt to resubmit in " + this.recoveryInterval
							+ " milliseconds.");
			publishException(e);
			sleepBeforeRecoveryAttempt();
		}
		else {
			logger.debug(() -> "Failed to execute listening task. " + e.getClass() + ": " + e.getMessage());
		}
	}

	private void receiveAndReply() {
		byte[] value;
		try {
			value = this.boundListOperations.rightPop(this.receiveTimeout, TimeUnit.MILLISECONDS);
		}
		catch (Exception e) {
			handlePopException(e);
			return;
		}
		String uuid;
		if (value != null) {
			if (!isActive()) {
				this.boundListOperations.rightPush(value);
				return;
			}
			uuid = StringRedisSerializer.UTF_8.deserialize(value);
			if (uuid == null) {
				return;
			}
			try {
				value = this.template.boundListOps(uuid).rightPop(this.receiveTimeout, TimeUnit.MILLISECONDS);
			}
			catch (Exception e) {
				handlePopException(e);
				return;
			}
			if (value != null) {
				getRequestSendAndProduceReply(value, uuid);
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void getRequestSendAndProduceReply(byte[] value, String uuid) {
		if (!isActive()) {
			this.template.boundListOps(uuid).rightPush(value);
			byte[] serialized = StringRedisSerializer.UTF_8.serialize(uuid);
			if (serialized != null) {
				this.boundListOperations.rightPush(serialized);
			}
			return;
		}

		Message<Object> requestMessage = prepareRequestMessage(value);

		if (requestMessage != null) {
			Message<?> replyMessage = sendAndReceiveMessage(requestMessage);
			if (replyMessage != null) {
				byte[] replyPayload = null;
				if (this.extractPayload) {
					replyPayload = extractReplyPayload(replyMessage);
				}
				else {
					if (this.serializer != null) {
						replyPayload = ((RedisSerializer<Object>) this.serializer).serialize(replyMessage);
					}
				}
				if (replyPayload != null) {
					this.template.boundListOps(uuid + QUEUE_NAME_SUFFIX).leftPush(replyPayload);
				}
			}
		}
	}

	@Nullable
	@SuppressWarnings("unchecked")
	private Message<Object> prepareRequestMessage(byte[] value) {
		Message<Object> requestMessage;
		if (this.extractPayload) {
			Object payload = value;
			if (this.serializer != null) {
				payload = this.serializer.deserialize(value);
				if (payload == null) {
					return null;
				}
			}
			requestMessage = getMessageBuilderFactory().withPayload(payload).build();
		}
		else {
			try {
				requestMessage = (Message<Object>) this.serializer.deserialize(value);
				if (requestMessage == null) {
					return null;
				}
			}
			catch (Exception e) {
				throw new MessagingException("Deserialization of Message failed.", e);
			}
		}
		return requestMessage;
	}

	@SuppressWarnings("unchecked")
	private byte[] extractReplyPayload(Message<?> replyMessage) {
		byte[] value;
		if (!(replyMessage.getPayload() instanceof byte[])) {
			if (replyMessage.getPayload() instanceof String && !this.serializerExplicitlySet) {
				value = StringRedisSerializer.UTF_8.serialize((String) replyMessage.getPayload());
			}
			else {
				value = ((RedisSerializer<Object>) this.serializer).serialize(replyMessage.getPayload());
			}
		}
		else {
			value = (byte[]) replyMessage.getPayload();
		}
		return value;
	}

	@Override
	protected void doStart() {
		super.doStart();
		restart();
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
			logger.debug(() -> "No application event publisher for exception: " + e.getMessage());
		}
	}

	private void restart() {
		this.taskExecutor.execute(new ListenerTask());
	}

	@Override
	protected void doStop(Runnable callback) {
		this.stopCallback = callback;
		doStop();
	}

	@Override
	protected void doStop() {
		super.doStop();
		this.listening = false;
	}

	public boolean isListening() {
		return this.listening;
	}

	/**
	 * Returns the size of the Queue specified by {@link #boundListOperations}. The queue is
	 * represented by a Redis list. If the queue does not exist <code>0</code>
	 * is returned. See also https://redis.io/commands/llen
	 * @return Size of the queue. Never negative.
	 */
	@ManagedMetric
	public long getQueueSize() {
		Long size = this.boundListOperations.size();
		return size == null ? 0 : size;
	}

	/**
	 * Clear the Redis Queue specified by {@link #boundListOperations}.
	 */
	@ManagedOperation
	public void clearQueue() {
		this.boundListOperations.getOperations().delete(this.boundListOperations.getKey());
	}


	private class ListenerTask implements SchedulingAwareRunnable {

		ListenerTask() {
		}

		@Override
		public boolean isLongLived() {
			return true;
		}

		@Override
		public void run() {
			try {
				while (isActive()) {
					RedisQueueInboundGateway.this.listening = true;
					receiveAndReply();
				}
			}
			finally {
				if (isActive()) {
					restart();
				}
				else if (RedisQueueInboundGateway.this.stopCallback != null) {
					RedisQueueInboundGateway.this.stopCallback.run();
					RedisQueueInboundGateway.this.stopCallback = null;
				}
			}
		}

	}

}
