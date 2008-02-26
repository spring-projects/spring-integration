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

package org.springframework.integration.adapter.stream;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;

import org.junit.Test;

import org.springframework.integration.channel.DispatcherPolicy;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.channel.SimpleChannel;
import org.springframework.integration.dispatcher.DefaultMessageDispatcher;
import org.springframework.integration.dispatcher.MessageDispatcher;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.message.StringMessage;
import org.springframework.integration.scheduling.SimpleMessagingTaskScheduler;

/**
 * @author Mark Fisher
 */
public class CharacterStreamTargetAdapterTests {

	private SimpleMessagingTaskScheduler scheduler = new SimpleMessagingTaskScheduler(1);


	@Test
	public void testSingleString() {
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		CharacterStreamTargetAdapter adapter = new CharacterStreamTargetAdapter(stream);
		adapter.handle(new StringMessage("foo"));
		String result = new String(stream.toByteArray());
		assertEquals("foo", result);
	}

	@Test
	public void testTwoStringsAndNoNewLinesByDefault() {
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		MessageChannel channel = new SimpleChannel();
		CharacterStreamTargetAdapter adapter = new CharacterStreamTargetAdapter(stream);
		MessageDispatcher dispatcher = new DefaultMessageDispatcher(channel, scheduler);
		dispatcher.addHandler(adapter);
		channel.send(new StringMessage("foo"), 0);
		channel.send(new StringMessage("bar"), 0);
		assertEquals(1, dispatcher.dispatch());
		String result1 = new String(stream.toByteArray());
		assertEquals("foo", result1);
		assertEquals(1, dispatcher.dispatch());
		String result2 = new String(stream.toByteArray());
		assertEquals("foobar", result2);
	}

	@Test
	public void testTwoStringsWithNewLines() {
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		MessageChannel channel = new SimpleChannel();
		CharacterStreamTargetAdapter adapter = new CharacterStreamTargetAdapter(stream);
		adapter.setShouldAppendNewLine(true);
		MessageDispatcher dispatcher = new DefaultMessageDispatcher(channel, scheduler);
		dispatcher.addHandler(adapter);
		channel.send(new StringMessage("foo"), 0);
		channel.send(new StringMessage("bar"), 0);
		assertEquals(1, dispatcher.dispatch());
		String result1 = new String(stream.toByteArray());
		String newLine = System.getProperty("line.separator");
		assertEquals("foo" + newLine, result1);
		assertEquals(1, dispatcher.dispatch());
		String result2 = new String(stream.toByteArray());
		assertEquals("foo" + newLine + "bar" + newLine, result2);
	}

	@Test
	public void testMaxMessagesPerTaskSameAsMessageCount() {
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		CharacterStreamTargetAdapter adapter = new CharacterStreamTargetAdapter(stream);
		DispatcherPolicy dispatcherPolicy = new DispatcherPolicy();
		dispatcherPolicy.setMaxMessagesPerTask(2);
		SimpleChannel channel = new SimpleChannel(5, dispatcherPolicy);
		MessageDispatcher dispatcher = new DefaultMessageDispatcher(channel, scheduler);
		dispatcher.addHandler(adapter);
		channel.send(new StringMessage("foo"), 0);
		channel.send(new StringMessage("bar"), 0);
		assertEquals(2, dispatcher.dispatch());
		String result = new String(stream.toByteArray());
		assertEquals("foobar", result);
	}

	@Test
	public void testMaxMessagesPerTaskExceedsMessageCountWithAppendedNewLines() {
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		CharacterStreamTargetAdapter adapter = new CharacterStreamTargetAdapter(stream);
		DispatcherPolicy dispatcherPolicy = new DispatcherPolicy();
		dispatcherPolicy.setMaxMessagesPerTask(10);
		dispatcherPolicy.setReceiveTimeout(0);
		SimpleChannel channel = new SimpleChannel(5, dispatcherPolicy);
		MessageDispatcher dispatcher = new DefaultMessageDispatcher(channel, scheduler);
		adapter.setShouldAppendNewLine(true);
		dispatcher.addHandler(adapter);
		channel.send(new StringMessage("foo"), 0);
		channel.send(new StringMessage("bar"), 0);
		assertEquals(2, dispatcher.dispatch());
		String result = new String(stream.toByteArray());
		String newLine = System.getProperty("line.separator");
		assertEquals("foo" + newLine + "bar" + newLine, result);
	}

	@Test
	public void testSingleNonStringObject() {
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		MessageChannel channel = new SimpleChannel();
		CharacterStreamTargetAdapter adapter = new CharacterStreamTargetAdapter(stream);
		MessageDispatcher dispatcher = new DefaultMessageDispatcher(channel, scheduler);
		dispatcher.addHandler(adapter);
		TestObject testObject = new TestObject("foo");
		channel.send(new GenericMessage<TestObject>(testObject));
		int count = dispatcher.dispatch();
		assertEquals(1, count);
		String result = new String(stream.toByteArray());
		assertEquals("foo", result);
	}

	@Test
	public void testTwoNonStringObjectWithOutNewLines() {
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		CharacterStreamTargetAdapter adapter = new CharacterStreamTargetAdapter(stream);
		DispatcherPolicy dispatcherPolicy = new DispatcherPolicy();
		dispatcherPolicy.setReceiveTimeout(0);
		dispatcherPolicy.setMaxMessagesPerTask(2);
		SimpleChannel channel = new SimpleChannel(5, dispatcherPolicy);
		MessageDispatcher dispatcher = new DefaultMessageDispatcher(channel, scheduler);
		dispatcher.addHandler(adapter);
		TestObject testObject1 = new TestObject("foo");
		TestObject testObject2 = new TestObject("bar");
		channel.send(new GenericMessage<TestObject>(testObject1), 0);
		channel.send(new GenericMessage<TestObject>(testObject2), 0);
		assertEquals(2, dispatcher.dispatch());
		String result = new String(stream.toByteArray());
		assertEquals("foobar", result);
	}

	@Test
	public void testTwoNonStringObjectWithNewLines() {
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		CharacterStreamTargetAdapter adapter = new CharacterStreamTargetAdapter(stream);
		DispatcherPolicy dispatcherPolicy = new DispatcherPolicy();
		dispatcherPolicy.setReceiveTimeout(0);
		dispatcherPolicy.setMaxMessagesPerTask(2);
		SimpleChannel channel = new SimpleChannel(5, dispatcherPolicy);
		DefaultMessageDispatcher dispatcher = new DefaultMessageDispatcher(channel, scheduler);
		adapter.setShouldAppendNewLine(true);
		dispatcher.addHandler(adapter);
		TestObject testObject1 = new TestObject("foo");
		TestObject testObject2 = new TestObject("bar");
		channel.send(new GenericMessage<TestObject>(testObject1), 0);
		channel.send(new GenericMessage<TestObject>(testObject2), 0);
		dispatcher.dispatch();
		String result = new String(stream.toByteArray());
		String newLine = System.getProperty("line.separator");
		assertEquals("foo" + newLine + "bar" + newLine, result);
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
