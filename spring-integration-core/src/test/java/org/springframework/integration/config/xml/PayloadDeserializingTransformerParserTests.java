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

package org.springframework.integration.config.xml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.serializer.Deserializer;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.integration.transformer.MessageTransformationException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.FileCopyUtils;

/**
 * @author Mark Fisher
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class PayloadDeserializingTransformerParserTests {

	@Autowired
	private MessageChannel directInput;

	@Autowired
	private MessageChannel queueInput;

	@Autowired
	private MessageChannel customDeserializerInput;

	@Autowired
	private PollableChannel output;


	@Test
	public void directChannelWithSerializedStringMessage() throws Exception {
		byte[] bytes = serialize("foo");
		directInput.send(new GenericMessage<byte[]>(bytes));
		Message<?> result = output.receive(0);
		assertNotNull(result);
		assertTrue(result.getPayload() instanceof String);
		assertEquals("foo", result.getPayload());
	}

	@Test
	public void queueChannelWithSerializedStringMessage() throws Exception {
		byte[] bytes = serialize("foo");
		queueInput.send(new GenericMessage<byte[]>(bytes));
		Message<?> result = output.receive(3000);
		assertNotNull(result);
		assertTrue(result.getPayload() instanceof String);
		assertEquals("foo", result.getPayload());
	}

	@Test
	public void directChannelWithSerializedObjectMessage() throws Exception {
		byte[] bytes = serialize(new TestBean());
		directInput.send(new GenericMessage<byte[]>(bytes));
		Message<?> result = output.receive(0);
		assertNotNull(result);
		assertEquals(TestBean.class, result.getPayload().getClass());
		assertEquals("test", ((TestBean) result.getPayload()).name);
	}

	@Test
	public void queueChannelWithSerializedObjectMessage() throws Exception {
		byte[] bytes = serialize(new TestBean());
		queueInput.send(new GenericMessage<byte[]>(bytes));
		Message<?> result = output.receive(3000);
		assertNotNull(result);
		assertEquals(TestBean.class, result.getPayload().getClass());
		assertEquals("test", ((TestBean) result.getPayload()).name);
	}

	@Test(expected = MessageTransformationException.class)
	public void invalidPayload() {
		byte[] bytes = new byte[] { 1, 2, 3 };
		directInput.send(new GenericMessage<byte[]>(bytes));
	}

	@Test
	public void customDeserializer() throws Exception {
		customDeserializerInput.send(new GenericMessage<byte[]>("test".getBytes("UTF-8")));
		Message<?> result = output.receive(3000);
		assertNotNull(result);
		assertEquals(String.class, result.getPayload().getClass());
		assertEquals("TEST", result.getPayload());
	}


	private static byte[] serialize(Object object) throws Exception {
		ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
		ObjectOutputStream objectStream = new ObjectOutputStream(byteStream);
		objectStream.writeObject(object);
		return byteStream.toByteArray();
	}


	@SuppressWarnings("serial")
	private static class TestBean implements Serializable {

		public final String name = "test";

	}


	public static class TestDeserializer implements Deserializer<Object> {

		public Object deserialize(InputStream source) throws IOException {
			return FileCopyUtils.copyToString(new InputStreamReader(source, "UTF-8")).toUpperCase();
		}
	}

}
