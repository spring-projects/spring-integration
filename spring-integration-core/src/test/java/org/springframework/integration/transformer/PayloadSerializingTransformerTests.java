/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.transformer;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;

import org.junit.Test;

import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 */
public class PayloadSerializingTransformerTests {

	@Test
	public void serializeString() throws Exception {
		PayloadSerializingTransformer transformer = new PayloadSerializingTransformer();
		Message<?> result = transformer.transform(new GenericMessage<String>("foo"));
		Object payload = result.getPayload();
		assertThat(payload).isNotNull();
		assertThat(payload instanceof byte[]).isTrue();
		ByteArrayInputStream byteStream = new ByteArrayInputStream((byte[]) payload);
		ObjectInputStream objectStream = new ObjectInputStream(byteStream);
		Object deserialized = objectStream.readObject();
		assertThat(deserialized).isEqualTo("foo");
	}

	@Test
	public void serializeObject() throws Exception {
		PayloadSerializingTransformer transformer = new PayloadSerializingTransformer();
		TestBean testBean = new TestBean("test");
		Message<?> result = transformer.transform(new GenericMessage<TestBean>(testBean));
		Object payload = result.getPayload();
		assertThat(payload).isNotNull();
		assertThat(payload instanceof byte[]).isTrue();
		ByteArrayInputStream byteStream = new ByteArrayInputStream((byte[]) payload);
		ObjectInputStream objectStream = new ObjectInputStream(byteStream);
		Object deserialized = objectStream.readObject();
		assertThat(deserialized.getClass()).isEqualTo(TestBean.class);
		assertThat(((TestBean) deserialized).name).isEqualTo(testBean.name);
	}

	@Test(expected = MessageTransformationException.class)
	public void invalidPayload() {
		PayloadSerializingTransformer transformer = new PayloadSerializingTransformer();
		transformer.transform(new GenericMessage<Object>(new Object()));
	}

	@Test
	public void customSerializer() {
		PayloadSerializingTransformer transformer = new PayloadSerializingTransformer();
		transformer.setConverter(source -> "Converted".getBytes());
		Message<?> message = transformer.transform(MessageBuilder.withPayload("Test").build());
		assertThat(new String((byte[]) message.getPayload())).isEqualTo("Converted");
	}

	@SuppressWarnings("serial")
	private static class TestBean implements Serializable {

		private final String name;

		TestBean(String name) {
			this.name = name;
		}

	}

}
