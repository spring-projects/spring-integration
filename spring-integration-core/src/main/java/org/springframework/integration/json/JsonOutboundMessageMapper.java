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

import org.codehaus.jackson.map.ObjectMapper;

import org.springframework.integration.Message;
import org.springframework.integration.mapping.OutboundMessageMapper;

/**
 * {@link OutboundMessageMapper} implementation the converts a {@link Message} to a JSON string representation.
 * 
 * @author Jeremy Grelle
 * @since 2.0
 */
public class JsonOutboundMessageMapper implements OutboundMessageMapper<String> {

	private volatile boolean shouldExtractPayload = false;

	private final ObjectMapper objectMapper = new ObjectMapper();


	public void setShouldExtractPayload(boolean shouldExtractPayload) {
		this.shouldExtractPayload = shouldExtractPayload;
	}

	public String fromMessage(Message<?> message) throws Exception {
		StringWriter writer = new StringWriter();
		if (this.shouldExtractPayload) {
			this.objectMapper.writeValue(writer, message.getPayload());
		}
		else {
			this.objectMapper.writeValue(writer, message);
		}
		return writer.toString();
	}

}
