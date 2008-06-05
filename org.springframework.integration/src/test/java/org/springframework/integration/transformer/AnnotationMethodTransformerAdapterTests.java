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
import org.springframework.integration.handler.annotation.HeaderAttribute;
import org.springframework.integration.handler.annotation.HeaderProperty;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageHandlingException;
import org.springframework.integration.message.MessagingException;
import org.springframework.integration.message.StringMessage;

/**
 * @author Mark Fisher
 */
public class AnnotationMethodTransformerAdapterTests {

	@Test
	public void testSimplePayload() throws Exception {
		TestBean testBean = new TestBean();
		AnnotationMethodTransformerAdapter adapter = new AnnotationMethodTransformerAdapter();
		adapter.setObject(testBean);
		adapter.setMethod(testBean.getClass().getMethod("exclaim", String.class));
		Message<?> message = new StringMessage("foo");
		adapter.transform(message);
		assertEquals("FOO!", message.getPayload());
	}

	@Test
	public void testTypeConversion() throws Exception {
		TestBean testBean = new TestBean();
		AnnotationMethodTransformerAdapter adapter = new AnnotationMethodTransformerAdapter();
		adapter.setObject(testBean);
		adapter.setMethod(testBean.getClass().getMethod("exclaim", String.class));
		Message<?> message = new GenericMessage<Integer>(123);
		adapter.transform(message);
		assertEquals("123!", message.getPayload());
	}

	@Test(expected=MessagingException.class)
	public void testTypeConversionFailure() throws Exception {
		TestBean testBean = new TestBean();
		AnnotationMethodTransformerAdapter adapter = new AnnotationMethodTransformerAdapter();
		adapter.setObject(testBean);
		adapter.setMethod(testBean.getClass().getMethod("exclaim", String.class));
		Message<?> message = new GenericMessage<Date>(new Date());
		adapter.transform(message);
	}

	@Test
	public void testHeaderAttributeAnnotation() throws Exception {
		TestBean testBean = new TestBean();
		AnnotationMethodTransformerAdapter adapter = new AnnotationMethodTransformerAdapter();
		adapter.setObject(testBean);
		adapter.setMethod(testBean.getClass().getMethod("attributeTest", String.class, Integer.class));
		Message<?> message = new StringMessage("foo");
		message.getHeader().setAttribute("number", 123);
		adapter.transform(message);
		assertEquals("foo123", message.getPayload());
	}

	@Test(expected=MessageHandlingException.class)
	public void testHeaderAttributeNotProvided() throws Exception {
		TestBean testBean = new TestBean();
		AnnotationMethodTransformerAdapter adapter = new AnnotationMethodTransformerAdapter();
		adapter.setObject(testBean);
		adapter.setMethod(testBean.getClass().getMethod("attributeTest", String.class, Integer.class));
		Message<?> message = new StringMessage("foo");
		message.getHeader().setAttribute("wrong", 123);
		adapter.transform(message);
	}

	@Test
	public void testHeaderPropertyAnnotation() throws Exception {
		TestBean testBean = new TestBean();
		AnnotationMethodTransformerAdapter adapter = new AnnotationMethodTransformerAdapter();
		adapter.setObject(testBean);
		adapter.setMethod(testBean.getClass().getMethod("propertyTest", String.class, String.class));
		Message<?> message = new StringMessage("foo");
		message.getHeader().setProperty("suffix", "bar");
		adapter.transform(message);
		assertEquals("foobar", message.getPayload());
	}

	@Test(expected=MessageHandlingException.class)
	public void testHeaderPropertyNotAvailable() throws Exception {
		TestBean testBean = new TestBean();
		AnnotationMethodTransformerAdapter adapter = new AnnotationMethodTransformerAdapter();
		adapter.setObject(testBean);
		adapter.setMethod(testBean.getClass().getMethod("propertyTest", String.class, String.class));
		Message<?> message = new StringMessage("foo");
		message.getHeader().setProperty("wrong", "bar");
		adapter.transform(message);
	}

	@Test
	public void testPropertyEnricher() throws Exception {
		TestBean testBean = new TestBean();
		AnnotationMethodTransformerAdapter adapter = new AnnotationMethodTransformerAdapter();
		adapter.setObject(testBean);
		adapter.setMethod(testBean.getClass().getMethod("propertyEnricherTest", String.class));
		Message<?> message = new StringMessage("test");
		message.getHeader().setProperty("prop1", "bad");
		message.getHeader().setProperty("prop3", "baz");
		adapter.transform(message);
		assertEquals("test", message.getPayload());
		assertEquals("foo", message.getHeader().getProperty("prop1"));
		assertEquals("bar", message.getHeader().getProperty("prop2"));
		assertEquals("baz", message.getHeader().getProperty("prop3"));
	}

	@Test
	public void testPropertyPayload() throws Exception {
		TestBean testBean = new TestBean();
		AnnotationMethodTransformerAdapter adapter = new AnnotationMethodTransformerAdapter();
		adapter.setObject(testBean);
		adapter.setMethod(testBean.getClass().getMethod("propertyPayloadTest", Properties.class));
		Properties props = new Properties();
		props.setProperty("prop1", "bad");
		props.setProperty("prop3", "baz");
		Message<Properties> message = new GenericMessage<Properties>(props);
		adapter.transform(message);
		assertEquals(Properties.class, message.getPayload().getClass());
		Properties payload = message.getPayload();
		assertEquals("foo", payload.getProperty("prop1"));
		assertEquals("bar", payload.getProperty("prop2"));
		assertEquals("baz", payload.getProperty("prop3"));
		assertNull(message.getHeader().getProperty("prop1"));
		assertNull(message.getHeader().getProperty("prop2"));
		assertNull(message.getHeader().getProperty("prop3"));
	}


	private static class TestBean {

		@Transformer
		public String exclaim(String s) {
			return s.toUpperCase() + "!";
		}

		@Transformer
		public String attributeTest(String s, @HeaderAttribute("number") Integer num) {
			return s + num;
		}

		@Transformer
		public String propertyTest(String s, @HeaderProperty("suffix") String suffix) {
			return s + suffix;
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
