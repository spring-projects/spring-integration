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

package org.springframework.integration.config;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.message.Message;
import org.springframework.integration.router.AggregatingMessageHandler;
import org.springframework.integration.router.CompletionStrategy;
import org.springframework.integration.router.CompletionStrategyAdapter;
import org.springframework.integration.router.ResequencingMessageHandler;
import org.springframework.integration.util.MethodInvoker;

/**
 * @author Marius Bogoevici
 */
public class ResequencerParserTests {

	private ApplicationContext context;


	@Before
	public void setUp() {
		this.context = new ClassPathXmlApplicationContext("resequencerParserTests.xml", this.getClass());
	}

	@Test
	public void testResequencing() {
		ResequencingMessageHandler resequencingHandler = (ResequencingMessageHandler) context
				.getBean("defaultResequencer");
		MessageChannel replyChannel = (MessageChannel) context.getBean("replyChannel");
		List<Message<?>> outboundMessages = new ArrayList<Message<?>>();
		outboundMessages.add(createMessage("123", "id1", 3, 3, replyChannel));
		outboundMessages.add(createMessage("789", "id1", 3, 1, replyChannel));
		outboundMessages.add(createMessage("456", "id1", 3, 2, replyChannel));
		for (Message<?> message : outboundMessages) {
			resequencingHandler.handle(message);
		}
		Message<?> message1 = replyChannel.receive(500);
		Message<?> message2 = replyChannel.receive(500);
		Message<?> message3 = replyChannel.receive(500);
		Assert.assertNotNull(message1);
		Assert.assertEquals(1, message1.getHeader().getSequenceNumber());
		Assert.assertNotNull(message2);
		Assert.assertEquals(2, message2.getHeader().getSequenceNumber());
		Assert.assertNotNull(message3);
		Assert.assertEquals(3, message3.getHeader().getSequenceNumber());
	}
	
	@Test
	public void testDefaultResequencerProperties() {
		ResequencingMessageHandler resequencingHandler = (ResequencingMessageHandler) context
				.getBean("defaultResequencer");
		DirectFieldAccessor messageHandlerFieldAccessor = new DirectFieldAccessor(resequencingHandler);
		Assert.assertNull(messageHandlerFieldAccessor.getPropertyValue("defaultReplyChannel"));
		Assert.assertNull(messageHandlerFieldAccessor.getPropertyValue("discardChannel"));
		Assert.assertEquals("The ResequencingMessageHandler is not set with the appropriate timeout value", 1000l,
				messageHandlerFieldAccessor.getPropertyValue("sendTimeout"));
		Assert.assertEquals(
						"The ResequencingMessageHandler is not configured with the appropriate 'send partial results on timeout' flag",
						false, messageHandlerFieldAccessor.getPropertyValue("sendPartialResultOnTimeout"));
		Assert.assertEquals("The ResequencingMessageHandler is not configured with the appropriate reaper interval",
				1000l, messageHandlerFieldAccessor.getPropertyValue("reaperInterval"));
		Assert.assertEquals(
				"The ResequencingMessageHandler is not configured with the appropriate tracked correlationId capacity",
				1000, messageHandlerFieldAccessor.getPropertyValue("trackedCorrelationIdCapacity"));
		Assert.assertEquals("The ResequencingMessageHandler is not configured with the appropriate timeout",
				60000l, messageHandlerFieldAccessor.getPropertyValue("timeout"));
		Assert.assertEquals("The ResequencingMessageHandler is not configured with the appropriate 'release partial sequences' flag",
				true, messageHandlerFieldAccessor.getPropertyValue("releasePartialSequences"));		
	}

	@Test
	public void testPropertyAssignment() throws Exception {
		ResequencingMessageHandler completeResequencingMessageHandler = (ResequencingMessageHandler) context
				.getBean("completelyDefinedResequencer");
		MessageChannel defaultReplyChannel = (MessageChannel) context.getBean("replyChannel");
		MessageChannel discardChannel = (MessageChannel) context.getBean("discardChannel");
		DirectFieldAccessor messageHandlerFieldAccessor = new DirectFieldAccessor(completeResequencingMessageHandler);
		Assert.assertEquals("The ResequencingMessageHandler is not injected with the appropriate default reply channel",
				defaultReplyChannel, messageHandlerFieldAccessor.getPropertyValue("defaultReplyChannel"));
		Assert.assertEquals("The ResequencingMessageHandler is not injected with the appropriate discard channel",
				discardChannel, messageHandlerFieldAccessor.getPropertyValue("discardChannel"));
		Assert.assertEquals("The ResequencingMessageHandler is not set with the appropriate timeout value", 86420000l,
				messageHandlerFieldAccessor.getPropertyValue("sendTimeout"));
		Assert.assertEquals(
						"The ResequencingMessageHandler is not configured with the appropriate 'send partial results on timeout' flag",
						true, messageHandlerFieldAccessor.getPropertyValue("sendPartialResultOnTimeout"));
		Assert.assertEquals("The ResequencingMessageHandler is not configured with the appropriate reaper interval",
				135l, messageHandlerFieldAccessor.getPropertyValue("reaperInterval"));
		Assert.assertEquals(
				"The ResequencingMessageHandler is not configured with the appropriate tracked correlationId capacity",
				99, messageHandlerFieldAccessor.getPropertyValue("trackedCorrelationIdCapacity"));
		Assert.assertEquals("The ResequencingMessageHandler is not configured with the appropriate timeout",
				42l, messageHandlerFieldAccessor.getPropertyValue("timeout"));
		Assert.assertEquals("The ResequencingMessageHandler is not configured with the appropriate 'release partial sequences' flag",
				false, messageHandlerFieldAccessor.getPropertyValue("releasePartialSequences"));
	}

	private static <T> Message<T> createMessage(T payload, Object correlationId, int sequenceSize, int sequenceNumber,
			MessageChannel replyChannel) {
		GenericMessage<T> message = new GenericMessage<T>(payload);
		message.getHeader().setCorrelationId(correlationId);
		message.getHeader().setSequenceSize(sequenceSize);
		message.getHeader().setSequenceNumber(sequenceNumber);
		message.getHeader().setReturnAddress(replyChannel);
		return message;
	}

}
