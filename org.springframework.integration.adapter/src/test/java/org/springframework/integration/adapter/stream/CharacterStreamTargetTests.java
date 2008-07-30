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

import org.junit.Before;
import org.junit.Test;

import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.dispatcher.BroadcastingDispatcher;
import org.springframework.integration.dispatcher.PollingDispatcher;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.message.StringMessage;
import org.springframework.integration.scheduling.PollingSchedule;

/**
 * @author Mark Fisher
 */
public class CharacterStreamTargetTests {

	private QueueChannel channel;

	private PollingDispatcher dispatcher;


	@Before
	public void initialize() {
		this.channel = new QueueChannel(10);
		this.dispatcher = new PollingDispatcher(channel, new BroadcastingDispatcher(), new PollingSchedule(0));
	}


	@Test
	public void testSingleString() {
		StringWriter writer = new StringWriter();
		CharacterStreamTarget target = new CharacterStreamTarget(writer);
		target.send(new StringMessage("foo"));
		assertEquals("foo", writer.toString());
	}

	@Test
	public void testTwoStringsAndNoNewLinesByDefault() {
		StringWriter writer = new StringWriter();
		CharacterStreamTarget target = new CharacterStreamTarget(writer);
		dispatcher.addTarget(target);
		channel.send(new StringMessage("foo"), 0);
		channel.send(new StringMessage("bar"), 0);
		dispatcher.run();
		assertEquals("foo", writer.toString());
		dispatcher.run();
		assertEquals("foobar", writer.toString());
	}

	@Test
	public void testTwoStringsWithNewLines() {
		StringWriter writer = new StringWriter();
		CharacterStreamTarget target = new CharacterStreamTarget(writer);
		target.setShouldAppendNewLine(true);
		dispatcher.addTarget(target);
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
		CharacterStreamTarget target = new CharacterStreamTarget(writer);
		dispatcher.setMaxMessagesPerTask(2);
		dispatcher.addTarget(target);
		channel.send(new StringMessage("foo"), 0);
		channel.send(new StringMessage("bar"), 0);
		dispatcher.run();
		assertEquals("foobar", writer.toString());
	}

	@Test
	public void testMaxMessagesPerTaskExceedsMessageCountWithAppendedNewLines() {
		StringWriter writer = new StringWriter();
		CharacterStreamTarget target = new CharacterStreamTarget(writer);
		dispatcher.setMaxMessagesPerTask(10);
		dispatcher.setReceiveTimeout(0);
		dispatcher.addTarget(target);		
		target.setShouldAppendNewLine(true);
		channel.send(new StringMessage("foo"), 0);
		channel.send(new StringMessage("bar"), 0);
		dispatcher.run();
		String newLine = System.getProperty("line.separator");
		assertEquals("foo" + newLine + "bar" + newLine, writer.toString());
	}

	@Test
	public void testSingleNonStringObject() {
		StringWriter writer = new StringWriter();
		CharacterStreamTarget target = new CharacterStreamTarget(writer);
		dispatcher.addTarget(target);
		TestObject testObject = new TestObject("foo");
		channel.send(new GenericMessage<TestObject>(testObject));
		dispatcher.run();
		assertEquals("foo", writer.toString());
	}

	@Test
	public void testTwoNonStringObjectWithOutNewLines() {
		StringWriter writer = new StringWriter();
		CharacterStreamTarget target = new CharacterStreamTarget(writer);
		dispatcher.setReceiveTimeout(0);
		dispatcher.setMaxMessagesPerTask(2);
		dispatcher.addTarget(target);
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
		CharacterStreamTarget target = new CharacterStreamTarget(writer);
		target.setShouldAppendNewLine(true);
		dispatcher.setReceiveTimeout(0);
		dispatcher.setMaxMessagesPerTask(2);
		dispatcher.addTarget(target);
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
