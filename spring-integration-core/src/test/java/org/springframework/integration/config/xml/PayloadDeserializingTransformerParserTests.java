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

package org.springframework.integration.config.xml;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.serializer.Deserializer;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.transformer.MessageTransformationException;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.util.FileCopyUtils;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 */
@SpringJUnitConfig
public class PayloadDeserializingTransformerParserTests {

	@Autowired
	private MessageChannel directInput;

	@Autowired
	private MessageChannel queueInput;

	@Autowired
	private MessageChannel customDeserializerInput;

	@Autowired
	private PollableChannel output;

	@Autowired
	@Qualifier("direct.handler")
	private MessageHandler handler;


	@Test
	public void directChannelWithSerializedStringMessage() throws Exception {
		byte[] bytes = serialize("foo");
		directInput.send(new GenericMessage<byte[]>(bytes));
		Message<?> result = output.receive(10000);
		assertThat(result).isNotNull();
		assertThat(result.getPayload() instanceof String).isTrue();
		assertThat(result.getPayload()).isEqualTo("foo");
		Set<?> patterns =
				TestUtils.getPropertyValue(this.handler, "transformer.converter.allowedPatterns", Set.class);
		assertThat(patterns.size()).isEqualTo(1);
		assertThat(patterns.iterator().next()).isEqualTo("*");
	}

	@Test
	public void queueChannelWithSerializedStringMessage() throws Exception {
		byte[] bytes = serialize("foo");
		queueInput.send(new GenericMessage<>(bytes));
		Message<?> result = output.receive(10000);
		assertThat(result).isNotNull();
		assertThat(result.getPayload() instanceof String).isTrue();
		assertThat(result.getPayload()).isEqualTo("foo");
	}

	@Test
	public void directChannelWithSerializedObjectMessage() throws Exception {
		byte[] bytes = serialize(new TestBean());
		directInput.send(new GenericMessage<>(bytes));
		Message<?> result = output.receive(10000);
		assertThat(result).isNotNull();
		assertThat(result.getPayload().getClass()).isEqualTo(TestBean.class);
		assertThat(((TestBean) result.getPayload()).name).isEqualTo("test");
	}

	@Test
	public void queueChannelWithSerializedObjectMessage() throws Exception {
		byte[] bytes = serialize(new TestBean());
		queueInput.send(new GenericMessage<>(bytes));
		Message<?> result = output.receive(10000);
		assertThat(result).isNotNull();
		assertThat(result.getPayload().getClass()).isEqualTo(TestBean.class);
		assertThat(((TestBean) result.getPayload()).name).isEqualTo("test");
	}

	@Test
	public void invalidPayload() {
		byte[] bytes = {1, 2, 3};
		assertThatExceptionOfType(MessageTransformationException.class)
				.isThrownBy(() -> directInput.send(new GenericMessage<>(bytes)));
	}

	@Test
	public void customDeserializer() {
		customDeserializerInput.send(new GenericMessage<>("test".getBytes(StandardCharsets.UTF_8)));
		Message<?> result = output.receive(10000);
		assertThat(result).isNotNull();
		assertThat(result.getPayload().getClass()).isEqualTo(String.class);
		assertThat(result.getPayload()).isEqualTo("TEST");
	}


	private static byte[] serialize(Object object) throws Exception {
		ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
		ObjectOutputStream objectStream = new ObjectOutputStream(byteStream);
		objectStream.writeObject(object);
		return byteStream.toByteArray();
	}


	@SuppressWarnings("serial")
	private static class TestBean implements Serializable {

		TestBean() {
			super();
		}

		public final String name = "test";

	}


	public static class TestDeserializer implements Deserializer<Object> {

		@Override
		public Object deserialize(InputStream source) throws IOException {
			return FileCopyUtils.copyToString(new InputStreamReader(source, StandardCharsets.UTF_8)).toUpperCase();
		}

	}

}
