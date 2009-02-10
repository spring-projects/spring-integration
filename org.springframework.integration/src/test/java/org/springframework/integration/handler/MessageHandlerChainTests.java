/*
 * Copyright 2002-2008 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.handler;

import static org.easymock.EasyMock.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.integration.core.Message;
import org.springframework.integration.core.MessageChannel;
import org.springframework.integration.message.MessageBuilder;
import org.springframework.integration.message.MessageHandler;

/**
 * @author Mark Fisher
 * @author Iwein Fuld
 */
public class MessageHandlerChainTests {

	private MessageChannel outputChannel = createMock(MessageChannel.class);

	private Message<String> message = MessageBuilder.withPayload("foo").build();

	private MessageHandler handler = createMock(MessageHandler.class);

	private ProducingHandlerStub producer = new ProducingHandlerStub(handler);

	private Object[] allMocks = new Object[] { outputChannel, handler };

	@Test
	public void chainWithOutputChannel() {
		handler.handleMessage(message);
		expectLastCall().times(3);
		expect(outputChannel.send(eq(message), anyLong())).andReturn(true);
		replay(allMocks);
		List<MessageHandler> handlers = new ArrayList<MessageHandler>();
		handlers.add(producer);
		handlers.add(producer);
		handlers.add(producer);
		MessageHandlerChain chain = new MessageHandlerChain();
		chain.setBeanName("testChain");
		chain.setHandlers(handlers);
		chain.setOutputChannel(outputChannel);
		chain.handleMessage(message);
	}

	@Test(expected = IllegalArgumentException.class)
	public void chainWithOutputChannelButLastHandlerDoesNotProduceReplies() {
		replay(allMocks);
		List<MessageHandler> handlers = new ArrayList<MessageHandler>();
		handlers.add(producer);
		handlers.add(producer);
		handlers.add(handler);
		MessageHandlerChain chain = new MessageHandlerChain();
		chain.setBeanName("testChain");
		chain.setHandlers(handlers);
		chain.setOutputChannel(outputChannel);
		chain.handleMessage(message);
	}
	@Test
	public void chainWithoutOutputChannelButLastHandlerDoesNotProduceReplies() {
		handler.handleMessage(message);
		expectLastCall().times(3);
		replay(allMocks);
		List<MessageHandler> handlers = new ArrayList<MessageHandler>();
		handlers.add(producer);
		handlers.add(producer);
		handlers.add(handler);
		MessageHandlerChain chain = new MessageHandlerChain();
		chain.setBeanName("testChain");
		chain.setHandlers(handlers);
		chain.handleMessage(message);
	}

	@Test
	public void chainForwardsToReplyChannel() {
		Message<String> message = MessageBuilder.withPayload("test").setReplyChannel(outputChannel).build();
		handler.handleMessage(message);
		expectLastCall().times(3);
		//equality is lost when recreating the message
		expect(outputChannel.send(isA(Message.class), anyLong())).andReturn(true);
		replay(allMocks);
		List<MessageHandler> handlers = new ArrayList<MessageHandler>();
		handlers.add(producer);
		handlers.add(producer);
		handlers.add(producer);
		MessageHandlerChain chain = new MessageHandlerChain();
		chain.setBeanName("testChain");
		chain.setHandlers(handlers);
		chain.handleMessage(message);
	}

	@Test
	public void chainResolvesReplyChannelName() {
		Message<String> message = MessageBuilder.withPayload("test").setReplyChannelName("testChannel").build();
		handler.handleMessage(message);
		expectLastCall().times(3);
		//equality is lost when recreating the message
		expect(outputChannel.send(isA(Message.class), anyLong())).andReturn(true);
		replay(allMocks);
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerSingleton("testChannel", outputChannel);
		List<MessageHandler> handlers = new ArrayList<MessageHandler>();
		handlers.add(producer);
		handlers.add(producer);
		handlers.add(producer);
		MessageHandlerChain chain = new MessageHandlerChain();
		chain.setBeanName("testChain");
		chain.setHandlers(handlers);
		chain.setBeanFactory(beanFactory);
		chain.handleMessage(message);
	}

	private class ProducingHandlerStub extends AbstractReplyProducingMessageHandler {

		private final MessageHandler messageHandler;

		public ProducingHandlerStub(MessageHandler handler) {
			messageHandler = handler;
		}

		@Override
		protected void handleRequestMessage(Message<?> requestMessage, ReplyMessageHolder replyMessageHolder) {
			messageHandler.handleMessage(requestMessage);
			replyMessageHolder.add(requestMessage);
		}

	}
//	private class ProducingHandlerStub implements MessageHandler {
//		private MessageChannel output;
//		
//		private final MessageHandler messageHandler;
//		
//		public ProducingHandlerStub(MessageHandler handler) {
//			messageHandler = handler;
//		}
//		
//		void setOutputChannel(MessageChannel channel) {
//			this.output = channel;
//			
//		}
//		
//		public void handleMessage(Message<?> message) {
//			messageHandler.handleMessage(message);
//			output.send(message);
//		}
//	}

}
