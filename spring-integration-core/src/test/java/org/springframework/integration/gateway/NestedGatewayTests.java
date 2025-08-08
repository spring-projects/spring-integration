/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.gateway;

import org.junit.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author Mark Fisher
 * @author Gary Russell
 */
public class NestedGatewayTests {

	@Test
	public void nestedWithinHandler() {
		DirectChannel innerChannel = new DirectChannel();
		DirectChannel outerChannel = new DirectChannel();
		innerChannel.subscribe(new AbstractReplyProducingMessageHandler() {

			@Override
			protected Object handleRequestMessage(Message<?> requestMessage) {
				return requestMessage.getPayload() + "-reply";
			}
		});
		final MessagingGatewaySupport innerGateway = new MessagingGatewaySupport() {

		};
		innerGateway.setRequestChannel(innerChannel);
		innerGateway.setBeanFactory(mock(BeanFactory.class));
		innerGateway.afterPropertiesSet();
		outerChannel.subscribe(new AbstractReplyProducingMessageHandler() {

			@Override
			protected Object handleRequestMessage(Message<?> requestMessage) {
				return innerGateway.sendAndReceiveMessage(
						"pre-" + requestMessage.getPayload()).getPayload() + "-post";
			}
		});
		MessagingGatewaySupport outerGateway = new MessagingGatewaySupport() {

		};
		outerGateway.setRequestChannel(outerChannel);
		outerGateway.setBeanFactory(mock(BeanFactory.class));
		outerGateway.afterPropertiesSet();
		Message<?> reply = outerGateway.sendAndReceiveMessage("test");
		assertThat(reply.getPayload()).isEqualTo("pre-test-reply-post");
	}

	@Test
	public void replyChannelRetained() {
		DirectChannel requestChannel = new DirectChannel();
		DirectChannel replyChannel = new DirectChannel();
		requestChannel.subscribe(new AbstractReplyProducingMessageHandler() {

			@Override
			protected Object handleRequestMessage(Message<?> requestMessage) {
				return requestMessage.getPayload() + "-reply";
			}
		});
		MessagingGatewaySupport gateway = new MessagingGatewaySupport() {

		};
		gateway.setRequestChannel(requestChannel);
		gateway.setBeanFactory(mock(BeanFactory.class));
		gateway.afterPropertiesSet();
		Message<?> message = MessageBuilder.withPayload("test")
				.setReplyChannel(replyChannel).build();
		Message<?> reply = gateway.sendAndReceiveMessage(message);
		assertThat(reply.getPayload()).isEqualTo("test-reply");
		assertThat(reply.getHeaders().getReplyChannel()).isEqualTo(replyChannel);
	}

	@Test
	public void errorChannelRetained() {
		DirectChannel requestChannel = new DirectChannel();
		DirectChannel errorChannel = new DirectChannel();
		requestChannel.subscribe(new AbstractReplyProducingMessageHandler() {

			@Override
			protected Object handleRequestMessage(Message<?> requestMessage) {
				return requestMessage.getPayload() + "-reply";
			}
		});
		MessagingGatewaySupport gateway = new MessagingGatewaySupport() {

		};
		gateway.setRequestChannel(requestChannel);
		gateway.setBeanFactory(mock(BeanFactory.class));
		gateway.afterPropertiesSet();
		Message<?> message = MessageBuilder.withPayload("test")
				.setErrorChannel(errorChannel).build();
		Message<?> reply = gateway.sendAndReceiveMessage(message);
		assertThat(reply.getPayload()).isEqualTo("test-reply");
		assertThat(reply.getHeaders().getErrorChannel()).isEqualTo(errorChannel);
	}

}
