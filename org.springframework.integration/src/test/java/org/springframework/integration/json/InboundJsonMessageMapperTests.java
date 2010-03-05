package org.springframework.integration.json;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.junit.Test;
import org.springframework.integration.core.Message;
import org.springframework.integration.core.MessageHeaders;
import org.springframework.integration.message.MessageBuilder;

public class InboundJsonMessageMapperTests {

	ObjectMapper mapper = new ObjectMapper();

	@Test
	public void testToMessageWithHeadersAndStringPayload() throws Exception {
		String jsonMessage = "{\"headers\":{\"$timestamp\":1,\"$id\":\"myUniqueId\"},\"payload\":\"myPayloadStuff\"}";
		Message<String> expected = MessageBuilder.withPayload("myPayloadStuff").setHeader(MessageHeaders.TIMESTAMP, new Long(1)).setHeader(MessageHeaders.ID,"myUniqueId").build();
		InboundJsonMessageMapper mapper = new InboundJsonMessageMapper(String.class);
		Message<?> result = mapper.toMessage(jsonMessage);
		assertEquals(expected, result);
	}
	
	@Test
	public void testToMessageWithStringPayload() throws Exception {
		String jsonMessage = "\"myPayloadStuff\"";
		String expected = "myPayloadStuff";
		InboundJsonMessageMapper mapper = new InboundJsonMessageMapper(String.class);
		mapper.setMapToPayload(true);
		Message<?> result = mapper.toMessage(jsonMessage);
		assertEquals(expected, result.getPayload());
	}
	
	@Test
	public void testToMessageWithHeadersAndBeanPayload() throws Exception {
		TestBean bean = new TestBean();
		String jsonMessage = "{\"headers\":{\"$timestamp\":1,\"$id\":\"myUniqueId\"},\"payload\":"+getBeanAsJson(bean)+"}";
		Message<TestBean> expected = MessageBuilder.withPayload(bean).setHeader(MessageHeaders.TIMESTAMP, new Long(1)).setHeader(MessageHeaders.ID,"myUniqueId").build();
		InboundJsonMessageMapper mapper = new InboundJsonMessageMapper(TestBean.class);
		Message<?> result = mapper.toMessage(jsonMessage);
		assertEquals(expected, result);
	}
	
	@Test
	public void testToMessageWithBeanPayload() throws Exception {
		TestBean expected = new TestBean();
		String jsonMessage = getBeanAsJson(expected);
		InboundJsonMessageMapper mapper = new InboundJsonMessageMapper(TestBean.class);
		mapper.setMapToPayload(true);
		Message<?> result = mapper.toMessage(jsonMessage);
		assertEquals(expected, result.getPayload());
	}
	
	@Test
	public void testToMessageWithBeanHeaderAndStringPayload() throws Exception {
		TestBean bean = new TestBean();
		String jsonMessage = "{\"headers\":{\"$timestamp\":1,\"$id\":\"myUniqueId\", \"myHeader\":"+getBeanAsJson(bean)+"},\"payload\":\"myPayloadStuff\"}";
		Message<String> expected = MessageBuilder.withPayload("myPayloadStuff").
			setHeader(MessageHeaders.TIMESTAMP, new Long(1)).setHeader(MessageHeaders.ID,"myUniqueId").setHeader("myHeader", bean).build();
		InboundJsonMessageMapper mapper = new InboundJsonMessageMapper(String.class);
		Map<String, Class<?>> headerTypes = new HashMap<String, Class<?>>();
		headerTypes.put("myHeader", TestBean.class);
		mapper.setHeaderTypes(headerTypes);
		Message<?> result = mapper.toMessage(jsonMessage);
		assertEquals(expected, result);
	}
	
	@Test 
	public void testToMessageWithHeadersAndListOfStringsPayload() throws Exception {
		String jsonMessage = "{\"headers\":{\"$timestamp\":1,\"$id\":\"myUniqueId\"},\"payload\":[\"myPayloadStuff1\",\"myPayloadStuff2\",\"myPayloadStuff3\"]}";
		List<String> expectedList = Arrays.asList(new String[]{"myPayloadStuff1", "myPayloadStuff2", "myPayloadStuff3"});
		Message<List<String>> expected = MessageBuilder.withPayload(expectedList).setHeader(MessageHeaders.TIMESTAMP, new Long(1)).setHeader(MessageHeaders.ID,"myUniqueId").build();
		InboundJsonMessageMapper mapper = new InboundJsonMessageMapper(new TypeReference<List<String>>(){});
		Message<?> result = mapper.toMessage(jsonMessage);
		assertEquals(expected, result);
	}
	
	@Test 
	public void testToMessageWithHeadersAndListOfBeansPayload() throws Exception {
		TestBean bean1 = new TestBean();
		TestBean bean2 = new TestBean();
		String jsonMessage = "{\"headers\":{\"$timestamp\":1,\"$id\":\"myUniqueId\"},\"payload\":["+getBeanAsJson(bean1)+","+getBeanAsJson(bean2)+"]}";
		List<TestBean> expectedList = Arrays.asList(new TestBean[]{bean1, bean2});
		Message<List<TestBean>> expected = MessageBuilder.withPayload(expectedList).setHeader(MessageHeaders.TIMESTAMP, new Long(1)).setHeader(MessageHeaders.ID,"myUniqueId").build();
		InboundJsonMessageMapper mapper = new InboundJsonMessageMapper(new TypeReference<List<TestBean>>(){});
		Message<?> result = mapper.toMessage(jsonMessage);
		assertEquals(expected, result);
	}
	
	@Test
	public void testToMessageInvalidFormatPayloadAndHeadersReversed() throws Exception {
		String jsonMessage = "{\"payload\":\"myPayloadStuff\",\"headers\":{\"$timestamp\":1,\"$id\":\"myUniqueId\"}}";
		InboundJsonMessageMapper mapper = new InboundJsonMessageMapper(String.class);
		
		try {
			mapper.toMessage(jsonMessage);
			fail();
		} catch(IllegalArgumentException ex) {
			//Expected
		}
	}
	
	@Test
	public void testToMessageInvalidFormatPayloadNoHeaders() throws Exception {
		String jsonMessage = "{\"payload\":\"myPayloadStuff\"}";
		InboundJsonMessageMapper mapper = new InboundJsonMessageMapper(String.class);
		
		try {
			mapper.toMessage(jsonMessage);
			fail();
		} catch(IllegalArgumentException ex) {
			//Expected
		}
	}
	
	@Test
	public void testToMessageInvalidFormatHeadersNoPayload() throws Exception {
		String jsonMessage = "{\"headers\":{\"$timestamp\":1,\"$id\":\"myUniqueId\"}}";
		InboundJsonMessageMapper mapper = new InboundJsonMessageMapper(String.class);
		
		try {
			mapper.toMessage(jsonMessage);
			fail();
		} catch(IllegalArgumentException ex) {
			//Expected
		}
	}
	
	@Test
	public void testToMessageInvalidFormatHeadersAndStringPayloadWithMapToPayload() throws Exception {
		String jsonMessage = "{\"headers\":{\"$timestamp\":1,\"$id\":\"myUniqueId\"},\"payload\":\"myPayloadStuff\"}";
		InboundJsonMessageMapper mapper = new InboundJsonMessageMapper(String.class);
		mapper.setMapToPayload(true);
		try {
			mapper.toMessage(jsonMessage);
			fail();
		} catch(IllegalArgumentException ex) {
			//Expected
		}
	}
	
	@Test
	public void testToMessageInvalidFormatHeadersAndBeanPayloadWithMapToPayload() throws Exception {
		TestBean bean = new TestBean();
		String jsonMessage = "{\"headers\":{\"$timestamp\":1,\"$id\":\"myUniqueId\"},\"payload\":"+getBeanAsJson(bean)+"}";
		InboundJsonMessageMapper mapper = new InboundJsonMessageMapper(TestBean.class);
		mapper.setMapToPayload(true);
		try {
			mapper.toMessage(jsonMessage);
			fail();
		} catch(IllegalArgumentException ex) {
			//Expected
		}
	}
	
	@Test
	public void testToMessageWithHeadersAndPayloadTypeMappingFailure() throws Exception {
		TestBean bean = new TestBean();
		String jsonMessage = "{\"headers\":{\"$timestamp\":1,\"$id\":\"myUniqueId\"},\"payload\":"+getBeanAsJson(bean)+"}";
		InboundJsonMessageMapper mapper = new InboundJsonMessageMapper(Long.class);
		try {
			mapper.toMessage(jsonMessage);
			fail();
		} catch(IllegalArgumentException ex) {
			//Expected
		}
	}
	
	@Test
	public void testToMessageWithBeanHeaderTypeMappingFailure() throws Exception {
		TestBean bean = new TestBean();
		String jsonMessage = "{\"headers\":{\"$timestamp\":1,\"$id\":\"myUniqueId\",\"myHeader\":"+getBeanAsJson(bean)+"},\"payload\":\"myPayloadStuff\"}";
		InboundJsonMessageMapper mapper = new InboundJsonMessageMapper(String.class);
		Map<String, Class<?>> headerTypes = new HashMap<String, Class<?>>();
		headerTypes.put("myHeader", Long.class);
		mapper.setHeaderTypes(headerTypes);
		try {
			mapper.toMessage(jsonMessage);
			fail();
		} catch(IllegalArgumentException ex) {
			//Expected
		}
	}
	
	
	
	private String getBeanAsJson(TestBean bean) throws JsonGenerationException, JsonMappingException, IOException {
		StringWriter writer = new StringWriter();
		mapper.writeValue(writer, bean);
		return writer.toString();
	}
}
