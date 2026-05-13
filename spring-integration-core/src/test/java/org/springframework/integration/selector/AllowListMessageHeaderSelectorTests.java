/*
 * Copyright 2026-present the original author or authors.
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

package org.springframework.integration.selector;

import org.junit.jupiter.api.Test;

import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Artem Bilan
 *
 * @since 6.5.9
 */
class AllowListMessageHeaderSelectorTests {

	@Test
	void verifyAllowListMessageHeaderSelectorWithPatterns() {
		AllowListMessageHeaderSelector allowListMessageHeaderSelector =
				new AllowListMessageHeaderSelector("headerName", "!test*malicious*", "test*");

		Message<?> message =
				MessageBuilder.withPayload("test")
						.setHeader("headerName", "test1")
						.build();

		assertThat(allowListMessageHeaderSelector.accept(message)).isTrue();

		message =
				MessageBuilder.withPayload("test")
						.setHeader("headerName", "test1malicious2")
						.build();

		assertThat(allowListMessageHeaderSelector.accept(message)).isFalse();

		assertThat(allowListMessageHeaderSelector.accept(new GenericMessage<>("with no header"))).isTrue();

		allowListMessageHeaderSelector.setAcceptNulls(false);

		assertThat(allowListMessageHeaderSelector.accept(new GenericMessage<>("with no header"))).isFalse();
	}

	@Test
	void verifyAllowListMessageHeaderSelectorWithByteArray() {
		AllowListMessageHeaderSelector allowListMessageHeaderSelector =
				new AllowListMessageHeaderSelector("headerName", "!test*malicious*", "test*");

		Message<?> message =
				MessageBuilder.withPayload("test")
						.setHeader("headerName", "test1".getBytes())
						.build();

		assertThat(allowListMessageHeaderSelector.accept(message)).isTrue();

		message =
				MessageBuilder.withPayload("test")
						.setHeader("headerName", "test1malicious2".getBytes())
						.build();

		assertThat(allowListMessageHeaderSelector.accept(message)).isFalse();
	}

	@Test
	void verifyAllowListMessageHeaderSelectorEvadesRCEWhenHeaderIsStringArray() {
		AllowListMessageHeaderSelector selector =
				new AllowListMessageHeaderSelector("targetClass", "!*ProcessBuilder*", "*");

		Message<?> message =
				MessageBuilder.withPayload("test")
						.setHeader("targetClass", new String[] {"java.lang.ProcessBuilder"})
						.build();

		assertThat(selector.accept(message)).isFalse();
	}

	@Test
	void verifyAllowListMessageHeaderSelectorEvadesRCEWhenHeaderIsCharArray() {
		AllowListMessageHeaderSelector selector =
				new AllowListMessageHeaderSelector("targetClass", "!*ProcessBuilder*", "*");

		Message<?> message =
				MessageBuilder.withPayload("test")
						.setHeader("targetClass", "java.lang.ProcessBuilder".toCharArray())
						.build();

		assertThat(selector.accept(message)).isFalse();
	}

}
