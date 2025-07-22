/*
 * Copyright 2002-present the original author or authors.
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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import tools.jackson.core.json.JsonWriteFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.node.StringNode;

import org.springframework.integration.mapping.support.JsonHeaders;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.support.json.JacksonJsonObjectMapper;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.GenericMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 * @author Jooyoung Pyoung
 *
 * @since 2.0
 */
public class ObjectToJsonTransformerTests {

	@Test
	public void simpleStringPayload() {
		ObjectToJsonTransformer transformer = new ObjectToJsonTransformer();
		String result = (String) transformer.transform(new GenericMessage<>("foo")).getPayload();
		assertThat(result).isEqualTo("\"foo\"");
	}

	@Test
	public void withDefaultContentType() {
		ObjectToJsonTransformer transformer = new ObjectToJsonTransformer();
		Message<?> result = transformer.transform(new GenericMessage<>("foo"));
		assertThat(result.getHeaders().get(MessageHeaders.CONTENT_TYPE))
				.isEqualTo(ObjectToJsonTransformer.JSON_CONTENT_TYPE);
	}

	@Test
	public void withProvidedContentType() {
		ObjectToJsonTransformer transformer = new ObjectToJsonTransformer();
		Message<?> message = MessageBuilder.withPayload("foo")
				.setHeader(MessageHeaders.CONTENT_TYPE, "text/xml")
				.build();
		Message<?> result = transformer.transform(message);
		assertThat(result.getHeaders().get(MessageHeaders.CONTENT_TYPE)).isEqualTo("text/xml");
	}

	@Test
	public void withProvidedContentTypeWithOverride() {
		ObjectToJsonTransformer transformer = new ObjectToJsonTransformer();
		transformer.setContentType(ObjectToJsonTransformer.JSON_CONTENT_TYPE);
		Message<?> message = MessageBuilder.withPayload("foo")
				.setHeader(MessageHeaders.CONTENT_TYPE, "text/xml")
				.build();
		Message<?> result = transformer.transform(message);
		assertThat(result.getHeaders().get(MessageHeaders.CONTENT_TYPE))
				.isEqualTo(ObjectToJsonTransformer.JSON_CONTENT_TYPE);
	}

	@Test
	public void withProvidedContentTypeAsEmptyString() {
		ObjectToJsonTransformer transformer = new ObjectToJsonTransformer();
		transformer.setContentType("");
		Message<?> message = MessageBuilder.withPayload("foo").build();
		Message<?> result = transformer.transform(message);
		assertThat(result.getHeaders().containsKey(MessageHeaders.CONTENT_TYPE)).isFalse();
	}

	@Test
	public void withProvidedContentTypeAsEmptyStringDoesNotOverride() {
		ObjectToJsonTransformer transformer = new ObjectToJsonTransformer();
		transformer.setContentType("");
		Message<?> message = MessageBuilder.withPayload("foo")
				.setHeader(MessageHeaders.CONTENT_TYPE, "text/xml")
				.build();
		Message<?> result = transformer.transform(message);
		assertThat(result.getHeaders().get(MessageHeaders.CONTENT_TYPE)).isEqualTo("text/xml");
	}

	@Test
	public void withProvidedContentTypeAsNull() {
		ObjectToJsonTransformer transformer = new ObjectToJsonTransformer();
		assertThatThrownBy(() -> transformer.setContentType(null))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	public void simpleIntegerPayload() {
		ObjectToJsonTransformer transformer = new ObjectToJsonTransformer();
		String result = (String) transformer.transform(new GenericMessage<>(123)).getPayload();
		assertThat(result).isEqualTo("123");
	}

	@Test
	public void simpleIntegerAsBytesPayload() {
		ObjectToJsonTransformer transformer = new ObjectToJsonTransformer(ObjectToJsonTransformer.ResultType.BYTES);
		Object result = transformer.transform(new GenericMessage<>(123)).getPayload();
		assertThat(result).isInstanceOf(byte[].class);
		assertThat(new String((byte[]) result)).isEqualTo("123");
	}

	@Test
	public void objectPayload() {
		ObjectToJsonTransformer transformer = new ObjectToJsonTransformer();
		TestAddress address = new TestAddress(123, "Main Street");
		TestPerson person = new TestPerson("John", "Doe", 42);
		person.setAddress(address);
		String result = (String) transformer.transform(new GenericMessage<>(person)).getPayload();
		assertThat(result.contains("\"firstName\":\"John\"")).isTrue();
		assertThat(result.contains("\"lastName\":\"Doe\"")).isTrue();
		assertThat(result.contains("\"age\":42")).isTrue();
		Pattern addressPattern = Pattern.compile("(\"address\":\\{.*?\\})");
		Matcher matcher = addressPattern.matcher(result);
		assertThat(matcher.find()).isTrue();
		String addressResult = matcher.group(1);
		assertThat(addressResult.contains("\"number\":123")).isTrue();
		assertThat(addressResult.contains("\"street\":\"Main Street\"")).isTrue();
	}

	@Test
	public void objectPayloadWithCustomObjectMapper() {
		ObjectMapper customMapper = JsonMapper.builder()
				.configure(JsonWriteFeature.QUOTE_PROPERTY_NAMES, false)
				.build();
		ObjectToJsonTransformer transformer = new ObjectToJsonTransformer(new JacksonJsonObjectMapper(customMapper));
		TestPerson person = new TestPerson("John", "Doe", 42);
		person.setAddress(new TestAddress(123, "Main Street"));
		String result = (String) transformer.transform(new GenericMessage<>(person)).getPayload();
		assertThat(result.contains("firstName:\"John\"")).isTrue();
		assertThat(result.contains("lastName:\"Doe\"")).isTrue();
		assertThat(result.contains("age:42")).isTrue();
		Pattern addressPattern = Pattern.compile("(address:\\{.*?\\})");
		Matcher matcher = addressPattern.matcher(result);
		assertThat(matcher.find()).isTrue();
		String addressResult = matcher.group(1);
		assertThat(addressResult.contains("number:123")).isTrue();
		assertThat(addressResult.contains("street:\"Main Street\"")).isTrue();
	}

	@Test
	public void collectionOrMap() {
		ObjectToJsonTransformer transformer = new ObjectToJsonTransformer();
		List<String> list = Collections.singletonList("foo");
		Message<?> out = transformer.transform(new GenericMessage<>(list));
		assertThat(out.getHeaders().get(JsonHeaders.TYPE_ID).toString()).contains("SingletonList");
		assertThat(out.getHeaders().get(JsonHeaders.CONTENT_TYPE_ID)).isEqualTo(String.class);
		Map<String, Long> map = Collections.singletonMap("foo", 1L);
		out = transformer.transform(new GenericMessage<>(map));
		assertThat(out.getHeaders().get(JsonHeaders.TYPE_ID).toString()).contains("SingletonMap");
		assertThat(out.getHeaders().get(JsonHeaders.CONTENT_TYPE_ID)).isEqualTo(Long.class);
		assertThat(out.getHeaders().get(JsonHeaders.KEY_TYPE_ID)).isEqualTo(String.class);
	}

	@Test
	public void collectionOrMapWithNullFirstElement() {
		ObjectToJsonTransformer transformer = new ObjectToJsonTransformer();
		List<String> list = Collections.singletonList(null);
		Message<?> out = transformer.transform(new GenericMessage<>(list));
		assertThat(out.getHeaders().get(JsonHeaders.TYPE_ID).toString()).contains("SingletonList");
		assertThat(out.getHeaders()).doesNotContainKey(JsonHeaders.CONTENT_TYPE_ID);
		Map<String, String> map = Collections.singletonMap("foo", null);
		out = transformer.transform(new GenericMessage<>(map));
		assertThat(out.getHeaders().get(JsonHeaders.TYPE_ID).toString()).contains("SingletonMap");
		assertThat(out.getHeaders()).doesNotContainKey(JsonHeaders.CONTENT_TYPE_ID);
		assertThat(out.getHeaders().get(JsonHeaders.KEY_TYPE_ID)).isEqualTo(String.class);
	}

	@Test
	public void testJsonStringAndJsonNode() {
		ObjectToJsonTransformer transformer = new ObjectToJsonTransformer(ObjectToJsonTransformer.ResultType.NODE);
		Object result = transformer.transform(new GenericMessage<>("{\"foo\": \"FOO\", \"bar\": 1}")).getPayload();
		assertThat(result).isInstanceOf(ObjectNode.class);
		ObjectNode objectNode = (ObjectNode) result;
		assertThat(objectNode.size()).isEqualTo(2);
		assertThat(objectNode.path("foo").textValue()).isEqualTo("FOO");
		assertThat(objectNode.path("bar").intValue()).isEqualTo(1);

		result = transformer.transform(new GenericMessage<>("foo")).getPayload();
		assertThat(result).isInstanceOf(StringNode.class);
		assertThat(((StringNode) result).textValue()).isEqualTo("foo");
	}

}
