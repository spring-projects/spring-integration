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

package org.springframework.integration.splitter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import org.springframework.integration.annotation.Header;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageBuilder;
import org.springframework.integration.message.StringMessage;

/**
 * @author Mark Fisher
 */
public class MethodInvokingSplitterTests {

	private SplitterTestBean testBean = new SplitterTestBean();


	@Test
	public void splitStringToStringArray() throws Exception {
		StringMessage message = new StringMessage("foo.bar");
		MethodInvokingSplitter splitter = this.getSplitter("stringToStringArray");
		List<Message<?>> replies = splitter.split(message);
		Message<?> reply1 = replies.get(0);
		assertNotNull(reply1);
		assertEquals("foo", reply1.getPayload());
		Message<?> reply2 = replies.get(1);
		assertNotNull(reply2);
		assertEquals("bar", reply2.getPayload());
	}

	@Test
	public void splitStringToStringList() throws Exception {
		StringMessage message = new StringMessage("foo.bar");
		MethodInvokingSplitter splitter = this.getSplitter("stringToStringList");
		List<Message<?>> replies = splitter.split(message);
		Message<?> reply1 = replies.get(0);
		assertNotNull(reply1);
		assertEquals("foo", reply1.getPayload());
		Message<?> reply2 = replies.get(1);
		assertNotNull(reply2);
		assertEquals("bar", reply2.getPayload());
	}

	@Test
	public void splitMessageToStringArray() throws Exception {
		StringMessage message = new StringMessage("foo.bar");
		MethodInvokingSplitter splitter = this.getSplitter("messageToStringArray");
		List<Message<?>> replies = splitter.split(message);
		Message<?> reply1 = replies.get(0);
		assertNotNull(reply1);
		assertEquals("foo", reply1.getPayload());
		Message<?> reply2 = replies.get(1);
		assertNotNull(reply2);
		assertEquals("bar", reply2.getPayload());
	}

	@Test
	public void splitMessageToStringList() throws Exception {
		StringMessage message = new StringMessage("foo.bar");
		MethodInvokingSplitter splitter = this.getSplitter("messageToStringList");
		List<Message<?>> replies = splitter.split(message);
		Message<?> reply1 = replies.get(0);
		assertNotNull(reply1);
		assertEquals("foo", reply1.getPayload());
		Message<?> reply2 = replies.get(1);
		assertNotNull(reply2);
		assertEquals("bar", reply2.getPayload());
	}

	@Test
	public void splitMessageToMessageArray() throws Exception {
		StringMessage message = new StringMessage("foo.bar");
		MethodInvokingSplitter splitter = this.getSplitter("messageToMessageArray");
		List<Message<?>> replies = splitter.split(message);
		Message<?> reply1 = replies.get(0);
		assertNotNull(reply1);
		assertEquals("foo", reply1.getPayload());
		Message<?> reply2 = replies.get(1);
		assertNotNull(reply2);
		assertEquals("bar", reply2.getPayload());
	}

	@Test
	public void splitMessageToMessageList() throws Exception {
		StringMessage message = new StringMessage("foo.bar");
		MethodInvokingSplitter splitter = this.getSplitter("messageToMessageList");
		List<Message<?>> replies = splitter.split(message);
		Message<?> reply1 = replies.get(0);
		assertNotNull(reply1);
		assertEquals("foo", reply1.getPayload());
		Message<?> reply2 = replies.get(1);
		assertNotNull(reply2);
		assertEquals("bar", reply2.getPayload());
	}

	@Test
	public void splitStringToMessageArray() throws Exception {
		StringMessage message = new StringMessage("foo.bar");
		MethodInvokingSplitter splitter = this.getSplitter("stringToMessageArray");
		List<Message<?>> replies = splitter.split(message);
		Message<?> reply1 = replies.get(0);
		assertNotNull(reply1);
		assertEquals("foo", reply1.getPayload());
		Message<?> reply2 = replies.get(1);
		assertNotNull(reply2);
		assertEquals("bar", reply2.getPayload());
	}

	@Test
	public void splitStringToMessageList() throws Exception {
		StringMessage message = new StringMessage("foo.bar");
		MethodInvokingSplitter splitter = this.getSplitter("stringToMessageList");
		List<Message<?>> replies = splitter.split(message);
		Message<?> reply1 = replies.get(0);
		assertNotNull(reply1);
		assertEquals("foo", reply1.getPayload());
		Message<?> reply2 = replies.get(1);
		assertNotNull(reply2);
		assertEquals("bar", reply2.getPayload());
	}

	@Test
	public void splitStringToStringArrayConfiguredByMethodName() throws Exception {
		StringMessage message = new StringMessage("foo.bar");
		MethodInvokingSplitter splitter = new MethodInvokingSplitter(testBean, "stringToStringArray");
		List<Message<?>> replies = splitter.split(message);
		Message<?> reply1 = replies.get(0);
		assertNotNull(reply1);
		assertEquals("foo", reply1.getPayload());
		Message<?> reply2 = replies.get(1);
		assertNotNull(reply2);
		assertEquals("bar", reply2.getPayload());
	}

	@Test
	public void splitStringToStringListConfiguredByMethodName() throws Exception {
		StringMessage message = new StringMessage("foo.bar");
		MethodInvokingSplitter splitter = new MethodInvokingSplitter(testBean, "stringToStringList");
		List<Message<?>> replies = splitter.split(message);
		Message<?> reply1 = replies.get(0);
		assertNotNull(reply1);
		assertEquals("foo", reply1.getPayload());
		Message<?> reply2 = replies.get(1);
		assertNotNull(reply2);
		assertEquals("bar", reply2.getPayload());
	}

	@Test
	public void splitMessageToStringArrayConfiguredByMethodName() throws Exception {
		StringMessage message = new StringMessage("foo.bar");
		MethodInvokingSplitter splitter = new MethodInvokingSplitter(testBean, "messageToStringArray");
		List<Message<?>> replies = splitter.split(message);
		Message<?> reply1 = replies.get(0);
		assertNotNull(reply1);
		assertEquals("foo", reply1.getPayload());
		Message<?> reply2 = replies.get(1);
		assertNotNull(reply2);
		assertEquals("bar", reply2.getPayload());
	}

	@Test
	public void splitMessageToStringListConfiguredByMethodName() throws Exception {
		StringMessage message = new StringMessage("foo.bar");
		MethodInvokingSplitter splitter = new MethodInvokingSplitter(testBean, "messageToStringList");
		List<Message<?>> replies = splitter.split(message);
		Message<?> reply1 = replies.get(0);
		assertNotNull(reply1);
		assertEquals("foo", reply1.getPayload());
		Message<?> reply2 = replies.get(1);
		assertNotNull(reply2);
		assertEquals("bar", reply2.getPayload());
	}

	@Test
	public void splitMessageToMessageArrayConfiguredByMethodName() throws Exception {
		StringMessage message = new StringMessage("foo.bar");
		MethodInvokingSplitter splitter = new MethodInvokingSplitter(testBean, "messageToMessageArray");
		List<Message<?>> replies = splitter.split(message);
		Message<?> reply1 = replies.get(0);
		assertNotNull(reply1);
		assertEquals("foo", reply1.getPayload());
		Message<?> reply2 = replies.get(1);
		assertNotNull(reply2);
		assertEquals("bar", reply2.getPayload());
	}

	@Test
	public void splitMessageToMessageListConfiguredByMethodName() throws Exception {
		StringMessage message = new StringMessage("foo.bar");
		MethodInvokingSplitter splitter = new MethodInvokingSplitter(testBean, "messageToMessageList");
		List<Message<?>> replies = splitter.split(message);
		Message<?> reply1 = replies.get(0);
		assertNotNull(reply1);
		assertEquals("foo", reply1.getPayload());
		Message<?> reply2 = replies.get(1);
		assertNotNull(reply2);
		assertEquals("bar", reply2.getPayload());
	}

	@Test
	public void splitStringToMessageArrayConfiguredByMethodName() throws Exception {
		StringMessage message = new StringMessage("foo.bar");
		MethodInvokingSplitter splitter = new MethodInvokingSplitter(testBean, "stringToMessageArray");
		List<Message<?>> replies = splitter.split(message);
		Message<?> reply1 = replies.get(0);
		assertNotNull(reply1);
		assertEquals("foo", reply1.getPayload());
		Message<?> reply2 = replies.get(1);
		assertNotNull(reply2);
		assertEquals("bar", reply2.getPayload());
	}

	@Test
	public void splitStringToMessageListConfiguredByMethodName() throws Exception {
		StringMessage message = new StringMessage("foo.bar");
		MethodInvokingSplitter splitter = new MethodInvokingSplitter(testBean, "stringToMessageList");
		List<Message<?>> replies = splitter.split(message);
		Message<?> reply1 = replies.get(0);
		assertNotNull(reply1);
		assertEquals("foo", reply1.getPayload());
		Message<?> reply2 = replies.get(1);
		assertNotNull(reply2);
		assertEquals("bar", reply2.getPayload());
	}

	@Test
	public void testHeaderForObjectReturnValues() throws Exception {
		StringMessage message = new StringMessage("foo.bar");
		MethodInvokingSplitter splitter = this.getSplitter("stringToStringArray");
		List<Message<?>> replies = splitter.split(message);
		Message<?> reply1 = replies.get(0);
		assertNotNull(reply1);
		assertEquals(new Integer(2), reply1.getHeaders().getSequenceSize());
		assertEquals(new Integer(1), reply1.getHeaders().getSequenceNumber());
		assertEquals(message.getHeaders().getId(), reply1.getHeaders().getCorrelationId());
		Message<?> reply2 = replies.get(1);
		assertNotNull(reply2);
		assertEquals(new Integer(2), reply2.getHeaders().getSequenceSize());
		assertEquals(new Integer(2), reply2.getHeaders().getSequenceNumber());
		assertEquals(message.getHeaders().getId(), reply2.getHeaders().getCorrelationId());
	}

	@Test
	public void testHeaderForMessageReturnValues() throws Exception {
		StringMessage message = new StringMessage("foo.bar");
		MethodInvokingSplitter splitter = this.getSplitter("messageToMessageList");
		List<Message<?>> replies = splitter.split(message);
		Message<?> reply1 = replies.get(0);
		assertNotNull(reply1);
		assertEquals(new Integer(2), reply1.getHeaders().getSequenceSize());
		assertEquals(new Integer(1), reply1.getHeaders().getSequenceNumber());
		assertEquals(message.getHeaders().getId(), reply1.getHeaders().getCorrelationId());
		Message<?> reply2 = replies.get(1);
		assertNotNull(reply2);
		assertEquals(new Integer(2), reply2.getHeaders().getSequenceSize());
		assertEquals(new Integer(2), reply2.getHeaders().getSequenceNumber());
		assertEquals(message.getHeaders().getId(), reply2.getHeaders().getCorrelationId());
	}

	@Test
	public void splitMessageHeader() throws Exception {
		Message<String> message = MessageBuilder.withPayload("ignored")
				.setHeader("testHeader", "foo.bar").build();
		MethodInvokingSplitter splitter = this.getSplitter("splitHeader");
		List<Message<?>> replies = splitter.split(message);
		Message<?> reply1 = replies.get(0);
		assertNotNull(reply1);
		assertEquals("foo", reply1.getPayload());
		Message<?> reply2 = replies.get(1);
		assertNotNull(reply2);
		assertEquals("bar", reply2.getPayload());
	}

	@Test
	public void splitPayloadAndHeader() throws Exception {
		Message<String> message = MessageBuilder.withPayload("a.b")
				.setHeader("testHeader", "c.d").build();
		Method splittingMethod = this.testBean.getClass().getMethod("splitPayloadAndHeader", String.class, String.class);
		MethodInvokingSplitter splitter = new MethodInvokingSplitter(testBean, splittingMethod);
		List<Message<?>> replies = splitter.split(message);
		Message<?> reply1 = replies.get(0);
		assertNotNull(reply1);
		assertEquals("a", reply1.getPayload());
		Message<?> reply2 = replies.get(1);
		assertNotNull(reply2);
		assertEquals("b", reply2.getPayload());
		Message<?> reply3 = replies.get(2);
		assertNotNull(reply3);
		assertEquals("c", reply3.getPayload());
		Message<?> reply4 = replies.get(3);
		assertNotNull(reply4);
		assertEquals("d", reply4.getPayload());
	}


	private MethodInvokingSplitter getSplitter(String methodName) throws Exception {
		Class<?> paramType = methodName.startsWith("message") ? Message.class : String.class;
		Method splittingMethod = this.testBean.getClass().getMethod(methodName, paramType);
		return new MethodInvokingSplitter(testBean, splittingMethod);
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

		public String[] splitHeader(@Header("testHeader") String input) {
			return input.split("\\.");
		}

		public List<String> splitPayloadAndHeader(String payload, @Header("testHeader") String header) {
			String regex = "\\.";
			List<String> results = new ArrayList<String>();
			for (String s: payload.split(regex)) {
				results.add(s);
			}
			for (String s: header.split(regex)) {
				results.add(s);
			}
			return results;
		}
	}

}
