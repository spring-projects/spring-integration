/*
 * Copyright 2002-2010 the original author or authors.
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
import java.util.HashMap;
import java.util.Map;

import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.integration.Message;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.transformer.AbstractTransformer;
import org.springframework.util.Assert;

/**
 * Transformer implementation that converts a payload instance into a JSON string representation.
 *
 * @author Mark Fisher
 * @author James Carr <james.r.carr@gmail.com>
 * @since 2.0
 */
public class ObjectToJsonTransformer extends AbstractTransformer {
	private static final String JSON_CONTENT_TYPE = "application/json";
	private final ObjectMapper objectMapper;
	private boolean contentTypeShouldBePopulated = false;


	public ObjectToJsonTransformer(ObjectMapper objectMapper) {
		Assert.notNull(objectMapper, "objectMapper must not be null");
		this.objectMapper = objectMapper;
	}

	public ObjectToJsonTransformer() {
		this.objectMapper = new ObjectMapper();
	}


	protected String transformPayload(Object payload) throws Exception {
		StringWriter writer = new StringWriter();
		this.objectMapper.writeValue(writer, payload);
		return writer.toString();
	}

	public void setContentType(boolean contentTypeShouldBePopulated) {
		this.contentTypeShouldBePopulated = contentTypeShouldBePopulated;
		
	}

	@Override
	protected Object doTransform(Message<?> message) throws Exception {
		final String marshalledPayload = transformPayload(message.getPayload());
		final Map<String, Object> headers = new HashMap<String, Object>(message.getHeaders());
		if(contentTypeShouldBePopulated){
			headers.put("content-type", JSON_CONTENT_TYPE);
		}
		return new GenericMessage<String>(marshalledPayload, headers);
	}

}
