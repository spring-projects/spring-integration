/*
 * Copyright © 2016 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2016-present the original author or authors.
 */

package org.springframework.integration.mongodb.support;

import org.bson.types.Binary;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.integration.support.converter.AllowListDeserializingConverter;
import org.springframework.messaging.Message;

/**
 * @author Artem Bilan
 * @author Gary Russell
 * @since 5.0
 */
@ReadingConverter
public class BinaryToMessageConverter implements Converter<Binary, Message<?>> {

	private final AllowListDeserializingConverter deserializingConverter = new AllowListDeserializingConverter();

	@Override
	public Message<?> convert(Binary source) {
		return (Message<?>) this.deserializingConverter.convert(source.getData());
	}

	/**
	 * Add patterns for packages/classes that are allowed to be deserialized. A class can
	 * be fully qualified or a wildcard '*' is allowed at the beginning or end of the
	 * class name. Examples: {@code com.foo.*}, {@code *.MyClass}.
	 * @param patterns the patterns.
	 * @since 5.4
	 */
	public void addAllowedPatterns(String... patterns) {
		this.deserializingConverter.addAllowedPatterns(patterns);
	}

}
