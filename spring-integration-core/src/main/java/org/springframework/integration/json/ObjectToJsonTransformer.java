/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.integration.json;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.util.Map;

import org.springframework.integration.support.json.JsonObjectMapper;
import org.springframework.integration.support.json.JsonObjectMapperProvider;
import org.springframework.integration.transformer.AbstractTransformer;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.util.Assert;
import org.springframework.util.LinkedCaseInsensitiveMap;
import org.springframework.util.StringUtils;

/**
 * Transformer implementation that converts a payload instance into a JSON string
 * representation. By default this transformer uses
 * {@linkplain org.springframework.integration.support.json.JsonObjectMapperProvider}
 * factory to get an instance of a Jackson or Jackson 2 JSON-processor
 * {@linkplain JsonObjectMapper} implementation depending on the jackson-databind or
 * jackson-mapper-asl libs on the classpath. Any other {@linkplain JsonObjectMapper}
 * implementation can be provided.
 * <p>Since version 3.0, adds headers to represent the object types that were mapped
 * from (including one level of container and Map content types). These headers
 * are compatible with the Spring AMQP Json type mapper such that messages mapped
 * or converted by either technology are compatible. One difference, however, is the
 * Spring AMQP converter, when converting to JSON, sets the header types to the class
 * name. This transformer sets the header types to the class itself.
 * <p>The compatibility is achieved because, when mapping the Spring Integration
 * message in the outbound endpoint (via the {@code DefaultAmqpHeaderMapper}), the
 * classes are converted to String at that time.
 * <p>Note: the first element of container/map types are used to determine the
 * container/map content types. If the first element is null, the type is set to
 * {@link Object}.
 *
 * @author Mark Fisher
 * @author James Carr
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.0
 */
public class ObjectToJsonTransformer extends AbstractTransformer {

	public enum ResultType {
		STRING, NODE, BYTES
	}

	public static final String JSON_CONTENT_TYPE = "application/json";

	private final JsonObjectMapper<?, ?> jsonObjectMapper;

	private final ResultType resultType;

	private volatile String contentType = JSON_CONTENT_TYPE;

	private volatile boolean contentTypeExplicitlySet = false;

	public ObjectToJsonTransformer() {
		this(JsonObjectMapperProvider.newInstance());
	}

	public ObjectToJsonTransformer(JsonObjectMapper<?, ?> jsonObjectMapper) {
		this(jsonObjectMapper, ResultType.STRING);
	}

	public ObjectToJsonTransformer(ResultType resultType) {
		this(JsonObjectMapperProvider.newInstance(), resultType);
	}

	public ObjectToJsonTransformer(JsonObjectMapper<?, ?> jsonObjectMapper, ResultType resultType) {
		Assert.notNull(jsonObjectMapper, "jsonObjectMapper must not be null");
		Assert.notNull(resultType, "'resultType' must not be null");
		this.jsonObjectMapper = jsonObjectMapper;
		this.resultType = resultType;
	}

	/**
	 * Set the content-type header value.
	 * @param contentType The content type.
	 */
	public void setContentType(String contentType) {
		// only null assertion is needed since "" is a valid value
		Assert.notNull(contentType, "'contentType' must not be null");
		this.contentTypeExplicitlySet = true;
		this.contentType = contentType.trim();
	}

	@Override
	public String getComponentType() {
		return "object-to-json-transformer";
	}

	@Override
	protected Object doTransform(Message<?> message) {
		Object payload = buildJsonPayload(message.getPayload());

		Map<String, Object> headers = new LinkedCaseInsensitiveMap<>();
		headers.putAll(message.getHeaders());

		if (headers.containsKey(MessageHeaders.CONTENT_TYPE)) {
			// override, unless empty
			if (this.contentTypeExplicitlySet && StringUtils.hasLength(this.contentType)) {
				headers.put(MessageHeaders.CONTENT_TYPE, this.contentType);
			}
		}
		else if (StringUtils.hasLength(this.contentType)) {
			headers.put(MessageHeaders.CONTENT_TYPE, this.contentType);
		}

		this.jsonObjectMapper.populateJavaTypes(headers, message.getPayload());

		return getMessageBuilderFactory()
				.withPayload(payload)
				.copyHeaders(headers)
				.build();
	}

	private Object buildJsonPayload(Object payload) {
		try {
			switch (this.resultType) {

				case STRING:
					return this.jsonObjectMapper.toJson(payload);

				case NODE:
					return this.jsonObjectMapper.toJsonNode(payload);

				case BYTES:
					try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
						this.jsonObjectMapper.toJson(payload, new OutputStreamWriter(baos));
						return baos.toByteArray();
					}

				default:
					throw new IllegalArgumentException("Unsupported ResultType provided: " + this.resultType);
			}
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

}
