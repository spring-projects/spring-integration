/*
 * Copyright 2026-present the original author or authors.
 */

package org.springframework.integration.jms.inbound;

import jakarta.jms.Message;
import org.junit.jupiter.api.Test;

import org.springframework.integration.jms.StubTextMessage;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.converter.MessageConversionException;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.messaging.MessagingException;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * @author Glenn Renfro
 *
 * @since 7.2
 */
class JmsDestinationPollingSourceTests {

	@Test
	void nullPayloadFromConverterThrowsMessageConversionException() throws Exception {
		Message jmsMessage = new StubTextMessage("test");

		MessageConverter converter = mock();

		JmsTemplate jmsTemplate = mock();
		given(jmsTemplate.getMessageConverter()).willReturn(converter);
		given(jmsTemplate.receiveSelected(any())).willReturn(jmsMessage);

		JmsDestinationPollingSource source = new JmsDestinationPollingSource(jmsTemplate);

		assertThatExceptionOfType(MessagingException.class)
				.isThrownBy(source::receive)
				.withCauseInstanceOf(MessageConversionException.class);
	}

}
