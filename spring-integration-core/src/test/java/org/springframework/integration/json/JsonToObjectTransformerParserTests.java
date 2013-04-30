/*
 * Copyright 2002-2013 the original author or authors.
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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

import org.codehaus.jackson.JsonParser.Feature;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.core.MessageHandler;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 * @since 2.0
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class JsonToObjectTransformerParserTests {

	@Autowired
	private volatile MessageChannel defaultObjectMapperInput;

	@Autowired
	private volatile MessageChannel customObjectMapperInput;

	@Autowired
	private volatile MessageChannel customJsonObjectMapperInput;

	@Autowired
	@Qualifier("defaultJacksonMapperTransformer.handler")
	private MessageHandler defaultJacksonMapperTransformer;

	@Autowired
	@Qualifier("customJacksonMapperTransformer.handler")
	private MessageHandler customJacksonMapperTransformer;

	@Autowired
	@Qualifier("customJsonMapperTransformer.handler")
	private MessageHandler customJsonMapperTransformer;

	@Autowired
	private ObjectMapper customObjectMapper;

	@Autowired
	private JsonObjectMapper<?> jsonObjectMapper;

	@Test
	public void defaultObjectMapper() {
		Object jsonToObjectTransformer = TestUtils.getPropertyValue(this.defaultJacksonMapperTransformer, "transformer");
		assertEquals(Jackson2JsonObjectMapper.class, TestUtils.getPropertyValue(jsonToObjectTransformer, "jsonObjectMapper").getClass());

		String jsonString = "{\"firstName\":\"John\",\"lastName\":\"Doe\",\"age\":42,\"address\":{\"number\":123,\"street\":\"Main Street\"}}";
		QueueChannel replyChannel = new QueueChannel();
		Message<String> message = MessageBuilder.withPayload(jsonString).setReplyChannel(replyChannel).build();
		this.defaultObjectMapperInput.send(message);
		Message<?> reply = replyChannel.receive(0);
		assertNotNull(reply);
		assertNotNull(reply.getPayload());
		assertEquals(TestPerson.class, reply.getPayload().getClass());
		TestPerson person = (TestPerson) reply.getPayload();
		assertEquals("John", person.getFirstName());
		assertEquals("Doe", person.getLastName());
		assertEquals(42, person.getAge());
		assertEquals("123 Main Street", person.getAddress().toString());
	}

	@Test
	public void customObjectMapper() {
		Object jsonToObjectTransformer = TestUtils.getPropertyValue(this.customJacksonMapperTransformer, "transformer");
		JsonObjectMapper<?> jsonObjectMapper = TestUtils.getPropertyValue(jsonToObjectTransformer, "jsonObjectMapper", JsonObjectMapper.class);
		assertSame(this.customObjectMapper, TestUtils.getPropertyValue(jsonObjectMapper, "objectMapper"));

		String jsonString = "{firstName:'John', lastName:'Doe', age:42, address:{number:123, street:'Main Street'}}";
		QueueChannel replyChannel = new QueueChannel();
		Message<String> message = MessageBuilder.withPayload(jsonString).setReplyChannel(replyChannel).build();
		this.customObjectMapperInput.send(message);
		Message<?> reply = replyChannel.receive(0);
		assertNotNull(reply);
		assertNotNull(reply.getPayload());
		assertEquals(TestPerson.class, reply.getPayload().getClass());
		TestPerson person = (TestPerson) reply.getPayload();
		assertEquals("John", person.getFirstName());
		assertEquals("Doe", person.getLastName());
		assertEquals(42, person.getAge());
		assertEquals("123 Main Street", person.getAddress().toString());
	}

	@Test
	public void testInt2831CustomJsonObjectMapper() {
		Object jsonToObjectTransformer = TestUtils.getPropertyValue(this.customJsonMapperTransformer, "transformer");
		assertSame(this.jsonObjectMapper, TestUtils.getPropertyValue(jsonToObjectTransformer, "jsonObjectMapper", JsonObjectMapper.class));

		String jsonString = "{firstName:'John', lastName:'Doe', age:42, address:{number:123, street:'Main Street'}}";
		QueueChannel replyChannel = new QueueChannel();
		Message<String> message = MessageBuilder.withPayload(jsonString).setReplyChannel(replyChannel).build();
		this.customJsonObjectMapperInput.send(message);
		Message<?> reply = replyChannel.receive(0);
		assertNotNull(reply);
		assertNotNull(reply.getPayload());
		assertEquals(TestJsonContainer.class, reply.getPayload().getClass());
		TestJsonContainer result = (TestJsonContainer) reply.getPayload();
		assertEquals(jsonString, result.getJson());
	}


	static class TestPerson {

		private String firstName;

		private String lastName;

		private int age;

		private TestAddress address;


		public String getFirstName() {
			return this.firstName;
		}

		public void setFirstName(String firstName) {
			this.firstName = firstName;
		}

		public void setLastName(String lastName) {
			this.lastName = lastName;
		}

		public String getLastName() {
			return this.lastName;
		}

		public void setAge(int age) {
			this.age = age;
		}

		public int getAge() {
			return this.age;
		}

		public void setAddress(TestAddress address) {
			this.address = address;
		}

		public TestAddress getAddress() {
			return this.address;
		}

		@Override
		public String toString() {
			return "name=" + this.firstName + " " + this.lastName
					+ ", age=" + this.age + ", address=" + this.address;
		}
	}


	static class TestAddress {

		private int number;

		private String street;


		public void setNumber(int number) {
			this.number = number;
		}

		public void setStreet(String street) {
			this.street = street;
		}

		@Override
		public String toString() {
			return this.number + " " + this.street;
		}
	}


	static class CustomObjectMapper extends ObjectMapper {

		public CustomObjectMapper() {
			this.configure(Feature.ALLOW_UNQUOTED_FIELD_NAMES, Boolean.TRUE);
			this.configure(Feature.ALLOW_SINGLE_QUOTES, Boolean.TRUE);
		}
	}

	@SuppressWarnings("rawtypes")
	static class CustomJsonObjectMapper extends JsonObjectMapperAdapter {

		@Override
		public Object fromJson(String json, Class valueType) throws Exception {
			return new TestJsonContainer(json);
		}
	}

	static class TestJsonContainer {

		private final String json;

		TestJsonContainer(String json) {
			this.json = json;
		}

		public String getJson() {
			return json;
		}
	}

}
