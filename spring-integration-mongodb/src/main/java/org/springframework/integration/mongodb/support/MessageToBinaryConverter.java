/*
 * Copyright © 2016 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2016-present the original author or authors.
 */

package org.springframework.integration.mongodb.support;

import org.bson.types.Binary;

import org.springframework.core.convert.converter.Converter;
import org.springframework.core.serializer.support.SerializingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.messaging.Message;

/**
 * @author Artem Bilan
 * @since 5.0
 */
@WritingConverter
public class MessageToBinaryConverter implements Converter<Message<?>, Binary> {

	private final Converter<Object, byte[]> serializingConverter = new SerializingConverter();

	@Override
	public Binary convert(Message<?> source) {
		return new Binary(this.serializingConverter.convert(source));
	}

}
