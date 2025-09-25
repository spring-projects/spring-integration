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

package org.springframework.integration.cloudevents.v1.transformer;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import io.cloudevents.CloudEventExtension;
import io.cloudevents.CloudEventExtensions;
import org.jspecify.annotations.Nullable;

import org.springframework.integration.cloudevents.v1.transformer.utils.HeaderPatternMatcher;
import org.springframework.messaging.MessageHeaders;

/**
 * CloudEvent extension implementation that extracts extensions from Spring Integration message headers.
 * <p>
 * This class implements the CloudEvent extension contract by filtering message headers
 * based on configurable patterns and converting matching headers into CloudEvent extensions.
 * It supports pattern-based inclusion and exclusion of headers using Spring's pattern matching utilities.
 * <p>
 * Pattern matching supports:
 * <ul>
 *   <li>Wildcard patterns (e.g., "trace-*" matches "trace-id", "trace-span") means the matching header will be moved
 *   to the CloudEvent extensions.</li>
 *   <li>Negation patterns with '!' prefix (e.g., "!internal-*" excludes internal headers) means the matching header
 *   will be not be moved to the CloudEvent extensions or left in the message header.</li>
 *   <li>Comma-delimited multiple patterns (e.g., "trace-*,span-*,!internal-*")</li>
 * </ul>
 *
 * @author Glenn Renfro
 *
 * @since 7.0
 */
public class ToCloudEventTransformerExtensions implements CloudEventExtension {

	/**
	 * Internal map storing the CloudEvent extensions extracted from message headers.
	 */
	private final Map<String, String> cloudEventExtensions;

	/**
	 * Constructs CloudEvent extensions by filtering message headers against patterns.
	 * <p>
	 * Headers are evaluated against the provided patterns using {@link HeaderPatternMatcher}.
	 * Only headers that match the patterns (and are not excluded by negation patterns)
	 * will be included as CloudEvent extensions.
	 *
	 * @param headers the Spring Integration message headers to process
	 * @param patterns comma-delimited patterns for header matching, may be null to include no extensions
	 */
	public ToCloudEventTransformerExtensions(MessageHeaders headers, @Nullable String patterns) {
		this.cloudEventExtensions = new HashMap<>();
		headers.keySet().forEach(key -> {
			Boolean result = HeaderPatternMatcher.categorizeHeader(key, patterns);
			if (result != null && result) {
				this.cloudEventExtensions.put(key, (String) Objects.requireNonNull(headers.get(key)));
			}
		});
	}

	@Override
	public void readFrom(CloudEventExtensions extensions) {
		extensions.getExtensionNames()
				.forEach(key -> {
					this.cloudEventExtensions.put(key, this.cloudEventExtensions.get(key));
				});
	}

	@Override
	public @Nullable Object getValue(String key) throws IllegalArgumentException {
		return this.cloudEventExtensions.get(key);
	}

	@Override
	public Set<String> getKeys() {
		return this.cloudEventExtensions.keySet();
	}

}
