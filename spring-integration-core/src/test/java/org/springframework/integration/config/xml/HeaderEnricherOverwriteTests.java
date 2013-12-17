/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.integration.config.xml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Mark Fisher
 * @since 2.0
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class HeaderEnricherOverwriteTests {

	@Autowired
	private ApplicationContext context;


	@Test
	public void replyChannelExplicitOverwriteTrue() {
		MessageChannel inputChannel = context.getBean("replyChannelExplicitOverwriteTrueInput", MessageChannel.class);
		PollableChannel replyChannel = context.getBean("replyChannelExplicitOverwriteTrueOutput", PollableChannel.class);
		QueueChannel replyChannelToOverwrite = new QueueChannel();
		Message<?> message = MessageBuilder.withPayload("test").setReplyChannel(replyChannelToOverwrite).build();
		inputChannel.send(message);
		Message<?> result = replyChannel.receive(0);
		assertNotNull(result);
		assertEquals("TEST", result.getPayload());
		assertEquals(replyChannel, result.getHeaders().getReplyChannel());
		assertNull(replyChannelToOverwrite.receive(0));
	}

	@Test
	public void replyChannelExplicitOverwriteFalse() {
		MessageChannel inputChannel = context.getBean("replyChannelExplicitOverwriteFalseInput", MessageChannel.class);
		QueueChannel replyChannel = new QueueChannel();
		Message<?> message = MessageBuilder.withPayload("test").setReplyChannel(replyChannel).build();
		inputChannel.send(message);
		Message<?> result = replyChannel.receive(0);
		assertNotNull(result);
		assertEquals("TEST", result.getPayload());
		assertEquals(replyChannel, result.getHeaders().getReplyChannel());
	}

	@Test
	public void replyChannelExplicitOverwriteFalseButNoExistingHeader() {
		MessageChannel inputChannel = context.getBean(
				"replyChannelExplicitOverwriteFalseButNoExistingHeaderInput", MessageChannel.class);
		PollableChannel replyChannel = context.getBean(
				"replyChannelExplicitOverwriteFalseButNoExistingHeaderOutput", PollableChannel.class);
		Message<?> message = MessageBuilder.withPayload("test").build();
		inputChannel.send(message);
		Message<?> result = replyChannel.receive(0);
		assertNotNull(result);
		assertEquals("TEST", result.getPayload());
		assertEquals(replyChannel, result.getHeaders().getReplyChannel());
	}

	@Test
	public void replyChannelDefaultOverwriteTrue() {
		MessageChannel inputChannel = context.getBean("replyChannelDefaultOverwriteTrueInput", MessageChannel.class);
		PollableChannel replyChannel = context.getBean("replyChannelDefaultOverwriteTrueOutput", PollableChannel.class);
		QueueChannel replyChannelToOverwrite = new QueueChannel();
		Message<?> message = MessageBuilder.withPayload("test").setReplyChannel(replyChannelToOverwrite).build();
		inputChannel.send(message);
		Message<?> result = replyChannel.receive(0);
		assertNotNull(result);
		assertEquals("TEST", result.getPayload());
		assertEquals(replyChannel, result.getHeaders().getReplyChannel());
		assertNull(replyChannelToOverwrite.receive(0));
	}

	@Test
	public void replyChannelDefaultOverwriteFalse() {
		MessageChannel inputChannel = context.getBean("replyChannelDefaultOverwriteFalseInput", MessageChannel.class);
		QueueChannel replyChannel = new QueueChannel();
		Message<?> message = MessageBuilder.withPayload("test").setReplyChannel(replyChannel).build();
		inputChannel.send(message);
		Message<?> result = replyChannel.receive(0);
		assertNotNull(result);
		assertEquals("TEST", result.getPayload());
		assertEquals(replyChannel, result.getHeaders().getReplyChannel());
	}

	@Test
	public void replyChannelDefaultOverwriteTrueButExplicitOverwriteFalse() {
		MessageChannel inputChannel = context.getBean("replyChannelDefaultOverwriteTrueButExplicitOverwriteFalseInput", MessageChannel.class);
		QueueChannel replyChannel = new QueueChannel();
		Message<?> message = MessageBuilder.withPayload("test").setReplyChannel(replyChannel).build();
		inputChannel.send(message);
		Message<?> result = replyChannel.receive(0);
		assertNotNull(result);
		assertEquals("TEST", result.getPayload());
		assertEquals(replyChannel, result.getHeaders().getReplyChannel());
	}

	@Test
	public void replyChannelDefaultOverwriteFalseButNoExistingHeader() {
		MessageChannel inputChannel = context.getBean(
				"replyChannelDefaultOverwriteFalseButNoExistingHeaderInput", MessageChannel.class);
		PollableChannel replyChannel = context.getBean(
				"replyChannelDefaultOverwriteFalseButNoExistingHeaderOutput", PollableChannel.class);
		Message<?> message = MessageBuilder.withPayload("test").build();
		inputChannel.send(message);
		Message<?> result = replyChannel.receive(0);
		assertNotNull(result);
		assertEquals("TEST", result.getPayload());
		assertEquals(replyChannel, result.getHeaders().getReplyChannel());
	}

	@Test
	public void priorityExplicitOverwriteTrue() {
		MessageChannel channel = context.getBean("priorityExplicitOverwriteTrueInput", MessageChannel.class);
		MessagingTemplate template = new MessagingTemplate();
		template.setDefaultDestination(channel);
		Message<?> result = template.sendAndReceive(new GenericMessage<String>("test"));
		assertNotNull(result);
		assertEquals(new Integer(42), new IntegrationMessageHeaderAccessor(result).getPriority());
	}

	@Test
	public void priorityExplicitOverwriteFalse() {
		MessageChannel input = context.getBean("priorityExplicitOverwriteFalseInput", MessageChannel.class);
		QueueChannel replyChannel = new QueueChannel();
		Message<?> message = MessageBuilder.withPayload("test")
				.setReplyChannel(replyChannel)
				.setPriority(77)
				.build();
		input.send(message);
		Message<?> result = replyChannel.receive(0);
		assertNotNull(result);
		assertEquals(new Integer(77), new IntegrationMessageHeaderAccessor(result).getPriority());
	}

	@Test
	public void customExplicitOverwriteTrue() {
		MessageChannel inputChannel = context.getBean("customExplicitOverwriteTrueInput", MessageChannel.class);
		QueueChannel replyChannel = new QueueChannel();
		Message<?> message = MessageBuilder.withPayload("test")
				.setReplyChannel(replyChannel)
				.setHeader("foo", "bar")
				.build();
		inputChannel.send(message);
		Message<?> result = replyChannel.receive(0);
		assertNotNull(result);
		assertEquals("TEST", result.getPayload());
		assertEquals("zzz", result.getHeaders().get("foo"));
	}

	@Test
	public void customExplicitOverwriteFalse() {
		MessageChannel inputChannel = context.getBean("customExplicitOverwriteFalseInput", MessageChannel.class);
		QueueChannel replyChannel = new QueueChannel();
		Message<?> message = MessageBuilder.withPayload("test")
				.setReplyChannel(replyChannel)
				.setHeader("foo", "bar")
				.build();
		inputChannel.send(message);
		Message<?> result = replyChannel.receive(0);
		assertNotNull(result);
		assertEquals("TEST", result.getPayload());
		assertEquals("bar", result.getHeaders().get("foo"));
	}

	@Test
	public void customExplicitOverwriteFalseButNoExistingHeader() {
		MessageChannel inputChannel = context.getBean("customExplicitOverwriteFalseButNoExistingHeaderInput", MessageChannel.class);
		QueueChannel replyChannel = new QueueChannel();
		Message<?> message = MessageBuilder.withPayload("test")
				.setReplyChannel(replyChannel)
				.build();
		inputChannel.send(message);
		Message<?> result = replyChannel.receive(0);
		assertNotNull(result);
		assertEquals("TEST", result.getPayload());
		assertEquals("zzz", result.getHeaders().get("foo"));
	}

	@Test
	public void expressionExplicitOverwriteTrue() {
		MessageChannel inputChannel = context.getBean("expressionExplicitOverwriteTrueInput", MessageChannel.class);
		QueueChannel replyChannel = new QueueChannel();
		Message<?> message = MessageBuilder.withPayload("test")
				.setReplyChannel(replyChannel)
				.setHeader("foo", "bar")
				.build();
		inputChannel.send(message);
		Message<?> result = replyChannel.receive(0);
		assertNotNull(result);
		assertEquals("TEST", result.getPayload());
		assertEquals("123", result.getHeaders().get("foo"));
	}

	@Test
	public void expressionExplicitOverwriteFalse() {
		MessageChannel inputChannel = context.getBean("expressionExplicitOverwriteFalseInput", MessageChannel.class);
		QueueChannel replyChannel = new QueueChannel();
		Message<?> message = MessageBuilder.withPayload("test")
				.setReplyChannel(replyChannel)
				.setHeader("foo", "bar")
				.build();
		inputChannel.send(message);
		Message<?> result = replyChannel.receive(0);
		assertNotNull(result);
		assertEquals("TEST", result.getPayload());
		assertEquals("bar", result.getHeaders().get("foo"));
	}

	@Test
	public void expressionExplicitOverwriteFalseButNoExistingHeader() {
		MessageChannel inputChannel = context.getBean("expressionExplicitOverwriteFalseButNoExistingHeaderInput", MessageChannel.class);
		QueueChannel replyChannel = new QueueChannel();
		Message<?> message = MessageBuilder.withPayload("test")
				.setReplyChannel(replyChannel)
				.build();
		inputChannel.send(message);
		Message<?> result = replyChannel.receive(0);
		assertNotNull(result);
		assertEquals("TEST", result.getPayload());
		assertEquals("123", result.getHeaders().get("foo"));
	}

	@Test
	public void beanExplicitOverwriteTrue() {
		MessageChannel inputChannel = context.getBean("beanExplicitOverwriteTrueInput", MessageChannel.class);
		QueueChannel replyChannel = new QueueChannel();
		Message<?> message = MessageBuilder.withPayload("test")
				.setReplyChannel(replyChannel)
				.setHeader("foo", "bar")
				.build();
		inputChannel.send(message);
		Message<?> result = replyChannel.receive(0);
		assertNotNull(result);
		assertEquals("TEST", result.getPayload());
		assertEquals("ABC", result.getHeaders().get("foo"));
	}

	@Test
	public void beanExplicitOverwriteFalse() {
		MessageChannel inputChannel = context.getBean("beanExplicitOverwriteFalseInput", MessageChannel.class);
		QueueChannel replyChannel = new QueueChannel();
		Message<?> message = MessageBuilder.withPayload("test")
				.setReplyChannel(replyChannel)
				.setHeader("foo", "bar")
				.build();
		inputChannel.send(message);
		Message<?> result = replyChannel.receive(0);
		assertNotNull(result);
		assertEquals("TEST", result.getPayload());
		assertEquals("bar", result.getHeaders().get("foo"));
	}

	@Test
	public void beanExplicitOverwriteFalseButNoExistingHeader() {
		MessageChannel inputChannel = context.getBean("beanExplicitOverwriteFalseButNoExistingHeaderInput", MessageChannel.class);
		QueueChannel replyChannel = new QueueChannel();
		Message<?> message = MessageBuilder.withPayload("test")
				.setReplyChannel(replyChannel)
				.build();
		inputChannel.send(message);
		Message<?> result = replyChannel.receive(0);
		assertNotNull(result);
		assertEquals("TEST", result.getPayload());
		assertEquals("ABC", result.getHeaders().get("foo"));
	}


	public static class TestBean {

		private final String text;

		public TestBean(String text) {
			this.text = text;
		}

		public String text() {
			return this.text;
		}
	}

}
