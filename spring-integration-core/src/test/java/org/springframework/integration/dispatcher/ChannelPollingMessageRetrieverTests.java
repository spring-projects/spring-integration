/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.integration.dispatcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.Iterator;

import org.junit.Test;

import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.channel.SimpleChannel;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.StringMessage;

/**
 * @author Mark Fisher
 */
public class ChannelPollingMessageRetrieverTests {

	@Test
	public void testSingleMessagePerRetrieval() {
		DispatcherPolicy dispatcherPolicy = new DispatcherPolicy();
		dispatcherPolicy.setReceiveTimeout(0);
		MessageChannel channel = new SimpleChannel(dispatcherPolicy);
		ChannelPollingMessageRetriever retriever = new ChannelPollingMessageRetriever(channel);
		Collection<Message<?>> results = retriever.retrieveMessages();
		assertTrue(results.isEmpty());
		channel.send(new StringMessage("test1"));
		channel.send(new StringMessage("test2"));
		results = retriever.retrieveMessages();
		assertEquals(1, results.size());
		assertEquals("test1", results.iterator().next().getPayload());
		results = retriever.retrieveMessages();
		assertEquals(1, results.size());
		assertEquals("test2", results.iterator().next().getPayload());
	}

	@Test
	public void testMultipleMessagesPerRetrieval() {
		DispatcherPolicy dispatcherPolicy = new DispatcherPolicy();
		dispatcherPolicy.setReceiveTimeout(0);
		dispatcherPolicy.setMaxMessagesPerTask(2);
		MessageChannel channel = new SimpleChannel(dispatcherPolicy);
		ChannelPollingMessageRetriever retriever = new ChannelPollingMessageRetriever(channel);
		Collection<Message<?>> results = retriever.retrieveMessages();
		assertTrue(results.isEmpty());
		channel.send(new StringMessage("test1"));
		channel.send(new StringMessage("test2"));
		channel.send(new StringMessage("test3"));
		results = retriever.retrieveMessages();
		assertEquals(2, results.size());
		Iterator<Message<?>> iter = results.iterator();
		assertEquals("test1", iter.next().getPayload());
		assertEquals("test2", iter.next().getPayload());
		results = retriever.retrieveMessages();
		assertEquals(1, results.size());
		assertEquals("test3", results.iterator().next().getPayload());
	}

	@Test
	public void testMaxMessagesConfiguredDynamically() {
		DispatcherPolicy dispatcherPolicy = new DispatcherPolicy();
		dispatcherPolicy.setReceiveTimeout(0);
		dispatcherPolicy.setMaxMessagesPerTask(1);
		MessageChannel channel = new SimpleChannel(dispatcherPolicy);
		ChannelPollingMessageRetriever retriever = new ChannelPollingMessageRetriever(channel);
		Collection<Message<?>> results = retriever.retrieveMessages();
		assertTrue(results.isEmpty());
		channel.send(new StringMessage("test1"));
		channel.send(new StringMessage("test2"));
		channel.send(new StringMessage("test3"));
		results = retriever.retrieveMessages();
		assertEquals(1, results.size());
		assertEquals("test1", results.iterator().next().getPayload());
		channel.getDispatcherPolicy().setMaxMessagesPerTask(5);
		results = retriever.retrieveMessages();
		assertEquals(2, results.size());
		Iterator<Message<?>> iter = results.iterator();
		assertEquals("test2", iter.next().getPayload());
		assertEquals("test3", iter.next().getPayload());
	}

}
