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

package org.springframework.integration.filter;

import org.junit.Test;

import org.springframework.integration.MessageRejectedException;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 */
public class MessageFilterTests {

	@Test
	public void filterAcceptsMessage() {
		MessageFilter filter = new MessageFilter(message -> true);
		Message<?> message = new GenericMessage<String>("test");
		QueueChannel output = new QueueChannel();
		filter.setOutputChannel(output);
		filter.handleMessage(message);
		Message<?> received = output.receive(0);
		assertThat(received.getPayload()).isEqualTo(message.getPayload());
		assertThat(received.getHeaders().getId()).isEqualTo(message.getHeaders().getId());
	}

	@Test
	public void filterRejectsMessageSilently() {
		MessageFilter filter = new MessageFilter(message -> false);
		QueueChannel output = new QueueChannel();
		filter.setOutputChannel(output);
		filter.handleMessage(new GenericMessage<String>("test"));
		assertThat(output.receive(0)).isNull();
	}

	@Test(expected = MessageRejectedException.class)
	public void filterThrowsException() {
		MessageFilter filter = new MessageFilter(message -> false);
		filter.setThrowExceptionOnRejection(true);
		QueueChannel output = new QueueChannel();
		filter.setOutputChannel(output);
		filter.handleMessage(new GenericMessage<String>("test"));
	}

	@Test
	public void filterAcceptsWithChannels() {
		DirectChannel inputChannel = new DirectChannel();
		QueueChannel outputChannel = new QueueChannel();
		MessageFilter filter = new MessageFilter(message -> true);
		filter.setOutputChannel(outputChannel);
		EventDrivenConsumer endpoint = new EventDrivenConsumer(inputChannel, filter);
		endpoint.start();
		Message<?> message = new GenericMessage<String>("test");
		assertThat(inputChannel.send(message)).isTrue();
		Message<?> reply = outputChannel.receive(0);
		assertThat(reply).isNotNull();
		assertThat(reply.getPayload()).isEqualTo(message.getPayload());
	}

	@Test
	public void filterRejectsSilentlyWithChannels() {
		DirectChannel inputChannel = new DirectChannel();
		QueueChannel outputChannel = new QueueChannel();
		MessageFilter filter = new MessageFilter(message -> false);
		filter.setOutputChannel(outputChannel);
		EventDrivenConsumer endpoint = new EventDrivenConsumer(inputChannel, filter);
		endpoint.start();
		Message<?> message = new GenericMessage<String>("test");
		assertThat(inputChannel.send(message)).isTrue();
		assertThat(outputChannel.receive(0)).isNull();
	}

	@Test(expected = MessageRejectedException.class)
	public void filterThrowsExceptionWithChannels() {
		DirectChannel inputChannel = new DirectChannel();
		QueueChannel outputChannel = new QueueChannel();
		MessageFilter filter = new MessageFilter(message -> false);
		filter.setOutputChannel(outputChannel);
		filter.setThrowExceptionOnRejection(true);
		EventDrivenConsumer endpoint = new EventDrivenConsumer(inputChannel, filter);
		endpoint.start();
		Message<?> message = new GenericMessage<String>("test");
		assertThat(inputChannel.send(message)).isTrue();
	}

	@Test
	public void filterDiscardsMessage() {
		DirectChannel inputChannel = new DirectChannel();
		QueueChannel outputChannel = new QueueChannel();
		QueueChannel discardChannel = new QueueChannel();
		MessageFilter filter = new MessageFilter(message -> false);
		filter.setOutputChannel(outputChannel);
		filter.setDiscardChannel(discardChannel);
		EventDrivenConsumer endpoint = new EventDrivenConsumer(inputChannel, filter);
		endpoint.start();
		Message<?> message = new GenericMessage<String>("test");
		assertThat(inputChannel.send(message)).isTrue();
		Message<?> reply = discardChannel.receive(0);
		assertThat(reply).isNotNull();
		assertThat(reply).isEqualTo(message);
		assertThat(outputChannel.receive(0)).isNull();
	}

	@Test(expected = MessageRejectedException.class)
	public void filterDiscardsMessageAndThrowsException() throws Exception {
		DirectChannel inputChannel = new DirectChannel();
		QueueChannel outputChannel = new QueueChannel();
		QueueChannel discardChannel = new QueueChannel();
		MessageFilter filter = new MessageFilter(message -> false);
		filter.setOutputChannel(outputChannel);
		filter.setDiscardChannel(discardChannel);
		filter.setThrowExceptionOnRejection(true);
		EventDrivenConsumer endpoint = new EventDrivenConsumer(inputChannel, filter);
		endpoint.start();
		Message<?> message = new GenericMessage<String>("test");
		try {
			assertThat(inputChannel.send(message)).isTrue();
		}
		catch (Exception e) {
			throw e;
		}
		finally {
			Message<?> reply = discardChannel.receive(0);
			assertThat(reply).isNotNull();
			assertThat(reply).isEqualTo(message);
			assertThat(outputChannel.receive(0)).isNull();
		}

	}

}
