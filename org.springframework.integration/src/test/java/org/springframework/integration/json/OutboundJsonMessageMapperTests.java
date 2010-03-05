package org.springframework.integration.json;

import static org.junit.Assert.*;

import java.io.IOException;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Test;
import org.springframework.integration.core.Message;
import org.springframework.integration.core.MessageHeaders;
import org.springframework.integration.message.MessageBuilder;


public class OutboundJsonMessageMapperTests {
	
	private JsonFactory jsonFactory = new JsonFactory();
	private ObjectMapper objectMapper = new ObjectMapper();
	
	@Test
	public void testFromMessageWithHeadersAndStringPayload() throws Exception {
		Message<String> testMessage = MessageBuilder.withPayload("myPayloadStuff").build();
		String expected = "{\"headers\":{\"$timestamp\":"+testMessage.getHeaders().getTimestamp()+
			",\"$history\":[],\"$id\":\""+testMessage.getHeaders().getId()+"\"},\"payload\":\"myPayloadStuff\"}"; 
		OutboundJsonMessageMapper mapper = new OutboundJsonMessageMapper();
		String result = mapper.fromMessage(testMessage);
		assertEquals(expected, result);
	}
	
	@Test
	public void testFromMessageExtractStringPayload() throws Exception {
		Message<String> testMessage = MessageBuilder.withPayload("myPayloadStuff").build();
		String expected = "\"myPayloadStuff\"";
		OutboundJsonMessageMapper mapper = new OutboundJsonMessageMapper();
		mapper.setShouldExtractPayload(true);
		String result = mapper.fromMessage(testMessage);
		assertEquals(expected, result);
	}
	
	@Test
	public void testFromMessageWithHeadersAndBeanPayload() throws Exception {
		TestBean payload = new TestBean();
		Message<TestBean> testMessage = MessageBuilder.withPayload(payload).setHeader(MessageHeaders.TIMESTAMP, new Long(1234)).setHeader(MessageHeaders.ID, "myUniqueId").build();
		String expectedHeaders = "\"headers\":{\"$timestamp\":"+testMessage.getHeaders().getTimestamp()+
			",\"$history\":[],\"$id\":\""+testMessage.getHeaders().getId()+"\""; 
		OutboundJsonMessageMapper mapper = new OutboundJsonMessageMapper();
		String result = mapper.fromMessage(testMessage);
		assertTrue(result.contains(expectedHeaders));
		TestBean parsedPayload = extractJsonPayloadToTestBean(result);
		assertEquals(payload, parsedPayload);
	}
	
	@Test
	public void testFromMessageExtractBeanPayload() throws Exception {
		TestBean payload = new TestBean();
		Message<TestBean> testMessage = MessageBuilder.withPayload(payload).build();
		OutboundJsonMessageMapper mapper = new OutboundJsonMessageMapper();
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
}
