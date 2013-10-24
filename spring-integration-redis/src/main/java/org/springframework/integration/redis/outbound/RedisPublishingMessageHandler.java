/*
 * Copyright 2007-2013 the original author or authors
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

package org.springframework.integration.redis.outbound;

import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.integration.Message;
import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.integration.support.converter.MessageConverter;
import org.springframework.integration.support.converter.SimpleMessageConverter;
import org.springframework.util.Assert;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 * @since 2.1
 */
public class RedisPublishingMessageHandler extends AbstractMessageHandler {

	private final RedisTemplate<?, ?> template;

	private volatile MessageConverter messageConverter = new SimpleMessageConverter();

	private volatile String defaultTopic;

	private volatile RedisSerializer<?> serializer = new StringRedisSerializer();

	public RedisPublishingMessageHandler(RedisConnectionFactory connectionFactory) {
		Assert.notNull(connectionFactory, "connectionFactory must not be null");
		this.template = new RedisTemplate();
		this.template.setConnectionFactory(connectionFactory);
		this.template.setEnableDefaultSerializer(false);
		this.template.afterPropertiesSet();
	}

	public void setSerializer(RedisSerializer<?> serializer) {
		Assert.notNull(serializer, "'serializer' must not be null");
		this.serializer = serializer;
	}

	public void setMessageConverter(MessageConverter messageConverter) {
		Assert.notNull(messageConverter, "messageConverter must not be null");
		this.messageConverter = messageConverter;
	}

	public void setDefaultTopic(String defaultTopic) {
		this.defaultTopic = defaultTopic;
	}

	private String determineTopic(Message<?> message) {
		// TODO: add support for determining topic by evaluating SpEL against the Message
		Assert.hasText(this.defaultTopic, "Failed to determine Redis topic " +
				"from Message, and no defaultTopic has been provided.");
		return this.defaultTopic;
	}

	@Override
	@SuppressWarnings("unchecked")
	protected void handleMessageInternal(Message<?> message) throws Exception {
		String topic = this.determineTopic(message);
		Object value = this.messageConverter.fromMessage(message);

		if (value instanceof byte[]) {
			this.template.convertAndSend(topic, value);
		}
		else {
			this.template.convertAndSend(topic, ((RedisSerializer<Object>) this.serializer).serialize(value));
		}
	}

}
