/*
 * Copyright 2016-present the original author or authors.
 */

package org.springframework.integration.mongodb.support;

import org.bson.types.Binary;
import org.jspecify.annotations.Nullable;

import org.springframework.core.convert.converter.Converter;
import org.springframework.core.serializer.support.SerializingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.messaging.Message;

/**
 * @author Artem Bilan
 *
 * @since 5.0
 */
@WritingConverter
public class MessageToBinaryConverter implements Converter<Message<?>, @Nullable Binary> {

	private final Converter<Object, byte[]> serializingConverter = new SerializingConverter();

	@Override
	public @Nullable Binary convert(Message<?> source) {
		byte[] data = this.serializingConverter.convert(source);
		return data != null ? new Binary(data) : null;
	}

}
