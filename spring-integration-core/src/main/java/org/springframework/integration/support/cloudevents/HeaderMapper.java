/*
 * Copyright 2020 the original author or authors.
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

package org.springframework.integration.support.cloudevents;

import java.util.AbstractMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.messaging.MessageHeaders;
import org.springframework.util.Assert;

import io.cloudevents.v1.ContextAttributes;

/**
 * A Cloud Event header mapper.
 *
 * @author Artem Bilan
 *
 * @since 5.3
 */
public final class HeaderMapper {

	/**
	 * Following the signature of {@link io.cloudevents.fun.FormatHeaderMapper}
	 * @param attributes The map of attributes
	 * @param extensions The map of extensions
	 * @return The map of headers
	 */
	public static Map<String, String> map(Map<String, String> attributes, Map<String, String> extensions) {
		Assert.notNull(attributes, "'attributes' must not be null");
		Assert.notNull(extensions, "'extensions' must not be null");

		Map<String, String> result =
				attributes.entrySet()
						.stream()
						.filter(attribute ->
								attribute.getValue() != null
										&& !ContextAttributes.datacontenttype.name().equals(attribute.getKey()))
						.map(header ->
								new AbstractMap.SimpleEntry<>(
										CloudEventHeaders.PREFIX + header.getKey().toLowerCase(Locale.US),
										header.getValue()))
						.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

		result.putAll(
				extensions.entrySet()
						.stream()
						.filter(extension -> extension.getValue() != null)
						.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
		);

		Optional.ofNullable(attributes
				.get(ContextAttributes.datacontenttype.name()))
				.ifPresent((dataContentType) -> {
					result.put(MessageHeaders.CONTENT_TYPE, dataContentType);
				});

		return result;
	}

	private HeaderMapper() {
	}

}
