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

package org.springframework.integration.handler;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.splitter.AbstractMessageSplitter;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @author Gunnar Hillert
 * @author Artem Bilan
 *
 * @since 2.0
 */
public class CollectionAndArrayTests {

	@Test
	public void listWithRequestReplyHandler() {
		MessageHandler handler = new AbstractReplyProducingMessageHandler() {

			@Override
			protected Object handleRequestMessage(Message<?> requestMessage) {
				return Arrays.asList("foo", "bar");
			}
		};
		QueueChannel channel = new QueueChannel();
		Message<?> message = MessageBuilder.withPayload("test").setReplyChannel(channel).build();
		handler.handleMessage(message);
		Message<?> reply1 = channel.receive(0);
		Message<?> reply2 = channel.receive(0);
		assertThat(reply1).isNotNull();
		assertThat(reply2).isNull();
		assertThat(List.class.isAssignableFrom(reply1.getPayload().getClass())).isTrue();
		assertThat(((List<?>) reply1.getPayload()).size()).isEqualTo(2);
	}

	@Test
	public void setWithRequestReplyHandler() {
		MessageHandler handler = new AbstractReplyProducingMessageHandler() {

			@Override
			protected Object handleRequestMessage(Message<?> requestMessage) {
				return new HashSet<>(Arrays.asList("foo", "bar"));
			}
		};
		QueueChannel channel = new QueueChannel();
		Message<?> message = MessageBuilder.withPayload("test").setReplyChannel(channel).build();
		handler.handleMessage(message);
		Message<?> reply1 = channel.receive(0);
		Message<?> reply2 = channel.receive(0);
		assertThat(reply1).isNotNull();
		assertThat(reply2).isNull();
		assertThat(reply1.getPayload()).isInstanceOf(Set.class);
		assertThat(((Set<?>) reply1.getPayload()).size()).isEqualTo(2);
	}

	@Test
	public void arrayWithRequestReplyHandler() {
		MessageHandler handler = new AbstractReplyProducingMessageHandler() {

			@Override
			protected Object handleRequestMessage(Message<?> requestMessage) {
				return new String[] {"foo", "bar"};
			}
		};
		QueueChannel channel = new QueueChannel();
		Message<?> message = MessageBuilder.withPayload("test").setReplyChannel(channel).build();
		handler.handleMessage(message);
		Message<?> reply1 = channel.receive(0);
		Message<?> reply2 = channel.receive(0);
		assertThat(reply1).isNotNull();
		assertThat(reply2).isNull();
		assertThat(reply1.getPayload().getClass().isArray()).isTrue();
		assertThat(((String[]) reply1.getPayload()).length).isEqualTo(2);
	}

	@Test
	public void listWithSplittingHandler() {
		AbstractMessageSplitter handler = new AbstractMessageSplitter() {

			@Override
			protected Object splitMessage(Message<?> message) {
				return Arrays.asList("foo", "bar");
			}
		};
		QueueChannel channel = new QueueChannel();
		Message<?> message = MessageBuilder.withPayload("test").setReplyChannel(channel).build();
		handler.handleMessage(message);
		Message<?> reply1 = channel.receive(0);
		Message<?> reply2 = channel.receive(0);
		assertThat(reply1).isNotNull();
		assertThat(reply2).isNotNull();
		assertThat(reply1.getPayload().getClass()).isEqualTo(String.class);
		assertThat(reply2.getPayload().getClass()).isEqualTo(String.class);
		assertThat(reply1.getPayload()).isEqualTo("foo");
		assertThat(reply2.getPayload()).isEqualTo("bar");
	}

	@Test
	public void setWithSplittingHandler() {
		AbstractMessageSplitter handler = new AbstractMessageSplitter() {

			@Override
			protected Object splitMessage(Message<?> message) {
				return new HashSet<String>(Arrays.asList("foo", "bar"));
			}
		};
		QueueChannel channel = new QueueChannel();
		Message<?> message = MessageBuilder.withPayload("test").setReplyChannel(channel).build();
		handler.handleMessage(message);
		Message<?> reply1 = channel.receive(0);
		Message<?> reply2 = channel.receive(0);
		assertThat(reply1).isNotNull();
		assertThat(reply2).isNotNull();
		assertThat(reply1.getPayload().getClass()).isEqualTo(String.class);
		assertThat(reply2.getPayload().getClass()).isEqualTo(String.class);
	}

	@Test
	public void arrayWithSplittingHandler() {
		AbstractMessageSplitter handler = new AbstractMessageSplitter() {

			@Override
			protected Object splitMessage(Message<?> message) {
				return new String[] {"foo", "bar"};
			}
		};
		QueueChannel channel = new QueueChannel();
		Message<?> message = MessageBuilder.withPayload("test").setReplyChannel(channel).build();
		handler.handleMessage(message);
		Message<?> reply1 = channel.receive(0);
		Message<?> reply2 = channel.receive(0);
		assertThat(reply1).isNotNull();
		assertThat(reply2).isNotNull();
		assertThat(reply1.getPayload().getClass()).isEqualTo(String.class);
		assertThat(reply2.getPayload().getClass()).isEqualTo(String.class);
		assertThat(reply1.getPayload()).isEqualTo("foo");
		assertThat(reply2.getPayload()).isEqualTo("bar");
	}

}
