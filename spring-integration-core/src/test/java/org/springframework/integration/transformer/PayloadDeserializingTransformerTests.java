/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.integration.transformer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.fail;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import org.junit.jupiter.api.Test;

import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 */
public class PayloadDeserializingTransformerTests {

	@Test
	public void deserializeString() throws Exception {
		ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
		ObjectOutputStream objectStream = new ObjectOutputStream(byteStream);
		objectStream.writeObject("foo");
		byte[] serialized = byteStream.toByteArray();
		PayloadDeserializingTransformer transformer = new PayloadDeserializingTransformer();
		Message<?> result = transformer.transform(new GenericMessage<>(serialized));
		Object payload = result.getPayload();
		assertThat(payload).isNotNull();
		assertThat(payload.getClass()).isEqualTo(String.class);
		assertThat(payload).isEqualTo("foo");
	}

	@Test
	public void deserializeObject() throws Exception {
		TestBean testBean = new TestBean("test");
		ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
		ObjectOutputStream objectStream = new ObjectOutputStream(byteStream);
		objectStream.writeObject(testBean);
		byte[] serialized = byteStream.toByteArray();
		PayloadDeserializingTransformer transformer = new PayloadDeserializingTransformer();
		Message<?> result = transformer.transform(new GenericMessage<>(serialized));
		Object payload = result.getPayload();
		assertThat(payload).isNotNull();
		assertThat(payload.getClass()).isEqualTo(TestBean.class);
		assertThat(((TestBean) payload).name).isEqualTo(testBean.name);
	}

	@Test
	public void deserializeObjectAllowList() throws Exception {
		TestBean testBean = new TestBean("test");
		ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
		ObjectOutputStream objectStream = new ObjectOutputStream(byteStream);
		objectStream.writeObject(testBean);
		byte[] serialized = byteStream.toByteArray();
		PayloadDeserializingTransformer transformer = new PayloadDeserializingTransformer();
		transformer.setAllowedPatterns("com.*");
		try {
			transformer.transform(new GenericMessage<>(serialized));
			fail("expected security exception");
		}
		catch (MessageTransformationException e) {
			assertThat(e.getCause().getCause()).isInstanceOf(SecurityException.class);
			assertThat(e.getCause().getCause().getMessage()).startsWith("Attempt to deserialize unauthorized");
		}
		transformer.setAllowedPatterns("org.*");
		Message<?> result = transformer.transform(new GenericMessage<>(serialized));
		Object payload = result.getPayload();
		assertThat(payload).isNotNull();
		assertThat(payload.getClass()).isEqualTo(TestBean.class);
		assertThat(((TestBean) payload).name).isEqualTo(testBean.name);
	}

	@Test
	public void invalidPayload() {
		byte[] bytes = {1, 2, 3};
		PayloadDeserializingTransformer transformer = new PayloadDeserializingTransformer();
		assertThatExceptionOfType(MessageTransformationException.class)
				.isThrownBy(() -> transformer.transform(new GenericMessage<>(bytes)));
	}

	@Test
	public void customDeserializer() {
		PayloadDeserializingTransformer transformer = new PayloadDeserializingTransformer();
		transformer.setConverter(source -> "Converted");
		Message<?> message = transformer.transform(MessageBuilder.withPayload("Test".getBytes()).build());
		assertThat(message.getPayload()).isEqualTo("Converted");
	}

	@SuppressWarnings("serial")
	private static class TestBean implements Serializable {

		private final String name;

		TestBean(String name) {
			this.name = name;
		}

	}

}
