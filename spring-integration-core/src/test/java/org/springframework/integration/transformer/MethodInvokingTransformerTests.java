/*
 * Copyright 2002-2019 the original author or authors.
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.lang.reflect.Method;
import java.util.Properties;

import org.junit.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.integration.annotation.Transformer;
import org.springframework.integration.handler.MethodInvokingMessageProcessor;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.support.GenericMessage;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 */
public class MethodInvokingTransformerTests {

	@Test
	public void simplePayloadConfiguredWithMethodReference() throws Exception {
		TestBean testBean = new TestBean();
		Method testMethod = testBean.getClass().getMethod("exclaim", String.class);
		MethodInvokingTransformer transformer = new MethodInvokingTransformer(testBean, testMethod);
		transformer.setBeanFactory(mock(BeanFactory.class));
		Message<?> message = new GenericMessage<>("foo");
		Message<?> result = transformer.transform(message);
		assertThat(result.getPayload()).isEqualTo("FOO!");
	}

	@Test
	public void simplePayloadConfiguredWithMethodName() {
		TestBean testBean = new TestBean();
		MethodInvokingTransformer transformer = new MethodInvokingTransformer(testBean, "exclaim");
		transformer.setBeanFactory(mock(BeanFactory.class));
		Message<?> message = new GenericMessage<String>("foo");
		Message<?> result = transformer.transform(message);
		assertThat(result.getPayload()).isEqualTo("FOO!");
	}

	@Test
	public void typeConversionConfiguredWithMethodReference() throws Exception {
		TestBean testBean = new TestBean();
		Method testMethod = testBean.getClass().getMethod("exclaim", String.class);
		MethodInvokingTransformer transformer = new MethodInvokingTransformer(testBean, testMethod);
		transformer.setBeanFactory(mock(BeanFactory.class));
		Message<?> message = new GenericMessage<>(123);
		Message<?> result = transformer.transform(message);
		assertThat(result.getPayload()).isEqualTo("123!");
	}

	@Test
	public void typeConversionConfiguredWithMethodName() {
		TestBean testBean = new TestBean();
		MethodInvokingTransformer transformer = new MethodInvokingTransformer(testBean, "exclaim");
		transformer.setBeanFactory(mock(BeanFactory.class));
		Message<?> message = new GenericMessage<>(123);
		Message<?> result = transformer.transform(message);
		assertThat(result.getPayload()).isEqualTo("123!");
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
	public void typeConversionFailureConfiguredWithMethodName() {
		TestBean testBean = new TestBean();
		MethodInvokingTransformer transformer = new MethodInvokingTransformer(testBean, "exclaim");
		Message<?> message = new GenericMessage<>(new TestBean());
		transformer.transform(message);
	}

	@Test
	public void headerAnnotationConfiguredWithMethodReference() throws Exception {
		TestBean testBean = new TestBean();
		Method testMethod = testBean.getClass().getMethod("headerTest", String.class, Integer.class);
		MethodInvokingTransformer transformer = new MethodInvokingTransformer(testBean, testMethod);
		transformer.setBeanFactory(mock(BeanFactory.class));
		Message<String> message = MessageBuilder.withPayload("foo")
				.setHeader("number", 123).build();
		Message<?> result = transformer.transform(message);
		assertThat(result.getPayload()).isEqualTo("foo123");
	}

	@Test
	public void headerAnnotationConfiguredWithMethodName() {
		TestBean testBean = new TestBean();
		MethodInvokingTransformer transformer = new MethodInvokingTransformer(testBean, "headerTest");
		transformer.setBeanFactory(mock(BeanFactory.class));
		Message<String> message = MessageBuilder.withPayload("foo")
				.setHeader("number", 123).build();
		Message<?> result = transformer.transform(message);
		assertThat(result.getPayload()).isEqualTo("foo123");
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
		transformer.setBeanFactory(mock(BeanFactory.class));
		Message<String> message = MessageBuilder.withPayload("foo").setHeader("number", 99).build();
		Message<?> result = transformer.transform(message);
		assertThat(result.getPayload()).isEqualTo("foo99");
	}

	@Test
	public void optionalHeaderValueNotProvided() throws Exception {
		TestBean testBean = new TestBean();
		Method testMethod = testBean.getClass().getMethod("optionalHeaderTest", String.class, Integer.class);
		MethodInvokingTransformer transformer = new MethodInvokingTransformer(testBean, testMethod);
		transformer.setBeanFactory(mock(BeanFactory.class));
		Message<String> message = MessageBuilder.withPayload("foo").build();
		Message<?> result = transformer.transform(message);
		assertThat(result.getPayload()).isEqualTo("foonull");
	}

	@Test
	public void messageReturnValueConfiguredWithMethodReference() throws Exception {
		TestBean testBean = new TestBean();
		Method testMethod = testBean.getClass().getMethod("messageReturnValueTest", Message.class);
		MethodInvokingTransformer transformer = new MethodInvokingTransformer(testBean, testMethod);
		transformer.setBeanFactory(mock(BeanFactory.class));
		Message<String> message = MessageBuilder.withPayload("test").build();
		Message<?> result = transformer.transform(message);
		assertThat(result.getPayload()).isEqualTo("test");
	}

	@Test
	public void messageReturnValueConfiguredWithMethodName() {
		TestBean testBean = new TestBean();
		MethodInvokingTransformer transformer = new MethodInvokingTransformer(testBean, "messageReturnValueTest");
		transformer.setBeanFactory(mock(BeanFactory.class));
		Message<String> message = MessageBuilder.withPayload("test").build();
		Message<?> result = transformer.transform(message);
		assertThat(result.getPayload()).isEqualTo("test");
	}

	@Test
	@SuppressWarnings("unchecked")
	public void propertiesPayloadConfiguredWithMethodReference() throws Exception {
		TestBean testBean = new TestBean();
		Method testMethod = testBean.getClass().getMethod("propertyPayloadTest", Properties.class);
		MethodInvokingTransformer transformer = new MethodInvokingTransformer(testBean, testMethod);
		transformer.setBeanFactory(mock(BeanFactory.class));
		Properties props = new Properties();
		props.setProperty("prop1", "bad");
		props.setProperty("prop3", "baz");
		Message<Properties> message = new GenericMessage<>(props);
		Message<Properties> result = (Message<Properties>) transformer.transform(message);
		assertThat(result.getPayload().getClass()).isEqualTo(Properties.class);
		Properties payload = result.getPayload();
		assertThat(payload.getProperty("prop1")).isEqualTo("foo");
		assertThat(payload.getProperty("prop2")).isEqualTo("bar");
		assertThat(payload.getProperty("prop3")).isEqualTo("baz");
		assertThat(result.getHeaders().get("prop1")).isNull();
		assertThat(result.getHeaders().get("prop2")).isNull();
		assertThat(result.getHeaders().get("prop3")).isNull();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void propertiesPayloadConfiguredWithMethodName() {
		TestBean testBean = new TestBean();
		MethodInvokingTransformer transformer = new MethodInvokingTransformer(testBean, "propertyPayloadTest");
		transformer.setBeanFactory(mock(BeanFactory.class));
		Properties props = new Properties();
		props.setProperty("prop1", "bad");
		props.setProperty("prop3", "baz");
		Message<Properties> message = new GenericMessage<>(props);
		Message<Properties> result = (Message<Properties>) transformer.transform(message);
		assertThat(result.getPayload().getClass()).isEqualTo(Properties.class);
		Properties payload = result.getPayload();
		assertThat(payload.getProperty("prop1")).isEqualTo("foo");
		assertThat(payload.getProperty("prop2")).isEqualTo("bar");
		assertThat(payload.getProperty("prop3")).isEqualTo("baz");
		assertThat(result.getHeaders().get("prop1")).isNull();
		assertThat(result.getHeaders().get("prop2")).isNull();
		assertThat(result.getHeaders().get("prop3")).isNull();
	}

	@Test
	public void nullReturningMethod() {
		TestBean testBean = new TestBean();
		MethodInvokingTransformer transformer = new MethodInvokingTransformer(testBean, "nullReturnValueTest");
		transformer.setBeanFactory(mock(BeanFactory.class));
		GenericMessage<String> message = new GenericMessage<>("test");
		Message<?> result = transformer.transform(message);
		assertThat(result).isNull();
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test // this changed in 2.0 see INT-785 and INT-1130
	public void headerEnricherConfiguredWithMethodReference() throws Exception {
		TestBean testBean = new TestBean();
		Method testMethod = testBean.getClass().getMethod("propertyEnricherTest", String.class);
		HeaderEnricher transformer = new HeaderEnricher();
		transformer.setDefaultOverwrite(true);
		MethodInvokingMessageProcessor processor = new MethodInvokingMessageProcessor(testBean, testMethod);
		processor.setBeanFactory(mock(BeanFactory.class));
		transformer.setMessageProcessor(processor);
		Message<String> message = MessageBuilder.withPayload("test")
				.setHeader("prop1", "bad")
				.setHeader("prop3", "baz").build();
		Message<?> result = transformer.transform(message);
		assertThat(result.getPayload()).isEqualTo("test");
		assertThat(result.getHeaders().get("prop1")).isEqualTo("foo");
		assertThat(result.getHeaders().get("prop2")).isEqualTo("bar");
		assertThat(result.getHeaders().get("prop3")).isEqualTo("baz");
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test // this changed in 2.0 see INT-785 and INT-1130
	public void headerEnricherConfiguredWithMethodName() {
		TestBean testBean = new TestBean();
		HeaderEnricher transformer = new HeaderEnricher();
		MethodInvokingMessageProcessor processor =
				new MethodInvokingMessageProcessor(testBean, "propertyEnricherTest");
		processor.setBeanFactory(mock(BeanFactory.class));
		transformer.setMessageProcessor(processor);
		transformer.setDefaultOverwrite(true);
		Message<String> message = MessageBuilder.withPayload("test")
				.setHeader("prop1", "bad")
				.setHeader("prop3", "baz").build();
		Message<?> result = transformer.transform(message);
		assertThat(result.getPayload()).isEqualTo("test");
		assertThat(result.getHeaders().get("prop1")).isEqualTo("foo");
		assertThat(result.getHeaders().get("prop2")).isEqualTo("bar");
		assertThat(result.getHeaders().get("prop3")).isEqualTo("baz");
	}


	@SuppressWarnings("unused")
	private static class TestBean {

		TestBean() {
			super();
		}

		@Transformer
		public String exclaim(String s) {
			return s.toUpperCase() + "!";
		}

		@Transformer
		public String headerTest(String s, @Header("number") Integer num) {
			return s + num;
		}

		@Transformer
		public String optionalHeaderTest(String s, @Header(value = "number", required = false) Integer num) {
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
