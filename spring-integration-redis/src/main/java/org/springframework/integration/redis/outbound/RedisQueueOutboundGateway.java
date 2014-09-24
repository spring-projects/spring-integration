/*
 * Copyright 2014 the original author or authors
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

import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.BoundListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.messaging.Message;
import org.springframework.util.AlternativeJdkIdGenerator;
import org.springframework.util.Assert;
import org.springframework.util.IdGenerator;

/**
 * @author David Liu
 * @since 4.1
 */
public class RedisQueueOutboundGateway extends AbstractReplyProducingMessageHandler {

	private final RedisTemplate<String, Object> template;

	private volatile String queueName;

	private volatile boolean extractPayload = true;

	private volatile RedisSerializer<?> serializer = new JdkSerializationRedisSerializer();

	private final static RedisSerializer<String> stringSerializer = new StringRedisSerializer();

	private volatile boolean serializerExplicitlySet;

	private static final int TIMEOUT = 1000;

	private volatile int receiveTimeout = TIMEOUT;

	private static final String QUEUE_NAME_SUFFIX = ".reply";

	private BoundListOperations<String, Object> boundListOperations = null;

	private static final IdGenerator defaultIdGenerator = new AlternativeJdkIdGenerator();

	public RedisQueueOutboundGateway(String queueName, RedisConnectionFactory connectionFactory) {
		Assert.hasText(queueName, "'queueName' is required");
		Assert.notNull(connectionFactory, "'connectionFactory' must not be null");
		this.queueName = queueName;
		this.template = new RedisTemplate<String, Object>();
		this.template.setConnectionFactory(connectionFactory);
		this.template.setEnableDefaultSerializer(false);
		this.template.setKeySerializer(new StringRedisSerializer());
		this.template.afterPropertiesSet();
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
		return "redis:queue-outbound-gatewway";
	}

	@Override
	protected Object handleRequestMessage(Message<?> message) {
		Object value = message;

		if (this.extractPayload) {
			value = message.getPayload();
		}
		if (!(value instanceof byte[])) {
			if (value instanceof String && !serializerExplicitlySet) {
				value = stringSerializer.serialize((String) value);
			}
			else {
				value = ((RedisSerializer<Object>) serializer).serialize(value);
			}
		}
		String uuid = defaultIdGenerator.generateId().toString();

		byte[] uuidByte;
		if (!serializerExplicitlySet) {
			uuidByte = stringSerializer.serialize(uuid);
		}
		else {
			uuidByte = ((RedisSerializer<Object>) serializer).serialize(uuid);
		}
		this.template.boundListOps(this.queueName).leftPush(uuidByte);
		this.template.boundListOps(uuid).leftPush(value);
		this.boundListOperations = template.boundListOps(uuid + QUEUE_NAME_SUFFIX);
		byte[] reply = (byte[]) this.boundListOperations.rightPop(this.receiveTimeout, TimeUnit.MILLISECONDS);
		if(reply != null && reply.length > 0) {
			Object replyMessage = this.serializer.deserialize(reply);
			if (replyMessage == null) {
				return null;
			}
			return this.getMessageBuilderFactory().withPayload(replyMessage).build();
		}
		return null;
	}

}
