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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Test;

import org.springframework.integration.history.MessageHistory;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.support.context.NamedComponent;
import org.springframework.integration.support.json.JsonOutboundMessageMapper;
import org.springframework.messaging.Message;

/**
 * @author Jeremy Grelle
 * @since 2.0
 */
public class JsonOutboundMessageMapperTests {

	private final JsonFactory jsonFactory = new JsonFactory();

	private final ObjectMapper objectMapper = new ObjectMapper();


	@Test
	public void testFromMessageWithHeadersAndStringPayload() throws Exception {
		Message<String> testMessage = MessageBuilder.withPayload("myPayloadStuff").build();
		JsonOutboundMessageMapper mapper = new JsonOutboundMessageMapper();
		String result = mapper.fromMessage(testMessage);
		assertTrue(result.contains("\"headers\":{"));
		assertTrue(result.contains("\"timestamp\":"+testMessage.getHeaders().getTimestamp()));
		assertTrue(result.contains("\"id\":\""+testMessage.getHeaders().getId()+"\""));
		assertTrue(result.contains("\"payload\":\"myPayloadStuff\""));
	}

	@Test
	public void testFromMessageWithMessageHistory() throws Exception {
		Message<String> testMessage = MessageBuilder.withPayload("myPayloadStuff").build();
		testMessage = MessageHistory.write(testMessage, new TestNamedComponent(1));
		testMessage = MessageHistory.write(testMessage, new TestNamedComponent(2));
		testMessage = MessageHistory.write(testMessage, new TestNamedComponent(3));
		JsonOutboundMessageMapper mapper = new JsonOutboundMessageMapper();
		String result = mapper.fromMessage(testMessage);
		assertTrue(result.contains("\"headers\":{"));
		assertTrue(result.contains("\"timestamp\":"+testMessage.getHeaders().getTimestamp()));
		assertTrue(result.contains("\"id\":\""+testMessage.getHeaders().getId()+"\""));
		assertTrue(result.contains("\"payload\":\"myPayloadStuff\""));
		assertTrue(result.contains("\"history\":"));
		assertTrue(result.contains("testName-1"));
		assertTrue(result.contains("testType-1"));
		assertTrue(result.contains("testName-2"));
		assertTrue(result.contains("testType-2"));
		assertTrue(result.contains("testName-3"));
		assertTrue(result.contains("testType-3"));
	}

	@Test
	public void testFromMessageExtractStringPayload() throws Exception {
		Message<String> testMessage = MessageBuilder.withPayload("myPayloadStuff").build();
		String expected = "\"myPayloadStuff\"";
		JsonOutboundMessageMapper mapper = new JsonOutboundMessageMapper();
		mapper.setShouldExtractPayload(true);
		String result = mapper.fromMessage(testMessage);
		assertEquals(expected, result);
	}

	@Test
	public void testFromMessageWithHeadersAndBeanPayload() throws Exception {
		TestBean payload = new TestBean();
		Message<TestBean> testMessage = MessageBuilder.withPayload(payload).build();
		JsonOutboundMessageMapper mapper = new JsonOutboundMessageMapper();
		String result = mapper.fromMessage(testMessage);
		assertTrue(result.contains("\"headers\":{"));
		assertTrue(result.contains("\"timestamp\":"+testMessage.getHeaders().getTimestamp()));
		assertTrue(result.contains("\"id\":\""+testMessage.getHeaders().getId()+"\""));
		TestBean parsedPayload = extractJsonPayloadToTestBean(result);
		assertEquals(payload, parsedPayload);
	}

	@Test
	public void testFromMessageExtractBeanPayload() throws Exception {
		TestBean payload = new TestBean();
		Message<TestBean> testMessage = MessageBuilder.withPayload(payload).build();
		JsonOutboundMessageMapper mapper = new JsonOutboundMessageMapper();
		mapper.setShouldExtractPayload(true);
		String result = mapper.fromMessage(testMessage);
		assertTrue(!result.contains("headers"));
		TestBean parsedPayload = objectMapper.readValue(result, TestBean.class);
		assertEquals(payload, parsedPayload);
	}

	private TestBean extractJsonPayloadToTestBean(String json) throws JsonParseException, IOException {
		JsonParser parser = jsonFactory.createJsonParser(json);
		do {
			parser.nextToken();
		} while(parser.getCurrentToken() != JsonToken.FIELD_NAME || !parser.getCurrentName().equals("payload"));
		parser.nextToken();
		return objectMapper.readValue(parser, TestBean.class);
	}


	private static class TestNamedComponent implements NamedComponent {

		private final int id;

		private TestNamedComponent(int id) {
			this.id = id;
		}

		public String getComponentName() {
			return "testName-" + this.id;
		}

		public String getComponentType() {
			return "testType-" + this.id;
		}

	}

}
