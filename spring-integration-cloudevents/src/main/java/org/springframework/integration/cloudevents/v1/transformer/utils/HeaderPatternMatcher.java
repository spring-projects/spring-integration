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

package org.springframework.integration.cloudevents.v1.transformer.utils;

import java.util.Set;

import org.jspecify.annotations.Nullable;

import org.springframework.integration.support.utils.PatternMatchUtils;
import org.springframework.util.StringUtils;

/**
 * Utility class for matching header values against comma-delimited patterns.
 * <p>
 * This class provides pattern matching functionality for header categorization for cloud events
 * using Spring's PatternMatchUtils for smart pattern matching with support for
 * wildcards and special pattern syntax.
 *
 * @author Glenn Renfro
 *
 * @since 7.0
 */
public final class HeaderPatternMatcher {

	private HeaderPatternMatcher() {

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

}
