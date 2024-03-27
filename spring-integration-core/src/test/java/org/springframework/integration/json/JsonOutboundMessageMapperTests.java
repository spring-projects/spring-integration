/*
 * Copyright 2002-2024 the original author or authors.
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

import java.io.IOException;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import org.springframework.integration.history.MessageHistory;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.support.context.NamedComponent;
import org.springframework.integration.support.json.JsonOutboundMessageMapper;
import org.springframework.messaging.Message;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Jeremy Grelle
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.0
 */
public class JsonOutboundMessageMapperTests {

	private final JsonFactory jsonFactory = new JsonFactory();

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	public void testFromMessageWithHeadersAndStringPayload() {
		Message<String> testMessage = MessageBuilder.withPayload("myPayloadStuff").build();
		JsonOutboundMessageMapper mapper = new JsonOutboundMessageMapper();
		String result = mapper.fromMessage(testMessage);
		assertThat(result).contains("\"headers\":{");
		assertThat(result).contains("\"timestamp\":" + testMessage.getHeaders().getTimestamp());
		assertThat(result).contains("\"id\":\"" + testMessage.getHeaders().getId() + "\"");
		assertThat(result).contains("\"payload\":\"myPayloadStuff\"");
	}

	@Test
	public void testFromMessageWithMessageHistory() {
		Message<String> testMessage = MessageBuilder.withPayload("myPayloadStuff").build();
		testMessage = MessageHistory.write(testMessage, new TestNamedComponent(1));
		testMessage = MessageHistory.write(testMessage, new TestNamedComponent(2));
		testMessage = MessageHistory.write(testMessage, new TestNamedComponent(3));
		JsonOutboundMessageMapper mapper = new JsonOutboundMessageMapper();
		String result = mapper.fromMessage(testMessage);
		assertThat(result).contains("\"headers\":{");
		assertThat(result).contains("\"timestamp\":" + testMessage.getHeaders().getTimestamp());
		assertThat(result).contains("\"id\":\"" + testMessage.getHeaders().getId() + "\"");
		assertThat(result).contains("\"payload\":\"myPayloadStuff\"");
		assertThat(result).contains("\"history\":");
		assertThat(result).contains("testName-1");
		assertThat(result).contains("testType-1");
		assertThat(result).contains("testName-2");
		assertThat(result).contains("testType-2");
		assertThat(result).contains("testName-3");
		assertThat(result).contains("testType-3");
	}

	@Test
	public void testFromMessageExtractStringPayload() {
		Message<String> testMessage = MessageBuilder.withPayload("myPayloadStuff").build();
		String expected = "\"myPayloadStuff\"";
		JsonOutboundMessageMapper mapper = new JsonOutboundMessageMapper();
		mapper.setShouldExtractPayload(true);
		String result = mapper.fromMessage(testMessage);
		assertThat(result).isEqualTo(expected);
	}

	@Test
	public void testFromMessageWithHeadersAndBeanPayload() throws Exception {
		TestBean payload = new TestBean();
		Message<TestBean> testMessage = MessageBuilder.withPayload(payload).build();
		JsonOutboundMessageMapper mapper = new JsonOutboundMessageMapper();
		String result = mapper.fromMessage(testMessage);
		assertThat(result).contains("\"headers\":{");
		assertThat(result).contains("\"timestamp\":" + testMessage.getHeaders().getTimestamp());
		assertThat(result).contains("\"id\":\"" + testMessage.getHeaders().getId() + "\"");
		TestBean parsedPayload = extractJsonPayloadToTestBean(result);
		assertThat(parsedPayload).isEqualTo(payload);
	}

	@Test
	public void testFromMessageExtractBeanPayload() throws Exception {
		TestBean payload = new TestBean();
		Message<TestBean> testMessage = MessageBuilder.withPayload(payload).build();
		JsonOutboundMessageMapper mapper = new JsonOutboundMessageMapper();
		mapper.setShouldExtractPayload(true);
		String result = mapper.fromMessage(testMessage);
		assertThat(result).doesNotContain("headers");
		TestBean parsedPayload = objectMapper.readValue(result, TestBean.class);
		assertThat(parsedPayload).isEqualTo(payload);
	}

	private TestBean extractJsonPayloadToTestBean(String json) throws IOException {
		JsonParser parser = jsonFactory.createParser(json);
		do {
			parser.nextToken();
		} while (parser.getCurrentToken() != JsonToken.FIELD_NAME || !parser.currentName().equals("payload"));
		parser.nextToken();
		return objectMapper.readValue(parser, TestBean.class);
	}

	private record TestNamedComponent(int id) implements NamedComponent {

		@Override
		public String getComponentName() {
			return "testName-" + this.id;
		}

		@Override
		public String getComponentType() {
			return "testType-" + this.id;
		}

	}

}
