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

package org.springframework.integration.redis.outbound;

import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.BoundListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.util.AlternativeJdkIdGenerator;
import org.springframework.util.Assert;
import org.springframework.util.IdGenerator;

/**
 * @author David Liu
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 4.1
 */
public class RedisQueueOutboundGateway extends AbstractReplyProducingMessageHandler {

	private static final String QUEUE_NAME_SUFFIX = ".reply";

	private static final int TIMEOUT = 1000;

	private static final IdGenerator DEFAULT_ID_GENERATOR = new AlternativeJdkIdGenerator();

	private static final RedisSerializer<String> STRING_SERIALIZER = new StringRedisSerializer();

	private final RedisTemplate<String, Object> template = new RedisTemplate<>();

	private final BoundListOperations<String, Object> boundListOps;

	private boolean extractPayload = true;

	private RedisSerializer<?> serializer;

	private boolean serializerExplicitlySet;

	private int receiveTimeout = TIMEOUT;

	public RedisQueueOutboundGateway(String queueName, RedisConnectionFactory connectionFactory) {
		Assert.hasText(queueName, "'queueName' is required");
		Assert.notNull(connectionFactory, "'connectionFactory' must not be null");
		this.template.setConnectionFactory(connectionFactory);
		this.template.setEnableDefaultSerializer(false);
		this.template.setKeySerializer(new StringRedisSerializer());
		this.template.afterPropertiesSet();
		this.boundListOps = this.template.boundListOps(queueName);
	}

	@Override
	public void setBeanClassLoader(ClassLoader beanClassLoader) {
		super.setBeanClassLoader(beanClassLoader);
		if (!this.serializerExplicitlySet) {
			this.serializer = new JdkSerializationRedisSerializer(beanClassLoader);
		}
	}

	public void setReceiveTimeout(int timeout) {
		this.receiveTimeout = timeout;
	}

	public void setExtractPayload(boolean extractPayload) {
		this.extractPayload = extractPayload;
	}

	public void setSerializer(RedisSerializer<?> serializer) {
		Assert.notNull(serializer, "'serializer' must not be null");
		this.serializer = serializer;
		this.serializerExplicitlySet = true;
	}

	@Override
	public String getComponentType() {
		return "redis:queue-outbound-gateway";
	}

	@Override
	@SuppressWarnings("unchecked")
	@Nullable
	protected Object handleRequestMessage(Message<?> message) {
		Object value = message;

		if (this.extractPayload) {
			value = message.getPayload();
		}
		Object beforeSerialization = value;
		if (!(value instanceof byte[])) {
			if (value instanceof String && !this.serializerExplicitlySet) {
				value = STRING_SERIALIZER.serialize((String) value);
			}
			else {
				value = ((RedisSerializer<Object>) this.serializer).serialize(value);
			}
		}
		if (value == null) {
			this.logger.debug(() -> "Serializer produced null for " + beforeSerialization);
			return null;
		}
		String uuid = DEFAULT_ID_GENERATOR.generateId().toString();

		byte[] uuidByte = uuid.getBytes();
		this.boundListOps.leftPush(uuidByte);
		this.template.boundListOps(uuid).leftPush(value);

		BoundListOperations<String, Object> boundListOperations = this.template.boundListOps(uuid + QUEUE_NAME_SUFFIX);
		byte[] reply = (byte[]) boundListOperations.rightPop(this.receiveTimeout, TimeUnit.MILLISECONDS);
		if (reply != null && reply.length > 0) {
			return createReply(reply);
		}
		return null;
	}

	@Nullable
	private Object createReply(byte[] reply) {
		Object replyMessage = this.serializer.deserialize(reply);
		if (replyMessage == null) {
			return null;
		}
		if (this.extractPayload) {
			return getMessageBuilderFactory()
					.withPayload(replyMessage);
		}
		else {
			return replyMessage;
		}
	}

}
