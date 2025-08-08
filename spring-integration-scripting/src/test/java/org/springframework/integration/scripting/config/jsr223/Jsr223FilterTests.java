/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.scripting.config.jsr223;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @author David Turanski
 * @since 2.1
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class Jsr223FilterTests {

	@Autowired
	private MessageChannel referencedScriptInput;

	@Autowired
	private MessageChannel inlineScriptInput;

	@Test
	public void referencedScript() {
		QueueChannel replyChannel = new QueueChannel();
		replyChannel.setBeanName("returnAddress");
		Message<?> message1 = MessageBuilder.withPayload("test-1")
				.setReplyChannel(replyChannel)
				.setHeader("type", "bad")
				.build();
		Message<?> message2 = MessageBuilder.withPayload("test-2")
				.setReplyChannel(replyChannel)
				.setHeader("type", "good")
				.build();
		this.referencedScriptInput.send(message1);
		this.referencedScriptInput.send(message2);
		assertThat(replyChannel.receive(0).getPayload()).isEqualTo("test-2");
		assertThat(replyChannel.receive(0)).isNull();
	}

	@Test
	public void inlineScript() {
		QueueChannel replyChannel = new QueueChannel();
		replyChannel.setBeanName("returnAddress");
		Message<?> message1 = MessageBuilder.withPayload("bad").setReplyChannel(replyChannel).build();
		Message<?> message2 = MessageBuilder.withPayload("good").setReplyChannel(replyChannel).build();
		this.inlineScriptInput.send(message1);
		this.inlineScriptInput.send(message2);
		Message<?> received = replyChannel.receive(0);
		assertThat(received).isNotNull();
		assertThat(received.getPayload()).isEqualTo("good");
		assertThat(received).isEqualTo(message2);
		assertThat(replyChannel.receive(0)).isNull();
	}

}
