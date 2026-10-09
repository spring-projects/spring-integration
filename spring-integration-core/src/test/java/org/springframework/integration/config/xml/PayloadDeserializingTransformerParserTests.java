/*
 * Copyright 2002-present the original author or authors.
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.serializer.DefaultDeserializer;
import org.springframework.core.serializer.Deserializer;
import org.springframework.integration.support.converter.AllowListDeserializingConverter;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.transformer.MessageTransformationException;
import org.springframework.integration.transformer.PayloadDeserializingTransformer;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.util.FileCopyUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 * @author Glenn Renfro
 */
@SpringJUnitConfig
@DirtiesContext
public class PayloadDeserializingTransformerParserTests {

	@Autowired
	private MessageChannel directInput;

	@Autowired
	private MessageChannel legacyDirectInput;

	@Autowired
	private MessageChannel queueInput;

	@Autowired
	private MessageChannel customDeserializerInput;

	@Autowired
	private MessageChannel allowListedInput;

	@Autowired
	private MessageChannel allowListedCustomDeserializerInput;

	@Autowired
	private PollableChannel output;

	@Autowired
	@Qualifier("allowListed.handler")
	private MessageHandler allowListedHandler;

	@Autowired
	@Qualifier("allowListedCustomDeserializer.handler")
	private MessageHandler allowListedCustomDeserializerHandler;

	@Autowired
	@Qualifier("constructorConfigured")
	private PayloadDeserializingTransformer constructorConfigured;

	@Autowired
	@Qualifier("direct.handler")
	private MessageHandler handler;

	@Test
	public void directChannelWithSerializedStringMessage() throws Exception {
		byte[] bytes = serialize("foo");
		directInput.send(new GenericMessage<>(bytes));
		Message<?> result = output.receive(10000);
		assertThat(result).isNotNull();
		assertThat(result.getPayload() instanceof String).isTrue();
		assertThat(result.getPayload()).isEqualTo("foo");
		Set<?> patterns =
				TestUtils.getPropertyValue(this.handler, "transformer.converter.allowedPatterns");
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

	@Test
	public void legacyConfigurationWithoutAllowListIsUnrestricted() throws Exception {
		this.legacyDirectInput.send(new GenericMessage<>(serialize(new DeclinedBean())));
		Message<?> result = this.output.receive(0);
		assertThat(result).extracting(Message::getPayload).isInstanceOf(DeclinedBean.class);
	}

	@Test
	public void allowListEnforcedAndRequired() throws Exception {
		allowListedInput.send(new GenericMessage<>(serialize(new TestBean())));
		Message<?> result = output.receive(10000);
		assertThat(result).extracting(Message::getPayload).isInstanceOf(TestBean.class);

		allowListedInput.send(new GenericMessage<>(serialize(new HashMap<>(Map.of("key", "value")))));
		result = output.receive(10000);
		assertThat(result).extracting(Message::getPayload).isEqualTo(Map.of("key", "value"));
		assertUnauthorized(allowListedInput);
		assertPatternsRequired(this.allowListedHandler);
	}

	@Test
	public void allowListPreservedWithCustomDeserializer() throws Exception {
		assertThat(TestUtils.<Object>getPropertyValue(this.allowListedCustomDeserializerHandler,
				"transformer.converter.deserializer"))
				.isInstanceOf(DefaultDeserializer.class);
		allowListedCustomDeserializerInput.send(new GenericMessage<>(serialize(new TestBean())));
		Message<?> result = output.receive(10000);
		assertThat(result).extracting(Message::getPayload).isInstanceOf(TestBean.class);
		assertUnauthorized(allowListedCustomDeserializerInput);
		assertPatternsRequired(this.allowListedCustomDeserializerHandler);
	}

	@Test
	@SuppressWarnings("deprecation")
	public void constructorArgConfiguration() throws Exception {
		assertThat(this.constructorConfigured.transform(new GenericMessage<>(serialize(new TestBean())))
				.getPayload())
				.isInstanceOf(TestBean.class);
		assertThatThrownBy(() -> this.constructorConfigured.transform(new GenericMessage<>(serialize(new DeclinedBean()))))
				.isInstanceOf(MessageTransformationException.class)
				.hasRootCauseInstanceOf(SecurityException.class);
		assertThatIllegalArgumentException().isThrownBy(this.constructorConfigured::setAllowedPatterns);
	}

	private void assertUnauthorized(MessageChannel channel) throws Exception {
		byte[] bytes = serialize(new DeclinedBean());
		assertThatExceptionOfType(MessageTransformationException.class)
				.isThrownBy(() -> channel.send(new GenericMessage<>(bytes)))
				.withRootCauseInstanceOf(SecurityException.class);
	}

	@SuppressWarnings("deprecation")
	private static void assertPatternsRequired(MessageHandler handler) {
		AllowListDeserializingConverter converter =
				TestUtils.getPropertyValue(handler, "transformer.converter");
		assertThatIllegalArgumentException().isThrownBy(converter::setAllowedPatterns);
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

	@SuppressWarnings("serial")
	private static class DeclinedBean implements Serializable {

	}

	public static class TestDeserializer implements Deserializer<Object> {

		@Override
		public Object deserialize(InputStream source) throws IOException {
			return FileCopyUtils.copyToString(new InputStreamReader(source, StandardCharsets.UTF_8)).toUpperCase();
		}

	}

}
