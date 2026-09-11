/*
 * Copyright 2025-present the original author or authors.
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

package org.springframework.integration.amqp.inbound;

import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import org.springframework.amqp.rabbitmq.client.AmqpConnectionFactory;
import org.springframework.amqp.rabbitmq.client.RabbitAmqpTemplate;
import org.springframework.amqp.rabbitmq.client.listener.RabbitAmqpMessageListener;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.transformer.MessageTransformingHandler;
import org.springframework.util.MimeTypeUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author Kumar Gaurav
 *
 * @since 7.2
 */
public class AmqpClientInboundGatewayUnitTests {

	@Test
	void replyToQueueAddressIsNotModifiedForReply() {
		AmqpConnectionFactory connectionFactory = mock();
		AmqpClientInboundGateway amqpClientInboundGateway =
				new AmqpClientInboundGateway(connectionFactory, "testQueue");

		RabbitAmqpTemplate replyTemplate = mock();
		given(replyTemplate.send(anyString(), any(org.springframework.amqp.core.Message.class)))
				.willReturn(CompletableFuture.completedFuture(true));
		new DirectFieldAccessor(amqpClientInboundGateway).setPropertyValue("replyTemplate", replyTemplate);

		DirectChannel requestChannel = new DirectChannel();
		requestChannel.subscribe(new MessageTransformingHandler(message -> message));

		amqpClientInboundGateway.setRequestChannel(requestChannel);
		amqpClientInboundGateway.setBeanFactory(mock());
		amqpClientInboundGateway.afterPropertiesSet();

		com.rabbitmq.client.amqp.Message amqpMessage = mock();
		given(amqpMessage.body()).willReturn("test data".getBytes());
		given(amqpMessage.contentType()).willReturn(MimeTypeUtils.TEXT_PLAIN_VALUE);
		given(amqpMessage.messageIdAsString()).willReturn("testMessageId");
		given(amqpMessage.replyTo()).willReturn("/queues/reply%20queue");

		RabbitAmqpMessageListener messageListener =
				TestUtils.getPropertyValue(amqpClientInboundGateway, "listenerContainer.messageListener");
		messageListener.onAmqpMessage(amqpMessage, null);

		ArgumentCaptor<org.springframework.amqp.core.Message> replyMessageCaptor =
				ArgumentCaptor.forClass(org.springframework.amqp.core.Message.class);
		verify(replyTemplate).send(eq("queues/reply%20queue"), replyMessageCaptor.capture());

		org.springframework.amqp.core.Message replyMessage = replyMessageCaptor.getValue();
		assertThat(replyMessage.getBody()).isEqualTo("test data".getBytes());
		assertThat(replyMessage.getMessageProperties().getCorrelationId()).isEqualTo("testMessageId");
	}

}
