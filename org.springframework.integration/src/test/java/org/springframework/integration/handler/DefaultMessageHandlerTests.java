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

package org.springframework.integration.handler;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Properties;

import org.junit.Test;

import org.springframework.integration.annotation.Handler;
import org.springframework.integration.handler.annotation.Header;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageBuilder;
import org.springframework.integration.message.MessagingException;
import org.springframework.integration.message.StringMessage;

/**
 * @author Mark Fisher
 */
public class DefaultMessageHandlerTests {

	@Test
	public void payloadAsMethodParameterAndObjectAsReturnValue() {
		DefaultMessageHandler handler = new DefaultMessageHandler();
		handler.setObject(new TestHandler());
		handler.setMethodName("acceptPayloadAndReturnObject");
		handler.afterPropertiesSet();
		Message<?> result = handler.handle(new StringMessage("testing"));
		assertEquals("testing-1", result.getPayload());
	}

	@Test
	public void payloadAsMethodParameterAndMessageAsReturnValue() {
		DefaultMessageHandler handler = new DefaultMessageHandler();
		handler.setObject(new TestHandler());
		handler.setMethodName("acceptPayloadAndReturnMessage");
		handler.afterPropertiesSet();
		Message<?> result = handler.handle(new StringMessage("testing"));
		assertEquals("testing-2", result.getPayload());
	}

	@Test
	public void messageAsMethodParameterAndObjectAsReturnValue() {
		DefaultMessageHandler handler = new DefaultMessageHandler();
		handler.setObject(new TestHandler());
		handler.setMethodName("acceptMessageAndReturnObject");
		handler.afterPropertiesSet();
		Message<?> result = handler.handle(new StringMessage("testing"));
		assertEquals("testing-3", result.getPayload());
	}

	@Test
	public void messageAsMethodParameterAndMessageAsReturnValue() {
		DefaultMessageHandler handler = new DefaultMessageHandler();
		handler.setObject(new TestHandler());
		handler.setMethodName("acceptMessageAndReturnMessage");
		handler.afterPropertiesSet();
		Message<?> result = handler.handle(new StringMessage("testing"));
		assertEquals("testing-4", result.getPayload());
	}

	@Test
	public void messageSubclassAsMethodParameterAndMessageAsReturnValue() {
		DefaultMessageHandler handler = new DefaultMessageHandler();
		handler.setObject(new TestHandler());
		handler.setMethodName("acceptMessageSubclassAndReturnMessage");
		handler.afterPropertiesSet();
		Message<?> result = handler.handle(new StringMessage("testing"));
		assertEquals("testing-5", result.getPayload());
	}

	@Test
	public void messageSubclassAsMethodParameterAndMessageSubclassAsReturnValue() {
		DefaultMessageHandler handler = new DefaultMessageHandler();
		handler.setObject(new TestHandler());
		handler.setMethodName("acceptMessageSubclassAndReturnMessageSubclass");
		handler.afterPropertiesSet();
		Message<?> result = handler.handle(new StringMessage("testing"));
		assertEquals("testing-6", result.getPayload());
	}

	@Test
	public void payloadAndHeaderAnnotationMethodParametersAndObjectAsReturnValue() {
		DefaultMessageHandler handler = new DefaultMessageHandler();
		handler.setObject(new TestHandler());
		handler.setMethodName("acceptPayloadAndHeaderAndReturnObject");
		handler.afterPropertiesSet();
		Message<?> request = MessageBuilder.fromPayload("testing")
				.setHeader("number", new Integer(123)).build();
		Message<?> result = handler.handle(request);
		assertEquals("testing-123", result.getPayload());
	}

	@Test
	public void messageOnlyWithAnnotatedMethod() throws Exception {
		AnnotatedTestHandler handler = new AnnotatedTestHandler();
		Method method = handler.getClass().getMethod("messageOnly", Message.class);
		DefaultMessageHandler adapter = new DefaultMessageHandler();
		adapter.setObject(handler);
		adapter.setMethod(method);
		Message<?> result = adapter.handle(new StringMessage("foo"));
		assertEquals("foo", result.getPayload());
	}

	@Test
	public void payloadWithAnnotatedMethod() throws Exception {
		AnnotatedTestHandler handler = new AnnotatedTestHandler();
		Method method = handler.getClass().getMethod("integerMethod", Integer.class);
		DefaultMessageHandler adapter = new DefaultMessageHandler();
		adapter.setObject(handler);
		adapter.setMethod(method);
		Message<?> result = adapter.handle(new GenericMessage<Integer>(new Integer(123)));
		assertEquals(new Integer(123), result.getPayload());
	}

	@Test
	public void convertedPayloadWithAnnotatedMethod() throws Exception {
		AnnotatedTestHandler handler = new AnnotatedTestHandler();
		Method method = handler.getClass().getMethod("integerMethod", Integer.class);
		DefaultMessageHandler adapter = new DefaultMessageHandler();
		adapter.setObject(handler);
		adapter.setMethod(method);
		Message<?> result = adapter.handle(new StringMessage("456"));
		assertEquals(new Integer(456), result.getPayload());
	}

	@Test(expected = MessagingException.class)
	public void conversionFailureWithAnnotatedMethod() throws Exception {
		AnnotatedTestHandler handler = new AnnotatedTestHandler();
		Method method = handler.getClass().getMethod("integerMethod", Integer.class);
		DefaultMessageHandler adapter = new DefaultMessageHandler();
		adapter.setObject(handler);
		adapter.setMethod(method);
		Message<?> result = adapter.handle(new StringMessage("foo"));
		assertEquals(new Integer(123), result.getPayload());
	}

	@Test
	public void messageAndHeaderWithAnnotatedMethod() throws Exception {
		AnnotatedTestHandler handler = new AnnotatedTestHandler();
		Method method = handler.getClass().getMethod("messageAndHeader", Message.class, Integer.class);
		DefaultMessageHandler adapter = new DefaultMessageHandler();
		adapter.setObject(handler);
		adapter.setMethod(method);
		Message<String> message = MessageBuilder.fromPayload("foo")
				.setHeader("number", 42).build();
		Message<?> result = adapter.handle(message);
		assertEquals("foo-42", result.getPayload());
	}

	@Test
	public void multipleHeadersWithAnnotatedMethod() throws Exception {
		AnnotatedTestHandler handler = new AnnotatedTestHandler();
		Method method = handler.getClass().getMethod("twoHeaders", String.class, Integer.class);
		DefaultMessageHandler adapter = new DefaultMessageHandler();
		adapter.setObject(handler);
		adapter.setMethod(method);
		Message<String> message = MessageBuilder.fromPayload("foo")
				.setHeader("prop", "bar")
				.setHeader("number", 42).build();
		Message<?> result = adapter.handle(message);
		assertEquals("bar-42", result.getPayload());
	}


	private static class TestHandler {

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

	private static class AnnotatedTestHandler {

		@Handler
		public String messageOnly(Message<?> message) {
			return (String) message.getPayload();
		}

		@Handler
		public String messageAndHeader(Message<?> message, @Header("number") Integer num) {
			return (String) message.getPayload() + "-" + num.toString();
		}

		@Handler
		public String twoHeaders(@Header String prop, @Header("number") Integer num) {
			return prop + "-" + num.toString();
		}

		@Handler
		public Integer optionalHeader(@Header(required=false) Integer num) {
			return num;
		}

		@Handler
		public Integer requiredHeader(@Header(value="num", required=true) Integer num) {
			return num;
		}

		@Handler
		public String optionalAndRequiredHeader(@Header(required=false) String prop, @Header(value="num", required=true) Integer num) {
			return prop + num;
		}

		@Handler
		public Properties propertiesMethod(Properties properties) {
			return properties;
		}

		@Handler
		@SuppressWarnings("unchecked")
		public Map mapMethod(Map map) {
			return map;
		}

		@Handler
		public Integer integerMethod(Integer i) {
			return i;
		}
	}

}
