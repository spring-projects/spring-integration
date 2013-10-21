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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.io.Serializable;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.serializer.Serializer;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.integration.transformer.MessageTransformationException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Mark Fisher
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class PayloadSerializingTransformerParserTests {

	@Autowired
	private MessageChannel directInput;

	@Autowired
	private MessageChannel queueInput;

	@Autowired
	private MessageChannel customSerializerInput;

	@Autowired
	private PollableChannel output;


	@Test
	public void directChannelWithStringMessage() throws Exception {
		directInput.send(new GenericMessage<String>("foo"));
		Message<?> result = output.receive(0);
		assertNotNull(result);
		assertTrue(result.getPayload() instanceof byte[]);
		assertEquals("foo", deserialize((byte[]) result.getPayload()));
	}

	@Test
	public void queueChannelWithStringMessage() throws Exception {
		queueInput.send(new GenericMessage<String>("foo"));
		Message<?> result = output.receive(3000);
		assertNotNull(result);
		assertTrue(result.getPayload() instanceof byte[]);
		assertEquals("foo", deserialize((byte[]) result.getPayload()));
	}

	@Test
	public void directChannelWithObjectMessage() throws Exception {
		directInput.send(new GenericMessage<TestBean>(new TestBean()));
		Message<?> result = output.receive(0);
		assertNotNull(result);
		assertTrue(result.getPayload() instanceof byte[]);
		Object deserialized = deserialize((byte[]) result.getPayload());
		assertEquals(TestBean.class, deserialized.getClass());
		assertEquals("test", ((TestBean) deserialized).name);
	}

	@Test
	public void queueChannelWithObjectMessage() throws Exception {
		queueInput.send(new GenericMessage<TestBean>(new TestBean()));
		Message<?> result = output.receive(3000);
		assertTrue(result.getPayload() instanceof byte[]);
		Object deserialized = deserialize((byte[]) result.getPayload());
		assertEquals(TestBean.class, deserialized.getClass());
		assertEquals("test", ((TestBean) deserialized).name);
	}

	@Test(expected = MessageTransformationException.class)
	public void invalidPayload() {
		directInput.send(new GenericMessage<Object>(new Object()));
	}

	@Test
	public void customSerializer() throws Exception {
		customSerializerInput.send(new GenericMessage<String>("test"));
		Message<?> result = output.receive(3000);
		assertNotNull(result);
		assertEquals(byte[].class, result.getPayload().getClass());
		assertEquals("TEST", new String((byte[]) result.getPayload(), "UTF-8"));
	}


	private static Object deserialize(byte[] bytes) throws Exception {
		ByteArrayInputStream byteStream = new ByteArrayInputStream(bytes);
		ObjectInputStream objectStream = new ObjectInputStream(byteStream);
		return objectStream.readObject();
	}


	@SuppressWarnings("serial")
	private static class TestBean implements Serializable {

		public final String name = "test";

	}


	public static class TestSerializer implements Serializer<Object> {

		public void serialize(Object source, OutputStream outputStream) throws IOException {
			outputStream.write(source.toString().toUpperCase().getBytes("UTF-8"));
			outputStream.flush();
			outputStream.close();
		}
	}

}
