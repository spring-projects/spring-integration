/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.integration.redis.channel;

import java.util.concurrent.Executor;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.integration.MessageDispatchingException;
import org.springframework.integration.channel.AbstractMessageChannel;
import org.springframework.integration.channel.BroadcastCapableChannel;
import org.springframework.integration.channel.ChannelUtils;
import org.springframework.integration.context.IntegrationProperties;
import org.springframework.integration.dispatcher.BroadcastingDispatcher;
import org.springframework.integration.support.converter.SimpleMessageConverter;
import org.springframework.integration.util.ErrorHandlingTaskExecutor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.util.Assert;
import org.springframework.util.ErrorHandler;
import org.springframework.util.StringUtils;

/**
 * An {@link AbstractMessageChannel} implementation with {@link BroadcastCapableChannel}
 * aspect to provide a pub-sub semantics to consume messages fgrom Redis topic.
 *
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.0
 */
@SuppressWarnings("rawtypes")
public class SubscribableRedisChannel extends AbstractMessageChannel
		implements BroadcastCapableChannel, SmartLifecycle {

	private final RedisMessageListenerContainer container = new RedisMessageListenerContainer();

	private final RedisConnectionFactory connectionFactory;

	private final RedisTemplate redisTemplate;

	private final String topicName;

	private final BroadcastingDispatcher dispatcher = new BroadcastingDispatcher(true);

	// defaults
	private Executor taskExecutor = new SimpleAsyncTaskExecutor();

	private RedisSerializer<?> serializer = new StringRedisSerializer();

	private MessageConverter messageConverter = new SimpleMessageConverter();

	private volatile Integer maxSubscribers;

	private volatile boolean initialized;

	public SubscribableRedisChannel(RedisConnectionFactory connectionFactory, String topicName) {
		Assert.notNull(connectionFactory, "'connectionFactory' must not be null");
		Assert.hasText(topicName, "'topicName' must not be empty");
		this.connectionFactory = connectionFactory;
		this.redisTemplate = new StringRedisTemplate(connectionFactory);
		this.topicName = topicName;
	}

	public void setTaskExecutor(Executor taskExecutor) {
		Assert.notNull(taskExecutor, "'taskExecutor' must not be null");
		this.taskExecutor = taskExecutor;
	}

	@Override
	public void setMessageConverter(MessageConverter messageConverter) {
		Assert.notNull(messageConverter, "'messageConverter' must not be null");
		this.messageConverter = messageConverter;
	}

	public void setSerializer(RedisSerializer<?> serializer) {
		Assert.notNull(serializer, "'serializer' must not be null");
		this.serializer = serializer;
	}

	/**
	 * Specify the maximum number of subscribers supported by the
	 * channel's dispatcher.
	 * @param maxSubscribers The maximum number of subscribers allowed.
	 */
	public void setMaxSubscribers(int maxSubscribers) {
		this.maxSubscribers = maxSubscribers;
		this.dispatcher.setMaxSubscribers(maxSubscribers);
	}

	@Override
	public boolean subscribe(MessageHandler handler) {
		return this.dispatcher.addHandler(handler);
	}

	@Override
	public boolean unsubscribe(MessageHandler handler) {
		return this.dispatcher.removeHandler(handler);
	}

	@Override
	protected boolean doSend(Message<?> message, long arg1) {
		Object value = this.messageConverter.fromMessage(message, Object.class);
		this.redisTemplate.convertAndSend(this.topicName, value); // NOSONAR - null can be sent
		return true;
	}

	@Override
	public void onInit() {
		if (this.initialized) {
			return;
		}
		super.onInit();
		if (this.maxSubscribers == null) {
			setMaxSubscribers(
					getIntegrationProperty(IntegrationProperties.CHANNELS_MAX_BROADCAST_SUBSCRIBERS, Integer.class));
		}
		if (this.messageConverter == null) {
			this.messageConverter = new SimpleMessageConverter();
		}
		BeanFactory beanFactory = getBeanFactory();
		if (this.messageConverter instanceof BeanFactoryAware) {
			((BeanFactoryAware) this.messageConverter).setBeanFactory(beanFactory);
		}
		this.container.setConnectionFactory(this.connectionFactory);
		if (!(this.taskExecutor instanceof ErrorHandlingTaskExecutor)) {
			ErrorHandler errorHandler = ChannelUtils.getErrorHandler(beanFactory);
			this.taskExecutor = new ErrorHandlingTaskExecutor(this.taskExecutor, errorHandler);
		}
		this.container.setTaskExecutor(this.taskExecutor);
		MessageListenerAdapter adapter = new MessageListenerAdapter(new MessageListenerDelegate());
		adapter.setSerializer(this.serializer);
		adapter.afterPropertiesSet();
		this.container.addMessageListener(adapter, new ChannelTopic(this.topicName));
		this.container.afterPropertiesSet();
		this.dispatcher.setBeanFactory(beanFactory);
		this.initialized = true;
	}

	/*
	 * SmartLifecycle implementation (delegates to the MessageListener container)
	 */

	@Override
	public boolean isAutoStartup() {
		return this.container.isAutoStartup();
	}

	@Override
	public int getPhase() {
		return this.container.getPhase();
	}

	@Override
	public boolean isRunning() {
		return this.container.isRunning();
	}

	@Override
	public void start() {
		this.container.start();
	}

	@Override
	public void stop() {
		this.container.stop();
	}

	@Override
	public void stop(Runnable callback) {
		this.container.stop(callback);
	}

	@Override
	public void destroy() {
		try {
			this.container.destroy();
		}
		catch (Exception ex) {
			throw new IllegalStateException("Cannot destroy " + this.container, ex);
		}
	}

	private class MessageListenerDelegate {

		MessageListenerDelegate() {
		}

		@SuppressWarnings({ "unused" })
		public void handleMessage(Object payload) {
			Message<?> siMessage = SubscribableRedisChannel.this.messageConverter.toMessage(payload, null);
			try {
				SubscribableRedisChannel.this.dispatcher.dispatch(siMessage);
			}
			catch (MessageDispatchingException e) {
				String exceptionMessage = e.getMessage();
				throw new MessageDeliveryException(siMessage,
						(exceptionMessage == null ? e.getClass().getSimpleName() : exceptionMessage)
								+ " for redis-channel '"
								+ (StringUtils.hasText(SubscribableRedisChannel.this.topicName)
								? SubscribableRedisChannel.this.topicName
								: "unknown")
								+ "' (" + getFullChannelName() + ").", e); // NOSONAR false positive - never null
			}
		}

	}

}
