/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.xml.transformer;

import java.io.IOException;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import org.junit.jupiter.api.Test;

import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.oxm.Unmarshaller;
import org.springframework.oxm.XmlMappingException;
import org.springframework.xml.transform.StringSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Jonas Partner
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 */
public class UnmarshallingTransformerTests {

	@Test
	public void testBytesToString() {
		Unmarshaller unmarshaller = new TestUnmarshaller(false);
		UnmarshallingTransformer transformer = new UnmarshallingTransformer(unmarshaller);
		Object transformed = transformer.transformPayload("world".getBytes());
		assertThat(transformed.getClass()).isEqualTo(String.class);
		assertThat(transformed.toString()).isEqualTo("hello world");
	}

	@Test
	public void testStringSourceToString() {
		Unmarshaller unmarshaller = new TestUnmarshaller(false);
		UnmarshallingTransformer transformer = new UnmarshallingTransformer(unmarshaller);
		Object transformed = transformer.transformPayload(new StringSource("world"));
		assertThat(transformed.getClass()).isEqualTo(String.class);
		assertThat(transformed.toString()).isEqualTo("hello world");
	}

	@Test
	public void testMessageReturnValue() {
		Unmarshaller unmarshaller = new TestUnmarshaller(true);
		UnmarshallingTransformer transformer = new UnmarshallingTransformer(unmarshaller);
		Object transformed = transformer.transformPayload(new StringSource("foo"));
		assertThat(transformed.getClass()).isEqualTo(GenericMessage.class);
		assertThat(((Message<?>) transformed).getPayload()).isEqualTo("message: foo");
	}

	@Test
	public void testMessageReturnValueFromTopLevel() {
		Unmarshaller unmarshaller = new TestUnmarshaller(true);
		UnmarshallingTransformer transformer = new UnmarshallingTransformer(unmarshaller);
		Message<?> result = transformer.transform(MessageBuilder.withPayload(new StringSource("bar")).build());
		assertThat(result).isNotNull();
		assertThat(result.getPayload()).isEqualTo("message: bar");
	}

	private record TestUnmarshaller(boolean returnMessage) implements Unmarshaller {

		@Override
		public Object unmarshal(Source source) throws XmlMappingException, IOException {
			if (source instanceof StringSource) {
				char[] chars = new char[8];
				((StringSource) source).getReader().read(chars);
				if (returnMessage) {
					return new GenericMessage<>("message: " + new String(chars).trim());
				}
				return "hello " + new String(chars).trim();
			}
			else if (source instanceof StreamSource) {
				byte[] bytes = new byte[8];
				((StreamSource) source).getInputStream().read(bytes);
				if (returnMessage) {
					return new GenericMessage<>("message: " + new String(bytes).trim());
				}
				return "hello " + new String(bytes).trim();
			}
			return null;
		}

		@Override
		public boolean supports(Class<?> clazz) {
			return true;
		}

	}

}
