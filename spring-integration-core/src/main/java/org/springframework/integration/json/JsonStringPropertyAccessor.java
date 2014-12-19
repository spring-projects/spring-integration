/*
 * Copyright 2013-2014 the original author or authors.
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

import java.io.IOException;

import org.springframework.core.Ordered;
import org.springframework.expression.AccessException;
import org.springframework.expression.PropertyAccessor;
import org.springframework.util.Assert;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ContainerNode;

/**
 * A SpEL {@link PropertyAccessor} that knows how to read on Jackson JSON objects.
 *
 * @author Eric Bottard
 * @author Artem Bilan
 * @author Gary Russell
 * @since 3.0
 */
public class JsonStringPropertyAccessor extends AbstractJsonPropertyAccessor implements Ordered {

	// Note: ObjectMapper is thread-safe
	private ObjectMapper objectMapper = new ObjectMapper();

	private int order = Integer.MAX_VALUE / 2;

	public void setObjectMapper(ObjectMapper objectMapper) {
		Assert.notNull(objectMapper, "'objectMapper' cannot be null");
		this.objectMapper = objectMapper;
	}

	@Override
	public int getOrder() {
		return this.order;
	}

	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	public Class<?>[] getSpecificTargetClasses() {
		return null; // so it can be positioned after the Reflection Accessor INT-3585
	}

	@Override
	ContainerNode<?> asJson(Object target) throws AccessException {
		if (target instanceof String) {
			try {
				JsonNode json = this.objectMapper.readTree((String) target);
				return assertContainerNode(json);
			}
			catch (JsonProcessingException e) {
				throw new AccessException("Exception while trying to deserialize String", e);
			}
			catch (IOException e) {
				throw new AccessException("Exception while trying to deserialize String", e);
			}
		}
		else {
			throw new IllegalStateException("Can't happen. Check SUPPORTED_CLASSES");
		}
	}

}
