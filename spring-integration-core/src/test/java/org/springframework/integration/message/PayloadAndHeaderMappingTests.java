/*
 * Copyright 2002-2009 the original author or authors.
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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.junit.Before;
import org.junit.Test;

import org.springframework.messaging.Message;
import org.springframework.integration.annotation.Header;
import org.springframework.integration.annotation.Headers;
import org.springframework.integration.annotation.Payload;
import org.springframework.messaging.MessageHandler;
import org.springframework.integration.handler.ServiceActivatingHandler;
import org.springframework.integration.support.MessageBuilder;

/**
 * @author Mark Fisher
 * @since 1.0.3
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class PayloadAndHeaderMappingTests {

	private TestBean bean;


	@Before
	public void setup() {
		bean = new TestBean();
	}


	@Test
	public void headerPropertiesAndObjectPayload() throws Exception {
		MessageHandler handler = this.getHandler("headerPropertiesAndObjectPayload", Properties.class, Object.class);
		Object payload = "test";
		Map<String, Object> headers = new HashMap<String, Object>();
		headers.put("foo", "1");
		headers.put("bar", "2");
		headers.put("baz", 99);
		Message<?> message = MessageBuilder.withPayload(payload).copyHeaders(headers).build();
		handler.handleMessage(message);
		assertEquals(payload, bean.lastPayload);
		assertTrue(bean.lastHeaders.containsKey("foo"));
		assertTrue(bean.lastHeaders.containsKey("bar"));
		//	assertFalse(bean.lastHeaders.containsKey("baz"));
	}

	@Test
	public void stringPayloadAndHeaderProperties() throws Exception {
		MessageHandler handler = this.getHandler("stringPayloadAndHeaderProperties", String.class, Properties.class);
		Object payload = "test";
		Map<String, Object> headers = new HashMap<String, Object>();
		headers.put("foo", "1");
		headers.put("bar", "2");
		headers.put("baz", 99);
		Message<?> message = MessageBuilder.withPayload(payload).copyHeaders(headers).build();
		handler.handleMessage(message);
		assertEquals(payload, bean.lastPayload);
		assertTrue(bean.lastHeaders.containsKey("foo"));
		assertTrue(bean.lastHeaders.containsKey("bar"));
		//assertFalse(bean.lastHeaders.containsKey("baz"));
	}

	@Test
	public void headerMapAndObjectPayload() throws Exception {
		MessageHandler handler = this.getHandler("headerMapAndObjectPayload", Map.class, Object.class);
		Object payload = "test";
		Map<String, Object> headers = new HashMap<String, Object>();
		headers.put("foo", "1");
		headers.put("bar", "2");
		headers.put("baz", 99);
		Message<?> message = MessageBuilder.withPayload(payload).copyHeaders(headers).build();
		handler.handleMessage(message);
		assertEquals(payload, bean.lastPayload);
		assertTrue(bean.lastHeaders.containsKey("foo"));
		assertTrue(bean.lastHeaders.containsKey("bar"));
		assertTrue(bean.lastHeaders.containsKey("baz"));
	}

	@Test
	public void objectPayloadAndHeaderMap() throws Exception {
		MessageHandler handler = this.getHandler("objectPayloadAndHeaderMap", Object.class, Map.class);
		Object payload = "test";
		Map<String, Object> headers = new HashMap<String, Object>();
		headers.put("foo", "1");
		headers.put("bar", "2");
		headers.put("baz", 99);
		Message<?> message = MessageBuilder.withPayload(payload).copyHeaders(headers).build();
		handler.handleMessage(message);
		assertEquals(payload, bean.lastPayload);
		assertTrue(bean.lastHeaders.containsKey("foo"));
		assertTrue(bean.lastHeaders.containsKey("bar"));
		assertTrue(bean.lastHeaders.containsKey("baz"));
	}

	@Test
	public void payloadMapAndHeaderString() throws Exception {
		MessageHandler handler = this.getHandler("payloadMapAndHeaderString", Map.class, String.class);
		Map<String, Object> payload = new HashMap<String, Object>();
		payload.put("abc", 1);
		payload.put("xyz", "test");
		Map<String, Object> headers = new HashMap<String, Object>();
		headers.put("foo", "1");
		headers.put("bar", "2");
		Message<?> message = MessageBuilder.withPayload(payload).copyHeaders(headers).build();
		handler.handleMessage(message);
		assertEquals(payload, bean.lastPayload);
		assertTrue(bean.lastHeaders.containsKey("foo"));
		assertFalse(bean.lastHeaders.containsKey("bar"));
	}

	@Test
	public void payloadMapAndHeaderStrings() throws Exception {
		MessageHandler handler = this.getHandler("payloadMapAndHeaderStrings", Map.class, String.class, String.class);
		Map<String, Object> payload = new HashMap<String, Object>();
		payload.put("abc", 1);
		payload.put("xyz", "test");
		Map<String, Object> headers = new HashMap<String, Object>();
		headers.put("foo", "1");
		headers.put("bar", "2");
		headers.put("baz", "3");
		Message<?> message = MessageBuilder.withPayload(payload).copyHeaders(headers).build();
		handler.handleMessage(message);
		assertEquals(payload, bean.lastPayload);
		assertTrue(bean.lastHeaders.containsKey("foo"));
		assertTrue(bean.lastHeaders.containsKey("bar"));
		assertFalse(bean.lastHeaders.containsKey("baz"));
	}

	@Test
	public void objectPayloadHeaderMapAndStringHeaders() throws Exception {
		MessageHandler handler = this.getHandler("objectPayloadHeaderMapAndStringHeaders",
				String.class, Map.class, String.class, Object.class);
		Object payload = "test";
		Map<String, Object> headers = new HashMap<String, Object>();
		headers.put("foo", "1");
		headers.put("bar", "2");
		headers.put("baz", 99);
		Message<?> message = MessageBuilder.withPayload(payload).copyHeaders(headers).build();
		handler.handleMessage(message);
		assertEquals(payload, bean.lastPayload);
		assertTrue(bean.lastHeaders.containsKey("foo"));
		assertEquals("1", bean.lastHeaders.get("foo"));
		assertTrue(bean.lastHeaders.containsKey("bar"));
		assertEquals("2", bean.lastHeaders.get("bar"));
		assertTrue(bean.lastHeaders.containsKey("baz"));
		assertTrue(bean.lastHeaders.containsKey("foo2"));
		assertEquals("1", bean.lastHeaders.get("foo2"));
		assertTrue(bean.lastHeaders.containsKey("bar2"));
		assertEquals("2", bean.lastHeaders.get("bar2"));
	}

	@Test
	public void payloadMapAndHeaderMap() throws Exception {
		MessageHandler handler = this.getHandler("payloadMapAndHeaderMap", Map.class, Map.class);
		Map<String, Object> payload = new HashMap<String, Object>();
		payload.put("abc", 1);
		payload.put("xyz", "test");
		Map<String, Object> headers = new HashMap<String, Object>();
		headers.put("foo", "1");
		headers.put("bar", "2");
		headers.put("baz", 99);
		Message<?> message = MessageBuilder.withPayload(payload).copyHeaders(headers).build();
		handler.handleMessage(message);
		assertEquals(payload, bean.lastPayload);
		assertTrue(bean.lastHeaders.containsKey("foo"));
		assertTrue(bean.lastHeaders.containsKey("bar"));
		assertTrue(bean.lastHeaders.containsKey("baz"));
	}

	@Test
	public void headerMapAndPayloadMap() throws Exception {
		MessageHandler handler = this.getHandler("headerMapAndPayloadMap", Map.class, Map.class);
		Map<String, Object> payload = new HashMap<String, Object>();
		payload.put("abc", 1);
		payload.put("xyz", "test");
		Map<String, Object> headers = new HashMap<String, Object>();
		headers.put("foo", "1");
		headers.put("bar", "2");
		headers.put("baz", 99);
		Message<?> message = MessageBuilder.withPayload(payload).copyHeaders(headers).build();
		handler.handleMessage(message);
		assertEquals(payload, bean.lastPayload);
		assertTrue(bean.lastHeaders.containsKey("foo"));
		assertTrue(bean.lastHeaders.containsKey("bar"));
		assertTrue(bean.lastHeaders.containsKey("baz"));
	}

	@Test
	public void headerMapOnlyWithStringPayload() throws Exception {
		MessageHandler handler = this.getHandler("headerMapOnly", Map.class);
		String payload = "test";
		Map<String, Object> headers = new HashMap<String, Object>();
		headers.put("foo", "1");
		headers.put("bar", "2");
		headers.put("baz", 99);
		Message<?> message = MessageBuilder.withPayload(payload).copyHeaders(headers).build();
		handler.handleMessage(message);
		assertNull(payload, bean.lastPayload);
		assertTrue(bean.lastHeaders.containsKey("foo"));
		assertTrue(bean.lastHeaders.containsKey("bar"));
		assertTrue(bean.lastHeaders.containsKey("baz"));
	}

	@Test
	public void headerMapOnlyWithMapPayload() throws Exception {
		MessageHandler handler = this.getHandler("headerMapOnly", Map.class);
		Map<String, Object> payload = new HashMap<String, Object>();
		payload.put("abc", 1);
		payload.put("xyz", "test");
		Map<String, Object> headers = new HashMap<String, Object>();
		headers.put("foo", "1");
		headers.put("bar", "2");
		headers.put("baz", 99);
		Message<?> message = MessageBuilder.withPayload(payload).copyHeaders(headers).build();
		handler.handleMessage(message);
		assertNull(bean.lastPayload);
		assertTrue(bean.lastHeaders.containsKey("foo"));
		assertTrue(bean.lastHeaders.containsKey("bar"));
		assertTrue(bean.lastHeaders.containsKey("baz"));
	}

	@Test
	public void mapOnlyNoAnnotationsWithMapPayload() throws Exception {
		MessageHandler handler = this.getHandler("mapOnlyNoAnnotations", Map.class);
		Map<String, Object> payload = new HashMap<String, Object>();
		payload.put("payload", 1);
		Map<String, Object> headers = new HashMap<String, Object>();
		Message<?> message = MessageBuilder.withPayload(payload).copyHeaders(headers).build();
		handler.handleMessage(message);
		assertEquals(payload, bean.lastPayload);
		assertNull(bean.lastHeaders);
	}

	@Test
	public void mapOnlyNoAnnotationsWithStringPayload() throws Exception {
		MessageHandler handler = this.getHandler("mapOnlyNoAnnotations", Map.class);
		String payload = "test";
		Map<String, Object> headers = new HashMap<String, Object>();
		headers.put("foo", "1");
		headers.put("bar", "2");
		headers.put("baz", 99);
		Message<?> message = MessageBuilder.withPayload(payload).copyHeaders(headers).build();
		handler.handleMessage(message);
		assertNull(bean.lastPayload);
		assertTrue(bean.lastHeaders.containsKey("foo"));
		assertTrue(bean.lastHeaders.containsKey("bar"));
		assertTrue(bean.lastHeaders.containsKey("baz"));
	}

	@Test
	public void mapOnlyNoAnnotationsWithIntegerPayload() throws Exception {
		MessageHandler handler = this.getHandler("mapOnlyNoAnnotations", Map.class);
		Integer payload = new Integer(123);
		Map<String, Object> headers = new HashMap<String, Object>();
		headers.put("foo", "1");
		headers.put("bar", "2");
		headers.put("baz", 99);
		Message<?> message = MessageBuilder.withPayload(payload).copyHeaders(headers).build();
		handler.handleMessage(message);
		assertNull(bean.lastPayload);
		assertTrue(bean.lastHeaders.containsKey("foo"));
		assertTrue(bean.lastHeaders.containsKey("bar"));
		assertTrue(bean.lastHeaders.containsKey("baz"));
	}

	@Test
	public void propertiesOnlyNoAnnotationsWithPropertiesPayload() throws Exception {
		MessageHandler handler = this.getHandler("propertiesOnlyNoAnnotations", Properties.class);
		Properties payload = new Properties();
		payload.setProperty("payload", "1");
		Map<String, Object> headers = new HashMap<String, Object>();
		Message<?> message = MessageBuilder.withPayload(payload).copyHeaders(headers).build();
		handler.handleMessage(message);
		assertEquals(payload, bean.lastPayload);
		assertNull(bean.lastHeaders);
	}

	@Test
	public void propertiesOnlyNoAnnotationsWithMapPayload() throws Exception {
		MessageHandler handler = this.getHandler("propertiesOnlyNoAnnotations", Properties.class);
		Map<String, Object> payload = new HashMap<String, Object>();
		payload.put("payload", 1);
		Map<String, Object> headers = new HashMap<String, Object>();
		Message<?> message = MessageBuilder.withPayload(payload).copyHeaders(headers).build();
		handler.handleMessage(message);
		assertEquals(payload, bean.lastPayload);
		assertNull(bean.lastHeaders);
	}

	@Test
	public void propertiesOnlyNoAnnotationsWithStringPayload() throws Exception {
		MessageHandler handler = this.getHandler("propertiesOnlyNoAnnotations", Properties.class);
		String payload = "payload=abc";
		Map<String, Object> headers = new HashMap<String, Object>();
		headers.put("foo", "1");
		headers.put("bar", "2");
		Message<?> message = MessageBuilder.withPayload(payload).copyHeaders(headers).build();
		handler.handleMessage(message);
		assertNull(bean.lastHeaders);
		// String payload should have been converted to Properties
		assertNotNull(bean.lastPayload);
		assertTrue(bean.lastPayload instanceof Properties);
		Properties payloadProps = (Properties) bean.lastPayload;
		assertTrue(payloadProps.containsKey("payload"));
		assertEquals("abc", payloadProps.get("payload"));
	}

	@Test
	public void propertiesOnlyNoAnnotationsWithIntegerPayload() throws Exception {
		MessageHandler handler = this.getHandler("propertiesOnlyNoAnnotations", Properties.class);
		Integer payload = new Integer(123);
		Map<String, Object> headers = new HashMap<String, Object>();
		headers.put("foo", "1");
		headers.put("bar", "2");
		Message<?> message = MessageBuilder.withPayload(payload).copyHeaders(headers).build();
		handler.handleMessage(message);
		assertNull(bean.lastPayload);
		assertTrue(bean.lastHeaders.containsKey("foo"));
		assertTrue(bean.lastHeaders.containsKey("bar"));
	}

	@Test
	public void headerPropertiesOnlyWithStringPayload() throws Exception {
		MessageHandler handler = this.getHandler("headerPropertiesOnly", Properties.class);
		String payload = "test";
		Map<String, Object> headers = new HashMap<String, Object>();
		headers.put("foo", "1");
		headers.put("bar", "2");
		headers.put("baz", 99);
		Message<?> message = MessageBuilder.withPayload(payload).copyHeaders(headers).build();
		handler.handleMessage(message);
		assertNull(bean.lastPayload);
		assertTrue(bean.lastHeaders.containsKey("foo"));
		assertTrue(bean.lastHeaders.containsKey("bar"));
		//assertFalse(bean.lastHeaders.containsKey("baz"));
	}

	@Test
	public void headerPropertiesOnlyWithMapPayload() throws Exception {
		MessageHandler handler = this.getHandler("headerPropertiesOnly", Properties.class);
		Map<String, Object> payload = new HashMap<String, Object>();
		payload.put("abc", 1);
		payload.put("xyz", "test");
		Map<String, Object> headers = new HashMap<String, Object>();
		headers.put("foo", "1");
		headers.put("bar", "2");
		headers.put("baz", 99);
		Message<?> message = MessageBuilder.withPayload(payload).copyHeaders(headers).build();
		handler.handleMessage(message);
		assertNull(bean.lastPayload);
		assertTrue(bean.lastHeaders.containsKey("foo"));
		assertTrue(bean.lastHeaders.containsKey("bar"));
		//assertFalse(bean.lastHeaders.containsKey("baz"));
	}

	@Test
	public void headerPropertiesOnlyWithPropertiesPayload() throws Exception {
		MessageHandler handler = this.getHandler("headerPropertiesOnly", Properties.class);
		Properties payload = new Properties();
		payload.setProperty("abc", "1");
		payload.setProperty("xyz", "2");
		Map<String, Object> headers = new HashMap<String, Object>();
		headers.put("foo", "1");
		headers.put("bar", "2");
		headers.put("baz", 99);
		Message<?> message = MessageBuilder.withPayload(payload).copyHeaders(headers).build();
		handler.handleMessage(message);
		assertNull(bean.lastPayload);
		assertTrue(bean.lastHeaders.containsKey("foo"));
		assertTrue(bean.lastHeaders.containsKey("bar"));
		//assertFalse(bean.lastHeaders.containsKey("baz"));
	}

	@Test
	public void payloadMapAndHeaderProperties() throws Exception {
		MessageHandler handler = this.getHandler("payloadMapAndHeaderProperties", Map.class, Properties.class);
		Map<String, Object> payload = new HashMap<String, Object>();
		payload.put("abc", 1);
		payload.put("xyz", "test");
		Map<String, Object> headers = new HashMap<String, Object>();
		headers.put("foo", "1");
		headers.put("bar", "2");
		headers.put("baz", 99);
		Message<?> message = MessageBuilder.withPayload(payload).copyHeaders(headers).build();
		handler.handleMessage(message);
		assertEquals(payload, bean.lastPayload);
		assertTrue(bean.lastHeaders.containsKey("foo"));
		assertTrue(bean.lastHeaders.containsKey("bar"));
		//assertFalse(bean.lastHeaders.containsKey("baz"));
	}

	@Test
	public void headerPropertiesPayloadMapAndStringHeader() throws Exception {
		MessageHandler handler = this.getHandler("headerPropertiesPayloadMapAndStringHeader",
				Properties.class, Map.class, String.class);
		Map<String, Object> payload = new HashMap<String, Object>();
		payload.put("abc", 1);
		payload.put("xyz", "test");
		Map<String, Object> headers = new HashMap<String, Object>();
		headers.put("foo", "1");
		headers.put("bar", "2");
		headers.put("baz", 99);
		Message<?> message = MessageBuilder.withPayload(payload).copyHeaders(headers).build();
		handler.handleMessage(message);
		assertEquals(payload, bean.lastPayload);
		assertTrue(bean.lastHeaders.containsKey("foo"));
		assertEquals("1", bean.lastHeaders.get("foo"));
		assertTrue(bean.lastHeaders.containsKey("bar"));
		assertTrue(bean.lastHeaders.containsKey("foo2"));
		assertEquals("1", bean.lastHeaders.get("foo2"));
		//assertFalse(bean.lastHeaders.containsKey("baz"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void twoMapsNoAnnotations() throws Exception {
		this.getHandler("twoMapsNoAnnotations", Map.class, Map.class);
	}

	@Test
	public void twoMapsWithAnnotationsWithStringPayload() throws Exception {
		MessageHandler handler = this.getHandler("twoMapsWithAnnotations", Map.class, Map.class);
		Map<String, Object> headers = new HashMap<String, Object>();
		headers.put("foo", "1");
		headers.put("bar", "2");
		Message<?> message = MessageBuilder.withPayload("test").copyHeaders(headers).build();
		handler.handleMessage(message);
		assertNull(bean.lastPayload);
		assertEquals("1", bean.lastHeaders.get("foo"));
		assertEquals("2", bean.lastHeaders.get("bar"));
		assertEquals("1", bean.lastHeaders.get("foo2"));
		assertEquals("2", bean.lastHeaders.get("bar2"));
	}

	@Test
	public void twoMapsWithAnnotationsWithMapPayload() throws Exception {
		MessageHandler handler = this.getHandler("twoMapsWithAnnotations", Map.class, Map.class);
		Map<String, Object> headers = new HashMap<String, Object>();
		headers.put("foo", "1");
		headers.put("bar", "2");
		Map<String, Object> payloadMap = new HashMap<String, Object>();
		payloadMap.put("baz", "99");
		Message<?> message = MessageBuilder.withPayload(payloadMap).copyHeaders(headers).build();
		handler.handleMessage(message);
		assertEquals("1", bean.lastHeaders.get("foo"));
		assertEquals("2", bean.lastHeaders.get("bar"));
		assertEquals("1", bean.lastHeaders.get("foo2"));
		assertEquals("2", bean.lastHeaders.get("bar2"));
		assertEquals(null, bean.lastHeaders.get("baz"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void twoStringsAmbiguousUsingMethodName() throws Exception {
		SingleAmbiguousMethodTestBean bean = new SingleAmbiguousMethodTestBean();
		new ServiceActivatingHandler(bean, "twoStrings");
	}

	@Test(expected = IllegalArgumentException.class)
	public void twoStringsAmbiguousWithoutMethodName() throws Exception {
		SingleAmbiguousMethodTestBean bean = new SingleAmbiguousMethodTestBean();
		new ServiceActivatingHandler(bean);
	}

	@Test(expected = IllegalArgumentException.class)
	public void twoStringsNoAnnotations() throws Exception {
		this.getHandler("twoStringsNoAnnotations", String.class, String.class);
	}

	@Test(expected = IllegalArgumentException.class)
	public void twoMapsNoAnnotationsAndObject() throws Exception {
		this.getHandler("twoMapsNoAnnotationsAndObject", Map.class, Object.class, Map.class);
	}

	@Test
	public void mapAndAnnotatedStringHeaderWithStringPayload() throws Exception {
		MessageHandler handler = this.getHandler(
				"mapAndAnnotatedStringHeaderExpectingMapAsHeaders", Map.class, String.class);
		Map<String, Object> headers = new HashMap<String, Object>();
		headers.put("foo", "1");
		headers.put("bar", "2");
		Message<?> message = MessageBuilder.withPayload("test")
				.copyHeaders(headers).build();
		handler.handleMessage(message);
		assertNull(bean.lastPayload);
		assertEquals("1", bean.lastHeaders.get("foo"));
		assertEquals("2", bean.lastHeaders.get("bar"));
		assertEquals("1", bean.lastHeaders.get("foo2"));
	}

	@Test
	public void mapAndAnnotatedStringHeaderWithMapPayload() throws Exception {
		MessageHandler handler = this.getHandler(
				"mapAndAnnotatedStringHeaderExpectingMapAsPayload", Map.class, String.class);
		Map<String, Object> payload = new HashMap<String, Object>();
		payload.put("test", "0");
		Map<String, Object> headers = new HashMap<String, Object>();
		headers.put("foo", "1");
		headers.put("bar", "2");
		Message<?> message = MessageBuilder.withPayload(payload)
				.copyHeaders(headers).build();
		handler.handleMessage(message);
		assertNotNull(bean.lastPayload);
		assertEquals(payload, bean.lastPayload);
		assertEquals("1", bean.lastHeaders.get("foo"));
		assertNull(bean.lastHeaders.get("bar"));
	}

	@Test
	public void singleStringHeaderOnlyWithStringPayload() throws Exception {
		MessageHandler handler = this.getHandler("singleStringHeaderOnly", String.class);
		Map<String, Object> headers = new HashMap<String, Object>();
		headers.put("foo", "1");
		headers.put("bar", "2");
		Message<?> message = MessageBuilder.withPayload("test")
				.copyHeaders(headers).build();
		handler.handleMessage(message);
		assertNull(bean.lastPayload);
		assertEquals("1", bean.lastHeaders.get("foo"));
		assertNull(bean.lastHeaders.get("bar"));
	}

	@Test
	public void singleStringHeaderOnlyWithIntegerPayload() throws Exception {
		MessageHandler handler = this.getHandler("singleStringHeaderOnly", String.class);
		Map<String, Object> headers = new HashMap<String, Object>();
		headers.put("foo", "1");
		headers.put("bar", "2");
		Message<?> message = MessageBuilder.withPayload(new Integer(123))
				.copyHeaders(headers).build();
		handler.handleMessage(message);
		assertNull(bean.lastPayload);
		assertEquals("1", bean.lastHeaders.get("foo"));
		assertNull(bean.lastHeaders.get("bar"));
	}

	@Test
	public void singleStringHeaderOnlyWithMapPayload() throws Exception {
		MessageHandler handler = this.getHandler("singleStringHeaderOnly", String.class);
		Map<String, Object> payload = new HashMap<String, Object>();
		payload.put("foo", 99);
		Map<String, Object> headers = new HashMap<String, Object>();
		headers.put("foo", "1");
		headers.put("bar", "2");
		Message<?> message = MessageBuilder.withPayload(payload)
				.copyHeaders(headers).build();
		handler.handleMessage(message);
		assertNull(bean.lastPayload);
		assertEquals("1", bean.lastHeaders.get("foo"));
		assertNull(bean.lastHeaders.get("bar"));
	}

	@Test
	public void singleIntegerHeaderOnlyWithIntegerPayload() throws Exception {
		MessageHandler handler = this.getHandler("singleIntegerHeaderOnly", Integer.class);
		Map<String, Object> headers = new HashMap<String, Object>();
		headers.put("foo", new Integer(123));
		headers.put("bar", new Integer(456));
		Message<?> message = MessageBuilder.withPayload(new Integer(789))
				.copyHeaders(headers).build();
		handler.handleMessage(message);
		assertNull(bean.lastPayload);
		assertEquals(new Integer(123), bean.lastHeaders.get("foo"));
		assertNull(bean.lastHeaders.get("bar"));
	}

	@Test
	public void singleIntegerHeaderOnlyWithIntegerPayloadAndStringHeader() throws Exception {
		MessageHandler handler = this.getHandler("singleIntegerHeaderOnly", Integer.class);
		Map<String, Object> headers = new HashMap<String, Object>();
		headers.put("foo", "999");
		headers.put("bar", new Integer(456));
		Message<?> message = MessageBuilder.withPayload(new Integer(789))
				.copyHeaders(headers).build();
		handler.handleMessage(message);
		assertNull(bean.lastPayload);
		assertEquals(new Integer(999), bean.lastHeaders.get("foo"));
		assertNull(bean.lastHeaders.get("bar"));
	}

	@Test
	public void singleIntegerHeaderOnlyWithStringPayload() throws Exception {
		MessageHandler handler = this.getHandler("singleIntegerHeaderOnly", Integer.class);
		Map<String, Object> headers = new HashMap<String, Object>();
		headers.put("foo", new Integer(123));
		headers.put("bar", new Integer(456));
		Message<?> message = MessageBuilder.withPayload("test")
				.copyHeaders(headers).build();
		handler.handleMessage(message);
		assertNull(bean.lastPayload);
		assertEquals(new Integer(123), bean.lastHeaders.get("foo"));
		assertNull(bean.lastHeaders.get("bar"));
	}

	@Test
	public void singleObjectHeaderOnlyWithStringPayload() throws Exception {
		MessageHandler handler = this.getHandler("singleObjectHeaderOnly", Object.class);
		Map<String, Object> headers = new HashMap<String, Object>();
		headers.put("foo", "123");
		headers.put("bar", "456");
		Message<?> message = MessageBuilder.withPayload("test")
				.copyHeaders(headers).build();
		handler.handleMessage(message);
		assertNull(bean.lastPayload);
		assertEquals("123", bean.lastHeaders.get("foo"));
		assertNull(bean.lastHeaders.get("bar"));
	}

	@Test
	public void singleObjectHeaderOnlyWithObjectPayload() throws Exception {
		MessageHandler handler = this.getHandler("singleObjectHeaderOnly", Object.class);
		Map<String, Object> headers = new HashMap<String, Object>();
		headers.put("foo", "123");
		headers.put("bar", "456");
		Message<?> message = MessageBuilder.withPayload(new Object())
				.copyHeaders(headers).build();
		handler.handleMessage(message);
		assertNull(bean.lastPayload);
		assertEquals("123", bean.lastHeaders.get("foo"));
		assertNull(bean.lastHeaders.get("bar"));
	}

	@Test
	public void singleObjectHeaderOnlyWithIntegerPayload() throws Exception {
		MessageHandler handler = this.getHandler("singleObjectHeaderOnly", Object.class);
		Map<String, Object> headers = new HashMap<String, Object>();
		headers.put("foo", new Integer(123));
		headers.put("bar", new Integer(456));
		Message<?> message = MessageBuilder.withPayload(new Integer(789))
				.copyHeaders(headers).build();
		handler.handleMessage(message);
		assertNull(bean.lastPayload);
		assertEquals(new Integer(123), bean.lastHeaders.get("foo"));
		assertNull(bean.lastHeaders.get("bar"));
	}

	@Test
	public void singleObjectHeaderOnlyWithMapPayload() throws Exception {
		MessageHandler handler = this.getHandler("singleObjectHeaderOnly", Object.class);
		Map<String, Object> payload = new HashMap<String, Object>();
		payload.put("foo", 99);
		Map<String, Object> headers = new HashMap<String, Object>();
		headers.put("foo", new Integer(123));
		headers.put("bar", new Integer(456));
		Message<?> message = MessageBuilder.withPayload(payload)
				.copyHeaders(headers).build();
		handler.handleMessage(message);
		assertNull(bean.lastPayload);
		assertEquals(new Integer(123), bean.lastHeaders.get("foo"));
		assertNull(bean.lastHeaders.get("bar"));
	}

	@Test
	public void twoPayloadExpressions() throws Exception {
		MessageHandler handler = this.getHandler("twoPayloadExpressions", String.class, String.class);
		Map<String, Object> payload = new HashMap<String, Object>();
		payload.put("foo", new Integer(123));
		payload.put("bar", new Integer(456));
		Message<?> message = MessageBuilder.withPayload(payload).build();
		handler.handleMessage(message);
		assertNull(bean.lastHeaders);
		assertNotNull(bean.lastPayload);
		assertEquals("123456", bean.lastPayload);
	}


	private ServiceActivatingHandler getHandler(String methodName, Class<?>... types) throws Exception {
		return new ServiceActivatingHandler(bean, TestBean.class.getMethod(methodName, types));
	}


	@SuppressWarnings("unused")
	private static class SingleAmbiguousMethodTestBean {

		public String concat(String s1, String s2) {
			return "s1" + "s2";
		}
	}

	@SuppressWarnings("unused")
	private static class TestBean {

		private volatile Map lastHeaders;

		private volatile Object lastPayload;


		public void headerPropertiesAndObjectPayload(Properties headers, Object payload) {
			this.lastHeaders = headers;
			this.lastPayload = payload;
		}

		public void stringPayloadAndHeaderProperties(String payload, Properties headers) {
			this.lastHeaders = headers;
			this.lastPayload = payload;
		}

		public void stringPayloadAndHeaderMap(String payload, Map headers) {
			this.lastHeaders = headers;
			this.lastPayload = payload;
		}

		public void headerMapAndObjectPayload(Map headers, Object payload) {
			this.lastHeaders = headers;
			this.lastPayload = payload;
		}

		public void objectPayloadAndHeaderMap(Object payload, Map headers) {
			this.lastHeaders = headers;
			this.lastPayload = payload;
		}

		public void objectPayloadHeaderMapAndStringHeaders(
				@Header("foo") String header1, Map headers, @Header("bar") String header2, Object payload) {
			this.lastHeaders = new HashMap<String, String>();
			this.lastHeaders.put("foo2", header1);
			this.lastHeaders.put("bar2", header2);
			this.lastHeaders.putAll(headers);
			this.lastPayload = payload;
			this.lastPayload = payload;
		}

		public void payloadMapAndHeaderString(Map payload, @Header("foo") String header) {
			this.lastHeaders = new HashMap<String, String>();
			this.lastHeaders.put("foo", header);
			this.lastPayload = payload;
		}

		public void payloadMapAndHeaderStrings(Map payload, @Header("foo") String header1, @Header("bar") String header2) {
			this.lastHeaders = new HashMap<String, String>();
			this.lastHeaders.put("foo", header1);
			this.lastHeaders.put("bar", header2);
			this.lastPayload = payload;
		}

		public void payloadMapAndHeaderMap(Map payload, @Headers Map headers) {
			this.lastHeaders = headers;
			this.lastPayload = payload;
		}

		public void headerMapAndPayloadMap(@Headers Map headers, Map payload) {
			this.lastHeaders = headers;
			this.lastPayload = payload;
		}

		public void payloadMapAndHeaderProperties(Map payload, @Headers Properties headers) {
			this.lastHeaders = headers;
			this.lastPayload = payload;
		}

		public void headerPropertiesPayloadMapAndStringHeader(@Headers Properties headers, Map payload, @Header("foo") String header) {
			this.lastHeaders = headers;
			this.lastHeaders.put("foo2", header);
			this.lastPayload = payload;
		}

		public void headerMapOnly(@Headers Map headers) {
			this.lastHeaders = headers;
		}

		public void headerPropertiesOnly(@Headers Properties headers) {
			this.lastHeaders = headers;
		}

		public void mapOnlyNoAnnotations(Map map) {
			if (map.containsKey("payload")) {
				this.lastPayload = map;
			}
			else {
				this.lastHeaders = map;
			}
		}

		public void propertiesOnlyNoAnnotations(Properties props) {
			if (props.containsKey("payload")) {
				this.lastPayload = props;
			}
			else {
				this.lastHeaders = props;
			}
		}

		public void twoStringsNoAnnotations(String s1, String s2) {
			// invalid due to ambiguity (no @Payload or @Headers)
		}

		public void twoMapsNoAnnotations(Map map1, Map<Object, Object> map2) {
			// invalid due to ambiguity (no @Payload or @Headers)
		}

		public void twoMapsWithAnnotations(@Headers Map map1, @Headers Map<Object, Object> map2) {
			this.lastHeaders = new HashMap(map1);
			for (Map.Entry<Object, Object> entry : map2.entrySet()) {
				this.lastHeaders.put(entry.getKey() + "2", entry.getValue());
			}
		}

		public void twoMapsWithAnnotationsAndObject(@Headers Map map1, Object o, @Headers Map<Object, Object> map2) {
			this.lastPayload = o;
			this.lastHeaders = new HashMap(map1);
			for (Map.Entry<Object, Object> entry : map2.entrySet()) {
				this.lastHeaders.put(entry.getKey() + "2", entry.getValue());
			}
		}

		public void twoMapsNoAnnotationsAndObject(Map map1, Object o, Map<Object, Object> map2) {
			// invalid due to ambiguity of Map parameters (no @Payload or @Headers)
		}

		public void mapAndAnnotatedStringHeaderExpectingMapAsHeaders(Map map, @Header("foo") String s) {
			this.lastHeaders = new HashMap(map);
			this.lastHeaders.put("foo2", s);
		}

		public void mapAndAnnotatedStringHeaderExpectingMapAsPayload(Map map, @Header("foo") String s) {
			this.lastPayload = map;
			this.lastHeaders = Collections.singletonMap("foo", s);
		}

		public void singleStringHeaderOnly(@Header("foo") String s) {
			this.lastHeaders = Collections.singletonMap("foo", s);
		}

		public void singleIntegerHeaderOnly(@Header("foo") Integer n) {
			this.lastHeaders = Collections.singletonMap("foo", n);
		}

		public void singleObjectHeaderOnly(@Header("foo") Object o) {
			this.lastHeaders = Collections.singletonMap("foo", o);
		}

		public void twoPayloadExpressions(@Payload("foo") String foo, @Payload("bar") String bar) {
			this.lastPayload = foo + bar;
		}
	}

}
