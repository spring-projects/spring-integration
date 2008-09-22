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
import static org.junit.Assert.assertNull;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.junit.Test;

import org.springframework.integration.annotation.Header;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageBuilder;
import org.springframework.integration.message.MessageHandlingException;
import org.springframework.integration.message.MessageMappingParameterResolver;
import org.springframework.integration.message.StringMessage;

/**
 * @author Mark Fisher
 */
public class MessageMappingParameterResolverTests {

	@Test
	public void optionalHeader() throws Exception {
		Method method = TestService.class.getMethod("optionalHeader", Integer.class);
		MessageMappingParameterResolver resolver = new MessageMappingParameterResolver(method);
		Object[] args = resolver.resolveParameters(new StringMessage("foo"));
		assertEquals(1, args.length);
		assertNull(args[0]);
	}

	@Test(expected = MessageHandlingException.class)
	public void tequiredHeaderNotProvided() throws Exception {
		Method method = TestService.class.getMethod("requiredHeader", Integer.class);
		MessageMappingParameterResolver resolver = new MessageMappingParameterResolver(method);
		resolver.resolveParameters(new StringMessage("foo"));
	}

	@Test
	public void requiredHeaderProvided() throws Exception {
		Method method = TestService.class.getMethod("requiredHeader", Integer.class);
		MessageMappingParameterResolver resolver = new MessageMappingParameterResolver(method);
		Message<String> message = MessageBuilder.withPayload("foo")
				.setHeader("num", new Integer(123)).build(); 
		Object[] args = resolver.resolveParameters(message);
		assertEquals(1, args.length);
		assertEquals(new Integer(123), args[0]);
	}

	@Test(expected = MessageHandlingException.class)
	public void optionalAndRequiredHeaderWithOnlyOptionalHeaderProvided() throws Exception {
		Method method = TestService.class.getMethod("optionalAndRequiredHeader", String.class, Integer.class);
		MessageMappingParameterResolver resolver = new MessageMappingParameterResolver(method);
		Message<String> message = MessageBuilder.withPayload("foo")
				.setHeader("prop", "bar").build();
		resolver.resolveParameters(message);
	}

	@Test
	public void optionalAndRequiredHeaderWithOnlyRequiredHeaderProvided() throws Exception {
		Method method = TestService.class.getMethod("optionalAndRequiredHeader", String.class, Integer.class);
		MessageMappingParameterResolver resolver = new MessageMappingParameterResolver(method);
		Message<String> message = MessageBuilder.withPayload("foo")
				.setHeader("num", new Integer(123)).build(); 
		Object[] args = resolver.resolveParameters(message);
		assertEquals(2, args.length);
		assertNull(args[0]);
		assertEquals(123, args[1]);
	}

	@Test
	public void optionalAndRequiredHeaderWithBothHeadersProvided() throws Exception {
		Method method = TestService.class.getMethod("optionalAndRequiredHeader", String.class, Integer.class);
		MessageMappingParameterResolver resolver = new MessageMappingParameterResolver(method);
		Message<String> message = MessageBuilder.withPayload("foo")
				.setHeader("num", new Integer(123))
				.setHeader("prop", "bar")
				.build(); 
		Object[] args = resolver.resolveParameters(message);
		assertEquals(2, args.length);
		assertEquals("bar", args[0]);
		assertEquals(123, args[1]);
	}

	@Test
	public void propertiesMethodWithNonPropertiesPayload() throws Exception {
		Method method = TestService.class.getMethod("propertiesMethod", Properties.class);
		MessageMappingParameterResolver resolver = new MessageMappingParameterResolver(method);
		Message<String> message = MessageBuilder.withPayload("test")
				.setHeader("prop1", "foo").setHeader("prop2", "bar").build();
		Object[] args = resolver.resolveParameters(message);
		Properties result = (Properties) args[0];
		assertEquals(2, result.size());
		assertEquals("foo", result.getProperty("prop1"));
		assertEquals("bar", result.getProperty("prop2"));
	}

	@Test
	public void propertiesMethodWithPropertiesPayload() throws Exception {
		Method method = TestService.class.getMethod("propertiesMethod", Properties.class);
		MessageMappingParameterResolver resolver = new MessageMappingParameterResolver(method);
		Properties payload = new Properties();
		payload.setProperty("prop1", "foo");
		payload.setProperty("prop2", "bar");
		Message<Properties> message = MessageBuilder.withPayload(payload)
				.setHeader("prop1", "not").setHeader("prop2", "these").build();
		Object[] args = resolver.resolveParameters(message);
		Properties result = (Properties) args[0];
		assertEquals(2, result.size());
		assertEquals("foo", result.getProperty("prop1"));
		assertEquals("bar", result.getProperty("prop2"));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testMapMethodWithNonMapPayload() throws Exception {
		Method method = TestService.class.getMethod("mapMethod", Map.class);
		MessageMappingParameterResolver resolver = new MessageMappingParameterResolver(method);
		Message<String> message = MessageBuilder.withPayload("test")
				.setHeader("attrib1", new Integer(123))
				.setHeader("attrib2", new Integer(456)).build();
		Object[] args = resolver.resolveParameters(message);
		Map<String, Object> result = (Map<String, Object>) args[0];
		assertEquals(new Integer(123), result.get("attrib1"));
		assertEquals(new Integer(456), result.get("attrib2"));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testMapMethodWithMapPayload() throws Exception {
		Method method = TestService.class.getMethod("mapMethod", Map.class);
		MessageMappingParameterResolver resolver = new MessageMappingParameterResolver(method);
		Map<String, Integer> payload = new HashMap<String, Integer>();
		payload.put("attrib1", new Integer(123));
		payload.put("attrib2", new Integer(456));
		Message<Map<String, Integer>> message = MessageBuilder.withPayload(payload)
				.setHeader("attrib1", new Integer(123))
				.setHeader("attrib2", new Integer(456)).build();
		Object[] args = resolver.resolveParameters(message);
		Map<String, Integer> result = (Map<String, Integer>) args[0];
		assertEquals(2, result.size());
		assertEquals(new Integer(123), result.get("attrib1"));
		assertEquals(new Integer(456), result.get("attrib2"));
	}


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
