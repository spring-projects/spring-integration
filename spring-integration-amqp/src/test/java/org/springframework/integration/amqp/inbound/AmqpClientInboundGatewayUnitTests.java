/*
 * Copyright 2026-present the original author or authors.
 */

package org.springframework.integration.amqp.inbound;

import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import org.springframework.amqp.rabbitmq.client.AmqpConnectionFactory;
import org.springframework.amqp.rabbitmq.client.RabbitAmqpTemplate;
import org.springframework.amqp.rabbitmq.client.listener.RabbitAmqpMessageListener;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.handler.BridgeHandler;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.util.MimeTypeUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author Kumar Gaurav
 *
 * @since 7.0.7
 */
public class AmqpClientInboundGatewayUnitTests {

	@Test
	void replyToQueueAddressIsNotModifiedForReply() {
		AmqpConnectionFactory connectionFactory = mock();
		AmqpClientInboundGateway amqpClientInboundGateway =
				new AmqpClientInboundGateway(connectionFactory, "testQueue");

		RabbitAmqpTemplate replyTemplate = mock();
		given(replyTemplate.send(any(), any()))
				.willReturn(CompletableFuture.completedFuture(true));
		new DirectFieldAccessor(amqpClientInboundGateway).setPropertyValue("replyTemplate", replyTemplate);

		DirectChannel requestChannel = new DirectChannel();
		requestChannel.subscribe(new BridgeHandler());

		amqpClientInboundGateway.setRequestChannel(requestChannel);
		amqpClientInboundGateway.setBeanFactory(mock());
		amqpClientInboundGateway.afterPropertiesSet();

		com.rabbitmq.client.amqp.Message amqpMessage = mock();
		given(amqpMessage.body()).willReturn("test data".getBytes());
		given(amqpMessage.contentType()).willReturn(MimeTypeUtils.TEXT_PLAIN_VALUE);
		given(amqpMessage.messageId()).willReturn("testMessageId");
		given(amqpMessage.replyTo()).willReturn("/queues/reply%20queue");

		RabbitAmqpMessageListener messageListener =
				TestUtils.getPropertyValue(amqpClientInboundGateway, "listenerContainer.messageListener",
						RabbitAmqpMessageListener.class);
		messageListener.onAmqpMessage(amqpMessage, null);

		ArgumentCaptor<org.springframework.amqp.core.Message> replyMessageCaptor = ArgumentCaptor.captor();
		verify(replyTemplate).send(eq("queues/reply%20queue"), replyMessageCaptor.capture());

		org.springframework.amqp.core.Message replyMessage = replyMessageCaptor.getValue();
		assertThat(replyMessage.getBody()).isEqualTo("test data".getBytes());
		assertThat(replyMessage.getMessageProperties().getCorrelationId()).isEqualTo("testMessageId");
	}

}
