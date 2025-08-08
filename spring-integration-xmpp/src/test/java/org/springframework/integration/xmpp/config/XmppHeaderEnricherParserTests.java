/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.xmpp.config;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.xmpp.XmppHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author Mark Fisher
 * @author Josh Long
 * @author Gunnar Hillert
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 2.0
 */
@SpringJUnitConfig
@DirtiesContext
public class XmppHeaderEnricherParserTests {

	@Autowired
	private MessageChannel input;

	@Autowired
	private DirectChannel output;

	@Test
	public void to() throws InterruptedException {
		MessagingTemplate messagingTemplate = new MessagingTemplate();
		CountDownLatch callLatch = new CountDownLatch(1);
		MessageHandler handler = mock(MessageHandler.class);
		willAnswer(invocation -> {
			Message<?> message = invocation.getArgument(0);
			String chatToUser = (String) message.getHeaders().get(XmppHeaders.TO);
			assertThat(chatToUser).isNotNull();
			assertThat(chatToUser).isEqualTo("test1@example.org");
			callLatch.countDown();
			return null;
		})
				.given(handler)
				.handleMessage(Mockito.any(Message.class));
		this.output.subscribe(handler);
		messagingTemplate.send(this.input, MessageBuilder.withPayload("foo").build());
		assertThat(callLatch.await(10, TimeUnit.SECONDS)).isTrue();
		verify(handler, times(1)).handleMessage(Mockito.any(Message.class));
	}

}
