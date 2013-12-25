/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.integration.transformer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.lang.reflect.Method;
import java.util.Properties;

import org.junit.Test;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.integration.annotation.Header;
import org.springframework.integration.annotation.Transformer;
import org.springframework.integration.handler.MethodInvokingMessageProcessor;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.integration.support.MessageBuilder;

/**
 * @author Mark Fisher
 */
public class MethodInvokingTransformerTests {

	@Test
	public void simplePayloadConfiguredWithMethodReference() throws Exception {
		TestBean testBean = new TestBean();
		Method testMethod = testBean.getClass().getMethod("exclaim", String.class);
		MethodInvokingTransformer transformer = new MethodInvokingTransformer(testBean, testMethod);
		Message<?> message = new GenericMessage<String>("foo");
		Message<?> result = transformer.transform(message);
		assertEquals("FOO!", result.getPayload());
	}

	@Test
	public void simplePayloadConfiguredWithMethodName() throws Exception {
		TestBean testBean = new TestBean();
		MethodInvokingTransformer transformer = new MethodInvokingTransformer(testBean, "exclaim");
		Message<?> message = new GenericMessage<String>("foo");
		Message<?> result = transformer.transform(message);
		assertEquals("FOO!", result.getPayload());
	}

	@Test
	public void typeConversionConfiguredWithMethodReference() throws Exception {
		TestBean testBean = new TestBean();
		Method testMethod = testBean.getClass().getMethod("exclaim", String.class);
		MethodInvokingTransformer transformer = new MethodInvokingTransformer(testBean, testMethod);
		Message<?> message = new GenericMessage<Integer>(123);
		Message<?> result = transformer.transform(message);
		assertEquals("123!", result.getPayload());
	}

	@Test
	public void typeConversionConfiguredWithMethodName() throws Exception {
		TestBean testBean = new TestBean();
		MethodInvokingTransformer transformer = new MethodInvokingTransformer(testBean, "exclaim");
		Message<?> message = new GenericMessage<Integer>(123);
		Message<?> result = transformer.transform(message);
		assertEquals("123!", result.getPayload());
	}

	@Test(expected = MessageHandlingException.class)
	public void typeConversionFailureConfiguredWithMethodReference() throws Exception {
		TestBean testBean = new TestBean();
		Method testMethod = testBean.getClass().getMethod("exclaim", String.class);
		MethodInvokingTransformer transformer = new MethodInvokingTransformer(testBean, testMethod);
		Message<?> message = new GenericMessage<TestBean>(new TestBean());
		transformer.transform(message);
	}

	@Test(expected = MessageHandlingException.class)
	public void typeConversionFailureConfiguredWithMethodName() throws Exception {
		TestBean testBean = new TestBean();
		MethodInvokingTransformer transformer = new MethodInvokingTransformer(testBean, "exclaim");
		Message<?> message = new GenericMessage<TestBean>(new TestBean());
		transformer.transform(message);
	}

	@Test
	public void headerAnnotationConfiguredWithMethodReference() throws Exception {
		TestBean testBean = new TestBean();
		Method testMethod = testBean.getClass().getMethod("headerTest", String.class, Integer.class);
		MethodInvokingTransformer transformer = new MethodInvokingTransformer(testBean, testMethod);
		Message<String> message = MessageBuilder.withPayload("foo")
				.setHeader("number", 123).build();
		Message<?> result = transformer.transform(message);
		assertEquals("foo123", result.getPayload());
	}

	@Test
	public void headerAnnotationConfiguredWithMethodName() throws Exception {
		TestBean testBean = new TestBean();
		MethodInvokingTransformer transformer = new MethodInvokingTransformer(testBean, "headerTest");
		Message<String> message = MessageBuilder.withPayload("foo")
				.setHeader("number", 123).build();
		Message<?> result = transformer.transform(message);
		assertEquals("foo123", result.getPayload());
	}

	@Test(expected = MessageHandlingException.class)
	public void headerValueNotProvided() throws Exception {
		TestBean testBean = new TestBean();
		Method testMethod = testBean.getClass().getMethod("headerTest", String.class, Integer.class);
		MethodInvokingTransformer transformer = new MethodInvokingTransformer(testBean, testMethod);
		Message<String> message = MessageBuilder.withPayload("foo")
				.setHeader("wrong", 123).build();
		transformer.transform(message);
	}

	@Test
	public void optionalHeaderAnnotation() throws Exception {
		TestBean testBean = new TestBean();
		Method testMethod = testBean.getClass().getMethod("optionalHeaderTest", String.class, Integer.class);
		MethodInvokingTransformer transformer = new MethodInvokingTransformer(testBean, testMethod);
		Message<String> message = MessageBuilder.withPayload("foo").setHeader("number", 99).build();
		Message<?> result = transformer.transform(message);
		assertEquals("foo99", result.getPayload());
	}

	@Test
	public void optionalHeaderValueNotProvided() throws Exception {
		TestBean testBean = new TestBean();
		Method testMethod = testBean.getClass().getMethod("optionalHeaderTest", String.class, Integer.class);
		MethodInvokingTransformer transformer = new MethodInvokingTransformer(testBean, testMethod);
		Message<String> message = MessageBuilder.withPayload("foo").build();
		Message<?> result = transformer.transform(message);
		assertEquals("foonull", result.getPayload());
	}

	@Test
	public void messageReturnValueConfiguredWithMethodReference() throws Exception {
		TestBean testBean = new TestBean();
		Method testMethod = testBean.getClass().getMethod("messageReturnValueTest", Message.class);
		MethodInvokingTransformer transformer = new MethodInvokingTransformer(testBean, testMethod);
		Message<String> message = MessageBuilder.withPayload("test").build();
		Message<?> result = transformer.transform(message);
		assertEquals("test", result.getPayload());
	}

	@Test
	public void messageReturnValueConfiguredWithMethodName() throws Exception {
		TestBean testBean = new TestBean();
		MethodInvokingTransformer transformer = new MethodInvokingTransformer(testBean, "messageReturnValueTest");
		Message<String> message = MessageBuilder.withPayload("test").build();
		Message<?> result = transformer.transform(message);
		assertEquals("test", result.getPayload());
	}

	@Test
	@SuppressWarnings("unchecked")
	public void propertiesPayloadConfiguredWithMethodReference() throws Exception {
		TestBean testBean = new TestBean();
		Method testMethod = testBean.getClass().getMethod("propertyPayloadTest", Properties.class);
		MethodInvokingTransformer transformer = new MethodInvokingTransformer(testBean, testMethod);
		Properties props = new Properties();
		props.setProperty("prop1", "bad");
		props.setProperty("prop3", "baz");
		Message<Properties> message = new GenericMessage<Properties>(props);
		Message<Properties> result = (Message<Properties>) transformer.transform(message);
		assertEquals(Properties.class, result.getPayload().getClass());
		Properties payload = result.getPayload();
		assertEquals("foo", payload.getProperty("prop1"));
		assertEquals("bar", payload.getProperty("prop2"));
		assertEquals("baz", payload.getProperty("prop3"));
		assertNull(result.getHeaders().get("prop1"));
		assertNull(result.getHeaders().get("prop2"));
		assertNull(result.getHeaders().get("prop3"));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void propertiesPayloadConfiguredWithMethodName() throws Exception {
		TestBean testBean = new TestBean();
		MethodInvokingTransformer transformer = new MethodInvokingTransformer(testBean, "propertyPayloadTest");
		Properties props = new Properties();
		props.setProperty("prop1", "bad");
		props.setProperty("prop3", "baz");
		Message<Properties> message = new GenericMessage<Properties>(props);
		Message<Properties> result = (Message<Properties>) transformer.transform(message);
		assertEquals(Properties.class, result.getPayload().getClass());
		Properties payload = result.getPayload();
		assertEquals("foo", payload.getProperty("prop1"));
		assertEquals("bar", payload.getProperty("prop2"));
		assertEquals("baz", payload.getProperty("prop3"));
		assertNull(result.getHeaders().get("prop1"));
		assertNull(result.getHeaders().get("prop2"));
		assertNull(result.getHeaders().get("prop3"));
	}

	@Test
	public void nullReturningMethod() {
		TestBean testBean = new TestBean();
		MethodInvokingTransformer transformer = new MethodInvokingTransformer(testBean, "nullReturnValueTest");
		GenericMessage<String> message = new GenericMessage<String>("test");
		Message<?> result = transformer.transform(message);
		assertNull(result);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test // this changed in 2.0 see INT-785 and INT-1130
	public void headerEnricherConfiguredWithMethodReference() throws Exception {
		TestBean testBean = new TestBean();
		Method testMethod = testBean.getClass().getMethod("propertyEnricherTest", String.class);
		HeaderEnricher transformer = new HeaderEnricher();
		transformer.setDefaultOverwrite(true);
		MethodInvokingMessageProcessor processor = new MethodInvokingMessageProcessor(testBean, testMethod);
		transformer.setMessageProcessor(processor);
		Message<String> message = MessageBuilder.withPayload("test")
				.setHeader("prop1", "bad")
				.setHeader("prop3", "baz").build();
		Message<?> result = transformer.transform(message);
		assertEquals("test", result.getPayload());
		assertEquals("foo", result.getHeaders().get("prop1"));
		assertEquals("bar", result.getHeaders().get("prop2"));
		assertEquals("baz", result.getHeaders().get("prop3"));
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test // this changed in 2.0 see INT-785 and INT-1130
	public void headerEnricherConfiguredWithMethodName() throws Exception {
		TestBean testBean = new TestBean();
		HeaderEnricher transformer = new HeaderEnricher();
		MethodInvokingMessageProcessor processor = new MethodInvokingMessageProcessor(testBean, "propertyEnricherTest");
		transformer.setMessageProcessor(processor);
		transformer.setDefaultOverwrite(true);
		Message<String> message = MessageBuilder.withPayload("test")
				.setHeader("prop1", "bad")
				.setHeader("prop3", "baz").build();
		Message<?> result = transformer.transform(message);
		assertEquals("test", result.getPayload());
		assertEquals("foo", result.getHeaders().get("prop1"));
		assertEquals("bar", result.getHeaders().get("prop2"));
		assertEquals("baz", result.getHeaders().get("prop3"));
	}


	@SuppressWarnings("unused")
	private static class TestBean {

		@Transformer
		public String exclaim(String s) {
			return s.toUpperCase() + "!";
		}

		@Transformer
		public String headerTest(String s, @Header("number") Integer num) {
			return s + num;
		}

		@Transformer
		public String optionalHeaderTest(String s, @Header(value="number", required=false) Integer num) {
			return s + num;
		}

		@Transformer
		public Properties propertyEnricherTest(String s) {
			Properties properties = new Properties();
			properties.setProperty("prop1", "foo");
			properties.setProperty("prop2", "bar");
			return properties;
		}

		@Transformer
		public Properties propertyPayloadTest(Properties properties) {
			properties.setProperty("prop1", "foo");
			properties.setProperty("prop2", "bar");
			return properties;
		}

		public Message<?> messageReturnValueTest(Message<?> message) {
			return message;
		}

		public Object nullReturnValueTest(Message<?> message) {
			return null;
		}
	}

}
