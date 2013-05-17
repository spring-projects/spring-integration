/*
 * Copyright 2013 the original author or authors.
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

package org.springframework.integration.redis.channel.registry;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.Lifecycle;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.channel.registry.ChannelRegistry;
import org.springframework.integration.core.MessageHandler;
import org.springframework.integration.core.SubscribableChannel;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.integration.redis.inbound.RedisInboundChannelAdapter;
import org.springframework.integration.redis.inbound.RedisQueueInboundChannelAdapter;
import org.springframework.integration.redis.outbound.RedisPublishingMessageHandler;
import org.springframework.integration.redis.outbound.RedisQueueOutboundChannelAdapter;
import org.springframework.util.Assert;

/**
 * A {@link ChannelRegistry} implementation backed by Redis.
 *
 * @author Mark Fisher
 * @author Gunnar Hillert
 * @since 3.0
 */
public class RedisChannelRegistry implements ChannelRegistry, DisposableBean {

	private final Log logger = LogFactory.getLog(this.getClass());

	private final StringRedisTemplate redisTemplate = new StringRedisTemplate();

	private final List<Lifecycle> lifecycleBeans = new ArrayList<Lifecycle>();


	public RedisChannelRegistry(RedisConnectionFactory connectionFactory) {
		Assert.notNull(connectionFactory, "connectionFactory must not be null");
		this.redisTemplate.setConnectionFactory(connectionFactory);
		this.redisTemplate.afterPropertiesSet();
	}


	@Override
	public void inbound(final String name, MessageChannel channel) {
		RedisQueueInboundChannelAdapter adapter = new RedisQueueInboundChannelAdapter("queue." + name, this.redisTemplate.getConnectionFactory());
		adapter.setOutputChannel(channel);
		adapter.afterPropertiesSet();
		this.lifecycleBeans.add(adapter);
		adapter.start();
	}

	@Override
	public void outbound(final String name, MessageChannel channel) {
		Assert.isInstanceOf(SubscribableChannel.class, channel);
		MessageHandler handler = new CompositeHandler(name, this.redisTemplate.getConnectionFactory());
		EventDrivenConsumer consumer = new EventDrivenConsumer((SubscribableChannel) channel, handler);
		consumer.afterPropertiesSet();
		this.lifecycleBeans.add(consumer);
		consumer.start();
	}

	@Override
	public void tap(final String name, MessageChannel channel) {
		Assert.hasText(name, "a valid name is required to register a tap channel");
		Assert.notNull(channel, "channel must not be null");
		RedisInboundChannelAdapter adapter = new RedisInboundChannelAdapter(this.redisTemplate.getConnectionFactory());
		adapter.setTopics("topic." + name);
		adapter.setOutputChannel(channel);
		adapter.afterPropertiesSet();
		this.lifecycleBeans.add(adapter);
		adapter.start();
	}

	@Override
	public void destroy() {
		for (Lifecycle bean : this.lifecycleBeans) {
			try {
				bean.stop();
			}
			catch (Exception e) {
				if (logger.isWarnEnabled()) {
					logger.warn("failed to stop adapter", e);
				}
			}
		}
	}


	private static class CompositeHandler extends AbstractMessageHandler {

		private final RedisPublishingMessageHandler topic;

		private final RedisQueueOutboundChannelAdapter queue;

		private CompositeHandler(String name, RedisConnectionFactory connectionFactory) {
			// TODO: replace with a multiexec that does both publish and lpush
			RedisPublishingMessageHandler topic = new RedisPublishingMessageHandler(connectionFactory);
			topic.setDefaultTopic("topic." + name);
			topic.afterPropertiesSet();
			this.topic = topic;
			RedisQueueOutboundChannelAdapter queue = new RedisQueueOutboundChannelAdapter("queue." + name, connectionFactory);
			queue.afterPropertiesSet();
			this.queue = queue;
		}

		@Override
		protected void handleMessageInternal(Message<?> message) throws Exception {
			topic.handleMessage(message);
			queue.handleMessage(message);
		}
	}

}
