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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.core.Message;
import org.springframework.integration.message.MessageBuilder;
import org.springframework.integration.message.MessageHandler;
import org.springframework.integration.message.StringMessage;

/**
 * @author Mark Fisher
 */
public class MessageHandlerChainTests {

	@Test
	public void chainWithOutputChannel() {
		QueueChannel outputChannel = new QueueChannel();
		List<MessageHandler> handlers = new ArrayList<MessageHandler>();
		handlers.add(createHandler(1));
		handlers.add(createHandler(2));
		handlers.add(createHandler(3));
		MessageHandlerChain chain = new MessageHandlerChain();
		chain.setBeanName("testChain");
		chain.setHandlers(handlers);
		chain.setOutputChannel(outputChannel);
		chain.handleMessage(new StringMessage("test"));
		Message<?> reply = outputChannel.receive(0);
		assertNotNull(reply);
		assertEquals("test123", reply.getPayload());
	}

	@Test(expected = IllegalArgumentException.class)
	public void chainWithOutputChannelButLastHandlerDoesNotProduceReplies() {
		QueueChannel outputChannel = new QueueChannel();
		List<MessageHandler> handlers = new ArrayList<MessageHandler>();
		handlers.add(createHandler(1));
		handlers.add(createHandler(2));
		handlers.add(new MessageHandler() {
			public void handleMessage(Message<?> message) {
			}
		});
		MessageHandlerChain chain = new MessageHandlerChain();
		chain.setBeanName("testChain");
		chain.setHandlers(handlers);
		chain.setOutputChannel(outputChannel);
		chain.handleMessage(new StringMessage("test"));
	}

	@Test
	public void chainForwardsToReplyChannel() {
		QueueChannel replyChannel = new QueueChannel();
		List<MessageHandler> handlers = new ArrayList<MessageHandler>();
		handlers.add(createHandler(1));
		handlers.add(createHandler(2));
		handlers.add(createHandler(3));
		MessageHandlerChain chain = new MessageHandlerChain();
		chain.setBeanName("testChain");
		chain.setHandlers(handlers);
		Message<String> message = MessageBuilder.withPayload("test")
				.setReplyChannel(replyChannel).build();
		chain.handleMessage(message);
		Message<?> reply = replyChannel.receive(0);
		assertNotNull(reply);
		assertEquals("test123", reply.getPayload());
	}

	@Test
	public void chainResolvesReplyChannelName() {
		QueueChannel replyChannel = new QueueChannel();
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerSingleton("testChannel", replyChannel);
		List<MessageHandler> handlers = new ArrayList<MessageHandler>();
		handlers.add(createHandler(1));
		handlers.add(createHandler(2));
		handlers.add(createHandler(3));
		MessageHandlerChain chain = new MessageHandlerChain();
		chain.setBeanName("testChain");
		chain.setHandlers(handlers);
		chain.setBeanFactory(beanFactory);
		Message<String> message = MessageBuilder.withPayload("test")
				.setReplyChannelName("testChannel").build();
		chain.handleMessage(message);
		Message<?> reply = replyChannel.receive(0);
		assertNotNull(reply);
		assertEquals("test123", reply.getPayload());
	}


	private static MessageHandler createHandler(final int index) {
		return new AbstractReplyProducingMessageHandler() {
			@Override
			protected void handleRequestMessage(Message<?> requestMessage, ReplyMessageHolder replyMessageHolder) {
				replyMessageHolder.set(requestMessage.getPayload().toString() + index);
			}
		};
	}

}
