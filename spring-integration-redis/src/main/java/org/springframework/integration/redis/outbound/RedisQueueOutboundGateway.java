/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.redis.outbound;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.jspecify.annotations.Nullable;

import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.BoundListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.redis.support.AllowListJdkSerializationRedisSerializer;
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

	private final Set<String> allowedPatterns = new LinkedHashSet<>();

	private boolean extractPayload = true;

	@SuppressWarnings("NullAway.Init")
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
			AllowListJdkSerializationRedisSerializer allowListSerializer =
					new AllowListJdkSerializationRedisSerializer(beanClassLoader);
			allowListSerializer.addAllowedPatterns(this.allowedPatterns.toArray(new String[0]));
			this.serializer = allowListSerializer;
		}
	}

	/**
	 * Add package/class patterns to the allowed list for the default
	 * {@link AllowListJdkSerializationRedisSerializer}.
	 * Ignored if a custom {@link #setSerializer(RedisSerializer)} is provided.
	 * @param patterns the patterns to add.
	 * @since 5.5.23
	 * @see AllowListJdkSerializationRedisSerializer#addAllowedPatterns(String...)
	 */
	public void addAllowedPatterns(String... patterns) {
		Collections.addAll(this.allowedPatterns, patterns);
		if (!this.serializerExplicitlySet
				&& this.serializer instanceof AllowListJdkSerializationRedisSerializer) {

			((AllowListJdkSerializationRedisSerializer) this.serializer).addAllowedPatterns(patterns);
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
				Assert.state(this.serializer != null, "'serializer' must not be null");
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
		Assert.state(this.serializer != null, "'serializer' must not be null");
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
