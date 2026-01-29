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

package org.springframework.integration.cloudevents.transformer.util;

import java.util.HashMap;
import java.util.Map;

import org.springframework.integration.support.utils.PatternMatchUtils;
import org.springframework.messaging.MessageHeaders;

/**
 * Utility class for CloudEvents.
 *
 * @author Glenn Renfro
 *
 * @since 7.1
 */
public final class CloudEventUtil {

	private CloudEventUtil() {
	}

	/**
	 * Extract CloudEvent extensions from message headers based on pattern matching.
	 * @param headers the message headers to extract extensions from
	 * @param extensionPatterns the patterns that identify extensions
	 * @return a map of header key-value pairs that match the extension patterns;
	 * an empty map if no headers match the patterns
	 */
	public static Map<String, Object> getCloudEventExtensions(MessageHeaders headers, String[] extensionPatterns) {
		if (extensionPatterns.length == 0) {
			return Map.of();
		}
		Map<String, Object> cloudEventExtensions = new HashMap<>();
		for (Map.Entry<String, Object> header : headers.entrySet()) {
			String headerKey = header.getKey();
			Boolean patternResult = PatternMatchUtils.smartMatch(headerKey, extensionPatterns);
			if (Boolean.TRUE.equals(patternResult)) {
				cloudEventExtensions.put(headerKey, header.getValue());
			}
		}
		return cloudEventExtensions;
	}

}
