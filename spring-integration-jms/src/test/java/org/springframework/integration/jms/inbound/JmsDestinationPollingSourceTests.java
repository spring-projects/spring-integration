/*
 * Copyright 2026-present the original author or authors.
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

package org.springframework.integration.jms.inbound;

import jakarta.jms.Message;
import org.junit.jupiter.api.Test;

import org.springframework.integration.jms.StubTextMessage;
import org.springframework.integration.test.support.TestApplicationContextAware;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.converter.MessageConversionException;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.messaging.MessagingException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * @author Glenn Renfro
 */
public class JmsDestinationPollingSourceTests implements TestApplicationContextAware {

	@Test
	public void nullPayloadFromConverterThrowsMessageConversionException() throws Exception {
		Message jmsMessage = new StubTextMessage("test");

		MessageConverter converter = mock();
		given(converter.fromMessage(any())).willReturn(null);

		JmsTemplate jmsTemplate = mock();
		given(jmsTemplate.getMessageConverter()).willReturn(converter);
		given(jmsTemplate.receiveSelected(nullable(String.class))).willReturn(jmsMessage);

		JmsDestinationPollingSource source = new JmsDestinationPollingSource(jmsTemplate);
		source.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		source.afterPropertiesSet();

		// A JMS message converted to a null payload is a conversion failure, not a message to discard.
		assertThatExceptionOfType(MessagingException.class)
				.isThrownBy(source::receive)
				.withCauseInstanceOf(MessageConversionException.class);
	}

	@Test
	public void payloadFromConverterIsUsed() throws Exception {
		Message jmsMessage = new StubTextMessage("test");

		MessageConverter converter = mock();
		given(converter.fromMessage(any())).willReturn("converted");

		JmsTemplate jmsTemplate = mock();
		given(jmsTemplate.getMessageConverter()).willReturn(converter);
		given(jmsTemplate.receiveSelected(nullable(String.class))).willReturn(jmsMessage);

		JmsDestinationPollingSource source = new JmsDestinationPollingSource(jmsTemplate);
		source.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		source.afterPropertiesSet();

		assertThat(source.receive().getPayload()).isEqualTo("converted");
	}

}
