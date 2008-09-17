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

package org.springframework.integration.message;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Properties;

import org.junit.Test;

import org.springframework.integration.annotation.Header;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageBuilder;
import org.springframework.integration.message.MessageMappingMethodInvoker;
import org.springframework.integration.message.StringMessage;

/**
 * @author Mark Fisher
 */
public class MessageMappingMethodInvokerTests {

	@Test
	public void payloadAsMethodParameterAndObjectAsReturnValue() {
		MessageMappingMethodInvoker invoker = new MessageMappingMethodInvoker(
				new TestBean(), "acceptPayloadAndReturnObject");
		Object result = invoker.invokeMethod(new StringMessage("testing"));
		assertEquals("testing-1", result);
	}

	@Test
	public void payloadAsMethodParameterAndMessageAsReturnValue() {
		MessageMappingMethodInvoker invoker = new MessageMappingMethodInvoker(
				new TestBean(), "acceptPayloadAndReturnMessage");
		Message<?> result = (Message<?>) invoker.invokeMethod(new StringMessage("testing"));
		assertEquals("testing-2", result.getPayload());
	}

	@Test
	public void messageAsMethodParameterAndObjectAsReturnValue() {
		MessageMappingMethodInvoker invoker = new MessageMappingMethodInvoker(
				new TestBean(), "acceptMessageAndReturnObject");
		Object result = invoker.invokeMethod(new StringMessage("testing"));
		assertEquals("testing-3", result);
	}

	@Test
	public void messageAsMethodParameterAndMessageAsReturnValue() {
		MessageMappingMethodInvoker invoker = new MessageMappingMethodInvoker(
				new TestBean(), "acceptMessageAndReturnMessage");
		Message<?> result = (Message<?>) invoker.invokeMethod(new StringMessage("testing"));
		assertEquals("testing-4", result.getPayload());
	}

	@Test
	public void messageSubclassAsMethodParameterAndMessageAsReturnValue() {
		MessageMappingMethodInvoker invoker = new MessageMappingMethodInvoker(
				new TestBean(), "acceptMessageSubclassAndReturnMessage");
		Message<?> result = (Message<?>) invoker.invokeMethod(new StringMessage("testing"));
		assertEquals("testing-5", result.getPayload());
	}

	@Test
	public void messageSubclassAsMethodParameterAndMessageSubclassAsReturnValue() {
		MessageMappingMethodInvoker invoker = new MessageMappingMethodInvoker(
				new TestBean(), "acceptMessageSubclassAndReturnMessageSubclass");
		Message<?> result = (Message<?>) invoker.invokeMethod(new StringMessage("testing"));
		assertEquals("testing-6", result.getPayload());
	}

	@Test
	public void payloadAndHeaderAnnotationMethodParametersAndObjectAsReturnValue() {
		MessageMappingMethodInvoker invoker = new MessageMappingMethodInvoker(
				new TestBean(), "acceptPayloadAndHeaderAndReturnObject");
		Message<?> request = MessageBuilder.withPayload("testing")
				.setHeader("number", new Integer(123)).build();
		Object result = invoker.invokeMethod(request);
		assertEquals("testing-123", result);
	}

	@Test
	public void messageOnlyWithAnnotatedMethod() throws Exception {
		AnnotatedTestService service = new AnnotatedTestService();
		Method method = service.getClass().getMethod("messageOnly", Message.class);
		MessageMappingMethodInvoker invoker = new MessageMappingMethodInvoker(service, method);
		Object result = invoker.invokeMethod(new StringMessage("foo"));
		assertEquals("foo", result);
	}

	@Test
	public void payloadWithAnnotatedMethod() throws Exception {
		AnnotatedTestService service = new AnnotatedTestService();
		Method method = service.getClass().getMethod("integerMethod", Integer.class);
		MessageMappingMethodInvoker invoker = new MessageMappingMethodInvoker(service, method);
		Object result = invoker.invokeMethod(new GenericMessage<Integer>(new Integer(123)));
		assertEquals(new Integer(123), result);
	}

	@Test
	public void convertedPayloadWithAnnotatedMethod() throws Exception {
		AnnotatedTestService service = new AnnotatedTestService();
		Method method = service.getClass().getMethod("integerMethod", Integer.class);
		MessageMappingMethodInvoker invoker = new MessageMappingMethodInvoker(service, method);
		Object result = invoker.invokeMethod(new StringMessage("456"));
		assertEquals(new Integer(456), result);
	}

	@Test(expected = IllegalArgumentException.class)
	public void conversionFailureWithAnnotatedMethod() throws Exception {
		AnnotatedTestService service = new AnnotatedTestService();
		Method method = service.getClass().getMethod("integerMethod", Integer.class);
		MessageMappingMethodInvoker invoker = new MessageMappingMethodInvoker(service, method);
		Object result = invoker.invokeMethod(new StringMessage("foo"));
		assertEquals(new Integer(123), result);
	}

	@Test
	public void messageAndHeaderWithAnnotatedMethod() throws Exception {
		AnnotatedTestService service = new AnnotatedTestService();
		Method method = service.getClass().getMethod("messageAndHeader", Message.class, Integer.class);
		MessageMappingMethodInvoker invoker = new MessageMappingMethodInvoker(service, method);
		Message<String> message = MessageBuilder.withPayload("foo")
				.setHeader("number", 42).build();
		Object result = invoker.invokeMethod(message);
		assertEquals("foo-42", result);
	}

	@Test
	public void multipleHeadersWithAnnotatedMethod() throws Exception {
		AnnotatedTestService service = new AnnotatedTestService();
		Method method = service.getClass().getMethod("twoHeaders", String.class, Integer.class);
		MessageMappingMethodInvoker invoker = new MessageMappingMethodInvoker(service, method);
		Message<String> message = MessageBuilder.withPayload("foo")
				.setHeader("prop", "bar")
				.setHeader("number", 42).build();
		Object result = invoker.invokeMethod(message);
		assertEquals("bar-42", result);
	}


	private static class TestBean {

		public String acceptPayloadAndReturnObject(String s) {
			return s + "-1";
		}

		public Message<?> acceptPayloadAndReturnMessage(String s) {
			return new StringMessage(s + "-2");
		}

		public String acceptMessageAndReturnObject(Message<?> m) {
			return m.getPayload() + "-3";
		}

		public Message<?> acceptMessageAndReturnMessage(Message<?> m) {
			return new StringMessage(m.getPayload() + "-4");
		}

		public Message<?> acceptMessageSubclassAndReturnMessage(StringMessage m) {
			return new StringMessage(m.getPayload() + "-5");
		}

		public StringMessage acceptMessageSubclassAndReturnMessageSubclass(StringMessage m) {
			return new StringMessage(m.getPayload() + "-6");
		}

		public String acceptPayloadAndHeaderAndReturnObject(String s, @Header("number") Integer n) {
			return s + "-" + n;
		}
	}

	private static class AnnotatedTestService {

		public String messageOnly(Message<?> message) {
			return (String) message.getPayload();
		}

		public String messageAndHeader(Message<?> message, @Header("number") Integer num) {
			return (String) message.getPayload() + "-" + num.toString();
		}

		public String twoHeaders(@Header String prop, @Header("number") Integer num) {
			return prop + "-" + num.toString();
		}

		public Integer optionalHeader(@Header(required=false) Integer num) {
			return num;
		}

		public Integer requiredHeader(@Header(value="num", required=true) Integer num) {
			return num;
		}

		public String optionalAndRequiredHeader(@Header(required=false) String prop, @Header(value="num", required=true) Integer num) {
			return prop + num;
		}

		public Properties propertiesMethod(Properties properties) {
			return properties;
		}

		@SuppressWarnings("unchecked")
		public Map mapMethod(Map map) {
			return map;
		}

		public Integer integerMethod(Integer i) {
			return i;
		}
	}

}
