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

package org.springframework.integration.handler;

import static org.junit.Assert.*;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.junit.Test;

import org.springframework.integration.annotation.Header;
import org.springframework.integration.annotation.Headers;
import org.springframework.integration.core.Message;
import org.springframework.integration.handler.ArgumentArrayMessageMapper;
import org.springframework.integration.message.MessageBuilder;
import org.springframework.integration.message.MessageHandlingException;
import org.springframework.integration.message.StringMessage;

/**
 * @author Mark Fisher
 * @author Iwein Fuld
 */
public class ArgumentArrayMessageMapperFromMessageTests {

	@Test
	public void fromMessageWithOptionalHeader() throws Exception {
		Method method = TestService.class.getMethod("optionalHeader", Integer.class);
		ArgumentArrayMessageMapper mapper = new ArgumentArrayMessageMapper(method);
		Object[] args = mapper.fromMessage(new StringMessage("foo"));
		assertEquals(1, args.length);
		assertNull(args[0]);
	}

	@Test(expected = MessageHandlingException.class)
	public void fromMessageWithRequiredHeaderNotProvided() throws Exception {
		Method method = TestService.class.getMethod("requiredHeader", Integer.class);
		ArgumentArrayMessageMapper mapper = new ArgumentArrayMessageMapper(method);
		mapper.fromMessage(new StringMessage("foo"));
	}

	@Test
	public void fromMessageWithRequiredHeaderProvided() throws Exception {
		Method method = TestService.class.getMethod("requiredHeader", Integer.class);
		ArgumentArrayMessageMapper mapper = new ArgumentArrayMessageMapper(method);
		Message<String> message = MessageBuilder.withPayload("foo")
				.setHeader("num", new Integer(123)).build(); 
		Object[] args = mapper.fromMessage(message);
		assertEquals(1, args.length);
		assertEquals(new Integer(123), args[0]);
	}

	@Test(expected = MessageHandlingException.class)
	public void fromMessageWithOptionalAndRequiredHeaderAndOnlyOptionalHeaderProvided() throws Exception {
		Method method = TestService.class.getMethod("optionalAndRequiredHeader", String.class, Integer.class);
		ArgumentArrayMessageMapper mapper = new ArgumentArrayMessageMapper(method);
		Message<String> message = MessageBuilder.withPayload("foo")
				.setHeader("prop", "bar").build();
		mapper.fromMessage(message);
	}

	@Test
	public void fromMessageWithOptionalAndRequiredHeaderAndOnlyRequiredHeaderProvided() throws Exception {
		Method method = TestService.class.getMethod("optionalAndRequiredHeader", String.class, Integer.class);
		ArgumentArrayMessageMapper mapper = new ArgumentArrayMessageMapper(method);
		Message<String> message = MessageBuilder.withPayload("foo")
				.setHeader("num", new Integer(123)).build(); 
		Object[] args = mapper.fromMessage(message);
		assertEquals(2, args.length);
		assertNull(args[0]);
		assertEquals(123, args[1]);
	}

	@Test
	public void fromMessageWithOptionalAndRequiredHeaderAndBothHeadersProvided() throws Exception {
		Method method = TestService.class.getMethod("optionalAndRequiredHeader", String.class, Integer.class);
		ArgumentArrayMessageMapper mapper = new ArgumentArrayMessageMapper(method);
		Message<String> message = MessageBuilder.withPayload("foo")
				.setHeader("num", new Integer(123))
				.setHeader("prop", "bar")
				.build(); 
		Object[] args = mapper.fromMessage(message);
		assertEquals(2, args.length);
		assertEquals("bar", args[0]);
		assertEquals(123, args[1]);
	}

	@Test
	public void fromMessageWithPropertiesMethodAndHeadersAnnotation() throws Exception {
		Method method = TestService.class.getMethod("propertiesHeaders", Properties.class);
		ArgumentArrayMessageMapper mapper = new ArgumentArrayMessageMapper(method);
		Message<String> message = MessageBuilder.withPayload("test")
				.setHeader("prop1", "foo").setHeader("prop2", "bar").build();
		Object[] args = mapper.fromMessage(message);
		Properties result = (Properties) args[0];
		assertEquals(2, result.size());
		assertEquals("foo", result.getProperty("prop1"));
		assertEquals("bar", result.getProperty("prop2"));
	}
	
	@Test
	public void fromMessageWithPropertiesAndObjectMethod() throws Exception {
		Method method = TestService.class.getMethod("propertiesHeadersAndPayload", Properties.class, Object.class);
		ArgumentArrayMessageMapper mapper = new ArgumentArrayMessageMapper(method);
		Message<String> message = MessageBuilder.withPayload("test")
		.setHeader("prop1", "foo").setHeader("prop2", "bar").build();
		Object[] args = mapper.fromMessage(message);
		Properties result = (Properties) args[0];
		assertEquals(2, result.size());
		assertEquals("foo", result.getProperty("prop1"));
		assertEquals("bar", result.getProperty("prop2"));
		assertEquals("test", args[1]);
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void fromMessageWithMapAndObjectMethod() throws Exception {
		Method method = TestService.class.getMethod("mapHeadersAndPayload", Map.class, Object.class);
		ArgumentArrayMessageMapper mapper = new ArgumentArrayMessageMapper(method);
		Message<String> message = MessageBuilder.withPayload("test")
		.setHeader("prop1", "foo").setHeader("prop2", "bar").build();
		Object[] args = mapper.fromMessage(message);
		Map result = (Map) args[0];
		//Map also contains id and timestamp
		assertEquals(4, result.size());
		assertEquals("foo", result.get("prop1"));
		assertEquals("bar", result.get("prop2"));
		assertEquals("test", args[1]);
	}

	@Test
	public void fromMessageWithPropertiesMethodAndPropertiesPayload() throws Exception {
		Method method = TestService.class.getMethod("propertiesPayload", Properties.class);
		ArgumentArrayMessageMapper mapper = new ArgumentArrayMessageMapper(method);
		Properties payload = new Properties();
		payload.setProperty("prop1", "foo");
		payload.setProperty("prop2", "bar");
		Message<Properties> message = MessageBuilder.withPayload(payload)
				.setHeader("prop1", "not").setHeader("prop2", "these").build();
		Object[] args = mapper.fromMessage(message);
		Properties result = (Properties) args[0];
		assertEquals(2, result.size());
		assertEquals("foo", result.getProperty("prop1"));
		assertEquals("bar", result.getProperty("prop2"));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void fromMessageWithMapMethodAndHeadersAnnotation() throws Exception {
		Method method = TestService.class.getMethod("mapHeaders", Map.class);
		ArgumentArrayMessageMapper mapper = new ArgumentArrayMessageMapper(method);
		Message<String> message = MessageBuilder.withPayload("test")
				.setHeader("attrib1", new Integer(123))
				.setHeader("attrib2", new Integer(456)).build();
		Object[] args = mapper.fromMessage(message);
		Map<String, Object> result = (Map<String, Object>) args[0];
		assertEquals(new Integer(123), result.get("attrib1"));
		assertEquals(new Integer(456), result.get("attrib2"));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void fromMessageWithMapMethodAndMapPayload() throws Exception {
		Method method = TestService.class.getMethod("mapPayload", Map.class);
		ArgumentArrayMessageMapper mapper = new ArgumentArrayMessageMapper(method);
		Map<String, Integer> payload = new HashMap<String, Integer>();
		payload.put("attrib1", new Integer(88));
		payload.put("attrib2", new Integer(99));
		Message<Map<String, Integer>> message = MessageBuilder.withPayload(payload)
				.setHeader("attrib1", new Integer(123))
				.setHeader("attrib2", new Integer(456)).build();
		Object[] args = mapper.fromMessage(message);
		Map<String, Integer> result = (Map<String, Integer>) args[0];
		assertEquals(2, result.size());
		assertEquals(new Integer(88), result.get("attrib1"));
		assertEquals(new Integer(99), result.get("attrib2"));
	}


	@SuppressWarnings("unused")
	private static class TestService {

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

		public Properties propertiesPayload(Properties properties) {
			return properties;
		}

		public Properties propertiesHeaders(@Headers Properties properties) {
			return properties;
		}

		public Object propertiesHeadersAndPayload(Properties headers, Object payload) {
			return payload;
		}

		@SuppressWarnings("unchecked")
		public Map mapPayload(Map map) {
			return map;
		}

		@SuppressWarnings("unchecked")
		public Map mapHeaders(@Headers Map map) {
			return map;
		}

		@SuppressWarnings("unchecked")
		public Object mapHeadersAndPayload(Map headers, Object payload) {
			return payload;
		}

		public Integer integerMethod(Integer i) {
			return i;
		}
	}

}
