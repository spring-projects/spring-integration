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

package org.springframework.integration.transformer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Date;
import java.util.Properties;

import org.junit.Test;

import org.springframework.integration.annotation.Transformer;
import org.springframework.integration.handler.annotation.Header;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageBuilder;
import org.springframework.integration.message.MessageHandlingException;
import org.springframework.integration.message.MessagingException;
import org.springframework.integration.message.StringMessage;

/**
 * @author Mark Fisher
 */
public class TransformerMessageHandlerTests {

	@Test
	public void simplePayload() throws Exception {
		TestBean testBean = new TestBean();
		TransformerMessageHandler handler = new TransformerMessageHandler();
		handler.setObject(testBean);
		handler.setMethod(testBean.getClass().getMethod("exclaim", String.class));
		Message<?> message = new StringMessage("foo");
		Message<?> result = handler.handle(message);
		assertEquals("FOO!", result.getPayload());
	}

	@Test
	public void typeConversion() throws Exception {
		TestBean testBean = new TestBean();
		TransformerMessageHandler handler = new TransformerMessageHandler();
		handler.setObject(testBean);
		handler.setMethod(testBean.getClass().getMethod("exclaim", String.class));
		Message<?> message = new GenericMessage<Integer>(123);
		Message<?> result = handler.handle(message);
		assertEquals("123!", result.getPayload());
	}

	@Test(expected = MessagingException.class)
	public void typeConversionFailure() throws Exception {
		TestBean testBean = new TestBean();
		TransformerMessageHandler handler = new TransformerMessageHandler();
		handler.setObject(testBean);
		handler.setMethod(testBean.getClass().getMethod("exclaim", String.class));
		Message<?> message = new GenericMessage<Date>(new Date());
		handler.handle(message);
	}

	@Test
	public void headerAnnotation() throws Exception {
		TestBean testBean = new TestBean();
		TransformerMessageHandler handler = new TransformerMessageHandler();
		handler.setObject(testBean);
		handler.setMethod(testBean.getClass().getMethod("headerTest", String.class, Integer.class));
		Message<String> message = MessageBuilder.fromPayload("foo")
				.setHeader("number", 123).build();
		Message<?> result = handler.handle(message);
		assertEquals("foo123", result.getPayload());
	}

	@Test(expected = MessageHandlingException.class)
	public void headerValueNotProvided() throws Exception {
		TestBean testBean = new TestBean();
		TransformerMessageHandler handler = new TransformerMessageHandler();
		handler.setObject(testBean);
		handler.setMethod(testBean.getClass().getMethod("headerTest", String.class, Integer.class));
		Message<String> message = MessageBuilder.fromPayload("foo")
				.setHeader("wrong", 123).build();
		handler.handle(message);
	}

	@Test
	public void optionalHeaderAnnotation() throws Exception {
		TestBean testBean = new TestBean();
		TransformerMessageHandler handler = new TransformerMessageHandler();
		handler.setObject(testBean);
		handler.setMethod(testBean.getClass().getMethod("optionalHeaderTest", String.class, Integer.class));
		Message<String> message = MessageBuilder.fromPayload("foo").setHeader("number", 99).build();
		Message<?> result = handler.handle(message);
		assertEquals("foo99", result.getPayload());
	}

	@Test
	public void optionalHeaderValueNotProvided() throws Exception {
		TestBean testBean = new TestBean();
		TransformerMessageHandler handler = new TransformerMessageHandler();
		handler.setObject(testBean);
		handler.setMethod(testBean.getClass().getMethod("optionalHeaderTest", String.class, Integer.class));
		Message<String> message = MessageBuilder.fromPayload("foo").build();
		Message<?> result = handler.handle(message);
		assertEquals("foonull", result.getPayload());
	}

	@Test
	public void headerEnricher() throws Exception {
		TestBean testBean = new TestBean();
		TransformerMessageHandler handler = new TransformerMessageHandler();
		handler.setObject(testBean);
		handler.setMethod(testBean.getClass().getMethod("propertyEnricherTest", String.class));
		Message<String> message = MessageBuilder.fromPayload("test")
				.setHeader("prop1", "bad")
				.setHeader("prop3", "baz").build();
		Message<?> result = handler.handle(message);
		assertEquals("test", result.getPayload());
		assertEquals("foo", result.getHeaders().get("prop1"));
		assertEquals("bar", result.getHeaders().get("prop2"));
		assertEquals("baz", result.getHeaders().get("prop3"));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void propertiesPayload() throws Exception {
		TestBean testBean = new TestBean();
		TransformerMessageHandler handler = new TransformerMessageHandler();
		handler.setObject(testBean);
		handler.setMethod(testBean.getClass().getMethod("propertyPayloadTest", Properties.class));
		Properties props = new Properties();
		props.setProperty("prop1", "bad");
		props.setProperty("prop3", "baz");
		Message<Properties> message = new GenericMessage<Properties>(props);
		Message<Properties> result = (Message<Properties>) handler.handle(message);
		assertEquals(Properties.class, result.getPayload().getClass());
		Properties payload = result.getPayload();
		assertEquals("foo", payload.getProperty("prop1"));
		assertEquals("bar", payload.getProperty("prop2"));
		assertEquals("baz", payload.getProperty("prop3"));
		assertNull(result.getHeaders().get("prop1"));
		assertNull(result.getHeaders().get("prop2"));
		assertNull(result.getHeaders().get("prop3"));
	}


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
	}

}
