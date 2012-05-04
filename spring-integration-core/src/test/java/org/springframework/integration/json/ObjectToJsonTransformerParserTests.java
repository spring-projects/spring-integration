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
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Mark Fisher
 * @author James Carr
 * @since 2.0
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class ObjectToJsonTransformerParserTests {

	@Autowired
	private volatile MessageChannel defaultObjectMapperInput;

	@Autowired
	private volatile MessageChannel customObjectMapperInput;

	@Autowired
	private volatile MessageChannel contentTypeHeaderAddedInput;

	private QueueChannel replyChannel = new QueueChannel();
	@Test
	public void shouldAddJsonHeaderIfIndicated(){
		TestPerson person = person(address(123, "Main Street"), "John", "Doe", 42);
		Message<TestPerson> message = MessageBuilder.withPayload(person).setReplyChannel(replyChannel).build();
		
		this.contentTypeHeaderAddedInput.send(message);
		Message<?> reply = replyChannel.receive(0);

		assertTrue(reply.getHeaders().containsKey("content-type"));
		assertEquals("application/json", reply.getHeaders().get("content-type"));
		assertMessageSerializedAsJson(reply);
	}

	@Test
	public void shouldNotAddJsonContentTypeIfNotIndicated(){
		TestPerson person = person(address(123, "Main Street"), "John", "Doe", 42);
		

		Message<TestPerson> message = MessageBuilder.withPayload(person).setReplyChannel(replyChannel).build();
		this.defaultObjectMapperInput.send(message);
		Message<?> reply = replyChannel.receive(0);
		
		assertFalse(reply.getHeaders().containsKey("content-type"));
	}
	@Test
	public void overwriteExistingHeaderIfSetContentTypeIsTrue(){
		TestPerson person = person(address(123, "Main Street"), "John", "Doe", 42);
		Message<TestPerson> message = MessageBuilder
				.withPayload(person)
				.setHeader("content-type", "x-java-serialized-object")
				.setReplyChannel(replyChannel).build();
		
		this.contentTypeHeaderAddedInput.send(message);
		Message<?> reply = replyChannel.receive(0);

		assertTrue(reply.getHeaders().containsKey("content-type"));
		assertEquals("application/json", reply.getHeaders().get("content-type"));
		assertMessageSerializedAsJson(reply);
		
	}
	
	@Test
	public void doNotOverwriteExistingHeaderIfSetContentTypeIsTrue(){
		TestPerson person = person(address(123, "Main Street"), "John", "Doe", 42);
		Message<TestPerson> message = MessageBuilder
				.withPayload(person)
				.setHeader("content-type", "x-java-serialized-object")
				.setReplyChannel(replyChannel).build();
		
		this.defaultObjectMapperInput.send(message);
		Message<?> reply = replyChannel.receive(0);

		assertEquals("x-java-serialized-object", reply.getHeaders().get("content-type"));
		
	}

	@Test
	public void defaultObjectMapper() {
		TestPerson person = person(address(123, "Main Street"), "John", "Doe", 42);
		

		Message<TestPerson> message = MessageBuilder.withPayload(person).setReplyChannel(replyChannel).build();
		this.defaultObjectMapperInput.send(message);
		Message<?> reply = replyChannel.receive(0);
		
		assertMessageSerializedAsJson(reply);
	}
	private void assertMessageSerializedAsJson(Message<?> reply) {
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
	private TestPerson person(TestAddress address, String firstName, String lastName, int age) {
		TestPerson person = new TestPerson();
		person.setFirstName(firstName);
		person.setLastName(lastName);
		person.setAge(age);
		person.setAddress(address);
		return person;
	}
	private TestAddress address(int number, String streetName) {
		TestAddress address = new TestAddress();
		address.setNumber(number);
		address.setStreet(streetName);
		return address;
	}

	@Test
	public void customObjectMapper() {
		TestAddress address = address(123, "Main Street");
		TestPerson person = person(address, "John", "Doe", 42);
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
