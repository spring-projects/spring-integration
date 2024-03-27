/*
 * Copyright 2002-2024 the original author or authors.
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
