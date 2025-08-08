/*
 * Copyright © 2015 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2015-present the original author or authors.
 */

package org.springframework.integration.codec;

import java.io.IOException;

import org.springframework.integration.context.IntegrationObjectSupport;
import org.springframework.integration.support.AbstractIntegrationMessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.util.Assert;

/**
 * A {@link MessageConverter} that delegates to a {@link Codec} to convert.
 *
 * @author Gary Russell
 *
 * @since 4.2
 *
 */
public class CodecMessageConverter extends IntegrationObjectSupport implements MessageConverter {

	private final Codec codec;

	private final Class<?> messageClass;

	public CodecMessageConverter(Codec codec) {
		this.codec = codec;
		this.messageClass = GenericMessage.class;
	}

	@Override
	public Object fromMessage(Message<?> message, Class<?> targetClass) {
		try {
			return this.codec.encode(message);
		}
		catch (IOException e) {
			throw new MessagingException(message, "Failed to encode Message", e);
		}
	}

	@Override
	public Message<?> toMessage(Object payload, MessageHeaders headers) {
		Assert.isInstanceOf(byte[].class, payload);
		try {
			Message<?> decoded = (Message<?>) this.codec.decode((byte[]) payload, this.messageClass);
			if (headers != null) {
				AbstractIntegrationMessageBuilder<?> builder = getMessageBuilderFactory().fromMessage(decoded);
				builder.copyHeaders(headers);
				return builder.build();
			}
			else {
				return decoded;
			}
		}
		catch (IOException e) {
			throw new MessagingException("Failed to decode", e);
		}
	}

}
