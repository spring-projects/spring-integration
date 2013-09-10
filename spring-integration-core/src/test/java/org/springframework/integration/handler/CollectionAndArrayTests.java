/*
 * Copyright 2002-2013 the original author or authors.
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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.instanceOf;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import org.springframework.messaging.Message;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.integration.splitter.AbstractMessageSplitter;
import org.springframework.integration.support.MessageBuilder;

/**
 * @author Mark Fisher
 * @author Gunnar Hillert
 * @since 2.0
 */
public class CollectionAndArrayTests {

	@Test
	public void listWithRequestReplyHandler() {
		MessageHandler handler = new AbstractReplyProducingMessageHandler() {
			@Override
			protected Object handleRequestMessage(Message<?> requestMessage) {
				return Arrays.asList(new String[] { "foo", "bar" });
			}
		};
		QueueChannel channel = new QueueChannel();
		Message<?> message = MessageBuilder.withPayload("test").setReplyChannel(channel).build();
		handler.handleMessage(message);
		Message<?> reply1 = channel.receive(0);
		Message<?> reply2 = channel.receive(0);
		assertNotNull(reply1);
		assertNull(reply2);
		assertTrue(List.class.isAssignableFrom(reply1.getPayload().getClass()));
		assertEquals(2, ((List<?>) reply1.getPayload()).size());
	}

	@Test
	public void setWithRequestReplyHandler() {
		MessageHandler handler = new AbstractReplyProducingMessageHandler() {
			@Override
			protected Object handleRequestMessage(Message<?> requestMessage) {
				return new HashSet<String>(Arrays.asList(new String[] { "foo", "bar" }));
			}
		};
		QueueChannel channel = new QueueChannel();
		Message<?> message = MessageBuilder.withPayload("test").setReplyChannel(channel).build();
		handler.handleMessage(message);
		Message<?> reply1 = channel.receive(0);
		Message<?> reply2 = channel.receive(0);
		assertNotNull(reply1);
		assertNull(reply2);
		assertThat(reply1.getPayload(), is(instanceOf(Set.class)));
		assertEquals(2, ((Set<?>) reply1.getPayload()).size());
	}

	@Test
	public void arrayWithRequestReplyHandler() {
		MessageHandler handler = new AbstractReplyProducingMessageHandler() {
			@Override
			protected Object handleRequestMessage(Message<?> requestMessage) {
				return new String[] { "foo", "bar" };
			}
		};
		QueueChannel channel = new QueueChannel();
		Message<?> message = MessageBuilder.withPayload("test").setReplyChannel(channel).build();
		handler.handleMessage(message);
		Message<?> reply1 = channel.receive(0);
		Message<?> reply2 = channel.receive(0);
		assertNotNull(reply1);
		assertNull(reply2);
		assertTrue(reply1.getPayload().getClass().isArray());
		assertEquals(2, ((String[]) reply1.getPayload()).length);
	}

	@Test
	public void listWithSplittingHandler() {
		AbstractMessageSplitter handler = new AbstractMessageSplitter() {
			@Override
			protected Object splitMessage(Message<?> message) {
				return Arrays.asList(new String[] { "foo", "bar" });
			}
		};
		QueueChannel channel = new QueueChannel();
		Message<?> message = MessageBuilder.withPayload("test").setReplyChannel(channel).build();
		handler.handleMessage(message);
		Message<?> reply1 = channel.receive(0);
		Message<?> reply2 = channel.receive(0);
		assertNotNull(reply1);
		assertNotNull(reply2);
		assertEquals(String.class, reply1.getPayload().getClass());
		assertEquals(String.class, reply2.getPayload().getClass());
		assertEquals("foo", reply1.getPayload());
		assertEquals("bar", reply2.getPayload());
	}

	@Test
	public void setWithSplittingHandler() {
		AbstractMessageSplitter handler = new AbstractMessageSplitter() {
			@Override
			protected Object splitMessage(Message<?> message) {
				return new HashSet<String>(Arrays.asList(new String[] { "foo", "bar" }));
			}
		};
		QueueChannel channel = new QueueChannel();
		Message<?> message = MessageBuilder.withPayload("test").setReplyChannel(channel).build();
		handler.handleMessage(message);
		Message<?> reply1 = channel.receive(0);
		Message<?> reply2 = channel.receive(0);
		assertNotNull(reply1);
		assertNotNull(reply2);
		assertEquals(String.class, reply1.getPayload().getClass());
		assertEquals(String.class, reply2.getPayload().getClass());
	}

	@Test
	public void arrayWithSplittingHandler() {
		AbstractMessageSplitter handler = new AbstractMessageSplitter() {
			@Override
			protected Object splitMessage(Message<?> message) {
				return new String[] { "foo", "bar" };
			}
		};
		QueueChannel channel = new QueueChannel();
		Message<?> message = MessageBuilder.withPayload("test").setReplyChannel(channel).build();
		handler.handleMessage(message);
		Message<?> reply1 = channel.receive(0);
		Message<?> reply2 = channel.receive(0);
		assertNotNull(reply1);
		assertNotNull(reply2);
		assertEquals(String.class, reply1.getPayload().getClass());
		assertEquals(String.class, reply2.getPayload().getClass());
		assertEquals("foo", reply1.getPayload());
		assertEquals("bar", reply2.getPayload());
	}

}
