/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.integration.endpoint;

import org.junit.Test;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.core.DestinationResolutionException;
import org.springframework.messaging.support.GenericMessage;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @author Gary Russell
 */
public class ReturnAddressTests {

	@Test
	public void returnAddressFallbackWithChannelReference() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"returnAddressTests.xml", this.getClass());
		MessageChannel channel3 = (MessageChannel) context.getBean("channel3");
		PollableChannel channel5 = (PollableChannel) context.getBean("channel5");
		context.start();
		Message<String> message = MessageBuilder.withPayload("*")
				.setReplyChannel(channel5).build();
		channel3.send(message);
		Message<?> response = channel5.receive(3000);
		assertThat(response).isNotNull();
		assertThat(response.getPayload()).isEqualTo("**");
		context.close();
	}

	@Test
	public void returnAddressFallbackWithChannelName() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"returnAddressTests.xml", this.getClass());
		MessageChannel channel3 = (MessageChannel) context.getBean("channel3");
		PollableChannel channel5 = (PollableChannel) context.getBean("channel5");
		context.start();
		Message<String> message = MessageBuilder.withPayload("*")
				.setReplyChannelName("channel5").build();
		channel3.send(message);
		Message<?> response = channel5.receive(3000);
		assertThat(response).isNotNull();
		assertThat(response.getPayload()).isEqualTo("**");
		context.close();
	}

	@Test
	public void returnAddressWithChannelReferenceAfterMultipleEndpoints() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"returnAddressTests.xml", this.getClass());
		MessageChannel channel1 = (MessageChannel) context.getBean("channel1");
		PollableChannel replyChannel = (PollableChannel) context.getBean("replyChannel");
		context.start();
		Message<String> message = MessageBuilder.withPayload("*")
				.setReplyChannel(replyChannel).build();
		channel1.send(message);
		Message<?> response = replyChannel.receive(3000);
		assertThat(response).isNotNull();
		assertThat(response.getPayload()).isEqualTo("********");
		PollableChannel channel2 = (PollableChannel) context.getBean("channel2");
		assertThat(channel2.receive(0)).isNull();
		context.close();
	}

	@Test
	public void returnAddressWithChannelNameAfterMultipleEndpoints() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"returnAddressTests.xml", this.getClass());
		MessageChannel channel1 = (MessageChannel) context.getBean("channel1");
		PollableChannel replyChannel = (PollableChannel) context.getBean("replyChannel");
		context.start();
		Message<String> message = MessageBuilder.withPayload("*")
				.setReplyChannelName("replyChannel").build();
		channel1.send(message);
		Message<?> response = replyChannel.receive(3000);
		assertThat(response).isNotNull();
		assertThat(response.getPayload()).isEqualTo("********");
		PollableChannel channel2 = (PollableChannel) context.getBean("channel2");
		assertThat(channel2.receive(0)).isNull();
		context.close();
	}

	@Test
	public void returnAddressFallbackButNotAvailable() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"returnAddressTests.xml", this.getClass());
		MessageChannel channel3 = (MessageChannel) context.getBean("channel3");
		context.start();
		GenericMessage<String> message = new GenericMessage<String>("*");
		try {
			channel3.send(message);
		}
		catch (MessagingException e) {
			assertThat(e.getCause() instanceof DestinationResolutionException).isTrue();
		}
		context.close();
	}

	@Test
	public void outputChannelWithNoReturnAddress() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"returnAddressTests.xml", this.getClass());
		MessageChannel channel4 = (MessageChannel) context.getBean("channel4");
		PollableChannel replyChannel = (PollableChannel) context.getBean("replyChannel");
		context.start();
		GenericMessage<String> message = new GenericMessage<String>("*");
		channel4.send(message);
		Message<?> response = replyChannel.receive(3000);
		assertThat(response).isNotNull();
		assertThat(response.getPayload()).isEqualTo("**");
		context.close();
	}

	@Test
	public void outputChannelTakesPrecedence() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"returnAddressTests.xml", this.getClass());
		MessageChannel channel4 = (MessageChannel) context.getBean("channel4");
		PollableChannel replyChannel = (PollableChannel) context.getBean("replyChannel");
		context.start();
		Message<String> message = MessageBuilder.withPayload("*")
				.setReplyChannelName("channel5").build();
		channel4.send(message);
		Message<?> response = replyChannel.receive(3000);
		assertThat(response).isNotNull();
		assertThat(response.getPayload()).isEqualTo("**");
		PollableChannel channel5 = (PollableChannel) context.getBean("channel5");
		assertThat(channel5.receive(0)).isNull();
		context.close();
	}

}
