/*
 * Copyright 2015-present the original author or authors.
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

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.codec.Codec;
import org.springframework.integration.test.support.TestApplicationContextAware;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 4.2
 *
 */
public class CodecTransformerTests implements TestApplicationContextAware {

	@Test
	public void testCodec() {
		MyCodec codec = new MyCodec();
		EncodingPayloadTransformer<String> enc = new EncodingPayloadTransformer<>(codec);
		Message<?> message = new GenericMessage<>("bar");
		byte[] transformed = enc.doTransform(message);
		assertThat(transformed).isEqualTo("foo".getBytes());
		DecodingTransformer<?> dec = new DecodingTransformer<>(codec, String.class);
		assertThat(dec.doTransform(new GenericMessage<>(transformed))).isEqualTo("foo");

		dec = new DecodingTransformer<Integer>(codec, new SpelExpressionParser().parseExpression("T(Integer)"));
		dec.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		dec.afterPropertiesSet();
		assertThat(dec.doTransform(new GenericMessage<>(transformed))).isEqualTo(42);

		dec = new DecodingTransformer<Integer>(codec, new SpelExpressionParser().parseExpression("headers['type']"));
		dec.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		dec.afterPropertiesSet();
		assertThat(dec.doTransform(new GenericMessage<>(transformed,
				Collections.singletonMap("type", Integer.class)))).isEqualTo(42);
	}

	public static class MyCodec implements Codec {

		@Override
		public void encode(Object object, OutputStream outputStream) {
		}

		@Override
		public byte[] encode(Object object) {
			return "foo".getBytes();
		}

		@Override
		public <T> T decode(InputStream inputStream, Class<T> type) {
			return null;
		}

		@SuppressWarnings("unchecked")
		@Override
		public <T> T decode(byte[] bytes, Class<T> type) {
			return (T) (type.equals(String.class)
					? new String(bytes)
					: type.equals(Integer.class)
						? Integer.valueOf(42)
						: Integer.valueOf(43));
		}

	}

}
