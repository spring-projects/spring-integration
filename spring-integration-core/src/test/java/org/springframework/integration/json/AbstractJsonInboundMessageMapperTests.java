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

package org.springframework.integration.json;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.junit.Test;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.integration.message.MessageMatcher;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.support.json.JacksonJsonObjectMapperProvider;
import org.springframework.integration.support.json.JsonInboundMessageMapper;
import org.springframework.integration.support.json.JsonInboundMessageMapper.JsonMessageParser;
import org.springframework.integration.support.json.JsonObjectMapper;
import org.springframework.messaging.Message;

/**
 * @author Jeremy Grelle
 * @author Mark Fisher
 * @author Dave Syer
 * @author Artem Bilan
 * @author Gary Russell
 */
public abstract class AbstractJsonInboundMessageMapperTests {

	private final JsonObjectMapper<?, ?> mapper = JacksonJsonObjectMapperProvider.newInstance();

	@Factory
    public static Matcher<Message<?>> sameExceptImmutableHeaders(Message<?> operand) {
        return new MessageMatcher(operand);
    }


	@Test
	public void testToMessageWithHeadersAndStringPayload() throws Exception {
		UUID id = UUID.randomUUID();
		String jsonMessage = "{\"headers\":{\"timestamp\":1,\"id\":\"" + id + "\",\"foo\":123,\"bar\":\"abc\"},\"payload\":\"myPayloadStuff\"}";
		Message<String> expected = MessageBuilder.withPayload("myPayloadStuff").setHeader("foo", 123).setHeader("bar", "abc").build();
		JsonInboundMessageMapper mapper = new JsonInboundMessageMapper(String.class, getParser());
		Message<?> result = mapper.toMessage(jsonMessage);
		assertThat(result, sameExceptImmutableHeaders(expected));
	}

	@Test
	public void testToMessageWithStringPayload() throws Exception {
		String jsonMessage = "\"myPayloadStuff\"";
		String expected = "myPayloadStuff";
		JsonInboundMessageMapper mapper = new JsonInboundMessageMapper(String.class, getParser());
		mapper.setMapToPayload(true);
		Message<?> result = mapper.toMessage(jsonMessage);
		assertEquals(expected, result.getPayload());
	}

	@Test
	public void testToMessageWithHeadersAndBeanPayload() throws Exception {
		TestBean bean = new TestBean();
		UUID id = UUID.randomUUID();
		String jsonMessage = "{\"headers\":{\"timestamp\":1,\"id\":\"" + id + "\",\"foo\":123,\"bar\":\"abc\"},\"payload\":" + getBeanAsJson(bean) + "}";
		Message<TestBean> expected = MessageBuilder.withPayload(bean).setHeader("foo", 123).setHeader("bar", "abc").build();
		JsonInboundMessageMapper mapper = new JsonInboundMessageMapper(TestBean.class, getParser());
		Message<?> result = mapper.toMessage(jsonMessage);
		assertThat(result, sameExceptImmutableHeaders(expected));
	}

	@Test
	public void testToMessageWithBeanPayload() throws Exception {
		TestBean expected = new TestBean();
		String jsonMessage = getBeanAsJson(expected);
		JsonInboundMessageMapper mapper = new JsonInboundMessageMapper(TestBean.class, getParser());
		mapper.setMapToPayload(true);
		Message<?> result = mapper.toMessage(jsonMessage);
		assertEquals(expected, result.getPayload());
	}

	@Test
	public void testToMessageWithBeanHeaderAndStringPayload() throws Exception {
		TestBean bean = new TestBean();
		UUID id = UUID.randomUUID();
		String jsonMessage = "{\"headers\":{\"timestamp\":1,\"id\":\"" + id + "\", \"myHeader\":" + getBeanAsJson(bean) + "},\"payload\":\"myPayloadStuff\"}";
		Message<String> expected = MessageBuilder.withPayload("myPayloadStuff").setHeader("myHeader", bean).build();
		JsonInboundMessageMapper mapper = new JsonInboundMessageMapper(String.class, getParser());
		Map<String, Class<?>> headerTypes = new HashMap<String, Class<?>>();
		headerTypes.put("myHeader", TestBean.class);
		mapper.setHeaderTypes(headerTypes);
		Message<?> result = mapper.toMessage(jsonMessage);
		assertThat(result, sameExceptImmutableHeaders(expected));
	}

	@Test
	public void testToMessageWithHeadersAndListOfStringsPayload() throws Exception {
		UUID id = UUID.randomUUID();
		String jsonMessage = "{\"headers\":{\"timestamp\":1,\"id\":\"" + id + "\",\"foo\":123,\"bar\":\"abc\"},\"payload\":[\"myPayloadStuff1\",\"myPayloadStuff2\",\"myPayloadStuff3\"]}";
		List<String> expectedList = Arrays.asList("myPayloadStuff1", "myPayloadStuff2", "myPayloadStuff3");
		Message<List<String>> expected = MessageBuilder.withPayload(expectedList).setHeader("foo", 123).setHeader("bar", "abc").build();
		JsonInboundMessageMapper mapper = new JsonInboundMessageMapper(new ParameterizedTypeReference<List<String>>(){}.getType(), getParser());
		Message<?> result = mapper.toMessage(jsonMessage);
		assertThat(result, sameExceptImmutableHeaders(expected));
	}

	@Test
	public void testToMessageWithHeadersAndListOfBeansPayload() throws Exception {
		TestBean bean1 = new TestBean();
		TestBean bean2 = new TestBean();
		UUID id = UUID.randomUUID();
		String jsonMessage = "{\"headers\":{\"timestamp\":1,\"id\":\"" + id + "\",\"foo\":123,\"bar\":\"abc\"},\"payload\":[" + getBeanAsJson(bean1) + "," + getBeanAsJson(bean2) + "]}";
		List<TestBean> expectedList = Arrays.asList(bean1, bean2);
		Message<List<TestBean>> expected = MessageBuilder.withPayload(expectedList).setHeader("foo", 123).setHeader("bar", "abc").build();
		JsonInboundMessageMapper mapper = new JsonInboundMessageMapper(new ParameterizedTypeReference<List<TestBean>>(){}.getType(), getParser());
		Message<?> result = mapper.toMessage(jsonMessage);
		assertThat(result, sameExceptImmutableHeaders(expected));
	}

	@Test
	public void testToMessageWithPayloadAndHeadersReversed() throws Exception {
		UUID id = UUID.randomUUID();
		String jsonMessage = "{\"payload\":\"myPayloadStuff\",\"headers\":{\"timestamp\":1,\"id\":\"" + id + "\",\"foo\":123,\"bar\":\"abc\"}}";
		Message<String> expected = MessageBuilder.withPayload("myPayloadStuff").setHeader("foo", 123).setHeader("bar", "abc").build();
		JsonInboundMessageMapper mapper = new JsonInboundMessageMapper(String.class, getParser());
		Message<?> result = mapper.toMessage(jsonMessage);
		assertThat(result, sameExceptImmutableHeaders(expected));
	}

	@Test
	public void testToMessageInvalidFormatPayloadNoHeaders() throws Exception {
		String jsonMessage = "{\"payload\":\"myPayloadStuff\"}";
		JsonInboundMessageMapper mapper = new JsonInboundMessageMapper(String.class, getParser());
		try {
			mapper.toMessage(jsonMessage);
			fail();
		}
		catch(IllegalArgumentException ex) {
			//Expected
		}
	}

	@Test
	public void testToMessageInvalidFormatHeadersNoPayload() throws Exception {
		UUID id = UUID.randomUUID();
		String jsonMessage = "{\"headers\":{\"$timestamp\":1,\"$id\":\"" + id + "\"}}";
		JsonInboundMessageMapper mapper = new JsonInboundMessageMapper(String.class, getParser());
		try {
			mapper.toMessage(jsonMessage);
			fail();
		}
		catch(IllegalArgumentException ex) {
			//Expected
		}
	}

	@Test
	public void testToMessageInvalidFormatHeadersAndStringPayloadWithMapToPayload() throws Exception {
		UUID id = UUID.randomUUID();
		String jsonMessage = "{\"headers\":{\"$timestamp\":1,\"$id\":\"" + id + "\"},\"payload\":\"myPayloadStuff\"}";
		JsonInboundMessageMapper mapper = new JsonInboundMessageMapper(String.class, getParser());
		mapper.setMapToPayload(true);
		try {
			mapper.toMessage(jsonMessage);
			fail();
		}
		catch(IllegalArgumentException ex) {
			//Expected
		}
	}

	@Test
	public void testToMessageInvalidFormatHeadersAndBeanPayloadWithMapToPayload() throws Exception {
		TestBean bean = new TestBean();
		UUID id = UUID.randomUUID();
		String jsonMessage = "{\"headers\":{\"$timestamp\":1,\"$id\":\"" + id + "\"},\"payload\":" + getBeanAsJson(bean) + "}";
		JsonInboundMessageMapper mapper = new JsonInboundMessageMapper(TestBean.class, getParser());
		mapper.setMapToPayload(true);
		try {
			mapper.toMessage(jsonMessage);
			fail();
		}
		catch(IllegalArgumentException ex) {
			//Expected
		}
	}

	@Test
	public void testToMessageWithHeadersAndPayloadTypeMappingFailure() throws Exception {
		TestBean bean = new TestBean();
		UUID id = UUID.randomUUID();
		String jsonMessage = "{\"headers\":{\"$timestamp\":1,\"$id\":\"" + id + "\"},\"payload\":" + getBeanAsJson(bean) + "}";
		JsonInboundMessageMapper mapper = new JsonInboundMessageMapper(Long.class, getParser());
		try {
			mapper.toMessage(jsonMessage);
			fail();
		}
		catch(IllegalArgumentException ex) {
			//Expected
		}
	}

	@Test
	public void testToMessageWithBeanHeaderTypeMappingFailure() throws Exception {
		TestBean bean = new TestBean();
		UUID id = UUID.randomUUID();
		String jsonMessage = "{\"headers\":{\"$timestamp\":1,\"$id\":\"" + id + "\",\"myHeader\":" + getBeanAsJson(bean) + "},\"payload\":\"myPayloadStuff\"}";
		JsonInboundMessageMapper mapper = new JsonInboundMessageMapper(String.class, getParser());
		Map<String, Class<?>> headerTypes = new HashMap<String, Class<?>>();
		headerTypes.put("myHeader", Long.class);
		mapper.setHeaderTypes(headerTypes);
		try {
			mapper.toMessage(jsonMessage);
			fail();
		}
		catch(IllegalArgumentException ex) {
			//Expected
		}
	}


	private String getBeanAsJson(TestBean bean) throws Exception {
		return mapper.toJson(bean);
	}

	protected abstract JsonMessageParser<?> getParser();
}
