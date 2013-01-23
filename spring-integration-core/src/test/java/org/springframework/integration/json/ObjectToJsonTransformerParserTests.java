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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.codehaus.jackson.JsonGenerator.Feature;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.MessageHeaders;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gary Russell
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
	private volatile MessageChannel customObjectMapperInput;

	@Test
	public void testContentType(){
		ObjectToJsonTransformer transformer =
				TestUtils.getPropertyValue(context.getBean("defaultTransformer"), "handler.transformer", ObjectToJsonTransformer.class);
		assertEquals("application/json", TestUtils.getPropertyValue(transformer, "contentType"));

		Message<?> transformed = transformer.transform(MessageBuilder.withPayload("foo").build());
		assertTrue(transformed.getHeaders().containsKey(MessageHeaders.CONTENT_TYPE));
		assertEquals("application/json", transformed.getHeaders().get(MessageHeaders.CONTENT_TYPE));

		transformer =
				TestUtils.getPropertyValue(context.getBean("customTransformer"), "handler.transformer", ObjectToJsonTransformer.class);
		assertEquals("application/json", TestUtils.getPropertyValue(transformer, "contentType"));

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
	public void customObjectMapper() {
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
		this.customObjectMapperInput.send(message);
		Message<?> reply = replyChannel.receive(0);
		assertNotNull(reply);
		assertNotNull(reply.getPayload());
		assertEquals(String.class, reply.getPayload().getClass());
		String resultString = (String) reply.getPayload();
		assertTrue(resultString.contains("firstName:\"John\""));
		assertTrue(resultString.contains("lastName:\"Doe\""));
		assertTrue(resultString.contains("age:42"));
		Pattern addressPattern = Pattern.compile("(address:\\{.*?\\})");
		Matcher matcher = addressPattern.matcher(resultString);
		assertTrue(matcher.find());
		String addressResult = matcher.group(1);
		assertTrue(addressResult.contains("number:123"));
		assertTrue(addressResult.contains("street:\"Main Street\""));
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


		public int getNumber() {
			return this.number;
		}

		public void setNumber(int number) {
			this.number = number;
		}

		public String getStreet() {
			return this.street;
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
			this.configure(Feature.QUOTE_FIELD_NAMES, Boolean.FALSE);
		}
	}

}
