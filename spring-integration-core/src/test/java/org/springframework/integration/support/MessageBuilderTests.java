/*
 * Copyright 2017-2022 the original author or authors.
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

package org.springframework.integration.support;

import java.io.Serial;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Gary Russell
 * @author Artem Bilan
 * @since 4.3.10
 */
public class MessageBuilderTests {

	@Test
	public void testReadOnlyHeaders() {
		DefaultMessageBuilderFactory factory = new DefaultMessageBuilderFactory();
		Message<?> message = factory.withPayload("bar").setHeader("foo", "baz").setHeader("qux", "fiz").build();
		assertThat(message.getHeaders().get("foo")).isEqualTo("baz");
		assertThat(message.getHeaders().get("qux")).isEqualTo("fiz");
		factory.setReadOnlyHeaders("foo");
		message = factory.fromMessage(message).build();
		assertThat(message.getHeaders().get("foo")).isNull();
		assertThat(message.getHeaders().get("qux")).isEqualTo("fiz");
		factory.addReadOnlyHeaders("qux");
		message = factory.fromMessage(message).build();
		assertThat(message.getHeaders().get("foo")).isNull();
		assertThat(message.getHeaders().get("qux")).isNull();
	}

	@Test
	public void personalInfoHeadersAreMaskedWithCustomMessage() {
		Message<String> message =
				MessageBuilder.withPayload("some_user")
						.setHeader("password", "some_password")
						.build();

		Message<String> piiMessage = new PiiMessageBuilderFactory().fromMessage(message).build();

		assertThat(piiMessage).isInstanceOf(PiiMessage.class);
		assertThat(piiMessage.getPayload()).isEqualTo("some_user");
		assertThat(piiMessage.getHeaders().get("password")).isEqualTo("some_password");
		assertThat(piiMessage.toString())
				.doesNotContain("some_password")
				.contains("******");
	}

	private static class PiiMessageBuilderFactory implements MessageBuilderFactory {

		@Override
		public <T> PiiMessageBuilder<T> fromMessage(Message<T> message) {
			return new PiiMessageBuilder<>(message.getPayload(), message);
		}

		@Override
		public <T> PiiMessageBuilder<T> withPayload(T payload) {
			return new PiiMessageBuilder<>(payload, null);
		}

	}

	private static class PiiMessageBuilder<P> extends BaseMessageBuilder<P, PiiMessageBuilder<P>> {

		PiiMessageBuilder(P payload, @Nullable Message<P> originalMessage) {
			super(payload, originalMessage);
		}

		@Override
		public Message<P> build() {
			return new PiiMessage<>(getPayload(), getHeaders());
		}

	}

	private static class PiiMessage<P> extends GenericMessage<P> {

		@Serial
		private static final long serialVersionUID = -354503673433669578L;

		PiiMessage(P payload, Map<String, Object> headers) {
			super(payload, headers);
		}

		@Override
		public String toString() {
			return "PiiMessage [payload=" + getPayload() + ", headers=" + maskHeaders(getHeaders()) + ']';
		}

		private static Map<String, Object> maskHeaders(Map<String, Object> headers) {
			return headers.entrySet()
					.stream()
					.map((entry) -> entry.getKey().equals("password") ? Map.entry(entry.getKey(), "******") : entry)
					.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
		}

	}

}
