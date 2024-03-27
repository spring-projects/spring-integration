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
