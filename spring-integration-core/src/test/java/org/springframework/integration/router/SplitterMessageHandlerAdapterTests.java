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

package org.springframework.integration.router;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.Test;

import org.springframework.integration.ConfigurationException;
import org.springframework.integration.channel.ChannelRegistry;
import org.springframework.integration.channel.DefaultChannelRegistry;
import org.springframework.integration.channel.SimpleChannel;
import org.springframework.integration.handler.AbstractMessageHandlerAdapter;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.StringMessage;

/**
 * @author Mark Fisher
 */
public class SplitterMessageHandlerAdapterTests {

	private SimpleChannel testChannel = new SimpleChannel();

	private ChannelRegistry channelRegistry = new DefaultChannelRegistry();

	private SplitterTestBean testBean = new SplitterTestBean();

	private Map<String, Object> attribs = new ConcurrentHashMap<String, Object>();


	public SplitterMessageHandlerAdapterTests() {
		this.channelRegistry.registerChannel("testChannel", testChannel);
		this.attribs.put(AbstractMessageHandlerAdapter.DEFAULT_OUTPUT_CHANNEL_NAME_KEY, "testChannel");
	}


	@Test
	public void testSplitPayloadToStringArray() throws Exception {
		StringMessage message = new StringMessage("foo.bar");
		SplitterMessageHandlerAdapter adapter = this.getAdapter("stringToStringArray");
		adapter.handle(message);
		Message<?> reply1 = testChannel.receive(0);
		assertNotNull(reply1);
		assertEquals("foo", reply1.getPayload());
		Message<?> reply2 = testChannel.receive(0);
		assertNotNull(reply2);
		assertEquals("bar", reply2.getPayload());
	}

	@Test
	public void testSplitPayloadToStringList() throws Exception {
		StringMessage message = new StringMessage("foo.bar");
		SplitterMessageHandlerAdapter adapter = this.getAdapter("stringToStringList");
		adapter.handle(message);
		Message<?> reply1 = testChannel.receive(0);
		assertNotNull(reply1);
		assertEquals("foo", reply1.getPayload());
		Message<?> reply2 = testChannel.receive(0);
		assertNotNull(reply2);
		assertEquals("bar", reply2.getPayload());
	}

	@Test
	public void testSplitMessageToStringArray() throws Exception {
		StringMessage message = new StringMessage("foo.bar");
		SplitterMessageHandlerAdapter adapter = this.getAdapter("messageToStringArray");
		adapter.handle(message);
		Message<?> reply1 = testChannel.receive(0);
		assertNotNull(reply1);
		assertEquals("foo", reply1.getPayload());
		Message<?> reply2 = testChannel.receive(0);
		assertNotNull(reply2);
		assertEquals("bar", reply2.getPayload());
	}

	@Test
	public void testSplitMessageToStringList() throws Exception {
		StringMessage message = new StringMessage("foo.bar");
		SplitterMessageHandlerAdapter adapter = this.getAdapter("messageToStringList");
		adapter.handle(message);
		Message<?> reply1 = testChannel.receive(0);
		assertNotNull(reply1);
		assertEquals("foo", reply1.getPayload());
		Message<?> reply2 = testChannel.receive(0);
		assertNotNull(reply2);
		assertEquals("bar", reply2.getPayload());
	}

	@Test
	public void testSplitMessageToMessageArray() throws Exception {
		StringMessage message = new StringMessage("foo.bar");
		SplitterMessageHandlerAdapter adapter = this.getAdapter("messageToMessageArray");
		adapter.handle(message);
		Message<?> reply1 = testChannel.receive(0);
		assertNotNull(reply1);
		assertEquals("foo", reply1.getPayload());
		Message<?> reply2 = testChannel.receive(0);
		assertNotNull(reply2);
		assertEquals("bar", reply2.getPayload());
	}

	@Test
	public void testSplitMessageToMessageList() throws Exception {
		StringMessage message = new StringMessage("foo.bar");
		SplitterMessageHandlerAdapter adapter = this.getAdapter("messageToMessageList");
		adapter.handle(message);
		Message<?> reply1 = testChannel.receive(0);
		assertNotNull(reply1);
		assertEquals("foo", reply1.getPayload());
		Message<?> reply2 = testChannel.receive(0);
		assertNotNull(reply2);
		assertEquals("bar", reply2.getPayload());
	}

	@Test
	public void testSplitStringToMessageArray() throws Exception {
		StringMessage message = new StringMessage("foo.bar");
		SplitterMessageHandlerAdapter adapter = this.getAdapter("stringToMessageArray");
		adapter.handle(message);
		Message<?> reply1 = testChannel.receive(0);
		assertNotNull(reply1);
		assertEquals("foo", reply1.getPayload());
		Message<?> reply2 = testChannel.receive(0);
		assertNotNull(reply2);
		assertEquals("bar", reply2.getPayload());
	}

	@Test
	public void testSplitStringToMessageList() throws Exception {
		StringMessage message = new StringMessage("foo.bar");
		SplitterMessageHandlerAdapter adapter = this.getAdapter("stringToMessageList");
		adapter.handle(message);
		Message<?> reply1 = testChannel.receive(0);
		assertNotNull(reply1);
		assertEquals("foo", reply1.getPayload());
		Message<?> reply2 = testChannel.receive(0);
		assertNotNull(reply2);
		assertEquals("bar", reply2.getPayload());
	}

	@Test(expected=ConfigurationException.class)
	public void testInvalidReturnType() throws Exception {
		Method splittingMethod = this.testBean.getClass().getMethod("invalidParameterCount", String.class, String.class);
		SplitterMessageHandlerAdapter adapter = new SplitterMessageHandlerAdapter(testBean, splittingMethod, attribs);
		adapter.setChannelRegistry(channelRegistry);
		adapter.afterPropertiesSet();
		StringMessage message = new StringMessage("foo.bar");
		adapter.handle(message);
	}

	@Test
	public void testHeaderForObjectReturnValues() throws Exception {
		StringMessage message = new StringMessage("foo.bar");
		SplitterMessageHandlerAdapter adapter = this.getAdapter("stringToStringArray");
		adapter.handle(message);
		Message<?> reply1 = testChannel.receive(0);
		assertNotNull(reply1);
		assertEquals(2, reply1.getHeader().getSequenceSize());
		assertEquals(1, reply1.getHeader().getSequenceNumber());
		assertEquals(message.getId(), reply1.getHeader().getCorrelationId());
		Message<?> reply2 = testChannel.receive(0);
		assertNotNull(reply2);
		assertEquals(2, reply2.getHeader().getSequenceSize());
		assertEquals(2, reply2.getHeader().getSequenceNumber());
		assertEquals(message.getId(), reply2.getHeader().getCorrelationId());
	}

	@Test
	public void testHeaderForMessageReturnValues() throws Exception {
		StringMessage message = new StringMessage("foo.bar");
		SplitterMessageHandlerAdapter adapter = this.getAdapter("messageToMessageList");
		adapter.handle(message);
		Message<?> reply1 = testChannel.receive(0);
		assertNotNull(reply1);
		assertEquals(2, reply1.getHeader().getSequenceSize());
		assertEquals(1, reply1.getHeader().getSequenceNumber());
		assertEquals(message.getId(), reply1.getHeader().getCorrelationId());
		Message<?> reply2 = testChannel.receive(0);
		assertNotNull(reply2);
		assertEquals(2, reply2.getHeader().getSequenceSize());
		assertEquals(2, reply2.getHeader().getSequenceNumber());
		assertEquals(message.getId(), reply2.getHeader().getCorrelationId());
	}


	private SplitterMessageHandlerAdapter getAdapter(String methodName) throws Exception {
		Class<?> paramType = methodName.startsWith("message") ? Message.class : String.class;
		Method splittingMethod = this.testBean.getClass().getMethod(methodName, paramType);
		SplitterMessageHandlerAdapter adapter = new SplitterMessageHandlerAdapter(testBean, splittingMethod, attribs);
		adapter.setChannelRegistry(channelRegistry);
		adapter.afterPropertiesSet();
		return adapter;
	}


	public static class SplitterTestBean {

		public String[] stringToStringArray(String input) {
			return input.split("\\.");
		}

		public List<String> stringToStringList(String input) {
			return Arrays.asList(input.split("\\."));
		}

		public String[] messageToStringArray(Message<?> input) {
			return input.getPayload().toString().split("\\.");
		}

		public List<String> messageToStringList(Message<?> input) {
			return Arrays.asList(input.getPayload().toString().split("\\."));
		}

		public Message<String>[] messageToMessageArray(Message<?> input) {
			String[] strings = input.getPayload().toString().split("\\.");
			Message<String>[] messages = new StringMessage[strings.length];
			for (int i = 0; i < strings.length; i++) {
				messages[i] = new StringMessage(strings[i]);
			}
			return messages;
		}

		public List<Message<String>> messageToMessageList(Message<?> input) {
			String[] strings = input.getPayload().toString().split("\\.");
			List<Message<String>> messages = new ArrayList<Message<String>>();
			for (String s : strings) {
				messages.add(new StringMessage(s));
			}
			return messages;
		}

		public Message<String>[] stringToMessageArray(String input) {
			String[] strings = input.split("\\.");
			Message<String>[] messages = new StringMessage[strings.length];
			for (int i = 0; i < strings.length; i++) {
				messages[i] = new StringMessage(strings[i]);
			}
			return messages;
		}

		public List<Message<String>> stringToMessageList(String input) {
			String[] strings = input.split("\\.");
			List<Message<String>> messages = new ArrayList<Message<String>>();
			for (String s : strings) {
				messages.add(new StringMessage(s));
			}
			return messages;
		}

		public String[] invalidParameterCount(String param1, String param2) {
			return null;
		}
	}

}
