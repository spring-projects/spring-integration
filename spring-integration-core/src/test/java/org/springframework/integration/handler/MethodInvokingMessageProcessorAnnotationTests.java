/*
 * Copyright 2002-2013 the original author or authors.
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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.MessagingException;
import org.springframework.integration.annotation.Header;
import org.springframework.integration.annotation.Headers;
import org.springframework.integration.annotation.Payload;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.integration.support.MessageBuilder;

/**
 * @author Mark Fisher
 * @author Iwein Fuld
 * @author Oleg Zhurakousky
 * @author Gary Russell
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class MethodInvokingMessageProcessorAnnotationTests {

	private final TestService testService = new TestService();

	private final Employee employee = new Employee("oleg", "zhurakousky");

	private static volatile int concurrencyFailures = 0;

	@Test
	public void multiThreadsUUIDToStringConversion() throws Exception {
		Method method = TestService.class.getMethod("headerId", String.class, String.class);
		final MethodInvokingMessageProcessor processor = new MethodInvokingMessageProcessor(testService, method);
		ExecutorService exec = Executors.newFixedThreadPool(100);
		processor.processMessage(new GenericMessage<String>("foo"));
		for (int i = 0; i < 100; i++) {
			exec.execute(new Runnable() {
				public void run() {
					Object result = processor.processMessage(new GenericMessage<String>("foo"));
					assertNotNull(result);
				}
			});
		}
		exec.shutdown();
		assertTrue(exec.awaitTermination(10, TimeUnit.SECONDS));
		assertEquals(0, concurrencyFailures);
	}

	@Test
	public void optionalHeader() throws Exception {
		Method method = TestService.class.getMethod("optionalHeader", Integer.class);
		MethodInvokingMessageProcessor processor = new MethodInvokingMessageProcessor(testService, method);
		Object result = processor.processMessage(new GenericMessage<String>("foo"));
		assertNull(result);
	}

	@Test(expected = MessageHandlingException.class)
	public void requiredHeaderNotProvided() throws Exception {
		Method method = TestService.class.getMethod("requiredHeader", Integer.class);
		MethodInvokingMessageProcessor processor = new MethodInvokingMessageProcessor(testService, method);
		processor.processMessage(new GenericMessage<String>("foo"));
	}

	@Test(expected = MessageHandlingException.class)
	public void requiredHeaderNotProvidedOnSecondMessage() throws Exception {
		Method method = TestService.class.getMethod("requiredHeader", Integer.class);
		MethodInvokingMessageProcessor processor = new MethodInvokingMessageProcessor(testService, method);
				Message<String> messageWithHeader = MessageBuilder.withPayload("foo")
				.setHeader("num", new Integer(123)).build();
		GenericMessage<String> messageWithoutHeader = new GenericMessage<String>("foo");

		processor.processMessage(messageWithHeader);
		processor.processMessage(messageWithoutHeader);
	}

	@Test
	public void fromMessageWithRequiredHeaderProvided() throws Exception {
		Method method = TestService.class.getMethod("requiredHeader", Integer.class);
		Message<String> message = MessageBuilder.withPayload("foo")
				.setHeader("num", new Integer(123)).build();
		MethodInvokingMessageProcessor processor = new MethodInvokingMessageProcessor(testService, method);
		Object result = processor.processMessage(message);
		assertEquals(new Integer(123), result);
	}

	@Test(expected = MessageHandlingException.class)
	public void fromMessageWithOptionalAndRequiredHeaderAndOnlyOptionalHeaderProvided() throws Exception {
		Method method = TestService.class.getMethod("optionalAndRequiredHeader", String.class, Integer.class);
		MethodInvokingMessageProcessor processor = new MethodInvokingMessageProcessor(testService, method);
		Message<String> message = MessageBuilder.withPayload("foo")
				.setHeader("prop", "bar").build();
		processor.processMessage(message);
	}

	@Test
	public void fromMessageWithOptionalAndRequiredHeaderAndOnlyRequiredHeaderProvided() throws Exception {
		Method method = TestService.class.getMethod("optionalAndRequiredHeader", String.class, Integer.class);
		MethodInvokingMessageProcessor processor = new MethodInvokingMessageProcessor(testService, method);
		Message<String> message = MessageBuilder.withPayload("foo")
				.setHeader("num", new Integer(123)).build();
		Object result = processor.processMessage(message);
		assertEquals("null123", result);
	}

	@Test
	public void fromMessageWithOptionalAndRequiredHeaderAndBothHeadersProvided() throws Exception {
		Method method = TestService.class.getMethod("optionalAndRequiredHeader", String.class, Integer.class);
		MethodInvokingMessageProcessor processor = new MethodInvokingMessageProcessor(testService, method);
		Message<String> message = MessageBuilder.withPayload("foo")
				.setHeader("num", new Integer(123))
				.setHeader("prop", "bar")
				.build();
		Object result = processor.processMessage(message);
		assertEquals("bar123", result);
	}

	@Test
	public void fromMessageWithPropertiesMethodAndHeadersAnnotation() throws Exception {
		Method method = TestService.class.getMethod("propertiesHeaders", Properties.class);
		MethodInvokingMessageProcessor processor = new MethodInvokingMessageProcessor(testService, method);
		Message<String> message = MessageBuilder.withPayload("test")
				.setHeader("prop1", "foo").setHeader("prop2", "bar").build();
		Object result = processor.processMessage(message);
		Properties props = (Properties) result;
		assertEquals("foo", props.getProperty("prop1"));
		assertEquals("bar", props.getProperty("prop2"));
	}

	@Test
	public void fromMessageWithPropertiesAndObjectMethod() throws Exception {
		Method method = TestService.class.getMethod("propertiesHeadersAndPayload", Properties.class, Object.class);
		MethodInvokingMessageProcessor processor = new MethodInvokingMessageProcessor(testService, method);
		Message<String> message = MessageBuilder.withPayload("test")
				.setHeader("prop1", "foo").setHeader("prop2", "bar").build();
		Object result = processor.processMessage(message);
		Properties props = (Properties) result;
		assertEquals("foo", props.getProperty("prop1"));
		assertEquals("bar", props.getProperty("prop2"));
		assertEquals("test", props.getProperty("payload"));
	}

	@Test
	public void fromMessageWithMapAndObjectMethod() throws Exception {
		Method method = TestService.class.getMethod("mapHeadersAndPayload", Map.class, Object.class);
		MethodInvokingMessageProcessor processor = new MethodInvokingMessageProcessor(testService, method);
		Message<String> message = MessageBuilder.withPayload("test")
				.setHeader("prop1", "foo").setHeader("prop2", "bar").build();
		Map<?, ?> result = (Map<?, ?>) processor.processMessage(message);
		assertEquals(5, result.size());
		assertTrue(result.containsKey(MessageHeaders.ID));
		assertTrue(result.containsKey(MessageHeaders.TIMESTAMP));
		assertEquals("foo", result.get("prop1"));
		assertEquals("bar", result.get("prop2"));
		assertEquals("test", result.get("payload"));
	}

	@Test
	public void fromMessageWithPropertiesMethodAndPropertiesPayload() throws Exception {
		Method method = TestService.class.getMethod("propertiesPayload", Properties.class);
		MethodInvokingMessageProcessor processor = new MethodInvokingMessageProcessor(testService, method);
		Properties payload = new Properties();
		payload.setProperty("prop1", "foo");
		payload.setProperty("prop2", "bar");
		Message<Properties> message = MessageBuilder.withPayload(payload)
				.setHeader("prop1", "not").setHeader("prop2", "these").build();
		Properties result = (Properties) processor.processMessage(message);
		assertEquals(2, result.size());
		assertEquals("foo", result.getProperty("prop1"));
		assertEquals("bar", result.getProperty("prop2"));
	}

	@Test
	public void fromMessageWithMapMethodAndHeadersAnnotation() throws Exception {
		Method method = TestService.class.getMethod("mapHeaders", Map.class);
		MethodInvokingMessageProcessor processor = new MethodInvokingMessageProcessor(testService, method);
		Message<String> message = MessageBuilder.withPayload("test")
				.setHeader("attrib1", new Integer(123))
				.setHeader("attrib2", new Integer(456)).build();
		Map<String, Object> result = (Map<String, Object>) processor.processMessage(message);
		assertEquals(new Integer(123), result.get("attrib1"));
		assertEquals(new Integer(456), result.get("attrib2"));
	}

	@Test
	public void fromMessageWithMapMethodAndMapPayload() throws Exception {
		Method method = TestService.class.getMethod("mapPayload", Map.class);
		MethodInvokingMessageProcessor processor = new MethodInvokingMessageProcessor(testService, method);
		Map<String, Integer> payload = new HashMap<String, Integer>();
		payload.put("attrib1", new Integer(88));
		payload.put("attrib2", new Integer(99));
		Message<Map<String, Integer>> message = MessageBuilder.withPayload(payload)
				.setHeader("attrib1", new Integer(123))
				.setHeader("attrib2", new Integer(456)).build();
		Map<String, Integer> result = (Map<String, Integer>) processor.processMessage(message);
		assertEquals(2, result.size());
		assertEquals(new Integer(88), result.get("attrib1"));
		assertEquals(new Integer(99), result.get("attrib2"));
	}

	@Test
	public void headerAnnotationWithExpression() throws Exception {
		Message<?> message = this.getMessage();
		Method method = TestService.class.getMethod("headerAnnotationWithExpression", String.class);
		MethodInvokingMessageProcessor processor = new MethodInvokingMessageProcessor(testService, method);
		Object result = processor.processMessage(message);
		Assert.assertEquals("monday", result);
	}

	@Test
	public void irrelevantAnnotation() throws Exception {
		Message<?> message = MessageBuilder.withPayload("foo").build();
		Method method = TestService.class.getMethod("irrelevantAnnotation", String.class);
		MethodInvokingMessageProcessor processor = new MethodInvokingMessageProcessor(testService, method);
		Object result = processor.processMessage(message);
		assertEquals("foo", result);
	}

	@Test
	public void multipleAnnotatedArgs() throws Exception {
		Message<?> message = this.getMessage();
		Method method = TestService.class.getMethod("multipleAnnotatedArguments",
													String.class,
													String.class,
													Employee.class,
													String.class,
													Map.class);
		MethodInvokingMessageProcessor processor = new MethodInvokingMessageProcessor(testService, method);
		Object[] parameters = (Object[]) processor.processMessage(message);
		Assert.assertNotNull(parameters);
		Assert.assertTrue(parameters.length == 5);
		Assert.assertTrue(parameters[0].equals("monday"));
		Assert.assertTrue(parameters[1].equals("September"));
		Assert.assertTrue(parameters[2].equals(employee));
		Assert.assertTrue(parameters[3].equals("oleg"));
		Assert.assertTrue(parameters[4] instanceof Map);
	}

	@Test
	public void fromMessageToPayload() throws Exception {
		Method method = TestService.class.getMethod("mapOnly", Map.class);
		MethodInvokingMessageProcessor processor = new MethodInvokingMessageProcessor(testService, method);
		Message<Employee> message = MessageBuilder.withPayload(employee).setHeader("number", "jkl").build();
		Object result = processor.processMessage(message);
		Assert.assertTrue(result instanceof Map);
		Assert.assertEquals("jkl", ((Map<?, ?>) result).get("number"));
	}

	@Test
	public void fromMessageToPayloadArg() throws Exception {
		Method method = TestService.class.getMethod("payloadAnnotationFirstName", String.class);
		MethodInvokingMessageProcessor processor = new MethodInvokingMessageProcessor(testService, method);
		Message<Employee> message = MessageBuilder.withPayload(employee).setHeader("number", "jkl").build();
		Object result = processor.processMessage(message);
		Assert.assertTrue(result instanceof String);
		Assert.assertEquals("oleg", result);
	}

	@Test
	public void fromMessageToPayloadArgs() throws Exception {
		Method method = TestService.class.getMethod("payloadAnnotationFullName", String.class, String.class);
		MethodInvokingMessageProcessor processor = new MethodInvokingMessageProcessor(testService, method);
		Message<Employee> message = MessageBuilder.withPayload(employee).setHeader("number", "jkl").build();
		Object result = processor.processMessage(message);
		Assert.assertEquals("oleg zhurakousky", result);
	}

	@Test
	public void fromMessageToPayloadArgsHeaderArgs() throws Exception {
		Method method = TestService.class.getMethod("payloadArgAndHeaderArg", String.class, String.class);
		MethodInvokingMessageProcessor processor = new MethodInvokingMessageProcessor(testService, method);
		Message<Employee> message = MessageBuilder.withPayload(employee).setHeader("day", "monday").build();
		Object result = processor.processMessage(message);
		Assert.assertEquals("olegmonday", result);
	}

	@Test(expected = MessagingException.class)
	public void fromMessageInvalidMethodWithMultipleMappingAnnotations() throws Exception {
		Method method = MultipleMappingAnnotationTestBean.class.getMethod("test", String.class);
		MethodInvokingMessageProcessor processor = new MethodInvokingMessageProcessor(testService, method);
		Message<?> message = MessageBuilder.withPayload("payload").setHeader("foo", "bar").build();
		processor.processMessage(message);
	}

	@Test
	public void fromMessageToHeadersWithExpressions() throws Exception {
		Method method = TestService.class.getMethod("headersWithExpressions", String.class, String.class);
		MethodInvokingMessageProcessor processor = new MethodInvokingMessageProcessor(testService, method);
		Employee employee = new Employee("John", "Doe");
		Message<?> message = MessageBuilder.withPayload("payload").setHeader("emp", employee).build();
		Object result = processor.processMessage(message);
		assertEquals("DOE, John", result);
	}

	@Test
	public void fromMessageToHyphenatedHeaderName() throws Exception {
		Method method = TestService.class.getMethod("headerNameWithHyphen", String.class);
		MethodInvokingMessageProcessor processor = new MethodInvokingMessageProcessor(testService, method);
		Message<?> message = MessageBuilder.withPayload("payload").setHeader("foo-bar", "abc").build();
		Object result = processor.processMessage(message);
		assertEquals("ABC", result);
	}


	@SuppressWarnings("unused")
	private static class MultipleMappingAnnotationTestBean {
		public void test(@Payload("payload") @Header("foo")  String s) {
		}
	}


	@SuppressWarnings("unused")
	private static class TestService {

		private final Log logger = LogFactory.getLog(this.getClass());

		public Map<?,?> mapOnly(Map<?,?> map) {
			return map;
		}

		public String payloadAnnotationFirstName(@Payload("fname") String fname) {
			return fname;
		}

		public String payloadAnnotationFullName(@Payload("fname") String first, @Payload("lname") String last) {
			return first + " " + last;
		}

		public String payloadArgAndHeaderArg(@Payload("fname") String fname, @Header String day) {
			return fname + day;
		}

		public Integer optionalHeader(@Header(required=false) Integer num) {
			return num;
		}

		public Integer requiredHeader(@Header(value="num", required=true) Integer num) {
			return num;
		}

		public String headersWithExpressions(@Header("emp.fname") String firstName,
				@Header("emp.lname.toUpperCase()") String lastName) {
			return lastName + ", " + firstName;
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
			headers.put("payload", payload);
			return headers;
		}

		public Map<?, ?> mapPayload(Map<?, ?> map) {
			return map;
		}

		public Map<?, ?> mapHeaders(@Headers Map<?, ?> map) {
			return map;
		}

		public Object mapHeadersAndPayload(Map<String, Object> headers, Object payload) {
			Map<String, Object> map = new HashMap<String, Object>(headers);
			map.put("payload", payload);
			return map;
		}

		public Integer integerMethod(Integer i) {
			return i;
		}

		public String headerAnnotationWithExpression(@Header("day") String value) {
			return value;
		}

		public Object[] multipleAnnotatedArguments(@Header("day") String argA,
											   @Header("month") String argB,
											   @Payload Employee payloadArg,
											   @Payload("fname") String value,
											   @Headers Map<?,?> headers) {
			return new Object[] { argA, argB, payloadArg, value, headers };
		}

		public String irrelevantAnnotation(@BogusAnnotation() String value) {
			return value;
		}

		public String headerNameWithHyphen(@Header("foo-bar") String foobar) {
			return foobar.toUpperCase();
		}

		Set<String> ids = Collections.synchronizedSet(new HashSet<String>());

		public String headerId(String payload, @Header("id") String id) {
			logger.debug(id);
			if (ids.contains(id)) {
				concurrencyFailures++;
			}
			ids.add(id);
			return "foo";
		}
	}

	private Message<?> getMessage() {
		MessageBuilder<Employee> builder = MessageBuilder.withPayload(employee);
		builder.setHeader("day", "monday");
		builder.setHeader("month", "September");
		Message<Employee> message = builder.build();
		return message;
	}


	public static class Employee {

		private String fname;

		private String lname;

		public Employee(String fname, String lname) {
			this.fname = fname;
			this.lname = lname;
		}

		public String getFname() {
			return fname;
		}

		public String getLname() {
			return lname;
		}
	}

}
