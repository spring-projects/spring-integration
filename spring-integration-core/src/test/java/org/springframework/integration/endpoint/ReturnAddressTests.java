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

package org.springframework.integration.endpoint;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.core.DestinationResolutionException;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 */
@SpringJUnitConfig
@DirtiesContext
public class ReturnAddressTests {

	@Autowired
	ApplicationContext context;

	@Test
	public void returnAddressFallbackWithChannelReference() {
		MessageChannel channel3 = (MessageChannel) context.getBean("channel3");
		PollableChannel channel5 = (PollableChannel) context.getBean("channel5");
		Message<String> message = MessageBuilder.withPayload("*")
				.setReplyChannel(channel5).build();
		channel3.send(message);
		Message<?> response = channel5.receive(3000);
		assertThat(response).isNotNull();
		assertThat(response.getPayload()).isEqualTo("**");
	}

	@Test
	public void returnAddressFallbackWithChannelName() {
		MessageChannel channel3 = (MessageChannel) context.getBean("channel3");
		PollableChannel channel5 = (PollableChannel) context.getBean("channel5");
		Message<String> message = MessageBuilder.withPayload("*")
				.setReplyChannelName("channel5").build();
		channel3.send(message);
		Message<?> response = channel5.receive(3000);
		assertThat(response).isNotNull();
		assertThat(response.getPayload()).isEqualTo("**");
	}

	@Test
	public void returnAddressWithChannelReferenceAfterMultipleEndpoints() {
		MessageChannel channel1 = (MessageChannel) context.getBean("channel1");
		PollableChannel replyChannel = (PollableChannel) context.getBean("replyChannel");
		Message<String> message = MessageBuilder.withPayload("*")
				.setReplyChannel(replyChannel).build();
		channel1.send(message);
		Message<?> response = replyChannel.receive(3000);
		assertThat(response).isNotNull();
		assertThat(response.getPayload()).isEqualTo("********");
		PollableChannel channel2 = (PollableChannel) context.getBean("channel2");
		assertThat(channel2.receive(0)).isNull();
	}

	@Test
	public void returnAddressWithChannelNameAfterMultipleEndpoints() {
		MessageChannel channel1 = (MessageChannel) context.getBean("channel1");
		PollableChannel replyChannel = (PollableChannel) context.getBean("replyChannel");
		Message<String> message = MessageBuilder.withPayload("*")
				.setReplyChannelName("replyChannel").build();
		channel1.send(message);
		Message<?> response = replyChannel.receive(3000);
		assertThat(response).isNotNull();
		assertThat(response.getPayload()).isEqualTo("********");
		PollableChannel channel2 = (PollableChannel) context.getBean("channel2");
		assertThat(channel2.receive(0)).isNull();
	}

	@Test
	public void returnAddressFallbackButNotAvailable() {
		MessageChannel channel3 = (MessageChannel) context.getBean("channel3");
		GenericMessage<String> message = new GenericMessage<>("*");
		assertThatExceptionOfType(MessagingException.class)
				.isThrownBy(() -> channel3.send(message))
				.withCauseInstanceOf(DestinationResolutionException.class);
	}

	@Test
	public void outputChannelWithNoReturnAddress() {
		MessageChannel channel4 = (MessageChannel) context.getBean("channel4");
		PollableChannel replyChannel = (PollableChannel) context.getBean("replyChannel");
		GenericMessage<String> message = new GenericMessage<>("*");
		channel4.send(message);
		Message<?> response = replyChannel.receive(3000);
		assertThat(response).isNotNull();
		assertThat(response.getPayload()).isEqualTo("**");
	}

	@Test
	public void outputChannelTakesPrecedence() {
		MessageChannel channel4 = (MessageChannel) context.getBean("channel4");
		PollableChannel replyChannel = (PollableChannel) context.getBean("replyChannel");
		Message<String> message = MessageBuilder.withPayload("*")
				.setReplyChannelName("channel5").build();
		channel4.send(message);
		Message<?> response = replyChannel.receive(3000);
		assertThat(response).isNotNull();
		assertThat(response.getPayload()).isEqualTo("**");
		PollableChannel channel5 = (PollableChannel) context.getBean("channel5");
		assertThat(channel5.receive(0)).isNull();
	}

}
