/*
 * Copyright © 2017 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2017-present the original author or authors.
 */

package org.springframework.integration.amqp.support;

import org.junit.Test;

import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.amqp.support.converter.ContentTypeDelegatingMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.amqp.support.converter.SimpleMessageConverter;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * @author Gary Russell
 * @since 5.0
 *
 */
public class MappingUtilsTests {

	@Test
	public void testMapping() {
		Message<?> requestMessage = MessageBuilder.withPayload("foo")
				.setHeader(AmqpHeaders.CONTENT_TYPE, "my/ct")
				.build();
		MessageConverter converter = new SimpleMessageConverter();
		AmqpHeaderMapper headerMapper = DefaultAmqpHeaderMapper.outboundMapper();
		MessageDeliveryMode defaultDeliveryMode = MessageDeliveryMode.NON_PERSISTENT;
		boolean headersMappedLast = false;
		org.springframework.amqp.core.Message mapped = MappingUtils.mapMessage(requestMessage, converter, headerMapper,
				defaultDeliveryMode, headersMappedLast);
		assertThat(mapped.getMessageProperties().getContentType()).isEqualTo("text/plain");

		headersMappedLast = true;
		mapped = MappingUtils.mapMessage(requestMessage, converter, headerMapper,
				defaultDeliveryMode, headersMappedLast);
		assertThat(mapped.getMessageProperties().getContentType()).isEqualTo("my/ct");

		ContentTypeDelegatingMessageConverter ctdConverter = new ContentTypeDelegatingMessageConverter();
		ctdConverter.addDelegate("my/ct", converter);
		mapped = MappingUtils.mapMessage(requestMessage, ctdConverter, headerMapper,
				defaultDeliveryMode, headersMappedLast);
		assertThat(mapped.getMessageProperties().getContentType()).isEqualTo("my/ct");

		headersMappedLast = false;
		mapped = MappingUtils.mapMessage(requestMessage, ctdConverter, headerMapper,
				defaultDeliveryMode, headersMappedLast);
		assertThat(mapped.getMessageProperties().getContentType()).isEqualTo("text/plain");

		headersMappedLast = true;
		requestMessage = MessageBuilder.withPayload("foo")
				.setHeader(AmqpHeaders.CONTENT_TYPE, 42)
				.build();
		try {
			mapped = MappingUtils.mapMessage(requestMessage, ctdConverter, headerMapper,
					defaultDeliveryMode, headersMappedLast);
			fail("Expected IllegalArgumentException");
		}
		catch (IllegalArgumentException e) {
			assertThat(e.getMessage())
					.isEqualTo("contentType header must be a MimeType or String, found: java.lang.Integer");
		}
	}

}
