/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.config.xml;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.io.Serializable;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.serializer.Serializer;
import org.springframework.integration.transformer.MessageTransformationException;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext
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
		assertThat(result).isNotNull();
		assertThat(result.getPayload() instanceof byte[]).isTrue();
		assertThat(deserialize((byte[]) result.getPayload())).isEqualTo("foo");
	}

	@Test
	public void queueChannelWithStringMessage() throws Exception {
		queueInput.send(new GenericMessage<String>("foo"));
		Message<?> result = output.receive(10000);
		assertThat(result).isNotNull();
		assertThat(result.getPayload() instanceof byte[]).isTrue();
		assertThat(deserialize((byte[]) result.getPayload())).isEqualTo("foo");
	}

	@Test
	public void directChannelWithObjectMessage() throws Exception {
		directInput.send(new GenericMessage<TestBean>(new TestBean()));
		Message<?> result = output.receive(0);
		assertThat(result).isNotNull();
		assertThat(result.getPayload() instanceof byte[]).isTrue();
		Object deserialized = deserialize((byte[]) result.getPayload());
		assertThat(deserialized.getClass()).isEqualTo(TestBean.class);
		assertThat(((TestBean) deserialized).name).isEqualTo("test");
	}

	@Test
	public void queueChannelWithObjectMessage() throws Exception {
		queueInput.send(new GenericMessage<TestBean>(new TestBean()));
		Message<?> result = output.receive(10000);
		assertThat(result.getPayload() instanceof byte[]).isTrue();
		Object deserialized = deserialize((byte[]) result.getPayload());
		assertThat(deserialized.getClass()).isEqualTo(TestBean.class);
		assertThat(((TestBean) deserialized).name).isEqualTo("test");
	}

	@Test(expected = MessageTransformationException.class)
	public void invalidPayload() {
		directInput.send(new GenericMessage<Object>(new Object()));
	}

	@Test
	public void customSerializer() throws Exception {
		customSerializerInput.send(new GenericMessage<String>("test"));
		Message<?> result = output.receive(10000);
		assertThat(result).isNotNull();
		assertThat(result.getPayload().getClass()).isEqualTo(byte[].class);
		assertThat(new String((byte[]) result.getPayload(), "UTF-8")).isEqualTo("TEST");
	}

	private static Object deserialize(byte[] bytes) throws Exception {
		ByteArrayInputStream byteStream = new ByteArrayInputStream(bytes);
		ObjectInputStream objectStream = new ObjectInputStream(byteStream);
		return objectStream.readObject();
	}

	@SuppressWarnings("serial")
	private static class TestBean implements Serializable {

		TestBean() {
			super();
		}

		public final String name = "test";

	}

	public static class TestSerializer implements Serializer<Object> {

		@Override
		public void serialize(Object source, OutputStream outputStream) throws IOException {
			outputStream.write(source.toString().toUpperCase().getBytes("UTF-8"));
			outputStream.flush();
			outputStream.close();
		}

	}

}
