/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.integration.handler;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.integration.context.IntegrationObjectSupport;
import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;

/**
 * @author Mark Fisher
 * @author Iwein Fuld
 * @author Gary Russell
 * @author Artem Bilan
 */
public class MessageHandlerChainTests {

	private final Message<String> message = MessageBuilder.withPayload("foo").build();

	private MessageChannel outputChannel;

	private MessageHandler handler3;

	private ProducingHandlerStub producer1;

	private ProducingHandlerStub producer2;

	private ProducingHandlerStub producer3;

	@BeforeEach
	public void setup() {
		outputChannel = mock(MessageChannel.class);
		MessageHandler handler1 = mock(MessageHandler.class);
		MessageHandler handler2 = mock(MessageHandler.class);
		handler3 = mock(MessageHandler.class);
		Mockito.when(outputChannel.send(Mockito.any(Message.class), eq(30000L))).thenReturn(true);
		producer1 = new ProducingHandlerStub(handler1);
		producer2 = new ProducingHandlerStub(handler2);
		producer3 = new ProducingHandlerStub(handler3);
	}

	@Test
	public void chainWithOutputChannel() {
		List<MessageHandler> handlers = new ArrayList<>();
		handlers.add(producer1);
		handlers.add(producer2);
		handlers.add(producer3);
		MessageHandlerChain chain = new MessageHandlerChain();
		chain.setBeanName("testChain");
		chain.setHandlers(handlers);
		chain.setOutputChannel(outputChannel);
		chain.setBeanFactory(mock(BeanFactory.class));
		chain.handleMessage(message);
		Mockito.verify(outputChannel).send(Mockito.eq(message), eq(30000L));
	}

	@Test
	public void chainWithOutputChannelButLastHandlerDoesNotProduceReplies() {
		List<MessageHandler> handlers = new ArrayList<>();
		handlers.add(producer1);
		handlers.add(producer2);
		handlers.add(handler3);
		MessageHandlerChain chain = new MessageHandlerChain();
		chain.setBeanName("testChain");
		chain.setHandlers(handlers);
		chain.setOutputChannel(outputChannel);
		chain.setBeanFactory(mock(BeanFactory.class));
		assertThatIllegalArgumentException().isThrownBy(chain::afterPropertiesSet);
	}

	@Test
	public void chainWithoutOutputChannelButLastHandlerDoesNotProduceReplies() {
		List<MessageHandler> handlers = new ArrayList<>();
		handlers.add(producer1);
		handlers.add(producer2);
		handlers.add(handler3);
		MessageHandlerChain chain = new MessageHandlerChain();
		chain.setBeanName("testChain");
		chain.setHandlers(handlers);
		chain.setBeanFactory(mock(BeanFactory.class));
		chain.handleMessage(message);
	}

	@Test
	public void chainForwardsToReplyChannel() {
		Message<String> message = MessageBuilder.withPayload("test").setReplyChannel(outputChannel).build();
		List<MessageHandler> handlers = new ArrayList<>();
		handlers.add(producer1);
		handlers.add(producer2);
		handlers.add(producer3);
		MessageHandlerChain chain = new MessageHandlerChain();
		chain.setBeanName("testChain");
		chain.setHandlers(handlers);
		chain.setBeanFactory(mock(BeanFactory.class));
		chain.handleMessage(message);
		Mockito.verify(outputChannel).send(Mockito.any(Message.class), eq(30000L));
	}

	@Test
	public void chainResolvesReplyChannelName() {
		Message<String> message = MessageBuilder.withPayload("test").setReplyChannelName("testChannel").build();
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerSingleton("testChannel", outputChannel);
		List<MessageHandler> handlers = new ArrayList<>();
		handlers.add(producer1);
		handlers.add(producer2);
		handlers.add(producer3);
		MessageHandlerChain chain = new MessageHandlerChain();
		chain.setBeanName("testChain");
		chain.setHandlers(handlers);
		chain.setBeanFactory(beanFactory);
		chain.handleMessage(message);
		Mockito.verify(outputChannel).send(Mockito.eq(message), eq(30000L));
	}

	@Test
	public void chainRejectsDuplicateHandlers() {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerSingleton("testChannel", outputChannel);
		List<MessageHandler> handlers = new ArrayList<>();
		handlers.add(producer1);
		handlers.add(producer2);
		handlers.add(producer1);
		MessageHandlerChain chain = new MessageHandlerChain();
		chain.setBeanName("testChain");
		chain.setHandlers(handlers);
		chain.setBeanFactory(beanFactory);
		assertThatIllegalArgumentException().isThrownBy(chain::afterPropertiesSet);
	}

	private static class ProducingHandlerStub extends IntegrationObjectSupport
			implements MessageHandler, MessageProducer {

		private volatile MessageChannel output;

		private final MessageHandler messageHandler;

		ProducingHandlerStub(MessageHandler handler) {
			this.messageHandler = handler;
		}

		@Override
		public void setOutputChannel(MessageChannel channel) {
			this.output = channel;

		}

		@Override
		public MessageChannel getOutputChannel() {
			return this.output;
		}

		@Override
		public void handleMessage(Message<?> message) {
			messageHandler.handleMessage(message);
			output.send(message);
		}

	}

}
