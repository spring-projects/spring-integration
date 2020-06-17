/*
 * Copyright 2002-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.message;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.integration.handler.ServiceActivatingHandler;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 *
 * @since 1.0.3
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class PayloadAndHeaderMappingTests {

	private static final ConfigurableApplicationContext applicationContext = TestUtils.createTestApplicationContext();

	private TestBean bean;

	@BeforeAll
	public static void start() {
		applicationContext.refresh();
	}

	@AfterAll
	public static void stop() {
		applicationContext.close();
	}

	@BeforeEach
	public void setup() {
		bean = new TestBean();
	}


	@Test
	public void headerPropertiesAndObjectPayload() throws Exception {
		MessageHandler handler = this.getHandler("headerPropertiesAndObjectPayload", Properties.class, Object.class);
		Object payload = "test";
		Map<String, Object> headers = new HashMap<>();
		headers.put("foo", "1");
		headers.put("bar", "2");
		headers.put("baz", 99);
		Message<?> message = MessageBuilder.withPayload(payload).copyHeaders(headers).build();
		handler.handleMessage(message);
		assertThat(bean.lastPayload).isEqualTo(payload);
		assertThat(bean.lastHeaders.containsKey("foo")).isTrue();
		assertThat(bean.lastHeaders.containsKey("bar")).isTrue();
		//	assertFalse(bean.lastHeaders.containsKey("baz"));
	}

	@Test
	public void stringPayloadAndHeaderProperties() throws Exception {
		MessageHandler handler = this.getHandler("stringPayloadAndHeaderProperties", String.class, Properties.class);
		Object payload = "test";
		Map<String, Object> headers = new HashMap<>();
		headers.put("foo", "1");
		headers.put("bar", "2");
		headers.put("baz", 99);
		Message<?> message = MessageBuilder.withPayload(payload).copyHeaders(headers).build();
		handler.handleMessage(message);
		assertThat(bean.lastPayload).isEqualTo(payload);
		assertThat(bean.lastHeaders.containsKey("foo")).isTrue();
		assertThat(bean.lastHeaders.containsKey("bar")).isTrue();
		//assertFalse(bean.lastHeaders.containsKey("baz"));
	}

	@Test
	public void headerMapAndObjectPayload() throws Exception {
		MessageHandler handler = this.getHandler("headerMapAndObjectPayload", Map.class, Object.class);
		Object payload = "test";
		Map<String, Object> headers = new HashMap<>();
		headers.put("foo", "1");
		headers.put("bar", "2");
		headers.put("baz", 99);
		Message<?> message = MessageBuilder.withPayload(payload).copyHeaders(headers).build();
		handler.handleMessage(message);
		assertThat(bean.lastPayload).isEqualTo(payload);
		assertThat(bean.lastHeaders.containsKey("foo")).isTrue();
		assertThat(bean.lastHeaders.containsKey("bar")).isTrue();
		assertThat(bean.lastHeaders.containsKey("baz")).isTrue();
	}

	@Test
	public void objectPayloadAndHeaderMap() throws Exception {
		MessageHandler handler = this.getHandler("objectPayloadAndHeaderMap", Object.class, Map.class);
		Object payload = "test";
		Map<String, Object> headers = new HashMap<>();
		headers.put("foo", "1");
		headers.put("bar", "2");
		headers.put("baz", 99);
		Message<?> message = MessageBuilder.withPayload(payload).copyHeaders(headers).build();
		handler.handleMessage(message);
		assertThat(bean.lastPayload).isEqualTo(payload);
		assertThat(bean.lastHeaders.containsKey("foo")).isTrue();
		assertThat(bean.lastHeaders.containsKey("bar")).isTrue();
		assertThat(bean.lastHeaders.containsKey("baz")).isTrue();
	}

	@Test
	public void payloadMapAndHeaderString() throws Exception {
		MessageHandler handler = this.getHandler("payloadMapAndHeaderString", Map.class, String.class);
		Map<String, Object> payload = new HashMap<>();
		payload.put("abc", 1);
		payload.put("xyz", "test");
		Map<String, Object> headers = new HashMap<>();
		headers.put("foo", "1");
		headers.put("bar", "2");
		Message<?> message = MessageBuilder.withPayload(payload).copyHeaders(headers).build();
		handler.handleMessage(message);
		assertThat(bean.lastPayload).isEqualTo(payload);
		assertThat(bean.lastHeaders.containsKey("foo")).isTrue();
		assertThat(bean.lastHeaders.containsKey("bar")).isFalse();
	}

	@Test
	public void payloadMapAndHeaderStrings() throws Exception {
		MessageHandler handler = this.getHandler("payloadMapAndHeaderStrings", Map.class, String.class, String.class);
		Map<String, Object> payload = new HashMap<>();
		payload.put("abc", 1);
		payload.put("xyz", "test");
		Map<String, Object> headers = new HashMap<>();
		headers.put("foo", "1");
		headers.put("bar", "2");
		headers.put("baz", "3");
		Message<?> message = MessageBuilder.withPayload(payload).copyHeaders(headers).build();
		handler.handleMessage(message);
		assertThat(bean.lastPayload).isEqualTo(payload);
		assertThat(bean.lastHeaders.containsKey("foo")).isTrue();
		assertThat(bean.lastHeaders.containsKey("bar")).isTrue();
		assertThat(bean.lastHeaders.containsKey("baz")).isFalse();
	}

	@Test
	public void objectPayloadHeaderMapAndStringHeaders() throws Exception {
		MessageHandler handler = this.getHandler("objectPayloadHeaderMapAndStringHeaders",
				String.class, Map.class, String.class, Object.class);
		Object payload = "test";
		Map<String, Object> headers = new HashMap<>();
		headers.put("foo", "1");
		headers.put("bar", "2");
		headers.put("baz", 99);
		Message<?> message = MessageBuilder.withPayload(payload).copyHeaders(headers).build();
		handler.handleMessage(message);
		assertThat(bean.lastPayload).isEqualTo(payload);
		assertThat(bean.lastHeaders.containsKey("foo")).isTrue();
		assertThat(bean.lastHeaders.get("foo")).isEqualTo("1");
		assertThat(bean.lastHeaders.containsKey("bar")).isTrue();
		assertThat(bean.lastHeaders.get("bar")).isEqualTo("2");
		assertThat(bean.lastHeaders.containsKey("baz")).isTrue();
		assertThat(bean.lastHeaders.containsKey("foo2")).isTrue();
		assertThat(bean.lastHeaders.get("foo2")).isEqualTo("1");
		assertThat(bean.lastHeaders.containsKey("bar2")).isTrue();
		assertThat(bean.lastHeaders.get("bar2")).isEqualTo("2");
	}

	@Test
	public void payloadMapAndHeaderMap() throws Exception {
		MessageHandler handler = this.getHandler("payloadMapAndHeaderMap", Map.class, Map.class);
		Map<String, Object> payload = new HashMap<>();
		payload.put("abc", 1);
		payload.put("xyz", "test");
		Map<String, Object> headers = new HashMap<>();
		headers.put("foo", "1");
		headers.put("bar", "2");
		headers.put("baz", 99);
		Message<?> message = MessageBuilder.withPayload(payload).copyHeaders(headers).build();
		handler.handleMessage(message);
		assertThat(bean.lastPayload).isEqualTo(payload);
		assertThat(bean.lastHeaders.containsKey("foo")).isTrue();
		assertThat(bean.lastHeaders.containsKey("bar")).isTrue();
		assertThat(bean.lastHeaders.containsKey("baz")).isTrue();
	}

	@Test
	public void headerMapAndPayloadMap() throws Exception {
		MessageHandler handler = this.getHandler("headerMapAndPayloadMap", Map.class, Map.class);
		Map<String, Object> payload = new HashMap<>();
		payload.put("abc", 1);
		payload.put("xyz", "test");
		Map<String, Object> headers = new HashMap<>();
		headers.put("foo", "1");
		headers.put("bar", "2");
		headers.put("baz", 99);
		Message<?> message = MessageBuilder.withPayload(payload).copyHeaders(headers).build();
		handler.handleMessage(message);
		assertThat(bean.lastPayload).isEqualTo(payload);
		assertThat(bean.lastHeaders.containsKey("foo")).isTrue();
		assertThat(bean.lastHeaders.containsKey("bar")).isTrue();
		assertThat(bean.lastHeaders.containsKey("baz")).isTrue();
	}

	@Test
	public void headerMapOnlyWithStringPayload() throws Exception {
		MessageHandler handler = this.getHandler("headerMapOnly", Map.class);
		String payload = "test";
		Map<String, Object> headers = new HashMap<>();
		headers.put("foo", "1");
		headers.put("bar", "2");
		headers.put("baz", 99);
		Message<?> message = MessageBuilder.withPayload(payload).copyHeaders(headers).build();
		handler.handleMessage(message);
		assertThat(bean.lastPayload).as(payload).isNull();
		assertThat(bean.lastHeaders.containsKey("foo")).isTrue();
		assertThat(bean.lastHeaders.containsKey("bar")).isTrue();
		assertThat(bean.lastHeaders.containsKey("baz")).isTrue();
	}

	@Test
	public void headerMapOnlyWithMapPayload() throws Exception {
		MessageHandler handler = this.getHandler("headerMapOnly", Map.class);
		Map<String, Object> payload = new HashMap<>();
		payload.put("abc", 1);
		payload.put("xyz", "test");
		Map<String, Object> headers = new HashMap<>();
		headers.put("foo", "1");
		headers.put("bar", "2");
		headers.put("baz", 99);
		Message<?> message = MessageBuilder.withPayload(payload).copyHeaders(headers).build();
		handler.handleMessage(message);
		assertThat(bean.lastPayload).isNull();
		assertThat(bean.lastHeaders.containsKey("foo")).isTrue();
		assertThat(bean.lastHeaders.containsKey("bar")).isTrue();
		assertThat(bean.lastHeaders.containsKey("baz")).isTrue();
	}

	@Test
	public void mapOnlyNoAnnotationsWithMapPayload() throws Exception {
		MessageHandler handler = this.getHandler("mapOnlyNoAnnotations", Map.class);
		Map<String, Object> payload = new HashMap<>();
		payload.put("payload", 1);
		Map<String, Object> headers = new HashMap<>();
		Message<?> message = MessageBuilder.withPayload(payload).copyHeaders(headers).build();
		handler.handleMessage(message);
		assertThat(bean.lastPayload).isEqualTo(payload);
		assertThat(bean.lastHeaders).isNull();
	}

	@Test
	public void mapOnlyNoAnnotationsWithStringPayload() throws Exception {
		MessageHandler handler = this.getHandler("mapOnlyNoAnnotations", Map.class);
		String payload = "test";
		Map<String, Object> headers = new HashMap<>();
		headers.put("foo", "1");
		headers.put("bar", "2");
		headers.put("baz", 99);
		Message<?> message = MessageBuilder.withPayload(payload).copyHeaders(headers).build();
		handler.handleMessage(message);
		assertThat(bean.lastPayload).isNull();
		assertThat(bean.lastHeaders.containsKey("foo")).isTrue();
		assertThat(bean.lastHeaders.containsKey("bar")).isTrue();
		assertThat(bean.lastHeaders.containsKey("baz")).isTrue();
	}

	@Test
	public void mapOnlyNoAnnotationsWithIntegerPayload() throws Exception {
		MessageHandler handler = this.getHandler("mapOnlyNoAnnotations", Map.class);
		int payload = 123;
		Map<String, Object> headers = new HashMap<>();
		headers.put("foo", "1");
		headers.put("bar", "2");
		headers.put("baz", 99);
		Message<?> message = MessageBuilder.withPayload(payload).copyHeaders(headers).build();
		handler.handleMessage(message);
		assertThat(bean.lastPayload).isNull();
		assertThat(bean.lastHeaders.containsKey("foo")).isTrue();
		assertThat(bean.lastHeaders.containsKey("bar")).isTrue();
		assertThat(bean.lastHeaders.containsKey("baz")).isTrue();
	}

	@Test
	public void propertiesOnlyNoAnnotationsWithPropertiesPayload() throws Exception {
		MessageHandler handler = this.getHandler("propertiesOnlyNoAnnotations", Properties.class);
		Properties payload = new Properties();
		payload.setProperty("payload", "1");
		Map<String, Object> headers = new HashMap<>();
		Message<?> message = MessageBuilder.withPayload(payload).copyHeaders(headers).build();
		handler.handleMessage(message);
		assertThat(bean.lastPayload).isEqualTo(payload);
		assertThat(bean.lastHeaders).isNull();
	}

	@Test
	public void propertiesOnlyNoAnnotationsWithMapPayload() throws Exception {
		MessageHandler handler = this.getHandler("propertiesOnlyNoAnnotations", Properties.class);
		Map<String, Object> payload = new HashMap<>();
		payload.put("payload", 1);
		Map<String, Object> headers = new HashMap<>();
		Message<?> message = MessageBuilder.withPayload(payload).copyHeaders(headers).build();
		handler.handleMessage(message);
		assertThat(bean.lastPayload).isEqualTo(payload);
		assertThat(bean.lastHeaders).isNull();
	}

	@Test
	public void propertiesOnlyNoAnnotationsWithStringPayload() throws Exception {
		MessageHandler handler = this.getHandler("propertiesOnlyNoAnnotations", Properties.class);
		String payload = "payload=abc";
		Map<String, Object> headers = new HashMap<>();
		headers.put("foo", "1");
		headers.put("bar", "2");
		Message<?> message = MessageBuilder.withPayload(payload).copyHeaders(headers).build();
		handler.handleMessage(message);
		assertThat(bean.lastHeaders).isNull();
		// String payload should have been converted to Properties
		assertThat(bean.lastPayload).isNotNull();
		assertThat(bean.lastPayload instanceof Properties).isTrue();
		Properties payloadProps = (Properties) bean.lastPayload;
		assertThat(payloadProps.containsKey("payload")).isTrue();
		assertThat(payloadProps.get("payload")).isEqualTo("abc");
	}

	@Test
	public void propertiesOnlyNoAnnotationsWithIntegerPayload() throws Exception {
		MessageHandler handler = this.getHandler("propertiesOnlyNoAnnotations", Properties.class);
		int payload = 123;
		Map<String, Object> headers = new HashMap<>();
		headers.put("foo", "1");
		headers.put("bar", "2");
		Message<?> message = MessageBuilder.withPayload(payload).copyHeaders(headers).build();
		handler.handleMessage(message);
		assertThat(bean.lastPayload).isNull();
		assertThat(bean.lastHeaders.containsKey("foo")).isTrue();
		assertThat(bean.lastHeaders.containsKey("bar")).isTrue();
	}

	@Test
	public void headerPropertiesOnlyWithStringPayload() throws Exception {
		MessageHandler handler = this.getHandler("headerPropertiesOnly", Properties.class);
		String payload = "test";
		Map<String, Object> headers = new HashMap<>();
		headers.put("foo", "1");
		headers.put("bar", "2");
		headers.put("baz", 99);
		Message<?> message = MessageBuilder.withPayload(payload).copyHeaders(headers).build();
		handler.handleMessage(message);
		assertThat(bean.lastPayload).isNull();
		assertThat(bean.lastHeaders.containsKey("foo")).isTrue();
		assertThat(bean.lastHeaders.containsKey("bar")).isTrue();
		//assertFalse(bean.lastHeaders.containsKey("baz"));
	}

	@Test
	public void headerPropertiesOnlyWithMapPayload() throws Exception {
		MessageHandler handler = this.getHandler("headerPropertiesOnly", Properties.class);
		Map<String, Object> payload = new HashMap<>();
		payload.put("abc", 1);
		payload.put("xyz", "test");
		Map<String, Object> headers = new HashMap<>();
		headers.put("foo", "1");
		headers.put("bar", "2");
		headers.put("baz", 99);
		Message<?> message = MessageBuilder.withPayload(payload).copyHeaders(headers).build();
		handler.handleMessage(message);
		assertThat(bean.lastPayload).isNull();
		assertThat(bean.lastHeaders.containsKey("foo")).isTrue();
		assertThat(bean.lastHeaders.containsKey("bar")).isTrue();
		//assertFalse(bean.lastHeaders.containsKey("baz"));
	}

	@Test
	public void headerPropertiesOnlyWithPropertiesPayload() throws Exception {
		MessageHandler handler = this.getHandler("headerPropertiesOnly", Properties.class);
		Properties payload = new Properties();
		payload.setProperty("abc", "1");
		payload.setProperty("xyz", "2");
		Map<String, Object> headers = new HashMap<>();
		headers.put("foo", "1");
		headers.put("bar", "2");
		headers.put("baz", 99);
		Message<?> message = MessageBuilder.withPayload(payload).copyHeaders(headers).build();
		handler.handleMessage(message);
		assertThat(bean.lastPayload).isNull();
		assertThat(bean.lastHeaders.containsKey("foo")).isTrue();
		assertThat(bean.lastHeaders.containsKey("bar")).isTrue();
		//assertFalse(bean.lastHeaders.containsKey("baz"));
	}

	@Test
	public void payloadMapAndHeaderProperties() throws Exception {
		MessageHandler handler = this.getHandler("payloadMapAndHeaderProperties", Map.class, Properties.class);
		Map<String, Object> payload = new HashMap<>();
		payload.put("abc", 1);
		payload.put("xyz", "test");
		Map<String, Object> headers = new HashMap<>();
		headers.put("foo", "1");
		headers.put("bar", "2");
		headers.put("baz", 99);
		Message<?> message = MessageBuilder.withPayload(payload).copyHeaders(headers).build();
		handler.handleMessage(message);
		assertThat(bean.lastPayload).isEqualTo(payload);
		assertThat(bean.lastHeaders.containsKey("foo")).isTrue();
		assertThat(bean.lastHeaders.containsKey("bar")).isTrue();
		//assertFalse(bean.lastHeaders.containsKey("baz"));
	}

	@Test
	public void headerPropertiesPayloadMapAndStringHeader() throws Exception {
		MessageHandler handler = this.getHandler("headerPropertiesPayloadMapAndStringHeader",
				Properties.class, Map.class, String.class);
		Map<String, Object> payload = new HashMap<>();
		payload.put("abc", 1);
		payload.put("xyz", "test");
		Map<String, Object> headers = new HashMap<>();
		headers.put("foo", "1");
		headers.put("bar", "2");
		headers.put("baz", 99);
		Message<?> message = MessageBuilder.withPayload(payload).copyHeaders(headers).build();
		handler.handleMessage(message);
		assertThat(bean.lastPayload).isEqualTo(payload);
		assertThat(bean.lastHeaders.containsKey("foo")).isTrue();
		assertThat(bean.lastHeaders.get("foo")).isEqualTo("1");
		assertThat(bean.lastHeaders.containsKey("bar")).isTrue();
		assertThat(bean.lastHeaders.containsKey("foo2")).isTrue();
		assertThat(bean.lastHeaders.get("foo2")).isEqualTo("1");
		//assertFalse(bean.lastHeaders.containsKey("baz"));
	}

	@Test
	public void twoMapsNoAnnotations() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> getHandler("twoMapsNoAnnotations", Map.class, Map.class));
	}

	@Test
	public void twoMapsWithAnnotationsWithStringPayload() throws Exception {
		MessageHandler handler = this.getHandler("twoMapsWithAnnotations", Map.class, Map.class);
		Map<String, Object> headers = new HashMap<>();
		headers.put("foo", "1");
		headers.put("bar", "2");
		Message<?> message = MessageBuilder.withPayload("test").copyHeaders(headers).build();
		handler.handleMessage(message);
		assertThat(bean.lastPayload).isNull();
		assertThat(bean.lastHeaders.get("foo")).isEqualTo("1");
		assertThat(bean.lastHeaders.get("bar")).isEqualTo("2");
		assertThat(bean.lastHeaders.get("foo2")).isEqualTo("1");
		assertThat(bean.lastHeaders.get("bar2")).isEqualTo("2");
	}

	@Test
	public void twoMapsWithAnnotationsWithMapPayload() throws Exception {
		MessageHandler handler = this.getHandler("twoMapsWithAnnotations", Map.class, Map.class);
		Map<String, Object> headers = new HashMap<>();
		headers.put("foo", "1");
		headers.put("bar", "2");
		Map<String, Object> payloadMap = new HashMap<>();
		payloadMap.put("baz", "99");
		Message<?> message = MessageBuilder.withPayload(payloadMap).copyHeaders(headers).build();
		handler.handleMessage(message);
		assertThat(bean.lastHeaders.get("foo")).isEqualTo("1");
		assertThat(bean.lastHeaders.get("bar")).isEqualTo("2");
		assertThat(bean.lastHeaders.get("foo2")).isEqualTo("1");
		assertThat(bean.lastHeaders.get("bar2")).isEqualTo("2");
		assertThat(bean.lastHeaders.get("baz")).isEqualTo(null);
	}

	@Test
	public void twoStringsAmbiguousUsingMethodName() {
		assertThatIllegalStateException()
				.isThrownBy(() -> new ServiceActivatingHandler(new SingleAmbiguousMethodTestBean(), "twoStrings"));
	}

	@Test
	public void twoStringsAmbiguousWithoutMethodName() {
		assertThatIllegalStateException()
				.isThrownBy(() -> new ServiceActivatingHandler(new SingleAmbiguousMethodTestBean()));
	}

	@Test
	public void twoStringsNoAnnotations() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> getHandler("twoStringsNoAnnotations", String.class, String.class));
	}

	@Test
	public void twoMapsNoAnnotationsAndObject() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> getHandler("twoMapsNoAnnotationsAndObject", Map.class, Object.class, Map.class));
	}

	@Test
	public void mapAndAnnotatedStringHeaderWithStringPayload() throws Exception {
		MessageHandler handler = this.getHandler(
				"mapAndAnnotatedStringHeaderExpectingMapAsHeaders", Map.class, String.class);
		Map<String, Object> headers = new HashMap<>();
		headers.put("foo", "1");
		headers.put("bar", "2");
		Message<?> message = MessageBuilder.withPayload("test")
				.copyHeaders(headers).build();
		handler.handleMessage(message);
		assertThat(bean.lastPayload).isNull();
		assertThat(bean.lastHeaders.get("foo")).isEqualTo("1");
		assertThat(bean.lastHeaders.get("bar")).isEqualTo("2");
		assertThat(bean.lastHeaders.get("foo2")).isEqualTo("1");
	}

	@Test
	public void mapAndAnnotatedStringHeaderWithMapPayload() throws Exception {
		MessageHandler handler = this.getHandler(
				"mapAndAnnotatedStringHeaderExpectingMapAsPayload", Map.class, String.class);
		Map<String, Object> payload = new HashMap<>();
		payload.put("test", "0");
		Map<String, Object> headers = new HashMap<>();
		headers.put("foo", "1");
		headers.put("bar", "2");
		Message<?> message = MessageBuilder.withPayload(payload)
				.copyHeaders(headers).build();
		handler.handleMessage(message);
		assertThat(bean.lastPayload).isNotNull();
		assertThat(bean.lastPayload).isEqualTo(payload);
		assertThat(bean.lastHeaders.get("foo")).isEqualTo("1");
		assertThat(bean.lastHeaders.get("bar")).isNull();
	}

	@Test
	public void singleStringHeaderOnlyWithStringPayload() throws Exception {
		MessageHandler handler = this.getHandler("singleStringHeaderOnly", String.class);
		Map<String, Object> headers = new HashMap<>();
		headers.put("foo", "1");
		headers.put("bar", "2");
		Message<?> message = MessageBuilder.withPayload("test")
				.copyHeaders(headers).build();
		handler.handleMessage(message);
		assertThat(bean.lastPayload).isNull();
		assertThat(bean.lastHeaders.get("foo")).isEqualTo("1");
		assertThat(bean.lastHeaders.get("bar")).isNull();
	}

	@Test
	public void singleStringHeaderOnlyWithIntegerPayload() throws Exception {
		MessageHandler handler = this.getHandler("singleStringHeaderOnly", String.class);
		Map<String, Object> headers = new HashMap<>();
		headers.put("foo", "1");
		headers.put("bar", "2");
		Message<?> message = MessageBuilder.withPayload(123)
				.copyHeaders(headers).build();
		handler.handleMessage(message);
		assertThat(bean.lastPayload).isNull();
		assertThat(bean.lastHeaders.get("foo")).isEqualTo("1");
		assertThat(bean.lastHeaders.get("bar")).isNull();
	}

	@Test
	public void singleStringHeaderOnlyWithMapPayload() throws Exception {
		MessageHandler handler = this.getHandler("singleStringHeaderOnly", String.class);
		Map<String, Object> payload = new HashMap<>();
		payload.put("foo", 99);
		Map<String, Object> headers = new HashMap<>();
		headers.put("foo", "1");
		headers.put("bar", "2");
		Message<?> message = MessageBuilder.withPayload(payload)
				.copyHeaders(headers).build();
		handler.handleMessage(message);
		assertThat(bean.lastPayload).isNull();
		assertThat(bean.lastHeaders.get("foo")).isEqualTo("1");
		assertThat(bean.lastHeaders.get("bar")).isNull();
	}

	@Test
	public void singleIntegerHeaderOnlyWithIntegerPayload() throws Exception {
		MessageHandler handler = this.getHandler("singleIntegerHeaderOnly", Integer.class);
		Map<String, Object> headers = new HashMap<>();
		headers.put("foo", 123);
		headers.put("bar", 456);
		Message<?> message = MessageBuilder.withPayload(789)
				.copyHeaders(headers).build();
		handler.handleMessage(message);
		assertThat(bean.lastPayload).isNull();
		assertThat(bean.lastHeaders.get("foo")).isEqualTo(123);
		assertThat(bean.lastHeaders.get("bar")).isNull();
	}

	@Test
	public void singleIntegerHeaderOnlyWithIntegerPayloadAndStringHeader() throws Exception {
		MessageHandler handler = this.getHandler("singleIntegerHeaderOnly", Integer.class);
		Map<String, Object> headers = new HashMap<>();
		headers.put("foo", "999");
		headers.put("bar", 456);
		Message<?> message = MessageBuilder.withPayload(789)
				.copyHeaders(headers).build();
		handler.handleMessage(message);
		assertThat(bean.lastPayload).isNull();
		assertThat(bean.lastHeaders.get("foo")).isEqualTo(999);
		assertThat(bean.lastHeaders.get("bar")).isNull();
	}

	@Test
	public void singleIntegerHeaderOnlyWithStringPayload() throws Exception {
		MessageHandler handler = this.getHandler("singleIntegerHeaderOnly", Integer.class);
		Map<String, Object> headers = new HashMap<>();
		headers.put("foo", 123);
		headers.put("bar", 456);
		Message<?> message = MessageBuilder.withPayload("test")
				.copyHeaders(headers).build();
		handler.handleMessage(message);
		assertThat(bean.lastPayload).isNull();
		assertThat(bean.lastHeaders.get("foo")).isEqualTo(123);
		assertThat(bean.lastHeaders.get("bar")).isNull();
	}

	@Test
	public void singleObjectHeaderOnlyWithStringPayload() throws Exception {
		MessageHandler handler = this.getHandler("singleObjectHeaderOnly", Object.class);
		Map<String, Object> headers = new HashMap<>();
		headers.put("foo", "123");
		headers.put("bar", "456");
		Message<?> message = MessageBuilder.withPayload("test")
				.copyHeaders(headers).build();
		handler.handleMessage(message);
		assertThat(bean.lastPayload).isNull();
		assertThat(bean.lastHeaders.get("foo")).isEqualTo("123");
		assertThat(bean.lastHeaders.get("bar")).isNull();
	}

	@Test
	public void singleObjectHeaderOnlyWithObjectPayload() throws Exception {
		MessageHandler handler = this.getHandler("singleObjectHeaderOnly", Object.class);
		Map<String, Object> headers = new HashMap<>();
		headers.put("foo", "123");
		headers.put("bar", "456");
		Message<?> message = MessageBuilder.withPayload(new Object())
				.copyHeaders(headers).build();
		handler.handleMessage(message);
		assertThat(bean.lastPayload).isNull();
		assertThat(bean.lastHeaders.get("foo")).isEqualTo("123");
		assertThat(bean.lastHeaders.get("bar")).isNull();
	}

	@Test
	public void singleObjectHeaderOnlyWithIntegerPayload() throws Exception {
		MessageHandler handler = this.getHandler("singleObjectHeaderOnly", Object.class);
		Map<String, Object> headers = new HashMap<>();
		headers.put("foo", 123);
		headers.put("bar", 456);
		Message<?> message = MessageBuilder.withPayload(789)
				.copyHeaders(headers).build();
		handler.handleMessage(message);
		assertThat(bean.lastPayload).isNull();
		assertThat(bean.lastHeaders.get("foo")).isEqualTo(123);
		assertThat(bean.lastHeaders.get("bar")).isNull();
	}

	@Test
	public void singleObjectHeaderOnlyWithMapPayload() throws Exception {
		MessageHandler handler = this.getHandler("singleObjectHeaderOnly", Object.class);
		Map<String, Object> payload = new HashMap<>();
		payload.put("foo", 99);
		Map<String, Object> headers = new HashMap<>();
		headers.put("foo", 123);
		headers.put("bar", 456);
		Message<?> message = MessageBuilder.withPayload(payload)
				.copyHeaders(headers).build();
		handler.handleMessage(message);
		assertThat(bean.lastPayload).isNull();
		assertThat(bean.lastHeaders.get("foo")).isEqualTo(123);
		assertThat(bean.lastHeaders.get("bar")).isNull();
	}

	@Test
	public void twoPayloadExpressions() throws Exception {
		MessageHandler handler = this.getHandler("twoPayloadExpressions", String.class, String.class);
		Map<String, Object> payload = new HashMap<>();
		payload.put("foo", 123);
		payload.put("bar", 456);
		Message<?> message = MessageBuilder.withPayload(payload).build();
		handler.handleMessage(message);
		assertThat(bean.lastHeaders).isNull();
		assertThat(bean.lastPayload).isNotNull();
		assertThat(bean.lastPayload).isEqualTo("123456");
	}


	private ServiceActivatingHandler getHandler(String methodName, Class<?>... types) throws Exception {
		ServiceActivatingHandler serviceActivatingHandler =
				new ServiceActivatingHandler(bean, TestBean.class.getMethod(methodName, types));
		serviceActivatingHandler.setBeanFactory(applicationContext);
		serviceActivatingHandler.afterPropertiesSet();
		return serviceActivatingHandler;
	}


	@SuppressWarnings("unused")
	private static class SingleAmbiguousMethodTestBean {

		SingleAmbiguousMethodTestBean() {
			super();
		}

		public String concat(String s1, String s2) {
			return "s1" + "s2";
		}

	}

	@SuppressWarnings("unused")
	private static class TestBean {

		private volatile Map lastHeaders;

		private volatile Object lastPayload;


		TestBean() {
			super();
		}

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

		public void payloadMapAndHeaderStrings(Map payload, @Header("foo") String header1,
				@Header("bar") String header2) {

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

		public void headerPropertiesPayloadMapAndStringHeader(@Headers Properties headers, Map payload,
				@Header("foo") String header) {

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
