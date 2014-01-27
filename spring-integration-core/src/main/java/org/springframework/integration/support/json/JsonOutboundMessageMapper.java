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

package org.springframework.integration.support.json;

import org.springframework.integration.mapping.OutboundMessageMapper;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * {@link OutboundMessageMapper} implementation the converts a {@link Message} to a JSON string representation.
 *
 * @author Jeremy Grelle
 * @author Mark Fisher
 * @author Artem Bilan
 * @since 2.0
 */
public class JsonOutboundMessageMapper implements OutboundMessageMapper<String> {

	private volatile boolean shouldExtractPayload = false;

	private volatile JsonObjectMapper<?, ?> jsonObjectMapper;

	public JsonOutboundMessageMapper() {
		this(JacksonJsonObjectMapperProvider.newInstance());
	}

	public JsonOutboundMessageMapper(JsonObjectMapper<?, ?> jsonObjectMapper) {
		Assert.notNull(jsonObjectMapper, "jsonObjectMapper must not be null");
		this.jsonObjectMapper = jsonObjectMapper;
	}

	public void setShouldExtractPayload(boolean shouldExtractPayload) {
		this.shouldExtractPayload = shouldExtractPayload;
	}

	public String fromMessage(Message<?> message) throws Exception {
		return this.jsonObjectMapper.toJson(this.shouldExtractPayload ? message.getPayload() : message);
	}

}
