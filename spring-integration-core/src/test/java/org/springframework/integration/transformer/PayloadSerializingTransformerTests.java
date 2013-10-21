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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;

import org.junit.Test;

import org.springframework.core.convert.converter.Converter;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.integration.support.MessageBuilder;

/**
 * @author Mark Fisher
 */
public class PayloadSerializingTransformerTests {

	@Test
	public void serializeString() throws Exception {
		PayloadSerializingTransformer transformer = new PayloadSerializingTransformer();
		Message<?> result = transformer.transform(new GenericMessage<String>("foo"));
		Object payload = result.getPayload();
		assertNotNull(payload);
		assertTrue(payload instanceof byte[]);
		ByteArrayInputStream byteStream = new ByteArrayInputStream((byte[]) payload);
		ObjectInputStream objectStream = new ObjectInputStream(byteStream);
		Object deserialized = objectStream.readObject();
		assertEquals("foo", deserialized);
	}

	@Test
	public void serializeObject() throws Exception {
		PayloadSerializingTransformer transformer = new PayloadSerializingTransformer();
		TestBean testBean = new TestBean("test");
		Message<?> result = transformer.transform(new GenericMessage<TestBean>(testBean));
		Object payload = result.getPayload();
		assertNotNull(payload);
		assertTrue(payload instanceof byte[]);
		ByteArrayInputStream byteStream = new ByteArrayInputStream((byte[]) payload);
		ObjectInputStream objectStream = new ObjectInputStream(byteStream);
		Object deserialized = objectStream.readObject();
		assertEquals(TestBean.class, deserialized.getClass());
		assertEquals(testBean.name, ((TestBean) deserialized).name);
	}

	@Test(expected = MessageTransformationException.class)
	public void invalidPayload() {
		PayloadSerializingTransformer transformer = new PayloadSerializingTransformer();
		transformer.transform(new GenericMessage<Object>(new Object()));
	}
	
	@Test
	public void customSerializer() {
		PayloadSerializingTransformer transformer = new PayloadSerializingTransformer();
		transformer.setConverter(new Converter<Object, byte[]>(){
			public byte[] convert(Object source) {
				return "Converted".getBytes();
			}
		});
		Message<?> message = transformer.transform(MessageBuilder.withPayload("Test").build());
		assertEquals("Converted", new String((byte[]) message.getPayload()));
	}


	@SuppressWarnings("serial")
	private static class TestBean implements Serializable {

		private String name;
		
		public TestBean(String name) {
			this.name = name;
		}
	}

}
