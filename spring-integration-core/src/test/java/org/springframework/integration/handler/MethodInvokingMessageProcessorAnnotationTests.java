/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.integration.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;

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
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.support.GenericMessage;

/**
 * @author Mark Fisher
 * @author Iwein Fuld
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
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
		processor.setBeanFactory(mock(BeanFactory.class));
		ExecutorService exec = Executors.newFixedThreadPool(100);
		processor.processMessage(new GenericMessage<>("foo"));
		for (int i = 0; i < 100; i++) {
			exec.execute(() -> assertThat(processor.processMessage(new GenericMessage<>("foo"))).isNotNull());
		}
		exec.shutdown();
		assertThat(exec.awaitTermination(20, TimeUnit.SECONDS)).isTrue();
		assertThat(concurrencyFailures).isEqualTo(0);
	}

	@Test
	public void optionalHeader() throws Exception {
		Method method = TestService.class.getMethod("optionalHeader", Integer.class);
		MethodInvokingMessageProcessor processor = new MethodInvokingMessageProcessor(testService, method);
		processor.setBeanFactory(mock(BeanFactory.class));
		Object result = processor.processMessage(new GenericMessage<>("foo"));
		assertThat(result).isNull();
	}

	@Test
	public void requiredHeaderNotProvided() throws Exception {
		Method method = TestService.class.getMethod("requiredHeader", Integer.class);
		MethodInvokingMessageProcessor processor = new MethodInvokingMessageProcessor(testService, method);
		processor.setBeanFactory(mock(BeanFactory.class));
		assertThatExceptionOfType(MessageHandlingException.class)
				.isThrownBy(() -> processor.processMessage(new GenericMessage<>("foo")));
	}

	@Test
	public void requiredHeaderNotProvidedOnSecondMessage() throws Exception {
		Method method = TestService.class.getMethod("requiredHeader", Integer.class);
		MethodInvokingMessageProcessor processor = new MethodInvokingMessageProcessor(testService, method);
		processor.setBeanFactory(mock(BeanFactory.class));
		Message<String> messageWithHeader = MessageBuilder.withPayload("foo")
				.setHeader("num", 123).build();
		GenericMessage<String> messageWithoutHeader = new GenericMessage<>("foo");

		processor.processMessage(messageWithHeader);
		assertThatExceptionOfType(MessageHandlingException.class)
				.isThrownBy(() -> processor.processMessage(messageWithoutHeader));
	}

	@Test
	public void fromMessageWithRequiredHeaderProvided() throws Exception {
		Method method = TestService.class.getMethod("requiredHeader", Integer.class);
		Message<String> message = MessageBuilder.withPayload("foo")
				.setHeader("num", 123).build();
		MethodInvokingMessageProcessor processor = new MethodInvokingMessageProcessor(testService, method);
		processor.setBeanFactory(mock(BeanFactory.class));
		Object result = processor.processMessage(message);
		assertThat(result).isEqualTo(123);
	}

	@Test
	public void fromMessageWithOptionalAndRequiredHeaderAndOnlyOptionalHeaderProvided() throws Exception {
		Method method = TestService.class.getMethod("optionalAndRequiredHeader", String.class, Integer.class);
		MethodInvokingMessageProcessor processor = new MethodInvokingMessageProcessor(testService, method);
		processor.setBeanFactory(mock(BeanFactory.class));
		Message<String> message = MessageBuilder.withPayload("foo")
				.setHeader("prop", "bar").build();

		assertThatExceptionOfType(MessageHandlingException.class)
				.isThrownBy(() -> processor.processMessage(message));
	}

	@Test
	public void fromMessageWithOptionalAndRequiredHeaderAndOnlyRequiredHeaderProvided() throws Exception {
		Method method = TestService.class.getMethod("optionalAndRequiredHeader", String.class, Integer.class);
		MethodInvokingMessageProcessor processor = new MethodInvokingMessageProcessor(testService, method);
		processor.setBeanFactory(mock(BeanFactory.class));
		Message<String> message = MessageBuilder.withPayload("foo")
				.setHeader("num", 123).build();
		Object result = processor.processMessage(message);
		assertThat(result).isEqualTo("null123");
	}

	@Test
	public void fromMessageWithOptionalAndRequiredHeaderAndBothHeadersProvided() throws Exception {
		Method method = TestService.class.getMethod("optionalAndRequiredHeader", String.class, Integer.class);
		MethodInvokingMessageProcessor processor = new MethodInvokingMessageProcessor(testService, method);
		processor.setBeanFactory(mock(BeanFactory.class));
		Message<String> message = MessageBuilder.withPayload("foo")
				.setHeader("num", 123)
				.setHeader("prop", "bar")
				.build();
		Object result = processor.processMessage(message);
		assertThat(result).isEqualTo("bar123");
	}

	@Test
	public void fromMessageWithPropertiesMethodAndHeadersAnnotation() throws Exception {
		Method method = TestService.class.getMethod("propertiesHeaders", Properties.class);
		MethodInvokingMessageProcessor processor = new MethodInvokingMessageProcessor(testService, method);
		processor.setBeanFactory(mock(BeanFactory.class));
		Message<String> message = MessageBuilder.withPayload("test")
				.setHeader("prop1", "foo").setHeader("prop2", "bar").build();
		assertThat(TestUtils.getPropertyValue(processor, "delegate.handlerMethod.spelOnly", Boolean.class)).isFalse();
		for (int i = 0; i < 99; i++) {
			Object result = processor.processMessage(message);
			Properties props = (Properties) result;
			assertThat(props.getProperty("prop1")).isEqualTo("foo");
			assertThat(props.getProperty("prop2")).isEqualTo("bar");
			assertThat(TestUtils.getPropertyValue(processor, "delegate.handlerMethod.spelOnly", Boolean.class))
					.isFalse();
		}

		Object result = processor.processMessage(message);
		Properties props = (Properties) result;
		assertThat(props.getProperty("prop1")).isEqualTo("foo");
		assertThat(props.getProperty("prop2")).isEqualTo("bar");

		assertThat(TestUtils.getPropertyValue(processor, "delegate.handlerMethod.spelOnly", Boolean.class)).isTrue();
	}

	@Test
	public void fromMessageWithPropertiesAndObjectMethod() throws Exception {
		Method method = TestService.class.getMethod("propertiesHeadersAndPayload", Properties.class, Object.class);
		MethodInvokingMessageProcessor processor = new MethodInvokingMessageProcessor(testService, method);
		processor.setBeanFactory(mock(BeanFactory.class));
		Message<String> message = MessageBuilder.withPayload("test")
				.setHeader("prop1", "foo").setHeader("prop2", "bar").build();
		Object result = processor.processMessage(message);
		Properties props = (Properties) result;
		assertThat(props.getProperty("prop1")).isEqualTo("foo");
		assertThat(props.getProperty("prop2")).isEqualTo("bar");
		assertThat(props.getProperty("payload")).isEqualTo("test");
	}

	@Test
	public void fromMessageWithMapAndObjectMethod() throws Exception {
		Method method = TestService.class.getMethod("mapHeadersAndPayload", Map.class, Object.class);
		MethodInvokingMessageProcessor processor = new MethodInvokingMessageProcessor(testService, method);
		processor.setBeanFactory(mock(BeanFactory.class));
		Message<String> message = MessageBuilder.withPayload("test")
				.setHeader("prop1", "foo").setHeader("prop2", "bar").build();
		Map<?, ?> result = (Map<?, ?>) processor.processMessage(message);
		assertThat(result.size()).isEqualTo(5);
		assertThat(result.containsKey(MessageHeaders.ID)).isTrue();
		assertThat(result.containsKey(MessageHeaders.TIMESTAMP)).isTrue();
		assertThat(result.get("prop1")).isEqualTo("foo");
		assertThat(result.get("prop2")).isEqualTo("bar");
		assertThat(result.get("payload")).isEqualTo("test");
	}

	@Test
	public void fromMessageWithPropertiesMethodAndPropertiesPayload() throws Exception {
		Method method = TestService.class.getMethod("propertiesPayload", Properties.class);
		MethodInvokingMessageProcessor processor = new MethodInvokingMessageProcessor(testService, method);
		processor.setBeanFactory(mock(BeanFactory.class));
		Properties payload = new Properties();
		payload.setProperty("prop1", "foo");
		payload.setProperty("prop2", "bar");
		Message<Properties> message = MessageBuilder.withPayload(payload)
				.setHeader("prop1", "not").setHeader("prop2", "these").build();
		Properties result = (Properties) processor.processMessage(message);
		assertThat(result.size()).isEqualTo(2);
		assertThat(result.getProperty("prop1")).isEqualTo("foo");
		assertThat(result.getProperty("prop2")).isEqualTo("bar");
	}

	@Test
	public void fromMessageWithMapMethodAndHeadersAnnotation() throws Exception {
		Method method = TestService.class.getMethod("mapHeaders", Map.class);
		MethodInvokingMessageProcessor processor = new MethodInvokingMessageProcessor(testService, method);
		processor.setBeanFactory(mock(BeanFactory.class));
		Message<String> message = MessageBuilder.withPayload("test")
				.setHeader("attrib1", 123)
				.setHeader("attrib2", 456).build();
		Map<String, Object> result = (Map<String, Object>) processor.processMessage(message);
		assertThat(result.get("attrib1")).isEqualTo(123);
		assertThat(result.get("attrib2")).isEqualTo(456);
	}

	@Test
	public void fromMessageWithMapMethodAndMapPayload() throws Exception {
		Method method = TestService.class.getMethod("mapPayload", Map.class);
		MethodInvokingMessageProcessor processor = new MethodInvokingMessageProcessor(testService, method);
		processor.setBeanFactory(mock(BeanFactory.class));
		Map<String, Integer> payload = new HashMap<>();
		payload.put("attrib1", 88);
		payload.put("attrib2", 99);
		Message<Map<String, Integer>> message = MessageBuilder.withPayload(payload)
				.setHeader("attrib1", 123)
				.setHeader("attrib2", 456).build();
		Map<String, Integer> result = (Map<String, Integer>) processor.processMessage(message);
		assertThat(result.size()).isEqualTo(2);
		assertThat(result.get("attrib1")).isEqualTo(88);
		assertThat(result.get("attrib2")).isEqualTo(99);
	}

	@Test
	public void headerAnnotationWithExpression() throws Exception {
		Message<?> message = this.getMessage();
		Method method = TestService.class.getMethod("headerAnnotationWithExpression", String.class);
		MethodInvokingMessageProcessor processor = new MethodInvokingMessageProcessor(testService, method);
		processor.setBeanFactory(mock(BeanFactory.class));
		Object result = processor.processMessage(message);
		assertThat(result).isEqualTo("monday");
	}

	@Test
	public void irrelevantAnnotation() throws Exception {
		Message<?> message = MessageBuilder.withPayload("foo").build();
		Method method = TestService.class.getMethod("irrelevantAnnotation", String.class);
		MethodInvokingMessageProcessor processor = new MethodInvokingMessageProcessor(testService, method);
		processor.setBeanFactory(mock(BeanFactory.class));
		Object result = processor.processMessage(message);
		assertThat(result).isEqualTo("foo");
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
		processor.setBeanFactory(mock(BeanFactory.class));
		Object[] parameters = (Object[]) processor.processMessage(message);
		assertThat(parameters).isNotNull();
		assertThat(parameters.length).isEqualTo(5);
		assertThat(parameters[0]).isEqualTo("monday");
		assertThat(parameters[1]).isEqualTo("September");
		assertThat(employee).isEqualTo(parameters[2]);
		assertThat(parameters[3]).isEqualTo("oleg");
		assertThat(parameters[4] instanceof Map).isTrue();
	}

	@Test
	public void fromMessageToPayload() throws Exception {
		Method method = TestService.class.getMethod("mapOnly", Map.class);
		MethodInvokingMessageProcessor processor = new MethodInvokingMessageProcessor(testService, method);
		processor.setBeanFactory(mock(BeanFactory.class));
		Message<Employee> message = MessageBuilder.withPayload(employee).setHeader("number", "jkl").build();
		Object result = processor.processMessage(message);
		assertThat(result instanceof Map).isTrue();
		assertThat(((Map<?, ?>) result).get("number")).isEqualTo("jkl");
	}

	@Test
	public void fromMessageToPayloadArg() throws Exception {
		Method method = TestService.class.getMethod("payloadAnnotationFirstName", String.class);
		MethodInvokingMessageProcessor processor = new MethodInvokingMessageProcessor(testService, method);
		processor.setBeanFactory(mock(BeanFactory.class));
		Message<Employee> message = MessageBuilder.withPayload(employee).setHeader("number", "jkl").build();
		Object result = processor.processMessage(message);
		assertThat(result instanceof String).isTrue();
		assertThat(result).isEqualTo("oleg");
	}

	@Test
	public void fromMessageToPayloadArgs() throws Exception {
		Method method = TestService.class.getMethod("payloadAnnotationFullName", String.class, String.class);
		MethodInvokingMessageProcessor processor = new MethodInvokingMessageProcessor(testService, method);
		processor.setBeanFactory(mock(BeanFactory.class));
		Message<Employee> message = MessageBuilder.withPayload(employee).setHeader("number", "jkl").build();
		Object result = processor.processMessage(message);
		assertThat(result).isEqualTo("oleg zhurakousky");
	}

	@Test
	public void fromMessageToPayloadArgsHeaderArgs() throws Exception {
		Method method = TestService.class.getMethod("payloadArgAndHeaderArg", String.class, String.class);
		MethodInvokingMessageProcessor processor = new MethodInvokingMessageProcessor(testService, method);
		processor.setBeanFactory(mock(BeanFactory.class));
		Message<Employee> message = MessageBuilder.withPayload(employee).setHeader("day", "monday").build();
		Object result = processor.processMessage(message);
		assertThat(result).isEqualTo("olegmonday");
	}

	@Test
	public void fromMessageInvalidMethodWithMultipleMappingAnnotations() throws Exception {
		Method method = MultipleMappingAnnotationTestBean.class.getMethod("test", String.class);
		assertThatExceptionOfType(MessagingException.class)
				.isThrownBy(() -> new MethodInvokingMessageProcessor(testService, method));
	}

	@Test
	public void fromMessageToHeadersWithExpressions() throws Exception {
		Method method = TestService.class.getMethod("headersWithExpressions", String.class, String.class);
		MethodInvokingMessageProcessor processor = new MethodInvokingMessageProcessor(testService, method);
		processor.setBeanFactory(mock(BeanFactory.class));
		Employee employee = new Employee("John", "Doe");
		Message<?> message = MessageBuilder.withPayload("payload").setHeader("emp", employee).build();
		Object result = processor.processMessage(message);
		assertThat(result).isEqualTo("DOE, John");
	}

	@Test
	public void fromMessageToHyphenatedHeaderName() throws Exception {
		Method method = TestService.class.getMethod("headerNameWithHyphen", String.class);
		MethodInvokingMessageProcessor processor = new MethodInvokingMessageProcessor(testService, method);
		processor.setBeanFactory(mock(BeanFactory.class));
		Message<?> message = MessageBuilder.withPayload("payload").setHeader("foo-bar", "abc").build();
		Object result = processor.processMessage(message);
		assertThat(result).isEqualTo("ABC");
	}


	private Message<?> getMessage() {
		MessageBuilder<Employee> builder = MessageBuilder.withPayload(employee);
		builder.setHeader("day", "monday");
		builder.setHeader("month", "September");
		return builder.build();
	}

	@SuppressWarnings("unused")
	private static class MultipleMappingAnnotationTestBean {

		public void test(@Payload("payload") @Header("foo") String s) {
		}

	}


	@SuppressWarnings("unused")
	private static class TestService {

		private final Log logger = LogFactory.getLog(this.getClass());

		public Map<?, ?> mapOnly(Map<?, ?> map) {
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

		public Integer optionalHeader(@Header(required = false) Integer num) {
			return num;
		}

		public Integer requiredHeader(@Header(value = "num", required = true) Integer num) {
			return num;
		}

		public String headersWithExpressions(@Header("emp.fname") String firstName,
				@Header("emp.lname.toUpperCase()") String lastName) {
			return lastName + ", " + firstName;
		}

		public String optionalAndRequiredHeader(@Header(required = false) String prop,
				@Header(value = "num", required = true) Integer num) {
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
				@Headers Map<?, ?> headers) {
			return new Object[]{ argA, argB, payloadArg, value, headers };
		}

		public String irrelevantAnnotation(@BogusAnnotation String value) {
			return value;
		}

		public String headerNameWithHyphen(@Header("foo-bar") String foobar) {
			return foobar.toUpperCase();
		}

		Set<String> ids = Collections.synchronizedSet(new HashSet<>());

		public String headerId(String payload, @Header("id") String id) {
			logger.debug(id);
			if (ids.contains(id)) {
				concurrencyFailures++;
			}
			ids.add(id);
			return "foo";
		}

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
