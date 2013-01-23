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

import java.io.StringWriter;

import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.integration.Message;
import org.springframework.integration.MessageHeaders;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.transformer.AbstractTransformer;
import org.springframework.util.Assert;
import org.springframework.util.LinkedCaseInsensitiveMap;
import org.springframework.util.StringUtils;

/**
 * Transformer implementation that converts a payload instance into a JSON string representation.
 *
 * @author Mark Fisher
 * @author James Carr
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @since 2.0
 */
public class ObjectToJsonTransformer extends AbstractTransformer {

	public static final String JSON_CONTENT_TYPE = "application/json";

	private final ObjectMapper objectMapper;

	private volatile String contentType = JSON_CONTENT_TYPE;
	private volatile boolean contentTypeExplicitlySet = false;

	public ObjectToJsonTransformer(ObjectMapper objectMapper) {
		Assert.notNull(objectMapper, "objectMapper must not be null");
		this.objectMapper = objectMapper;
	}

	public ObjectToJsonTransformer() {
		this.objectMapper = new ObjectMapper();
	}

	/**
	 * Sets the content-type header value
	 *
	 * @param contentType
	 */
	public void setContentType(String contentType){
		// only null assertion is needed since "" is a valid value
		Assert.notNull(contentType, "'contentType' must not be null");
		this.contentTypeExplicitlySet = true;
		this.contentType = contentType.trim();
	}

	private String transformPayload(Object payload) throws Exception {
		StringWriter writer = new StringWriter();
		this.objectMapper.writeValue(writer, payload);
		return writer.toString();
	}

	@Override
	protected Object doTransform(Message<?> message) throws Exception {
		String payload = this.transformPayload(message.getPayload());
		MessageBuilder<String> messageBuilder = MessageBuilder.withPayload(payload);

		LinkedCaseInsensitiveMap<Object> headers = new LinkedCaseInsensitiveMap<Object>();
		headers.putAll(message.getHeaders());

		if (headers.containsKey(MessageHeaders.CONTENT_TYPE)) {
			if (this.contentTypeExplicitlySet){
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