/*
 * Copyright 2026-present the original author or authors.
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
