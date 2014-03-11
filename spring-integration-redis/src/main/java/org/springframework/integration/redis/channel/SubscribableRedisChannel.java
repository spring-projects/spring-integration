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

package org.springframework.integration.redis.channel;

import java.util.concurrent.Executor;

import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.DisposableBean;
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
import org.springframework.integration.channel.MessagePublishingErrorHandler;
import org.springframework.integration.context.IntegrationProperties;
import org.springframework.integration.dispatcher.BroadcastingDispatcher;
import org.springframework.integration.support.channel.BeanFactoryChannelResolver;
import org.springframework.integration.support.converter.SimpleMessageConverter;
import org.springframework.integration.util.ErrorHandlingTaskExecutor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.util.Assert;
import org.springframework.util.ErrorHandler;
import org.springframework.util.StringUtils;

/**
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @since 2.0
 */
@SuppressWarnings("rawtypes")
public class SubscribableRedisChannel extends AbstractMessageChannel implements SubscribableChannel, SmartLifecycle, DisposableBean {

	private final RedisMessageListenerContainer container = new RedisMessageListenerContainer();
	private final RedisConnectionFactory connectionFactory;
	private final RedisTemplate redisTemplate;
	private final String topicName;

	private final BroadcastingDispatcher dispatcher = new BroadcastingDispatcher(true);

	private volatile Integer maxSubscribers;

	private volatile boolean initialized;

	// defaults
	private volatile Executor taskExecutor = new SimpleAsyncTaskExecutor();
	private volatile RedisSerializer<?> serializer = new StringRedisSerializer();
	private volatile MessageConverter messageConverter = new SimpleMessageConverter();

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
	 *
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

	@SuppressWarnings("unchecked")
	@Override
	protected boolean doSend(Message<?> message, long arg1) {
		this.redisTemplate.convertAndSend(this.topicName, this.messageConverter.fromMessage(message, Object.class));
		return true;
	}

	@Override
	public void onInit() throws Exception {
		if (this.initialized){
			return;
		}
		super.onInit();
		if (this.maxSubscribers == null) {
			Integer maxSubscribers = this.getIntegrationProperty(IntegrationProperties.CHANNELS_MAX_BROADCAST_SUBSCRIBERS, Integer.class);
			this.setMaxSubscribers(maxSubscribers);
		}
		if (this.messageConverter == null){
			this.messageConverter = new SimpleMessageConverter();
		}
		if (this.messageConverter instanceof BeanFactoryAware) {
			((BeanFactoryAware) this.messageConverter).setBeanFactory(this.getBeanFactory());
		}
		this.container.setConnectionFactory(connectionFactory);
		if (!(this.taskExecutor instanceof ErrorHandlingTaskExecutor)) {
			ErrorHandler errorHandler = new MessagePublishingErrorHandler(
					new BeanFactoryChannelResolver(this.getBeanFactory()));
			this.taskExecutor = new ErrorHandlingTaskExecutor(this.taskExecutor, errorHandler);
		}
		this.container.setTaskExecutor(this.taskExecutor);
		MessageListenerAdapter adapter = new MessageListenerAdapter(new MessageListenerDelegate());
		adapter.setSerializer(serializer);
		adapter.afterPropertiesSet();
		this.container.addMessageListener(adapter, new ChannelTopic(topicName));
		this.container.afterPropertiesSet();
		this.dispatcher.setBeanFactory(this.getBeanFactory());
		this.initialized = true;
	}

	/*
	 * SmartLifecycle implementation (delegates to the MessageListener container)
	 */

	@Override
	public boolean isAutoStartup() {
		return (this.container != null) ? this.container.isAutoStartup() : false;
	}

	@Override
	public int getPhase() {
		return (this.container != null) ? this.container.getPhase() : 0;
	}

	@Override
	public boolean isRunning() {
		return (this.container != null) ? this.container.isRunning() : false;
	}

	@Override
	public void start() {
		if (this.container != null) {
			this.container.start();
		}
	}

	@Override
	public void stop() {
		if (this.container != null) {
			this.container.stop();
		}
	}

	@Override
	public void stop(Runnable callback) {
		if (this.container != null) {
			this.container.stop(callback);
		}
	}

	@Override
	public void destroy() throws Exception {
		if (this.container != null) {
			this.container.destroy();
		}
	}

	private class MessageListenerDelegate {

		@SuppressWarnings({ "unused", "unchecked" })
		public void handleMessage(String s) {
			Message<?> siMessage = messageConverter.toMessage(s, null);
			try {
				dispatcher.dispatch(siMessage);
			}
			catch (MessageDispatchingException e) {
				String topicName = SubscribableRedisChannel.this.topicName;
				topicName = StringUtils.hasText(topicName) ? topicName : "unknown";
				throw new MessageDeliveryException(siMessage, e.getMessage()
						+ " for redis-channel '"
						+ topicName + "' (" + SubscribableRedisChannel.this.getFullChannelName()
						+ ").", e);
			}
		}
	}
}
