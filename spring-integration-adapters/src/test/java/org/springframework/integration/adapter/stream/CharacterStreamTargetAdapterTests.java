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

package org.springframework.integration.adapter.stream;

import static org.junit.Assert.assertEquals;

import java.io.StringWriter;

import org.junit.Test;

import org.springframework.integration.channel.DispatcherPolicy;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.dispatcher.PollingDispatcher;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.message.StringMessage;

/**
 * @author Mark Fisher
 */
public class CharacterStreamTargetAdapterTests {

	@Test
	public void testSingleString() {
		StringWriter writer = new StringWriter();
		CharacterStreamTargetAdapter adapter = new CharacterStreamTargetAdapter(writer);
		adapter.send(new StringMessage("foo"));
		assertEquals("foo", writer.toString());
	}

	@Test
	public void testTwoStringsAndNoNewLinesByDefault() {
		MessageChannel channel = new QueueChannel();
		StringWriter writer = new StringWriter();
		CharacterStreamTargetAdapter adapter = new CharacterStreamTargetAdapter(writer);
		PollingDispatcher dispatcher = new PollingDispatcher(channel, null);
		dispatcher.subscribe(adapter);
		channel.send(new StringMessage("foo"), 0);
		channel.send(new StringMessage("bar"), 0);
		dispatcher.run();
		assertEquals("foo", writer.toString());
		dispatcher.run();
		assertEquals("foobar", writer.toString());
	}

	@Test
	public void testTwoStringsWithNewLines() {
		MessageChannel channel = new QueueChannel();
		StringWriter writer = new StringWriter();
		CharacterStreamTargetAdapter adapter = new CharacterStreamTargetAdapter(writer);
		adapter.setShouldAppendNewLine(true);
		PollingDispatcher dispatcher = new PollingDispatcher(channel, null);
		dispatcher.subscribe(adapter);
		channel.send(new StringMessage("foo"), 0);
		channel.send(new StringMessage("bar"), 0);
		dispatcher.run();
		String newLine = System.getProperty("line.separator");
		assertEquals("foo" + newLine, writer.toString());
		dispatcher.run();
		assertEquals("foo" + newLine + "bar" + newLine, writer.toString());
	}

	@Test
	public void testMaxMessagesPerTaskSameAsMessageCount() {
		StringWriter writer = new StringWriter();
		CharacterStreamTargetAdapter adapter = new CharacterStreamTargetAdapter(writer);
		DispatcherPolicy dispatcherPolicy = new DispatcherPolicy();
		dispatcherPolicy.setMaxMessagesPerTask(2);
		QueueChannel channel = new QueueChannel(5, dispatcherPolicy);
		PollingDispatcher dispatcher = new PollingDispatcher(channel, null);
		dispatcher.subscribe(adapter);
		channel.send(new StringMessage("foo"), 0);
		channel.send(new StringMessage("bar"), 0);
		dispatcher.run();
		assertEquals("foobar", writer.toString());
	}

	@Test
	public void testMaxMessagesPerTaskExceedsMessageCountWithAppendedNewLines() {
		StringWriter writer = new StringWriter();
		CharacterStreamTargetAdapter adapter = new CharacterStreamTargetAdapter(writer);
		DispatcherPolicy dispatcherPolicy = new DispatcherPolicy();
		dispatcherPolicy.setMaxMessagesPerTask(10);
		dispatcherPolicy.setReceiveTimeout(0);
		QueueChannel channel = new QueueChannel(5, dispatcherPolicy);
		PollingDispatcher dispatcher = new PollingDispatcher(channel, null);
		adapter.setShouldAppendNewLine(true);
		dispatcher.subscribe(adapter);
		channel.send(new StringMessage("foo"), 0);
		channel.send(new StringMessage("bar"), 0);
		dispatcher.run();
		String newLine = System.getProperty("line.separator");
		assertEquals("foo" + newLine + "bar" + newLine, writer.toString());
	}

	@Test
	public void testSingleNonStringObject() {
		MessageChannel channel = new QueueChannel();
		StringWriter writer = new StringWriter();
		CharacterStreamTargetAdapter adapter = new CharacterStreamTargetAdapter(writer);
		PollingDispatcher dispatcher = new PollingDispatcher(channel, null);
		dispatcher.subscribe(adapter);
		TestObject testObject = new TestObject("foo");
		channel.send(new GenericMessage<TestObject>(testObject));
		dispatcher.run();
		assertEquals("foo", writer.toString());
	}

	@Test
	public void testTwoNonStringObjectWithOutNewLines() {
		StringWriter writer = new StringWriter();
		CharacterStreamTargetAdapter adapter = new CharacterStreamTargetAdapter(writer);
		DispatcherPolicy dispatcherPolicy = new DispatcherPolicy();
		dispatcherPolicy.setReceiveTimeout(0);
		dispatcherPolicy.setMaxMessagesPerTask(2);
		QueueChannel channel = new QueueChannel(5, dispatcherPolicy);
		PollingDispatcher dispatcher = new PollingDispatcher(channel, null);
		dispatcher.subscribe(adapter);
		TestObject testObject1 = new TestObject("foo");
		TestObject testObject2 = new TestObject("bar");
		channel.send(new GenericMessage<TestObject>(testObject1), 0);
		channel.send(new GenericMessage<TestObject>(testObject2), 0);
		dispatcher.run();
		assertEquals("foobar", writer.toString());
	}

	@Test
	public void testTwoNonStringObjectWithNewLines() {
		StringWriter writer = new StringWriter();
		CharacterStreamTargetAdapter adapter = new CharacterStreamTargetAdapter(writer);
		DispatcherPolicy dispatcherPolicy = new DispatcherPolicy();
		dispatcherPolicy.setReceiveTimeout(0);
		dispatcherPolicy.setMaxMessagesPerTask(2);
		QueueChannel channel = new QueueChannel(5, dispatcherPolicy);
		adapter.setShouldAppendNewLine(true);
		PollingDispatcher dispatcher = new PollingDispatcher(channel, null);
		dispatcher.subscribe(adapter);
		TestObject testObject1 = new TestObject("foo");
		TestObject testObject2 = new TestObject("bar");
		channel.send(new GenericMessage<TestObject>(testObject1), 0);
		channel.send(new GenericMessage<TestObject>(testObject2), 0);
		dispatcher.run();
		String newLine = System.getProperty("line.separator");
		assertEquals("foo" + newLine + "bar" + newLine, writer.toString());
	}


	private static class TestObject {

		private String text;

		TestObject(String text) {
			this.text = text;
		}

		public String toString() {
			return this.text;
		}
	}

}
