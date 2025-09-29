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
import java.util.Objects;
import java.util.Set;

import io.cloudevents.CloudEventExtension;
import io.cloudevents.CloudEventExtensions;
import org.jspecify.annotations.Nullable;

import org.springframework.integration.support.utils.PatternMatchUtils;
import org.springframework.messaging.MessageHeaders;
import org.springframework.util.StringUtils;

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
class ToCloudEventTransformerExtensions implements CloudEventExtension {

	/**
	 * Map storing the CloudEvent extensions extracted from message headers.
	 */
	private final Map<String, String> cloudEventExtensions;

	/**
	 * Map storing the headers that need to remain in the {@link MessageHeaders} unchanged.
	 */
	private final Map<String, Object> filteredHeaders;

	/**
	 * Constructs CloudEvent extensions by filtering message headers against patterns.
	 * <p>
	 * Headers are evaluated against the provided patterns.
	 * Only headers that match the patterns (and are not excluded by negation patterns)
	 * will be included as CloudEvent extensions.
	 *
	 * @param headers the Spring Integration message headers to process
	 * @param patterns comma-delimited patterns for header matching, may be null to include no extensions
	 */
	ToCloudEventTransformerExtensions(MessageHeaders headers, @Nullable String patterns) {
		this.cloudEventExtensions = new HashMap<>();
		this.filteredHeaders = new HashMap<>();
		headers.keySet().forEach(key -> {
			Boolean result = categorizeHeader(key, patterns);
			if (result != null && result) {
				this.cloudEventExtensions.put(key, (String) Objects.requireNonNull(headers.get(key)));
			}
			else {
				this.filteredHeaders.put(key, Objects.requireNonNull(headers.get(key)));
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

	/**
	 * Categorizes a header value by matching it against a comma-delimited pattern string.
	 * <p>
	 * This method takes a header value and matches it against one or more patterns
	 * specified in a comma-delimited string. It uses Spring's smart pattern matching
	 * which supports wildcards and other pattern matching features.
	 *
	 * @param value the header value to match against the patterns
	 * @param pattern a comma-delimited string of patterns to match against, or null.  If pattern is null then null is returned.
	 * @return {@code Boolean.TRUE} if the value starts with a pattern token,
	 *         {@code Boolean.FALSE} if the value starts with the pattern token that is prefixed with a `!`,
	 *         or {@code null} if the header starts with a value that is not enumerated in the pattern
	 */
	public static @Nullable Boolean categorizeHeader(String value, @Nullable String pattern) {
		if (pattern == null) {
			return null;
		}
		Set<String> patterns = StringUtils.commaDelimitedListToSet(pattern);
		Boolean result = null;
		for (String patternItem : patterns) {
			result = PatternMatchUtils.smartMatch(value, patternItem);
			if (result != null && result) {
				break;
			}
			else if (result != null) {
				break;
			}
		}
		return result;
	}

	public Map<String, Object> getFilteredHeaders() {
		return this.filteredHeaders;
	}

}
