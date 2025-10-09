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

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import io.cloudevents.CloudEvent;
import io.cloudevents.CloudEventExtension;
import io.cloudevents.CloudEventExtensions;
import io.cloudevents.core.builder.CloudEventBuilder;
import org.jspecify.annotations.Nullable;

import org.springframework.integration.cloudevents.transformer.strategies.CloudEventMessageFormatStrategy;
import org.springframework.integration.cloudevents.transformer.strategies.FormatStrategy;
import org.springframework.integration.support.utils.PatternMatchUtils;
import org.springframework.integration.transformer.AbstractTransformer;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

/**
 * A Spring Integration transformer that converts messages to CloudEvent format.
 * Header filtering and extension mapping is performed based on configurable patterns,
 * allowing control over which headers are preserved and which become CloudEvent extensions.
 *
 * @author Glenn Renfro
 *
 * @since 7.0
 */
public class ToCloudEventTransformer extends AbstractTransformer {

	public static String CE_PREFIX = "ce-";

	private String id = "";

	private URI source = URI.create("");

	private String type = "";

	private @Nullable String dataContentType;

	private @Nullable URI dataSchema;

	private @Nullable String subject;

	private @Nullable OffsetDateTime time;

	private final String @Nullable [] cloudEventExtensionPatterns;

	private final FormatStrategy formatStrategy;

	/**
	 * ToCloudEventTransformer Constructor
	 *
	 * @param formatStrategy The strategy that determines how the CloudEvent will be rendered
	 * @param cloudEventExtensionPatterns an array of patterns for matching headers that should become CloudEvent extensions,
	 * supports wildcards and negation with '!' prefix   If a header matches one of the '!' it is excluded from
	 * cloud event headers and the message headers. Null to disable extension mapping.
	 */
	public ToCloudEventTransformer(FormatStrategy formatStrategy,
			String @Nullable ... cloudEventExtensionPatterns) {
		this.cloudEventExtensionPatterns = cloudEventExtensionPatterns;
		this.formatStrategy = formatStrategy;
	}

	/**
	 * Constructs a {@link ToCloudEventTransformer} that defaults to the {@link CloudEventMessageFormatStrategy}. This
	 * strategy will use the default CE_PREFIX and will not contain and cloudEventExtensionPatterns.
	 *
	 */
	public ToCloudEventTransformer() {
		this(new CloudEventMessageFormatStrategy(CE_PREFIX), (String[]) null);
	}

	/**
	 * Transforms the input message into a CloudEvent message.
	 * <p>
	 * This method performs the core transformation logic:
	 * <ol>
	 *   <li>Extracts CloudEvent extensions from message headers using configured patterns</li>
	 *   <li>Builds a CloudEvent with the configured properties and message payload</li>
	 *   <li>Applies the specified conversion type to format the output</li>
	 *   <li>Filters headers to exclude those mapped to CloudEvent extensions</li>
	 * </ol>
	 *
	 * @param message the input Spring Integration message to transform
	 * @return transformed message as CloudEvent in the specified format
	 * @throws RuntimeException if serialization fails for XML, JSON, or Avro formats
	 */
	@Override
	protected Object doTransform(Message<?> message) {
		ToCloudEventTransformerExtensions extensions =
				new ToCloudEventTransformerExtensions(message.getHeaders(), this.cloudEventExtensionPatterns);
		CloudEvent cloudEvent = CloudEventBuilder.v1()
				.withId(this.id)
				.withSource(this.source)
				.withType(this.type)
				.withTime(this.time)
				.withDataContentType(this.dataContentType)
				.withDataSchema(this.dataSchema)
				.withSubject(this.subject)
				.withData(getPayloadAsBytes(message.getPayload()))
				.withExtension(extensions)
				.build();
				return this.formatStrategy.toIntegrationMessage(cloudEvent, message.getHeaders());
	}

	private static byte[] getPayloadAsBytes(Object payload) {
		if (payload instanceof byte[] bytePayload) {
			return bytePayload;
		}
		else if (payload instanceof String stringPayload) {
			return stringPayload.getBytes();
		}
		else {
			return payload.toString().getBytes();
		}
	}

	@Override
	public String getComponentType() {
		return "ce:to-cloudevents-transformer";
	}

	public String getId() {
		return this.id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public URI getSource() {
		return this.source;
	}

	public void setSource(URI source) {
		this.source = source;
	}

	public String getType() {
		return this.type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public @Nullable String getDataContentType() {
		return this.dataContentType;
	}

	public void setDataContentType(@Nullable String dataContentType) {
		this.dataContentType = dataContentType;
	}

	public @Nullable URI getDataSchema() {
		return this.dataSchema;
	}

	public void setDataSchema(@Nullable URI dataSchema) {
		this.dataSchema = dataSchema;
	}

	public @Nullable String getSubject() {
		return this.subject;
	}

	public void setSubject(@Nullable String subject) {
		this.subject = subject;
	}

	public @Nullable OffsetDateTime getTime() {
		return this.time;
	}

	public void setTime(@Nullable OffsetDateTime time) {
		this.time = time;
	}

	private static class ToCloudEventTransformerExtensions implements CloudEventExtension {

		/**
		 * Map storing the CloudEvent extensions extracted from message headers.
		 */
		private final Map<String, String> cloudEventExtensions;

		/**
		 * Construct CloudEvent extensions by filtering message headers against patterns.
		 * <p>
		 * Headers are evaluated against the provided patterns.
		 * Only headers that match the patterns (and are not excluded by negation patterns)
		 * will be included as CloudEvent extensions.
		 *
		 * @param headers the Spring Integration message headers to process
		 * @param patterns comma-delimited patterns for header matching, may be null to include no extensions
		 */
		ToCloudEventTransformerExtensions(MessageHeaders headers, String @Nullable ... patterns) {
			this.cloudEventExtensions = new HashMap<>();
			headers.keySet().forEach(key -> {
				Boolean result = categorizeHeader(key, patterns);
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

		/**
		 * Categorizes a header value by matching it against a comma-delimited pattern string.
		 * <p>
		 * This method takes a header value and matches it against one or more patterns
		 * specified in a comma-delimited string. It uses Spring's smart pattern matching
		 * which supports wildcards and other pattern matching features.
		 *
		 * @param value the header value to match against the patterns
		 * @param patterns an array of string patterns to match against, or null.  If pattern is null then null is returned.
		 * @return {@code Boolean.TRUE} if the value starts with a pattern token,
		 *         {@code Boolean.FALSE} if the value starts with the pattern token that is prefixed with a `!`,
		 *         or {@code null} if the header starts with a value that is not enumerated in the pattern
		 */
		public static @Nullable Boolean categorizeHeader(String value, String @Nullable ... patterns) {
			Boolean result = null;
			if (patterns != null) {
				for (String patternItem : patterns) {
					result = PatternMatchUtils.smartMatch(value, patternItem);
					if (result != null && result) {
						break;
					}
					else if (result != null) {
						break;
					}
				}
			}
			return result;
		}

	}
}
