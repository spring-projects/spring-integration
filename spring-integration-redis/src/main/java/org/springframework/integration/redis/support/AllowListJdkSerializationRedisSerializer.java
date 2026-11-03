/*
 * Copyright 2026-present the original author or authors.
 */

package org.springframework.integration.redis.support;

import org.jspecify.annotations.Nullable;

import org.springframework.core.serializer.support.SerializingConverter;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.integration.support.converter.AllowListDeserializingConverter;

/**
 * A JDK serialization based {@link RedisSerializer} which deserializes only
 * classes matching the allowed patterns.
 * <p>
 * By default, only {@code java.lang}, {@code java.util}, {@code java.time} and Spring messaging
 * types are allowed, which is enough to deserialize a {@link org.springframework.messaging.Message}
 * with standard headers and simple payloads.
 * Any other types must be allowed via {@link #addAllowedPatterns(String...)}.
 * Arrays, primitives, {@link String} and {@link Number} types are always allowed.
 *
 * @author Artem Bilan
 *
 * @since 5.5.23
 *
 * @see AllowListDeserializingConverter
 */
public class AllowListJdkSerializationRedisSerializer implements RedisSerializer<Object> {

	private static final String[] DEFAULT_ALLOWED_PATTERNS = {
			"java.lang.*",
			"java.util.*",
			"java.time.*",
			"org.springframework.messaging.MessageHeaders",
			"org.springframework.messaging.support.*",
			"org.springframework.integration.message.*",
			"org.springframework.integration.support.MutableMessage*"
	};

	private static final byte[] EMPTY_ARRAY = new byte[0];

	private final SerializingConverter serializer = new SerializingConverter();

	private final AllowListDeserializingConverter deserializer;

	public AllowListJdkSerializationRedisSerializer() {
		this(null);
	}

	public AllowListJdkSerializationRedisSerializer(@Nullable ClassLoader classLoader) {
		this.deserializer =
				classLoader != null
						? new AllowListDeserializingConverter(classLoader)
						: new AllowListDeserializingConverter();
		this.deserializer.setAllowedPatterns(DEFAULT_ALLOWED_PATTERNS);
	}

	/**
	 * Add package/class patterns to the allowed list.
	 * A class can be fully qualified, or a wildcard '*' is allowed at the
	 * beginning or end of the class name.
	 * Examples: {@code com.example.*}, {@code *.MyClass}.
	 * @param patterns the patterns to add.
	 */
	public void addAllowedPatterns(String... patterns) {
		this.deserializer.addAllowedPatterns(patterns);
	}

	@Override
	public byte[] serialize(@Nullable Object value) throws SerializationException {
		if (value == null) {
			return EMPTY_ARRAY;
		}
		try {
			return this.serializer.convert(value);
		}
		catch (Exception ex) {
			throw new SerializationException("Cannot serialize", ex);
		}
	}

	@Override
	public @Nullable Object deserialize(byte @Nullable [] bytes) throws SerializationException {
		if (bytes == null || bytes.length == 0) {
			return null;
		}
		try {
			return this.deserializer.convert(bytes);
		}
		catch (Exception ex) {
			throw new SerializationException("Cannot deserialize", ex);
		}
	}

}
