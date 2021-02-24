/*
 * Copyright 2002-2021 the original author or authors.
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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.support.DefaultMessageBuilderFactory;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.support.json.Jackson2JsonObjectMapper;
import org.springframework.integration.support.json.JsonObjectMapper;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.0
 */
@SpringJUnitConfig
public class ObjectToJsonTransformerParserTests {

	@Autowired
	private ApplicationContext context;

	@Autowired
	private MessageChannel defaultObjectMapperInput;

	@Autowired
	private MessageChannel customJsonObjectMapperInput;

	@Autowired
	private MessageChannel jsonNodeInput;

	@Autowired
	private DefaultMessageBuilderFactory defaultMessageBuilderFactory;

	@Autowired
	private StandardEvaluationContext evaluationContext;

	@Test
	public void testContentType() {
		ObjectToJsonTransformer transformer =
				TestUtils.getPropertyValue(context.getBean("defaultTransformer"), "handler.transformer",
						ObjectToJsonTransformer.class);
		assertThat(TestUtils.getPropertyValue(transformer, "contentType")).isEqualTo("application/json");

		assertThat(TestUtils.getPropertyValue(transformer, "jsonObjectMapper").getClass())
				.isEqualTo(Jackson2JsonObjectMapper.class);

		Message<?> transformed = transformer.transform(MessageBuilder.withPayload("foo").build());

		// spring.integration.readOnly.headers=contentType, so no 'contentType'
		assertThat(transformed.getHeaders().containsKey(MessageHeaders.CONTENT_TYPE)).isFalse();

		// Reset readOnlyHeaders to defaults. Therefore the 'contentType' should be presented in subsequent tests
		this.defaultMessageBuilderFactory.setReadOnlyHeaders();

		transformer =
				TestUtils.getPropertyValue(context.getBean("emptyContentTypeTransformer"), "handler.transformer",
						ObjectToJsonTransformer.class);
		assertThat(TestUtils.getPropertyValue(transformer, "contentType")).isEqualTo("");

		transformed = transformer.transform(MessageBuilder.withPayload("foo").build());
		assertThat(transformed.getHeaders().containsKey(MessageHeaders.CONTENT_TYPE)).isFalse();

		transformed = transformer.transform(MessageBuilder.withPayload("foo").setHeader(MessageHeaders.CONTENT_TYPE,
				"foo").build());
		assertThat(transformed.getHeaders().get(MessageHeaders.CONTENT_TYPE)).isNotNull();
		assertThat(transformed.getHeaders().get(MessageHeaders.CONTENT_TYPE)).isEqualTo("foo");

		transformer =
				TestUtils.getPropertyValue(context.getBean("overriddenContentTypeTransformer"), "handler.transformer",
						ObjectToJsonTransformer.class);
		assertThat(TestUtils.getPropertyValue(transformer, "contentType")).isEqualTo("text/xml");
	}


	@Test
	public void defaultObjectMapper() {
		TestAddress address = new TestAddress();
		address.setNumber(123);
		address.setStreet("Main Street");
		TestPerson person = new TestPerson();
		person.setFirstName("John");
		person.setLastName("Doe");
		person.setAge(42);
		person.setAddress(address);
		QueueChannel replyChannel = new QueueChannel();
		Message<TestPerson> message = MessageBuilder.withPayload(person).setReplyChannel(replyChannel).build();
		this.defaultObjectMapperInput.send(message);
		Message<?> reply = replyChannel.receive(0);
		assertThat(reply).isNotNull();
		assertThat(reply.getPayload()).isNotNull();
		assertThat(reply.getPayload().getClass()).isEqualTo(String.class);
		String resultString = (String) reply.getPayload();
		assertThat(resultString.contains("\"firstName\":\"John\"")).isTrue();
		assertThat(resultString.contains("\"lastName\":\"Doe\"")).isTrue();
		assertThat(resultString.contains("\"age\":42")).isTrue();
		Pattern addressPattern = Pattern.compile("(\"address\":\\{.*?\\})");
		Matcher matcher = addressPattern.matcher(resultString);
		assertThat(matcher.find()).isTrue();
		String addressResult = matcher.group(1);
		assertThat(addressResult.contains("\"number\":123")).isTrue();
		assertThat(addressResult.contains("\"street\":\"Main Street\"")).isTrue();
	}

	@Test
	public void testInt2831CustomJsonObjectMapper() {
		TestPerson person = new TestPerson();
		person.setFirstName("John");
		person.setLastName("Doe");
		person.setAge(42);
		QueueChannel replyChannel = new QueueChannel();
		Message<TestPerson> message = MessageBuilder.withPayload(person).setReplyChannel(replyChannel).build();
		this.customJsonObjectMapperInput.send(message);
		Message<?> reply = replyChannel.receive(0);
		assertThat(reply).isNotNull();
		assertThat(reply.getPayload()).isNotNull();
		assertThat(reply.getPayload().getClass()).isEqualTo(String.class);
		String resultString = (String) reply.getPayload();
		assertThat(resultString).isEqualTo("{" + person.toString() + "}");
	}

	@Test
	public void testNodeResultType() {
		TestPerson person = new TestPerson();
		person.setFirstName("John");
		person.setLastName("Doe");
		person.setAge(42);
		QueueChannel replyChannel = new QueueChannel();
		Message<TestPerson> message = MessageBuilder.withPayload(person).setReplyChannel(replyChannel).build();
		this.jsonNodeInput.send(message);
		Message<?> reply = replyChannel.receive(0);
		assertThat(reply).isNotNull();
		Object payload = reply.getPayload();
		assertThat(payload).isInstanceOf(JsonNode.class);
		StandardEvaluationContext evaluationContext = new StandardEvaluationContext();
		evaluationContext.addPropertyAccessor(new JsonPropertyAccessor());
		Expression expression = new SpelExpressionParser()
				.parseExpression("firstName.toString() == 'John' and age.toString() == '42'");

		assertThat(expression.getValue(evaluationContext, payload, Boolean.class)).isTrue();
	}

	@Test
	public void testReflectionBeforeJsonString() {
		Expression exp = new SpelExpressionParser().parseExpression("payload.class.name");
		assertThat(exp.getValue(this.evaluationContext, new GenericMessage<>("foo"))).isEqualTo(String.class.getName());
		exp = new SpelExpressionParser().parseExpression("payload.foo");
		assertThat(exp.getValue(this.evaluationContext, new GenericMessage<>("{\"foo\" : \"bar\"}"))).isEqualTo("bar");
	}

	static class CustomJsonObjectMapper implements JsonObjectMapper<Object, Object> {

		@Override
		public String toJson(Object value) {
			return "{" + value.toString() + "}";
		}

	}

}
