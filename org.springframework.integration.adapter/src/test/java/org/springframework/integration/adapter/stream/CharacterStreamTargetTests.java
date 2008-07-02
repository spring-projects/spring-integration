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
import org.springframework.integration.dispatcher.PollingDispatcherTask;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.message.StringMessage;

/**
 * @author Mark Fisher
 */
public class CharacterStreamTargetTests {

	@Test
	public void testSingleString() {
		StringWriter writer = new StringWriter();
		CharacterStreamTarget target = new CharacterStreamTarget(writer);
		target.send(new StringMessage("foo"));
		assertEquals("foo", writer.toString());
	}

	@Test
	public void testTwoStringsAndNoNewLinesByDefault() {
		MessageChannel channel = new QueueChannel();
		StringWriter writer = new StringWriter();
		CharacterStreamTarget target = new CharacterStreamTarget(writer);
		PollingDispatcherTask task = new PollingDispatcherTask(channel, null);
		task.subscribe(target);
		channel.send(new StringMessage("foo"), 0);
		channel.send(new StringMessage("bar"), 0);
		task.run();
		assertEquals("foo", writer.toString());
		task.run();
		assertEquals("foobar", writer.toString());
	}

	@Test
	public void testTwoStringsWithNewLines() {
		MessageChannel channel = new QueueChannel();
		StringWriter writer = new StringWriter();
		CharacterStreamTarget target = new CharacterStreamTarget(writer);
		target.setShouldAppendNewLine(true);
		PollingDispatcherTask task = new PollingDispatcherTask(channel, null);
		task.subscribe(target);
		channel.send(new StringMessage("foo"), 0);
		channel.send(new StringMessage("bar"), 0);
		task.run();
		String newLine = System.getProperty("line.separator");
		assertEquals("foo" + newLine, writer.toString());
		task.run();
		assertEquals("foo" + newLine + "bar" + newLine, writer.toString());
	}

	@Test
	public void testMaxMessagesPerTaskSameAsMessageCount() {
		StringWriter writer = new StringWriter();
		CharacterStreamTarget target = new CharacterStreamTarget(writer);
		DispatcherPolicy dispatcherPolicy = new DispatcherPolicy();
		QueueChannel channel = new QueueChannel(5, dispatcherPolicy);
		PollingDispatcherTask task = new PollingDispatcherTask(channel, null);
		task.setMaxMessagesPerTask(2);
		task.subscribe(target);
		channel.send(new StringMessage("foo"), 0);
		channel.send(new StringMessage("bar"), 0);
		task.run();
		assertEquals("foobar", writer.toString());
	}

	@Test
	public void testMaxMessagesPerTaskExceedsMessageCountWithAppendedNewLines() {
		StringWriter writer = new StringWriter();
		CharacterStreamTarget target = new CharacterStreamTarget(writer);
		DispatcherPolicy dispatcherPolicy = new DispatcherPolicy();
		QueueChannel channel = new QueueChannel(5, dispatcherPolicy);
		PollingDispatcherTask task = new PollingDispatcherTask(channel, null);
		task.setMaxMessagesPerTask(10);
		task.setReceiveTimeout(0);
		task.subscribe(target);		
		target.setShouldAppendNewLine(true);
		channel.send(new StringMessage("foo"), 0);
		channel.send(new StringMessage("bar"), 0);
		task.run();
		String newLine = System.getProperty("line.separator");
		assertEquals("foo" + newLine + "bar" + newLine, writer.toString());
	}

	@Test
	public void testSingleNonStringObject() {
		MessageChannel channel = new QueueChannel();
		StringWriter writer = new StringWriter();
		CharacterStreamTarget target = new CharacterStreamTarget(writer);
		PollingDispatcherTask task = new PollingDispatcherTask(channel, null);
		task.subscribe(target);
		TestObject testObject = new TestObject("foo");
		channel.send(new GenericMessage<TestObject>(testObject));
		task.run();
		assertEquals("foo", writer.toString());
	}

	@Test
	public void testTwoNonStringObjectWithOutNewLines() {
		StringWriter writer = new StringWriter();
		CharacterStreamTarget target = new CharacterStreamTarget(writer);
		DispatcherPolicy dispatcherPolicy = new DispatcherPolicy();
		QueueChannel channel = new QueueChannel(5, dispatcherPolicy);
		PollingDispatcherTask task = new PollingDispatcherTask(channel, null);
		task.setReceiveTimeout(0);
		task.setMaxMessagesPerTask(2);
		task.subscribe(target);
		TestObject testObject1 = new TestObject("foo");
		TestObject testObject2 = new TestObject("bar");
		channel.send(new GenericMessage<TestObject>(testObject1), 0);
		channel.send(new GenericMessage<TestObject>(testObject2), 0);
		task.run();
		assertEquals("foobar", writer.toString());
	}

	@Test
	public void testTwoNonStringObjectWithNewLines() {
		StringWriter writer = new StringWriter();
		CharacterStreamTarget target = new CharacterStreamTarget(writer);
		DispatcherPolicy dispatcherPolicy = new DispatcherPolicy();
		QueueChannel channel = new QueueChannel(5, dispatcherPolicy);
		target.setShouldAppendNewLine(true);
		PollingDispatcherTask task = new PollingDispatcherTask(channel, null);
		task.setReceiveTimeout(0);
		task.setMaxMessagesPerTask(2);
		task.subscribe(target);
		TestObject testObject1 = new TestObject("foo");
		TestObject testObject2 = new TestObject("bar");
		channel.send(new GenericMessage<TestObject>(testObject1), 0);
		channel.send(new GenericMessage<TestObject>(testObject2), 0);
		task.run();
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
