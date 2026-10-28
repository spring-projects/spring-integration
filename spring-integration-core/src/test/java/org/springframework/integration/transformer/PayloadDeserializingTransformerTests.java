/*
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.transformer;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.core.serializer.DefaultDeserializer;
import org.springframework.integration.dsl.Transformers;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.fail;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 * @author Glenn Renfro
 */
public class PayloadDeserializingTransformerTests {

	private static final String TEST_BEAN_PATTERN = "org.springframework.integration.transformer.*";

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
		transformer.addAllowedPatterns(TEST_BEAN_PATTERN);
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

	@Test
	public void customTypeIsRejectedByDefault() throws Exception {
		PayloadDeserializingTransformer transformer = new PayloadDeserializingTransformer();
		byte[] serialized = serialize(new TestBean("test"));

		assertThatExceptionOfType(MessageTransformationException.class)
				.isThrownBy(() -> transformer.transform(new GenericMessage<>(serialized)))
				.havingRootCause()
				.isInstanceOf(SecurityException.class);
	}

	@Test
	public void addAllowedPatternsKeepsDefaults() throws Exception {
		PayloadDeserializingTransformer transformer = new PayloadDeserializingTransformer();
		transformer.addAllowedPatterns(TEST_BEAN_PATTERN);

		assertThat(transformer.transform(new GenericMessage<>(serialize(new TestBean("test")))).getPayload())
				.isInstanceOf(TestBean.class);
		assertThat(transformer.transform(new GenericMessage<>(serialize(new HashMap<>(Map.of("key", "value")))))
				.getPayload())
				.isEqualTo(Map.of("key", "value"));
	}

	@Test
	public void setDeserializerKeepsAllowedPatterns() throws Exception {
		PayloadDeserializingTransformer transformer = new PayloadDeserializingTransformer();
		transformer.setAllowedPatterns(TEST_BEAN_PATTERN);
		transformer.setDeserializer(new DefaultDeserializer());

		assertThat(transformer.transform(new GenericMessage<>(serialize(new TestBean("test")))).getPayload())
				.isInstanceOf(TestBean.class);
		assertThatExceptionOfType(MessageTransformationException.class)
				.isThrownBy(() -> transformer.transform(
						new GenericMessage<>(serialize(new HashMap<>(Map.of("key", "value"))))))
				.havingRootCause()
				.isInstanceOf(SecurityException.class);
	}

	@Test
	public void dslDeserializerWithoutPatternsUsesDefaults() throws Exception {
		PayloadDeserializingTransformer transformer = Transformers.deserializer();

		assertThat(transformer.transform(new GenericMessage<>(serialize(new HashMap<>(Map.of("key", "value")))))
				.getPayload())
				.isEqualTo(Map.of("key", "value"));
	}

	@Test
	public void dslDeserializerWithCustomDeserializerKeepsPatterns() throws Exception {
		PayloadDeserializingTransformer transformer =
				Transformers.deserializer(new DefaultDeserializer(), TEST_BEAN_PATTERN);

		assertThat(transformer.transform(new GenericMessage<>(serialize(new TestBean("test")))).getPayload())
				.isInstanceOf(TestBean.class);
	}

	private static byte[] serialize(Object value) throws Exception {
		ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
		try (ObjectOutputStream objectStream = new ObjectOutputStream(byteStream)) {
			objectStream.writeObject(value);
		}
		return byteStream.toByteArray();
	}

	@SuppressWarnings("serial")
	private static class TestBean implements Serializable {

		private final String name;

		TestBean(String name) {
			this.name = name;
		}

	}

}
