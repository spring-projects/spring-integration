/*
 * Copyright 2002-2011 the original author or authors.
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

import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.integration.Message;
import org.springframework.integration.channel.AbstractMessageChannel;
import org.springframework.integration.channel.MessagePublishingErrorHandler;
import org.springframework.integration.core.MessageHandler;
import org.springframework.integration.core.SubscribableChannel;
import org.springframework.integration.dispatcher.BroadcastingDispatcher;
import org.springframework.integration.dispatcher.MessageDispatcher;
import org.springframework.integration.support.channel.BeanFactoryChannelResolver;
import org.springframework.integration.support.converter.MessageConverter;
import org.springframework.integration.support.converter.SimpleMessageConverter;
import org.springframework.integration.util.ErrorHandlingTaskExecutor;
import org.springframework.util.ErrorHandler;

/**
 * @author Oleg Zhurakousky
 * @since 2.0
 */
@SuppressWarnings("rawtypes")
public class SubscribableRedisChannel extends AbstractMessageChannel implements SubscribableChannel, SmartLifecycle, DisposableBean {

	private final RedisMessageListenerContainer container = new RedisMessageListenerContainer();
	private final JedisConnectionFactory connectionFactory;
	private final RedisTemplate redisTemplate;
	private final String topicName;
	
	private volatile MessageDispatcher dispatcher;
	
	// defaults
	private volatile Executor taskExecutor = new SimpleAsyncTaskExecutor();
	private volatile RedisSerializer<?> serializer = new StringRedisSerializer();
	private volatile MessageConverter messageConverter = new SimpleMessageConverter();
	
	public SubscribableRedisChannel(JedisConnectionFactory connectionFactory, String topicName) {
		this.connectionFactory = connectionFactory;
		this.redisTemplate = new StringRedisTemplate(connectionFactory);
		this.topicName = topicName;
	}

	public void setTaskExecutor(Executor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}
	
	public void setMessageConverter(MessageConverter messageConverter) {
		this.messageConverter = messageConverter;
	}
	
	public void setSerializer(RedisSerializer<?> serializer) {
		this.serializer = serializer;
	}

	public boolean subscribe(MessageHandler handler) {
		return this.dispatcher.addHandler(handler);
	}

	public boolean unsubscribe(MessageHandler handler) {
		return this.dispatcher.removeHandler(handler);
	}
	
	@Override
	protected boolean doSend(Message<?> message, long arg1) {
		this.redisTemplate.convertAndSend(this.topicName, this.messageConverter.fromMessage(message));
		return true;
	}

	@Override
	public void onInit() throws Exception {
		super.onInit();
		this.configureDispatcher();
		if (this.messageConverter == null){
			this.messageConverter = new SimpleMessageConverter();
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
		this.container.addMessageListener(adapter, new ChannelTopic(topicName));
		this.container.afterPropertiesSet();
	}

	private void configureDispatcher() {
		this.dispatcher = new BroadcastingDispatcher();
	}

	/*
	 * SmartLifecycle implementation (delegates to the MessageListener container)
	 */

	public boolean isAutoStartup() {
		return (this.container != null) ? this.container.isAutoStartup() : false;
	}

	public int getPhase() {
		return (this.container != null) ? this.container.getPhase() : 0;
	}

	public boolean isRunning() {
		return (this.container != null) ? this.container.isRunning() : false;
	}

	public void start() {
		if (this.container != null) {
			this.container.start();
		}
	}

	public void stop() {
		this.connectionFactory.getConnection().discard();
		if (this.container != null) {
			this.container.stop();
		}
	}

	public void stop(Runnable callback) {
		if (this.container != null) {
			this.container.stop(callback);
		}
	}

	public void destroy() throws Exception {
		if (this.container != null) {
			this.container.destroy();
		}
	}
	
	private class MessageListenerDelegate {

		@SuppressWarnings("unused")
		public void handleMessage(String s) {
			Message<?> siMessage = messageConverter.toMessage(s);
			dispatcher.dispatch(siMessage);
		}
	}
}
