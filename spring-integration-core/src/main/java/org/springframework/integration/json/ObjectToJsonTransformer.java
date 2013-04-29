/*
 * Copyright 2002-2013 the original author or authors.
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

import org.springframework.integration.Message;
import org.springframework.integration.MessageHeaders;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.transformer.AbstractTransformer;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.LinkedCaseInsensitiveMap;
import org.springframework.util.StringUtils;

/**
 * Transformer implementation that converts a payload instance into a JSON string representation.
 * By default this transformer uses {@linkplain JacksonJsonObjectMapperProvider} factory
 * to get an instance of Jackson or Jackson 2 JSON-processor {@linkplain JsonObjectMapper} implementation
 * dependently of jackson-databind or jackson-mapper-asl libs in the classpath.
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

	public static final String JSON_CONTENT_TYPE = "application/json";

	private final JsonObjectMapper jsonObjectMapper;

	private volatile String contentType = JSON_CONTENT_TYPE;

	private volatile boolean contentTypeExplicitlySet = false;

	/**
	 * @deprecated in favor of {@link #ObjectToJsonTransformer(JsonObjectMapper)}
	 */
	@Deprecated
	public ObjectToJsonTransformer(Object objectMapper) {
		Assert.notNull(objectMapper, "objectMapper must not be null");
		try {
			Class<?> objectMapperClass = ClassUtils.forName("org.codehaus.jackson.map.ObjectMapper", ClassUtils.getDefaultClassLoader());
			Assert.isTrue(objectMapperClass.isAssignableFrom(objectMapper.getClass()));
			this.jsonObjectMapper = new JacksonJsonObjectMapper((org.codehaus.jackson.map.ObjectMapper) objectMapper);
		}
		catch (ClassNotFoundException e) {
			throw new IllegalArgumentException(e);
		}
	}

	public ObjectToJsonTransformer(JsonObjectMapper jsonObjectMapper) {
		Assert.notNull(jsonObjectMapper, "jsonObjectMapper must not be null");
		this.jsonObjectMapper = jsonObjectMapper;
	}

	public ObjectToJsonTransformer() {
		this.jsonObjectMapper = JacksonJsonObjectMapperProvider.newInstance();
	}

	/**
	 * Sets the content-type header value
	 *
	 * @param contentType
	 */
	public void setContentType(String contentType) {
		// only null assertion is needed since "" is a valid value
		Assert.notNull(contentType, "'contentType' must not be null");
		this.contentTypeExplicitlySet = true;
		this.contentType = contentType.trim();
	}

	@Override
	protected Object doTransform(Message<?> message) throws Exception {
		String payload = this.jsonObjectMapper.toJson(message.getPayload());
		MessageBuilder<String> messageBuilder = MessageBuilder.withPayload(payload);

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
		messageBuilder.copyHeaders(headers);
		return messageBuilder.build();
	}

}
