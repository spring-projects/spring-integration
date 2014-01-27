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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.support.json.Jackson2JsonObjectMapper;
import org.springframework.integration.support.json.JsonObjectMapperAdapter;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 * @since 2.0
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class ObjectToJsonTransformerParserTests {

	@Autowired
	private volatile ApplicationContext context;

	@Autowired
	private volatile MessageChannel defaultObjectMapperInput;

	@Autowired
	private volatile MessageChannel customJsonObjectMapperInput;

	@Autowired
	private volatile MessageChannel jsonNodeInput;

	@Test
	public void testContentType(){
		ObjectToJsonTransformer transformer =
				TestUtils.getPropertyValue(context.getBean("defaultTransformer"), "handler.transformer", ObjectToJsonTransformer.class);
		assertEquals("application/json", TestUtils.getPropertyValue(transformer, "contentType"));

		assertEquals(Jackson2JsonObjectMapper.class, TestUtils.getPropertyValue(transformer, "jsonObjectMapper").getClass());

		Message<?> transformed = transformer.transform(MessageBuilder.withPayload("foo").build());
		assertTrue(transformed.getHeaders().containsKey(MessageHeaders.CONTENT_TYPE));
		assertEquals("application/json", transformed.getHeaders().get(MessageHeaders.CONTENT_TYPE));

		transformer =
				TestUtils.getPropertyValue(context.getBean("emptyContentTypeTransformer"), "handler.transformer", ObjectToJsonTransformer.class);
		assertEquals("", TestUtils.getPropertyValue(transformer, "contentType"));

		transformed = transformer.transform(MessageBuilder.withPayload("foo").build());
		assertFalse(transformed.getHeaders().containsKey(MessageHeaders.CONTENT_TYPE));

		transformed = transformer.transform(MessageBuilder.withPayload("foo").setHeader(MessageHeaders.CONTENT_TYPE, "foo").build());
		assertNotNull(transformed.getHeaders().get(MessageHeaders.CONTENT_TYPE));
		assertEquals("foo", transformed.getHeaders().get(MessageHeaders.CONTENT_TYPE));

		transformer =
				TestUtils.getPropertyValue(context.getBean("overridenContentTypeTransformer"), "handler.transformer", ObjectToJsonTransformer.class);
		assertEquals("text/xml", TestUtils.getPropertyValue(transformer, "contentType"));
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
		assertNotNull(reply);
		assertNotNull(reply.getPayload());
		assertEquals(String.class, reply.getPayload().getClass());
		String resultString = (String) reply.getPayload();
		assertTrue(resultString.contains("\"firstName\":\"John\""));
		assertTrue(resultString.contains("\"lastName\":\"Doe\""));
		assertTrue(resultString.contains("\"age\":42"));
		Pattern addressPattern = Pattern.compile("(\"address\":\\{.*?\\})");
		Matcher matcher = addressPattern.matcher(resultString);
		assertTrue(matcher.find());
		String addressResult = matcher.group(1);
		assertTrue(addressResult.contains("\"number\":123"));
		assertTrue(addressResult.contains("\"street\":\"Main Street\""));
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
		assertNotNull(reply);
		assertNotNull(reply.getPayload());
		assertEquals(String.class, reply.getPayload().getClass());
		String resultString = (String) reply.getPayload();
		assertEquals("{" + person.toString() + "}", resultString);
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
		assertNotNull(reply);
		Object payload = reply.getPayload();
		assertThat(payload, Matchers.instanceOf(JsonNode.class));
		StandardEvaluationContext evaluationContext = new StandardEvaluationContext();
		evaluationContext.addPropertyAccessor(new JsonPropertyAccessor());
		Expression expression = new SpelExpressionParser().parseExpression("firstName.toString() == 'John' and age.toString() == '42'");

		assertTrue(expression.getValue(evaluationContext, payload, Boolean.class));
	}

	static class CustomJsonObjectMapper extends JsonObjectMapperAdapter<Object, Object> {

		@Override
		public String toJson(Object value) throws Exception {
			return "{" + value.toString() + "}";
		}
	}

}
