/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.integration.config.xml;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 *
 * @since 2.0
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class HeaderEnricherOverwriteTests {

	@Autowired
	private ApplicationContext context;


	@Test
	public void replyChannelExplicitOverwriteTrue() {
		MessageChannel inputChannel =
				this.context.getBean("replyChannelExplicitOverwriteTrueInput", MessageChannel.class);
		PollableChannel replyChannel =
				this.context.getBean("replyChannelExplicitOverwriteTrueOutput", PollableChannel.class);
		QueueChannel replyChannelToOverwrite = new QueueChannel();
		Message<?> message = MessageBuilder.withPayload("test").setReplyChannel(replyChannelToOverwrite).build();
		inputChannel.send(message);
		Message<?> result = replyChannel.receive(0);
		assertThat(result).isNotNull();
		assertThat(result.getPayload()).isEqualTo("TEST");
		assertThat(result.getHeaders().getReplyChannel()).isEqualTo(replyChannel);
		assertThat(replyChannelToOverwrite.receive(0)).isNull();
	}

	@Test
	public void replyChannelExplicitOverwriteFalse() {
		MessageChannel inputChannel =
				this.context.getBean("replyChannelExplicitOverwriteFalseInput", MessageChannel.class);
		QueueChannel replyChannel = new QueueChannel();
		Message<?> message = MessageBuilder.withPayload("test").setReplyChannel(replyChannel).build();
		inputChannel.send(message);
		Message<?> result = replyChannel.receive(0);
		assertThat(result).isNotNull();
		assertThat(result.getPayload()).isEqualTo("TEST");
		assertThat(result.getHeaders().getReplyChannel()).isEqualTo(replyChannel);
	}

	@Test
	public void replyChannelExplicitOverwriteFalseButNoExistingHeader() {
		MessageChannel inputChannel =
				this.context.getBean(
						"replyChannelExplicitOverwriteFalseButNoExistingHeaderInput", MessageChannel.class);
		PollableChannel replyChannel =
				this.context.getBean(
						"replyChannelExplicitOverwriteFalseButNoExistingHeaderOutput", PollableChannel.class);
		Message<?> message = MessageBuilder.withPayload("test").build();
		inputChannel.send(message);
		Message<?> result = replyChannel.receive(0);
		assertThat(result).isNotNull();
		assertThat(result.getPayload()).isEqualTo("TEST");
		assertThat(result.getHeaders().getReplyChannel()).isEqualTo(replyChannel);
	}

	@Test
	public void replyChannelDefaultOverwriteTrue() {
		MessageChannel inputChannel =
				this.context.getBean("replyChannelDefaultOverwriteTrueInput", MessageChannel.class);
		PollableChannel replyChannel =
				this.context.getBean("replyChannelDefaultOverwriteTrueOutput", PollableChannel.class);
		QueueChannel replyChannelToOverwrite = new QueueChannel();
		Message<?> message = MessageBuilder.withPayload("test").setReplyChannel(replyChannelToOverwrite).build();
		inputChannel.send(message);
		Message<?> result = replyChannel.receive(0);
		assertThat(result).isNotNull();
		assertThat(result.getPayload()).isEqualTo("TEST");
		assertThat(result.getHeaders().getReplyChannel()).isEqualTo(replyChannel);
		assertThat(replyChannelToOverwrite.receive(0)).isNull();
	}

	@Test
	public void replyChannelDefaultOverwriteFalse() {
		MessageChannel inputChannel =
				this.context.getBean("replyChannelDefaultOverwriteFalseInput", MessageChannel.class);
		QueueChannel replyChannel = new QueueChannel();
		Message<?> message = MessageBuilder.withPayload("test").setReplyChannel(replyChannel).build();
		inputChannel.send(message);
		Message<?> result = replyChannel.receive(0);
		assertThat(result).isNotNull();
		assertThat(result.getPayload()).isEqualTo("TEST");
		assertThat(result.getHeaders().getReplyChannel()).isEqualTo(replyChannel);
	}

	@Test
	public void replyChannelDefaultOverwriteTrueButExplicitOverwriteFalse() {
		MessageChannel inputChannel =
				this.context.getBean("replyChannelDefaultOverwriteTrueButExplicitOverwriteFalseInput",
						MessageChannel.class);
		QueueChannel replyChannel = new QueueChannel();
		Message<?> message = MessageBuilder.withPayload("test").setReplyChannel(replyChannel).build();
		inputChannel.send(message);
		Message<?> result = replyChannel.receive(0);
		assertThat(result).isNotNull();
		assertThat(result.getPayload()).isEqualTo("TEST");
		assertThat(result.getHeaders().getReplyChannel()).isEqualTo(replyChannel);
	}

	@Test
	public void replyChannelDefaultOverwriteFalseButNoExistingHeader() {
		MessageChannel inputChannel =
				this.context.getBean(
						"replyChannelDefaultOverwriteFalseButNoExistingHeaderInput", MessageChannel.class);
		PollableChannel replyChannel =
				this.context.getBean(
						"replyChannelDefaultOverwriteFalseButNoExistingHeaderOutput", PollableChannel.class);
		Message<?> message = MessageBuilder.withPayload("test").build();
		inputChannel.send(message);
		Message<?> result = replyChannel.receive(0);
		assertThat(result).isNotNull();
		assertThat(result.getPayload()).isEqualTo("TEST");
		assertThat(result.getHeaders().getReplyChannel()).isEqualTo(replyChannel);
	}

	@Test
	public void priorityExplicitOverwriteTrue() {
		MessageChannel channel = this.context.getBean("priorityExplicitOverwriteTrueInput", MessageChannel.class);
		MessagingTemplate template = new MessagingTemplate();
		template.setDefaultDestination(channel);
		Message<?> result = template.sendAndReceive(new GenericMessage<String>("test"));
		assertThat(result).isNotNull();
		assertThat(new IntegrationMessageHeaderAccessor(result).getPriority()).isEqualTo(42);
	}

	@Test
	public void priorityExplicitOverwriteFalse() {
		MessageChannel input = this.context.getBean("priorityExplicitOverwriteFalseInput", MessageChannel.class);
		QueueChannel replyChannel = new QueueChannel();
		Message<?> message = MessageBuilder.withPayload("test")
				.setReplyChannel(replyChannel)
				.setPriority(77)
				.build();
		input.send(message);
		Message<?> result = replyChannel.receive(0);
		assertThat(result).isNotNull();
		assertThat(new IntegrationMessageHeaderAccessor(result).getPriority()).isEqualTo(77);
	}

	@Test
	public void customExplicitOverwriteTrue() {
		MessageChannel inputChannel = this.context.getBean("customExplicitOverwriteTrueInput", MessageChannel.class);
		QueueChannel replyChannel = new QueueChannel();
		Message<?> message = MessageBuilder.withPayload("test")
				.setReplyChannel(replyChannel)
				.setHeader("foo", "bar")
				.build();
		inputChannel.send(message);
		Message<?> result = replyChannel.receive(0);
		assertThat(result).isNotNull();
		assertThat(result.getPayload()).isEqualTo("TEST");
		assertThat(result.getHeaders().get("foo")).isEqualTo("zzz");
	}

	@Test
	public void customExplicitOverwriteFalse() {
		MessageChannel inputChannel = this.context.getBean("customExplicitOverwriteFalseInput", MessageChannel.class);
		QueueChannel replyChannel = new QueueChannel();
		Message<?> message = MessageBuilder.withPayload("test")
				.setReplyChannel(replyChannel)
				.setHeader("foo", "bar")
				.build();
		inputChannel.send(message);
		Message<?> result = replyChannel.receive(0);
		assertThat(result).isNotNull();
		assertThat(result.getPayload()).isEqualTo("TEST");
		assertThat(result.getHeaders().get("foo")).isEqualTo("bar");
	}

	@Test
	public void customExplicitOverwriteFalseButNoExistingHeader() {
		MessageChannel inputChannel =
				this.context.getBean("customExplicitOverwriteFalseButNoExistingHeaderInput", MessageChannel.class);
		QueueChannel replyChannel = new QueueChannel();
		Message<?> message = MessageBuilder.withPayload("test")
				.setReplyChannel(replyChannel)
				.build();
		inputChannel.send(message);
		Message<?> result = replyChannel.receive(0);
		assertThat(result).isNotNull();
		assertThat(result.getPayload()).isEqualTo("TEST");
		assertThat(result.getHeaders().get("foo")).isEqualTo("zzz");
	}

	@Test
	public void expressionExplicitOverwriteTrue() {
		MessageChannel inputChannel =
				this.context.getBean("expressionExplicitOverwriteTrueInput", MessageChannel.class);
		QueueChannel replyChannel = new QueueChannel();
		Message<?> message = MessageBuilder.withPayload("test")
				.setReplyChannel(replyChannel)
				.setHeader("foo", "bar")
				.build();
		inputChannel.send(message);
		Message<?> result = replyChannel.receive(0);
		assertThat(result).isNotNull();
		assertThat(result.getPayload()).isEqualTo("TEST");
		assertThat(result.getHeaders().get("foo")).isEqualTo("123");
	}

	@Test
	public void expressionExplicitOverwriteFalse() {
		MessageChannel inputChannel =
				this.context.getBean("expressionExplicitOverwriteFalseInput", MessageChannel.class);
		QueueChannel replyChannel = new QueueChannel();
		Message<?> message = MessageBuilder.withPayload("test")
				.setReplyChannel(replyChannel)
				.setHeader("foo", "bar")
				.build();
		inputChannel.send(message);
		Message<?> result = replyChannel.receive(0);
		assertThat(result).isNotNull();
		assertThat(result.getPayload()).isEqualTo("TEST");
		assertThat(result.getHeaders().get("foo")).isEqualTo("bar");
	}

	@Test
	public void expressionExplicitOverwriteFalseButNoExistingHeader() {
		MessageChannel inputChannel =
				this.context.getBean("expressionExplicitOverwriteFalseButNoExistingHeaderInput", MessageChannel.class);
		QueueChannel replyChannel = new QueueChannel();
		Message<?> message = MessageBuilder.withPayload("test")
				.setReplyChannel(replyChannel)
				.build();
		inputChannel.send(message);
		Message<?> result = replyChannel.receive(0);
		assertThat(result).isNotNull();
		assertThat(result.getPayload()).isEqualTo("TEST");
		assertThat(result.getHeaders().get("foo")).isEqualTo("123");
	}

	@Test
	public void beanExplicitOverwriteTrue() {
		MessageChannel inputChannel = this.context.getBean("beanExplicitOverwriteTrueInput", MessageChannel.class);
		QueueChannel replyChannel = new QueueChannel();
		Message<?> message = MessageBuilder.withPayload("test")
				.setReplyChannel(replyChannel)
				.setHeader("foo", "bar")
				.build();
		inputChannel.send(message);
		Message<?> result = replyChannel.receive(0);
		assertThat(result).isNotNull();
		assertThat(result.getPayload()).isEqualTo("TEST");
		assertThat(result.getHeaders().get("foo")).isEqualTo("ABC");
	}

	@Test
	public void beanExplicitOverwriteFalse() {
		MessageChannel inputChannel = this.context.getBean("beanExplicitOverwriteFalseInput", MessageChannel.class);
		QueueChannel replyChannel = new QueueChannel();
		Message<?> message = MessageBuilder.withPayload("test")
				.setReplyChannel(replyChannel)
				.setHeader("foo", "bar")
				.build();
		inputChannel.send(message);
		Message<?> result = replyChannel.receive(0);
		assertThat(result).isNotNull();
		assertThat(result.getPayload()).isEqualTo("TEST");
		assertThat(result.getHeaders().get("foo")).isEqualTo("bar");
	}

	@Test
	public void beanExplicitOverwriteFalseButNoExistingHeader() {
		MessageChannel inputChannel =
				this.context.getBean("beanExplicitOverwriteFalseButNoExistingHeaderInput", MessageChannel.class);
		QueueChannel replyChannel = new QueueChannel();
		Message<?> message = MessageBuilder.withPayload("test")
				.setReplyChannel(replyChannel)
				.build();
		inputChannel.send(message);
		Message<?> result = replyChannel.receive(0);
		assertThat(result).isNotNull();
		assertThat(result.getPayload()).isEqualTo("TEST");
		assertThat(result.getHeaders().get("foo")).isEqualTo("ABC");
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
