/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.groovy.config;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.groovy.GroovyScriptExecutingMessageProcessor;
import org.springframework.integration.handler.MessageProcessor;
import org.springframework.integration.splitter.MethodInvokingSplitter;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 * @since 2.0
 */
@SpringJUnitConfig
public class GroovySplitterTests {

	@Autowired
	private MessageChannel referencedScriptInput;

	@Autowired
	private MessageChannel inlineScriptInput;

	@Autowired
	@Qualifier("groovySplitter.handler")
	private MessageHandler groovySplitterMessageHandler;

	@Test
	public void referencedScript() {
		QueueChannel replyChannel = new QueueChannel();
		replyChannel.setBeanName("returnAddress");
		Message<?> message = MessageBuilder.withPayload("x,y,z").setReplyChannel(replyChannel).build();
		this.referencedScriptInput.send(message);
		assertThat(replyChannel.receive(0).getPayload()).isEqualTo("x");
		assertThat(replyChannel.receive(0).getPayload()).isEqualTo("y");
		assertThat(replyChannel.receive(0).getPayload()).isEqualTo("z");
		assertThat(replyChannel.receive(0)).isNull();
	}

	@Test
	public void inlineScript() {
		QueueChannel replyChannel = new QueueChannel();
		replyChannel.setBeanName("returnAddress");
		Message<?> message = MessageBuilder.withPayload("a   b c").setReplyChannel(replyChannel).build();
		this.inlineScriptInput.send(message);
		assertThat(replyChannel.receive(0).getPayload()).isEqualTo("a");
		assertThat(replyChannel.receive(0).getPayload()).isEqualTo("b");
		assertThat(replyChannel.receive(0).getPayload()).isEqualTo("c");
		assertThat(replyChannel.receive(0)).isNull();
	}

	@Test
	public void testInt2433VerifyRiddingOfMessageProcessorsWrapping() {
		assertThat(this.groovySplitterMessageHandler instanceof MethodInvokingSplitter).isTrue();
		@SuppressWarnings("rawtypes")
		MessageProcessor messageProcessor =
				TestUtils.getPropertyValue(this.groovySplitterMessageHandler, "processor", MessageProcessor.class);
		//before it was MethodInvokingMessageProcessor
		assertThat(messageProcessor).isInstanceOf(GroovyScriptExecutingMessageProcessor.class);
	}

}
