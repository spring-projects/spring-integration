/*
 * Copyright 2017-2022 the original author or authors.
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
