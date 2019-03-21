/*
 * Copyright 2002-2014 the original author or authors.
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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Test;

import org.springframework.context.expression.MapAccessor;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.integration.mapping.support.JsonHeaders;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.support.json.BoonJsonObjectMapper;
import org.springframework.integration.support.json.Jackson2JsonObjectMapper;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.GenericMessage;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

/**
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 * @since 2.0
 */
public class ObjectToJsonTransformerTests {

	@Test
	public void simpleStringPayload() {
		ObjectToJsonTransformer transformer = new ObjectToJsonTransformer();
		String result = (String) transformer.transform(new GenericMessage<>("foo")).getPayload();
		assertEquals("\"foo\"", result);
	}

	@Test
	public void withDefaultContentType() {
		ObjectToJsonTransformer transformer = new ObjectToJsonTransformer();
		Message<?> result = transformer.transform(new GenericMessage<>("foo"));
		assertEquals(ObjectToJsonTransformer.JSON_CONTENT_TYPE, result.getHeaders().get(MessageHeaders.CONTENT_TYPE));
	}

	@Test
	public void withProvidedContentType() {
		ObjectToJsonTransformer transformer = new ObjectToJsonTransformer();
		Message<?> message = MessageBuilder.withPayload("foo")
				.setHeader(MessageHeaders.CONTENT_TYPE, "text/xml")
				.build();
		Message<?> result = transformer.transform(message);
		assertEquals("text/xml", result.getHeaders().get(MessageHeaders.CONTENT_TYPE));
	}

	@Test
	public void withProvidedContentTypeWithOverride() {
		ObjectToJsonTransformer transformer = new ObjectToJsonTransformer();
		transformer.setContentType(ObjectToJsonTransformer.JSON_CONTENT_TYPE);
		Message<?> message = MessageBuilder.withPayload("foo")
				.setHeader(MessageHeaders.CONTENT_TYPE, "text/xml")
				.build();
		Message<?> result = transformer.transform(message);
		assertEquals(ObjectToJsonTransformer.JSON_CONTENT_TYPE, result.getHeaders().get(MessageHeaders.CONTENT_TYPE));
	}

	@Test
	public void withProvidedContentTypeAsEmptyString() {
		ObjectToJsonTransformer transformer = new ObjectToJsonTransformer();
		transformer.setContentType("");
		Message<?> message = MessageBuilder.withPayload("foo").build();
		Message<?> result = transformer.transform(message);
		assertFalse(result.getHeaders().containsKey(MessageHeaders.CONTENT_TYPE));
	}

	@Test
	public void withProvidedContentTypeAsEmptyStringDoesNotOverride() {
		ObjectToJsonTransformer transformer = new ObjectToJsonTransformer();
		transformer.setContentType("");
		Message<?> message = MessageBuilder.withPayload("foo")
				.setHeader(MessageHeaders.CONTENT_TYPE, "text/xml")
				.build();
		Message<?> result = transformer.transform(message);
		assertEquals("text/xml", result.getHeaders().get(MessageHeaders.CONTENT_TYPE));
	}

	@Test(expected = IllegalArgumentException.class)
	public void withProvidedContentTypeAsNull() {
		ObjectToJsonTransformer transformer = new ObjectToJsonTransformer();
		transformer.setContentType(null);
	}

	@Test
	public void simpleIntegerPayload() {
		ObjectToJsonTransformer transformer = new ObjectToJsonTransformer();
		String result = (String) transformer.transform(new GenericMessage<>(123)).getPayload();
		assertEquals("123", result);
	}

	@Test
	public void objectPayload() {
		ObjectToJsonTransformer transformer = new ObjectToJsonTransformer();
		TestAddress address = new TestAddress(123, "Main Street");
		TestPerson person = new TestPerson("John", "Doe", 42);
		person.setAddress(address);
		String result = (String) transformer.transform(new GenericMessage<>(person)).getPayload();
		assertTrue(result.contains("\"firstName\":\"John\""));
		assertTrue(result.contains("\"lastName\":\"Doe\""));
		assertTrue(result.contains("\"age\":42"));
		Pattern addressPattern = Pattern.compile("(\"address\":\\{.*?\\})");
		Matcher matcher = addressPattern.matcher(result);
		assertTrue(matcher.find());
		String addressResult = matcher.group(1);
		assertTrue(addressResult.contains("\"number\":123"));
		assertTrue(addressResult.contains("\"street\":\"Main Street\""));
	}

	@Test
	public void objectPayloadWithCustomObjectMapper() {
		ObjectMapper customMapper = new ObjectMapper();
		customMapper.configure(JsonGenerator.Feature.QUOTE_FIELD_NAMES, Boolean.FALSE);
		ObjectToJsonTransformer transformer = new ObjectToJsonTransformer(new Jackson2JsonObjectMapper(customMapper));
		TestPerson person = new TestPerson("John", "Doe", 42);
		person.setAddress(new TestAddress(123, "Main Street"));
		String result = (String) transformer.transform(new GenericMessage<>(person)).getPayload();
		assertTrue(result.contains("firstName:\"John\""));
		assertTrue(result.contains("lastName:\"Doe\""));
		assertTrue(result.contains("age:42"));
		Pattern addressPattern = Pattern.compile("(address:\\{.*?\\})");
		Matcher matcher = addressPattern.matcher(result);
		assertTrue(matcher.find());
		String addressResult = matcher.group(1);
		assertTrue(addressResult.contains("number:123"));
		assertTrue(addressResult.contains("street:\"Main Street\""));
	}

	@Test
	public void collectionOrMap() {
		ObjectToJsonTransformer transformer = new ObjectToJsonTransformer();
		List<String> list = Collections.singletonList("foo");
		Message<?> out = transformer.transform(new GenericMessage<>(list));
		assertThat(out.getHeaders().get(JsonHeaders.TYPE_ID).toString(), containsString("SingletonList"));
		assertThat(out.getHeaders().get(JsonHeaders.CONTENT_TYPE_ID), equalTo(String.class));
		Map<String, Long> map = Collections.singletonMap("foo", 1L);
		out = transformer.transform(new GenericMessage<>(map));
		assertThat(out.getHeaders().get(JsonHeaders.TYPE_ID).toString(), containsString("SingletonMap"));
		assertThat(out.getHeaders().get(JsonHeaders.CONTENT_TYPE_ID), equalTo(Long.class));
		assertThat(out.getHeaders().get(JsonHeaders.KEY_TYPE_ID), equalTo(String.class));
	}

	@Test
	public void collectionOrMapWithNullFirstElement() {
		ObjectToJsonTransformer transformer = new ObjectToJsonTransformer();
		List<String> list = Collections.singletonList(null);
		Message<?> out = transformer.transform(new GenericMessage<>(list));
		assertThat(out.getHeaders().get(JsonHeaders.TYPE_ID).toString(), containsString("SingletonList"));
		assertThat(out.getHeaders().get(JsonHeaders.CONTENT_TYPE_ID), equalTo(Object.class));
		Map<String, String> map = Collections.singletonMap("foo", null);
		out = transformer.transform(new GenericMessage<>(map));
		assertThat(out.getHeaders().get(JsonHeaders.TYPE_ID).toString(), containsString("SingletonMap"));
		assertThat(out.getHeaders().get(JsonHeaders.CONTENT_TYPE_ID), equalTo(Object.class));
		assertThat(out.getHeaders().get(JsonHeaders.KEY_TYPE_ID), equalTo(String.class));
	}

	@Test
	public void testBoonJsonObjectMapper() {
		ObjectToJsonTransformer transformer = new ObjectToJsonTransformer(new BoonJsonObjectMapper());
		TestPerson person = new TestPerson("John", "Doe", 42);
		person.setAddress(new TestAddress(123, "Main Street"));
		String result = (String) transformer.transform(new GenericMessage<>(person)).getPayload();
		assertTrue(result.contains("\"firstName\":\"John\""));
		assertTrue(result.contains("\"lastName\":\"Doe\""));
		assertTrue(result.contains("\"age\":42"));
		Pattern addressPattern = Pattern.compile("(\"address\":\\{.*?\\})");
		Matcher matcher = addressPattern.matcher(result);
		assertTrue(matcher.find());
		String addressResult = matcher.group(1);
		assertTrue(addressResult.contains("\"number\":123"));
		assertTrue(addressResult.contains("\"street\":\"Main Street\""));
	}

	@Test
	public void testBoonJsonObjectMapper_toNode() {
		ObjectToJsonTransformer transformer = new ObjectToJsonTransformer(new BoonJsonObjectMapper(),
				ObjectToJsonTransformer.ResultType.NODE);
		TestPerson person = new TestPerson("John", "Doe", 42);
		person.setAddress(new TestAddress(123, "Main Street"));
		Object payload = transformer.transform(new GenericMessage<>(person)).getPayload();
		assertThat(payload, instanceOf(Map.class));

		SpelExpressionParser parser = new SpelExpressionParser();
		Expression expression = parser.parseExpression("firstName + ': ' + address.street");
		StandardEvaluationContext evaluationContext = new StandardEvaluationContext();
		evaluationContext.addPropertyAccessor(new MapAccessor());
		String value = expression.getValue(evaluationContext, payload, String.class);

		assertEquals("John: Main Street", value);
	}

	@Test
	public void testJsonStringAndJsonNode() {
		ObjectToJsonTransformer transformer = new ObjectToJsonTransformer(ObjectToJsonTransformer.ResultType.NODE);
		Object result = transformer.transform(new GenericMessage<>("{\"foo\": \"FOO\", \"bar\": 1}")).getPayload();
		assertThat(result, instanceOf(ObjectNode.class));
		ObjectNode objectNode = (ObjectNode) result;
		assertEquals(2, objectNode.size());
		assertEquals("FOO", objectNode.path("foo").textValue());
		assertEquals(1, objectNode.path("bar").intValue());

		result = transformer.transform(new GenericMessage<>("foo")).getPayload();
		assertThat(result, instanceOf(TextNode.class));
		assertEquals("foo", ((TextNode) result).textValue());
	}

}
