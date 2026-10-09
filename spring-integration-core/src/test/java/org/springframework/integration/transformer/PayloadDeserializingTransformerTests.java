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

package org.springframework.integration.transformer;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import org.junit.jupiter.api.Test;

import org.springframework.core.serializer.DefaultDeserializer;
import org.springframework.core.serializer.support.SerializationFailedException;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.support.converter.AllowListDeserializingConverter;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.fail;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 * @author Glenn Renfro
 */
public class PayloadDeserializingTransformerTests {

	@Test
	public void deserializeString() throws Exception {
		ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
		ObjectOutputStream objectStream = new ObjectOutputStream(byteStream);
		objectStream.writeObject("foo");
		byte[] serialized = byteStream.toByteArray();
		PayloadDeserializingTransformer transformer = new PayloadDeserializingTransformer(TestBean.class.getName());
		Message<?> result = transformer.transform(new GenericMessage<>(serialized));
		Object payload = result.getPayload();
		assertThat(payload).isNotNull();
		assertThat(payload.getClass()).isEqualTo(String.class);
		assertThat(payload).isEqualTo("foo");
	}

	@Test
	@SuppressWarnings("deprecation")
	public void legacyConstructorDeserializesAnyObject() throws Exception {
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
	@SuppressWarnings("deprecation")
	public void legacyConstructorSetAllowedPatterns() throws Exception {
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
		PayloadDeserializingTransformer transformer = new PayloadDeserializingTransformer(TestBean.class.getName());
		assertThatExceptionOfType(MessageTransformationException.class)
				.isThrownBy(() -> transformer.transform(new GenericMessage<>(bytes)));
	}

	@Test
	public void customDeserializer() {
		PayloadDeserializingTransformer transformer = new PayloadDeserializingTransformer(TestBean.class.getName());
		transformer.setConverter(source -> "Converted");
		Message<?> message = transformer.transform(MessageBuilder.withPayload("Test".getBytes()).build());
		assertThat(message.getPayload()).isEqualTo("Converted");
	}

	@Test
	public void patternsConstructorEnforcesAllowList() throws Exception {
		PayloadDeserializingTransformer transformer = new PayloadDeserializingTransformer(TestBean.class.getName());
		Message<?> result = transformer.transform(new GenericMessage<>(serialize(new TestBean("test"))));
		assertThat(result.getPayload()).isInstanceOf(TestBean.class);
		assertUnauthorized(transformer, serialize(new DeclinedBean()));
	}

	@Test
	public void patternsConstructorRejectsInvalidPatterns() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new PayloadDeserializingTransformer((String[]) null))
				.withMessage("'allowedPatterns' must not be empty");
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new PayloadDeserializingTransformer(new String[0]))
				.withMessage("'allowedPatterns' must not be empty");
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new PayloadDeserializingTransformer(TestBean.class.getName(), " "))
				.withMessageContaining("whitespace-only");
	}

	@Test
	@SuppressWarnings("deprecation")
	public void patternsConstructorRejectsClearingPatterns() throws Exception {
		PayloadDeserializingTransformer transformer = new PayloadDeserializingTransformer(TestBean.class.getName());
		assertThatIllegalArgumentException()
				.isThrownBy(transformer::setAllowedPatterns);
		assertUnauthorized(transformer, serialize(new DeclinedBean()));
	}

	@Test
	@SuppressWarnings("deprecation")
	public void setDeserializerPreservesPatternsAndConstructionMode() throws Exception {
		PayloadDeserializingTransformer transformer = new PayloadDeserializingTransformer(TestBean.class.getName());
		transformer.setDeserializer(new DefaultDeserializer(getClass().getClassLoader()));
		assertThat(transformer.transform(new GenericMessage<>(serialize(new TestBean("test")))).getPayload())
				.isInstanceOf(TestBean.class);
		assertUnauthorized(transformer, serialize(new DeclinedBean()));
		assertThatIllegalArgumentException()
				.isThrownBy(transformer::setAllowedPatterns);
	}

	@Test
	public void setDeserializerRejectsCustomConverter() {
		PayloadDeserializingTransformer transformer = new PayloadDeserializingTransformer(TestBean.class.getName());
		transformer.setConverter(source -> source);
		assertThatIllegalStateException()
				.isThrownBy(() -> transformer.setDeserializer(new DefaultDeserializer()));
	}

	@Test
	@SuppressWarnings("deprecation")
	public void legacySetDeserializerPreservesPatterns() throws Exception {
		PayloadDeserializingTransformer transformer = new PayloadDeserializingTransformer();
		transformer.setAllowedPatterns(TestBean.class.getName());
		transformer.setDeserializer(new DefaultDeserializer());
		assertUnauthorized(transformer, serialize(new DeclinedBean()));
		assertThatIllegalArgumentException().isThrownBy(transformer::setAllowedPatterns);
		assertUnauthorized(transformer, serialize(new DeclinedBean()));
	}

	@Test
	@SuppressWarnings("deprecation")
	public void addAllowedPatternsOnConverterAllowsPreviouslyRejectedClass() throws Exception {
		PayloadDeserializingTransformer transformer = new PayloadDeserializingTransformer(TestBean.class.getName());
		byte[] untrusted = serialize(new DeclinedBean());
		assertUnauthorized(transformer, untrusted);
		((AllowListDeserializingConverter) transformer.getConverter())
				.addAllowedPatterns(DeclinedBean.class.getName());
		assertThat(transformer.transform(new GenericMessage<>(untrusted)).getPayload())
				.isInstanceOf(DeclinedBean.class);
	}

	private static byte[] serialize(Object object) throws Exception {
		ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
		try (ObjectOutputStream objectStream = new ObjectOutputStream(byteStream)) {
			objectStream.writeObject(object);
		}
		return byteStream.toByteArray();
	}

	private static void assertUnauthorized(PayloadDeserializingTransformer transformer, byte[] bytes) {
		assertThatExceptionOfType(MessageTransformationException.class)
				.isThrownBy(() -> transformer.transform(new GenericMessage<>(bytes)))
				.havingCause()
				.isInstanceOf(SerializationFailedException.class)
				.havingCause()
				.isInstanceOf(SecurityException.class)
				.withMessageStartingWith("Attempt to deserialize unauthorized");
	}

	@SuppressWarnings("serial")
	private static class DeclinedBean implements Serializable {

	}

	@SuppressWarnings("serial")
	private static class TestBean implements Serializable {

		private final String name;

		TestBean(String name) {
			this.name = name;
		}

	}

}
