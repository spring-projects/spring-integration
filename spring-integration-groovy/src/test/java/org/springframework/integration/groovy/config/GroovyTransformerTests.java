/*
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.groovy.config;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.groovy.GroovyScriptExecutingMessageProcessor;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.transformer.AbstractMessageProcessingTransformer;
import org.springframework.integration.transformer.MessageTransformingHandler;
import org.springframework.integration.transformer.Transformer;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 * @author Glenn Renfro
 *
 * @since 2.0
 */
@SpringJUnitConfig
@DirtiesContext
public class GroovyTransformerTests {

	@Autowired
	private MessageChannel referencedScriptInput;

	@Autowired
	private MessageChannel inlineScriptInput;

	@Autowired
	@Qualifier("groovyTransformer.handler")
	private MessageHandler groovyTransformerMessageHandler;

	@Test
	public void referencedScript() {
		QueueChannel replyChannel = new QueueChannel();
		replyChannel.setBeanName("returnAddress");
		for (int i = 1; i <= 3; i++) {
			Message<?> message = MessageBuilder.withPayload("test-" + i).setReplyChannel(replyChannel).build();
			this.referencedScriptInput.send(message);
		}
		assertThat(replyChannel.receive(0).getPayload()).isEqualTo("groovy-test-1");
		assertThat(replyChannel.receive(0).getPayload()).isEqualTo("groovy-test-2");
		assertThat(replyChannel.receive(0).getPayload()).isEqualTo("groovy-test-3");
		assertThat(replyChannel.receive(0)).isNull();
	}

	@Test
	public void inlineScript() {
		QueueChannel replyChannel = new QueueChannel();
		replyChannel.setBeanName("returnAddress");
		for (int i = 1; i <= 3; i++) {
			Message<?> message = MessageBuilder.withPayload("test-" + i).setReplyChannel(replyChannel).build();
			this.inlineScriptInput.send(message);
		}
		assertThat(replyChannel.receive(0).getPayload()).isEqualTo("inline-test-1");
		assertThat(replyChannel.receive(0).getPayload()).isEqualTo("inline-test-2");
		assertThat(replyChannel.receive(0).getPayload()).isEqualTo("inline-test-3");
		assertThat(replyChannel.receive(0)).isNull();
	}

	@Test
	public void testInt2433VerifyRiddingOfMessageProcessorsWrapping() {
		assertThat(this.groovyTransformerMessageHandler instanceof MessageTransformingHandler).isTrue();
		Transformer transformer = TestUtils.getPropertyValue(this.groovyTransformerMessageHandler, "transformer");
		assertThat(transformer).isInstanceOf(AbstractMessageProcessingTransformer.class);
		//before it was MethodInvokingMessageProcessor
		assertThat(TestUtils.<Object>getPropertyValue(transformer, "messageProcessor"))
				.isInstanceOf(GroovyScriptExecutingMessageProcessor.class);
	}

}
