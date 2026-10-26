/*
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.jdbc.store;

import org.junit.jupiter.api.Test;

import org.springframework.core.serializer.DefaultDeserializer;
import org.springframework.core.serializer.Deserializer;
import org.springframework.integration.support.converter.AllowListDeserializingConverter;
import org.springframework.integration.test.util.TestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author Glenn Renfro
 *
 * @since 7.2
 */
class JdbcChannelMessageStoreAllowedPatternsTests {

	private final JdbcChannelMessageStore messageStore = new JdbcChannelMessageStore(mock());

	@Test
	void defaultsAreApplied() {
		assertThat(allowedPatterns()).containsExactlyElementsOf(AllowListDeserializingConverter.DEFAULT_ALLOWED_PATTERNS);
	}

	@Test
	void addAllowedPatternsKeepsDefaults() {
		this.messageStore.addAllowedPatterns("com.example.*");

		assertThat(allowedPatterns())
				.containsAll(AllowListDeserializingConverter.DEFAULT_ALLOWED_PATTERNS)
				.contains("com.example.*");
	}

	@Test
	@SuppressWarnings({"unchecked", "rawtypes"})
	void setDeserializerKeepsAllowedPatterns() {
		this.messageStore.addAllowedPatterns("com.example.*");
		this.messageStore.setDeserializer((Deserializer) new DefaultDeserializer());

		assertThat(allowedPatterns()).contains("com.example.*");
	}

	@Test
	void setAllowedPatternsReplacesDefaults() {
		this.messageStore.setAllowedPatterns("com.example.*");

		assertThat(allowedPatterns()).containsExactly("com.example.*");
	}

	private Iterable<String> allowedPatterns() {
		return TestUtils.getPropertyValue(this.messageStore, "deserializer.allowedPatterns");
	}

}
