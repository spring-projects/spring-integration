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

import org.codehaus.jackson.map.ObjectMapper;

import org.springframework.integration.transformer.AbstractPayloadTransformer;
import org.springframework.util.Assert;

/**
 * Transformer implementation that converts a JSON string payload into an instance of the provided target Class.
 *
 * @author Mark Fisher
 * @since 2.0
 */
public class JsonToObjectTransformer<T> extends AbstractPayloadTransformer<String, T> {

	private final Class<T> targetClass;

	private final ObjectMapper objectMapper;


	public JsonToObjectTransformer(Class<T> targetClass) {
		this(targetClass, null);
	}

	public JsonToObjectTransformer(Class<T> targetClass, ObjectMapper objectMapper) {
		Assert.notNull(targetClass, "targetClass must not be null");
		this.targetClass = targetClass;
		this.objectMapper = (objectMapper != null) ? objectMapper : new ObjectMapper();
	}


	protected T transformPayload(String payload) throws Exception {
		return this.objectMapper.readValue(payload, this.targetClass);
	}

}
