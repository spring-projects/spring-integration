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
package org.springframework.integration.redis.inbound;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.support.collections.RedisStore;
import org.springframework.integration.Message;
import org.springframework.integration.MessagingException;
import org.springframework.integration.context.IntegrationObjectSupport;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.json.JacksonJsonObjectMapperProvider;
import org.springframework.integration.json.JsonObjectMapper;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.jmx.export.annotation.ManagedMetric;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.util.Assert;

/**
 * This implementation of a {@link MessageSource} retrieves the last element of
 * a Redis List for each invocation of {@link #receive()}.
 *
 * @author Mark Fisher
 * @author Gunnar Hillert
 *
 * @since 3.0
 */
@ManagedResource
public class RedisQueueMessageSource extends IntegrationObjectSupport
										implements MessageSource<Object> {

	private final String queueName;

	private volatile boolean messageReturned = false;

	private final StringRedisTemplate redisTemplate = new StringRedisTemplate();

	private int redisTimeout = 10000;

	private final JsonObjectMapper<?> objectMapper = JacksonJsonObjectMapperProvider.newInstance();

	/**
	 * @param queueName Must not be an empty String
	 * @param connectionFactory Must not be null
	 */
	public RedisQueueMessageSource(String queueName, RedisConnectionFactory connectionFactory) {
		Assert.hasText(queueName, "queueName is required");
		Assert.notNull(connectionFactory, "connectionFactory must not be null");
		this.queueName = queueName;
		this.redisTemplate.setConnectionFactory(connectionFactory);
		this.redisTemplate.afterPropertiesSet();
	}

	/**
	 * When data is retrieved from the Redis queue, does the returned data represent
	 * just the payload for a Message, or does the data represent a serialized
	 * {@link Message}?. {@code messageReturned} defaults to false. This means
	 * the retrieved data will be used as the payload for a new Spring Integration
	 * Message. Otherwise, the data is deserialized as Spring Integration
	 * Message.
	 *
	 * @param extractPayload Defaults to false
	 */
	public void setMessageReturned(boolean messageReturned) {
		this.messageReturned = messageReturned;
	}

	/**
	 * This timeout (milliseconds) is used when retrieving elements from the queue
	 * specified by {@link #queueName}.
	 *
	 * If the queue does contain elements, the data is retrieved immediately. However,
	 * if the queue is empty, the Redis connection is blocked until either an element
	 * can be retrieved from the queue or until the specified timeout passes.
	 *
	 * A timeout of zero can be used to block indefinitely. If not set explicitly
	 * the timeout value will default to {@code 10000}
	 *
	 * See also: http://redis.io/commands/brpop
	 *
	 * @param redisTimeout Must be non-negative. Specified in milliseconds.
	 */
	public void setRedisTimeout(int redisTimeout) {
		this.redisTimeout = redisTimeout;
	}

	/**
	 * Returns a Message with the view into a {@link RedisStore} identified
	 * by {@link #keyExpression}
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Message<Object> receive() {

		if (logger.isDebugEnabled()) {
			logger.debug(String.format("Executing receive() with redisTimeout='%s' ms.", this.redisTimeout));
		}

		final String next = redisTemplate.boundListOps(queueName)
										.rightPop(this.redisTimeout, TimeUnit.MILLISECONDS);

		final Message<Object> message;

		if (next != null) {

			if (messageReturned) {
				try {
					final MessageDeserializationWrapper wrapper = objectMapper.fromJson(next, MessageDeserializationWrapper.class);
					message = (Message<Object>) wrapper.getMessage();
				}
				catch (Exception e) {
					throw new MessagingException("Deserialization of Message failed.", e);
				}
			}
			else {
				message = MessageBuilder.withPayload((Object) next).build();
			}
		}
		else {
			message = null;
		}

		if (logger.isDebugEnabled()) {
			logger.debug(String.format("Returning message %s", message));
		}

		return message;
	}

	/**
	 * Returns the size of the Queue specified by {@link #queueName}. The queue is
	 * represented by a Redis list. If the queue does not exist <code>0</code>
	 * is returned. See also http://redis.io/commands/llen
	 *
	 * @return Size of the queue. Never negative.
	 */
	@ManagedMetric
	public long getQueueSize() {
		return this.redisTemplate.boundListOps(queueName).size();
	}

	/**
	 * Clear the Redis Queue specified by {@link #queueName}.
	 */
	@ManagedOperation
	public void clearQueue() {
		this.redisTemplate.delete(queueName);
	}

	@SuppressWarnings("unused") // used by object mapper
	private static class MessageDeserializationWrapper {

		private volatile Map<String, Object> headers;

		private volatile Object payload;

		private volatile Message<?> message;

		void setHeaders(Map<String, Object> headers) {
			this.headers = headers;
		}

		void setPayload(Object payload) {
			this.payload = payload;
		}

		Message<?> getMessage() {
			if (this.message == null) {
				this.message = MessageBuilder.withPayload(this.payload).copyHeaders(this.headers).build();
			}
			return this.message;
		}
	}

}
