/*
 * Copyright 2025-present the original author or authors.
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

package org.springframework.integration.cloudevents.transformer;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.messaging.MessageHeaders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class ToCloudEventTransformerExtensionsTest {

	private String extensionPatterns;

	private Map<String, Object> headers;

	@BeforeEach
	void setUp() {
		this.extensionPatterns = "source-header,another-header";

		this.headers = new HashMap<>();
		this.headers.put("source-header", "header-value");
		this.headers.put("another-header", "another-value");
		this.headers.put("unmapped-header", "unmapped-value");
	}

	@Test
	void constructorMapsHeadersToExtensions() {
		MessageHeaders messageHeaders = new MessageHeaders(this.headers);
		ToCloudEventTransformerExtensions extensions = new ToCloudEventTransformerExtensions(
				messageHeaders, this.extensionPatterns
		);

		assertThat(extensions.getValue("source-header")).isEqualTo("header-value");
		assertThat(extensions.getValue("another-header")).isEqualTo("another-value");
		assertThat(extensions.getValue("unmapped-header")).isNull();
	}

	@Test
	void getKeysReturnsAllExtensionKeys() {
		MessageHeaders messageHeaders = new MessageHeaders(this.headers);
		ToCloudEventTransformerExtensions extensions = new ToCloudEventTransformerExtensions(messageHeaders,
				this.extensionPatterns);

		Set<String> keys = extensions.getKeys();
		assertThat(keys).contains("source-header");
		assertThat(keys).contains("another-header");
		assertThat(keys).doesNotContain("unmapped-header");
		assertThat(keys.size()).isGreaterThanOrEqualTo(2);
	}

	@Test
	void excludePatternExtensionKeys() {
		MessageHeaders messageHeaders = new MessageHeaders(this.headers);
		ToCloudEventTransformerExtensions extensions = new ToCloudEventTransformerExtensions(messageHeaders,
				"!source*,another*");

		Set<String> keys = extensions.getKeys();
		assertThat(keys).contains("another-header");
		assertThat(keys).doesNotContain("unmapped-header");
		assertThat(keys).doesNotContain("source-header");
		assertThat(keys.size()).isGreaterThanOrEqualTo(1);
	}

	@Test
	void forNonExistentExtensionKey() {
		MessageHeaders messageHeaders = new MessageHeaders(this.headers);
		ToCloudEventTransformerExtensions extensions = new ToCloudEventTransformerExtensions(
				messageHeaders, this.extensionPatterns);

		assertThat(extensions.getValue("non-existent-key")).isNull();
	}

	@Test
	void emptyExtensionNamesMap() {
		MessageHeaders messageHeaders = new MessageHeaders(this.headers);
		ToCloudEventTransformerExtensions extensions = new ToCloudEventTransformerExtensions(
				messageHeaders, null
		);

		assertThat(extensions.getKeys()).isEmpty();
		assertThat(extensions.getValue("any-key")).isNull();
	}

	@Test
	void emptyHeaders() {
		MessageHeaders messageHeaders = new MessageHeaders(new HashMap<>());
		ToCloudEventTransformerExtensions extensions = new ToCloudEventTransformerExtensions(
				messageHeaders, this.extensionPatterns);

		Set<String> keys = extensions.getKeys();
		assertThat(keys).isEmpty();
	}

	@Test
	void invalidHeaderType() {
		Map<String, Object> mixedHeaders = new HashMap<>();
		mixedHeaders.put("source-header", "string-value");
		mixedHeaders.put("another-header", 123); // Non-string value
		MessageHeaders messageHeaders = new MessageHeaders(mixedHeaders);
		assertThatExceptionOfType(ClassCastException.class).isThrownBy(
				() -> new ToCloudEventTransformerExtensions(messageHeaders, this.extensionPatterns));
	}
}
