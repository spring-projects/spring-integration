/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.ws.config;

import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.ws.WebServiceHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 *
 * @since 2.0
 */
@SpringJUnitConfig
@DirtiesContext
public class WebServiceHeaderEnricherTests {

	@Autowired
	@Qualifier("literalValueInput")
	private MessageChannel literalValueInput;

	@Autowired
	@Qualifier("expressionInput")
	private MessageChannel expressionInput;

	@Test
	public void literalValue() {
		MessagingTemplate template = new MessagingTemplate();
		template.setDefaultDestination(literalValueInput);
		Message<?> result = template.sendAndReceive(new GenericMessage<>("foo"));
		Map<String, Object> headers = result.getHeaders();
		assertThat(headers.get(WebServiceHeaders.SOAP_ACTION)).isEqualTo("http://test");
	}

	@Test
	public void expression() {
		MessagingTemplate template = new MessagingTemplate();
		template.setDefaultDestination(expressionInput);
		Message<?> result = template.sendAndReceive(new GenericMessage<>("foo"));
		Map<String, Object> headers = result.getHeaders();
		assertThat(headers.get(WebServiceHeaders.SOAP_ACTION)).isEqualTo("http://foo");
	}

}
