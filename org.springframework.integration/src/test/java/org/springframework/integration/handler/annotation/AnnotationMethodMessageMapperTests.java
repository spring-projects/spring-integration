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

package org.springframework.integration.handler.annotation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.junit.Test;

import org.springframework.integration.annotation.Handler;
import org.springframework.integration.handler.DefaultMessageHandlerAdapter;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageBuilder;
import org.springframework.integration.message.MessageHandlingException;
import org.springframework.integration.message.MessagingException;
import org.springframework.integration.message.StringMessage;

/**
 * @author Mark Fisher
 */
public class AnnotationMethodMessageMapperTests {

	@Test
	public void testOptionalAttribute() throws Exception {
		Method method = TestHandler.class.getMethod("optionalAttribute", Integer.class);
		AnnotationMethodMessageMapper mapper = new AnnotationMethodMessageMapper(method);
		Object[] args = (Object[]) mapper.mapMessage(new StringMessage("foo"));
		assertEquals(1, args.length);
		assertNull(args[0]);
	}

	@Test(expected=MessageHandlingException.class)
	public void testRequiredAttributeNotProvided() throws Exception {
		Method method = TestHandler.class.getMethod("requiredAttribute", Integer.class);
		AnnotationMethodMessageMapper mapper = new AnnotationMethodMessageMapper(method);
		mapper.mapMessage(new StringMessage("foo"));
	}

	@Test
	public void testRequiredAttributeProvided() throws Exception {
		Method method = TestHandler.class.getMethod("requiredAttribute", Integer.class);
		AnnotationMethodMessageMapper mapper = new AnnotationMethodMessageMapper(method);
		Message<String> message = MessageBuilder.fromPayload("foo")
				.setHeader("num", new Integer(123)).build(); 
		Object[] args = (Object[]) mapper.mapMessage(message);
		assertEquals(1, args.length);
		assertEquals(new Integer(123), args[0]);
	}

	@Test
	public void testOptionalProperty() throws Exception {
		Method method = TestHandler.class.getMethod("optionalProperty", String.class);
		AnnotationMethodMessageMapper mapper = new AnnotationMethodMessageMapper(method);
		Object[] args = (Object[]) mapper.mapMessage(new StringMessage("foo"));
		assertEquals(1, args.length);
		assertNull(args[0]);
	}

	@Test(expected=MessageHandlingException.class)
	public void testRequiredPropertyNotProvided() throws Exception {
		Method method = TestHandler.class.getMethod("requiredProperty", String.class);
		AnnotationMethodMessageMapper mapper = new AnnotationMethodMessageMapper(method);
		mapper.mapMessage(new StringMessage("foo"));
	}

	@Test
	public void testRequiredPropertyProvided() throws Exception {
		Method method = TestHandler.class.getMethod("requiredProperty", String.class);
		AnnotationMethodMessageMapper mapper = new AnnotationMethodMessageMapper(method);
		Message<String> message = MessageBuilder.fromPayload("foo")
				.setHeader("prop", "bar").build();
		Object[] args = (Object[]) mapper.mapMessage(message);
		assertEquals(1, args.length);
		assertEquals("bar", args[0]);
	}

	@Test
	public void testPropertiesMethodWithNonPropertiesPayload() throws Exception {
		Method method = TestHandler.class.getMethod("propertiesMethod", Properties.class);
		AnnotationMethodMessageMapper mapper = new AnnotationMethodMessageMapper(method);
		Message<String> message = MessageBuilder.fromPayload("test")
				.setHeader("prop1", "foo").setHeader("prop2", "bar").build();
		Object[] args = (Object[]) mapper.mapMessage(message);
		Properties result = (Properties) args[0];
		assertEquals(2, result.size());
		assertEquals("foo", result.getProperty("prop1"));
		assertEquals("bar", result.getProperty("prop2"));
	}

	@Test
	public void testPropertiesMethodWithPropertiesPayload() throws Exception {
		Method method = TestHandler.class.getMethod("propertiesMethod", Properties.class);
		AnnotationMethodMessageMapper mapper = new AnnotationMethodMessageMapper(method);
		Properties payload = new Properties();
		payload.setProperty("prop1", "foo");
		payload.setProperty("prop2", "bar");
		Message<Properties> message = MessageBuilder.fromPayload(payload)
				.setHeader("prop1", "not").setHeader("prop2", "these").build();
		Object[] args = (Object[]) mapper.mapMessage(message);
		Properties result = (Properties) args[0];
		assertEquals(2, result.size());
		assertEquals("foo", result.getProperty("prop1"));
		assertEquals("bar", result.getProperty("prop2"));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testMapMethodWithNonMapPayload() throws Exception {
		Method method = TestHandler.class.getMethod("mapMethod", Map.class);
		AnnotationMethodMessageMapper mapper = new AnnotationMethodMessageMapper(method);
		Message<String> message = MessageBuilder.fromPayload("test")
				.setHeader("attrib1", new Integer(123))
				.setHeader("attrib2", new Integer(456)).build();
		Object[] args = (Object[]) mapper.mapMessage(message);
		Map<String, Object> result = (Map<String, Object>) args[0];
		assertEquals(new Integer(123), result.get("attrib1"));
		assertEquals(new Integer(456), result.get("attrib2"));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testMapMethodWithMapPayload() throws Exception {
		Method method = TestHandler.class.getMethod("mapMethod", Map.class);
		AnnotationMethodMessageMapper mapper = new AnnotationMethodMessageMapper(method);
		Map<String, Integer> payload = new HashMap<String, Integer>();
		payload.put("attrib1", new Integer(123));
		payload.put("attrib2", new Integer(456));
		Message<Map<String, Integer>> message = MessageBuilder.fromPayload(payload)
				.setHeader("attrib1", new Integer(123))
				.setHeader("attrib2", new Integer(456)).build();
		Object[] args = (Object[]) mapper.mapMessage(message);
		Map<String, Integer> result = (Map<String, Integer>) args[0];
		assertEquals(2, result.size());
		assertEquals(new Integer(123), result.get("attrib1"));
		assertEquals(new Integer(456), result.get("attrib2"));
	}

	@Test
	public void testMessageOnlyWithAdapter() throws Exception {
		TestHandler handler = new TestHandler();
		Method method = handler.getClass().getMethod("messageOnly", Message.class);
		AnnotationMethodMessageMapper mapper = new AnnotationMethodMessageMapper(method);
		DefaultMessageHandlerAdapter adapter = new DefaultMessageHandlerAdapter();
		adapter.setObject(handler);
		adapter.setMethod(method);
		adapter.setMessageMapper(mapper);
		Message<?> result = adapter.handle(new StringMessage("foo"));
		assertEquals("foo", result.getPayload());
	}

	@Test
	public void testPayloadWithAdapter() throws Exception {
		TestHandler handler = new TestHandler();
		Method method = handler.getClass().getMethod("integerMethod", Integer.class);
		AnnotationMethodMessageMapper mapper = new AnnotationMethodMessageMapper(method);
		DefaultMessageHandlerAdapter adapter = new DefaultMessageHandlerAdapter();
		adapter.setObject(handler);
		adapter.setMethod(method);
		adapter.setMessageMapper(mapper);
		Message<?> result = adapter.handle(new GenericMessage<Integer>(new Integer(123)));
		assertEquals(new Integer(123), result.getPayload());
	}

	@Test
	public void testConvertedPayloadWithAdapter() throws Exception {
		TestHandler handler = new TestHandler();
		Method method = handler.getClass().getMethod("integerMethod", Integer.class);
		AnnotationMethodMessageMapper mapper = new AnnotationMethodMessageMapper(method);
		DefaultMessageHandlerAdapter adapter = new DefaultMessageHandlerAdapter();
		adapter.setObject(handler);
		adapter.setMethod(method);
		adapter.setMessageMapper(mapper);
		Message<?> result = adapter.handle(new StringMessage("456"));
		assertEquals(new Integer(456), result.getPayload());
	}

	@Test(expected=MessagingException.class)
	public void testConversionFailureWithAdapter() throws Exception {
		TestHandler handler = new TestHandler();
		Method method = handler.getClass().getMethod("integerMethod", Integer.class);
		AnnotationMethodMessageMapper mapper = new AnnotationMethodMessageMapper(method);
		DefaultMessageHandlerAdapter adapter = new DefaultMessageHandlerAdapter();
		adapter.setObject(handler);
		adapter.setMethod(method);
		adapter.setMessageMapper(mapper);
		Message<?> result = adapter.handle(new StringMessage("foo"));
		assertEquals(new Integer(123), result.getPayload());
	}

	@Test
	public void testMessageAndHeaderWithAdapter() throws Exception {
		TestHandler handler = new TestHandler();
		Method method = handler.getClass().getMethod("messageAndAttribute", Message.class, Integer.class);
		AnnotationMethodMessageMapper mapper = new AnnotationMethodMessageMapper(method);
		DefaultMessageHandlerAdapter adapter = new DefaultMessageHandlerAdapter();
		adapter.setObject(handler);
		adapter.setMethod(method);
		adapter.setMessageMapper(mapper);
		Message<String> message = MessageBuilder.fromPayload("foo")
				.setHeader("number", 42).build();
		Message<?> result = adapter.handle(message);
		assertEquals("foo-42", result.getPayload());
	}

	@Test
	public void testHeaderAndPropertyWithAdapter() throws Exception {
		TestHandler handler = new TestHandler();
		Method method = handler.getClass().getMethod("propertyAndAttribute", String.class, Integer.class);
		AnnotationMethodMessageMapper mapper = new AnnotationMethodMessageMapper(method);
		DefaultMessageHandlerAdapter adapter = new DefaultMessageHandlerAdapter();
		adapter.setObject(handler);
		adapter.setMethod(method);
		adapter.setMessageMapper(mapper);
		Message<String> message = MessageBuilder.fromPayload("foo")
				.setHeader("prop", "bar")
				.setHeader("number", 42).build();
		Message<?> result = adapter.handle(message);
		assertEquals("bar-42", result.getPayload());
	}


	private static class TestHandler {

		@Handler
		public String messageOnly(Message<?> message) {
			return (String) message.getPayload();
		}

		@Handler
		public String messageAndAttribute(Message<?> message, @HeaderAttribute("number") Integer num) {
			return (String) message.getPayload() + "-" + num.toString();
		}

		@Handler
		public String propertyAndAttribute(@HeaderProperty String prop, @HeaderAttribute("number") Integer num) {
			return prop + "-" + num.toString();
		}

		@Handler
		public Integer optionalAttribute(@HeaderAttribute(required=false) Integer num) {
			return num;
		}

		@Handler
		public Integer requiredAttribute(@HeaderAttribute(value="num", required=true) Integer num) {
			return num;
		}

		@Handler
		public String optionalProperty(@HeaderProperty(required=false) String prop) {
			return prop;
		}

		@Handler
		public String requiredProperty(@HeaderProperty(value="prop", required=true) String prop) {
			return prop;
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
