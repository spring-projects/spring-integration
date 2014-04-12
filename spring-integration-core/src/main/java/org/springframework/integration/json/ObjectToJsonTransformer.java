/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.integration.json;

import org.springframework.integration.support.AbstractIntegrationMessageBuilder;
import org.springframework.integration.support.json.JacksonJsonObjectMapperProvider;
import org.springframework.integration.support.json.JsonObjectMapper;
import org.springframework.integration.transformer.AbstractTransformer;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.util.Assert;
import org.springframework.util.LinkedCaseInsensitiveMap;
import org.springframework.util.StringUtils;

/**
 * Transformer implementation that converts a payload instance into a JSON string representation.
 * By default this transformer uses {@linkplain JacksonJsonObjectMapperProvider} factory
 * to get an instance of a Jackson or Jackson 2 JSON-processor {@linkplain JsonObjectMapper} implementation
 * depending on the jackson-databind or jackson-mapper-asl libs on the classpath.
 * Any other {@linkplain JsonObjectMapper} implementation can be provided.
 *
 * @author Mark Fisher
 * @author James Carr
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 * @since 2.0
 */
public class ObjectToJsonTransformer extends AbstractTransformer {

	public static enum ResultType {
		STRING, NODE
	}

	public static final String JSON_CONTENT_TYPE = "application/json";

	private final JsonObjectMapper<?, ?> jsonObjectMapper;

	private final ResultType resultType;

	private volatile String contentType = JSON_CONTENT_TYPE;

	private volatile boolean contentTypeExplicitlySet = false;

	public ObjectToJsonTransformer() {
		this(JacksonJsonObjectMapperProvider.newInstance());
	}

	public ObjectToJsonTransformer(JsonObjectMapper<?, ?> jsonObjectMapper) {
		this(jsonObjectMapper, ResultType.STRING);
	}

	public ObjectToJsonTransformer(ResultType resultType) {
		this(JacksonJsonObjectMapperProvider.newInstance(), resultType);
	}

	public ObjectToJsonTransformer(JsonObjectMapper<?, ?> jsonObjectMapper, ResultType resultType) {
		Assert.notNull(jsonObjectMapper, "jsonObjectMapper must not be null");
		Assert.notNull(resultType, "'resultType' must not be null");
		this.jsonObjectMapper = jsonObjectMapper;
		this.resultType = resultType;
	}

	/**
	 * Sets the content-type header value
	 *
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
	protected Object doTransform(Message<?> message) throws Exception {
		Object payload = ResultType.STRING.equals(this.resultType)
				? this.jsonObjectMapper.toJson(message.getPayload())
				: this.jsonObjectMapper.toJsonNode(message.getPayload());
		AbstractIntegrationMessageBuilder<Object> messageBuilder = this.getMessageBuilderFactory().withPayload(payload);

		LinkedCaseInsensitiveMap<Object> headers = new LinkedCaseInsensitiveMap<Object>();
		headers.putAll(message.getHeaders());

		if (headers.containsKey(MessageHeaders.CONTENT_TYPE)) {
			if (this.contentTypeExplicitlySet) {
				// override, unless empty
				if (StringUtils.hasLength(this.contentType)) {
					headers.put(MessageHeaders.CONTENT_TYPE, this.contentType);
				}
			}
		}
		else if (StringUtils.hasLength(this.contentType)) {
			headers.put(MessageHeaders.CONTENT_TYPE, this.contentType);
		}

		this.jsonObjectMapper.populateJavaTypes(headers, message.getPayload().getClass());

		messageBuilder.copyHeaders(headers);
		return messageBuilder.build();
	}

}
