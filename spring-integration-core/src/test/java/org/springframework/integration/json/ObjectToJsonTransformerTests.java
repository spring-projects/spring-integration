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

import org.codehaus.jackson.JsonGenerator.Feature;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Test;

/**
 * @author Mark Fisher
 * @since 2.0
 */
public class ObjectToJsonTransformerTests {

	@Test
	public void simpleStringPayload() throws Exception {
		ObjectToJsonTransformer transformer = new  ObjectToJsonTransformer();
		String result = transformer.transformPayload("foo");
		assertEquals("\"foo\"", result);
	}

	@Test
	public void simpleIntegerPayload() throws Exception {
		ObjectToJsonTransformer transformer = new  ObjectToJsonTransformer();
		String result = transformer.transformPayload(123);
		assertEquals("123", result);
	}

	@Test
	public void objectPayload() throws Exception {
		ObjectToJsonTransformer transformer = new  ObjectToJsonTransformer();
		TestAddress address = new TestAddress(123, "Main Street");
		TestPerson person = new TestPerson("John", "Doe", 42);
		person.setAddress(address);
		String result = transformer.transformPayload(person);
		String expected = "{\"address\":{\"number\":123,\"street\":\"Main Street\"},\"firstName\":\"John\",\"lastName\":\"Doe\",\"age\":42}";
		assertEquals(expected, result);
	}

	@Test
	public void objectPayloadWithCustomObjectMapper() throws Exception {
		ObjectMapper customMapper = new ObjectMapper();
		customMapper.configure(Feature.QUOTE_FIELD_NAMES, Boolean.FALSE);
		ObjectToJsonTransformer transformer = new  ObjectToJsonTransformer(customMapper);
		TestPerson person = new TestPerson("John", "Doe", 42);
		person.setAddress(new TestAddress(123, "Main Street"));
		String result = transformer.transformPayload(person);
		String expected = "{address:{number:123,street:\"Main Street\"},firstName:\"John\",lastName:\"Doe\",age:42}";
		assertEquals(expected, result);
	}


	@SuppressWarnings("unused")
	private static class TestPerson {

		private String firstName;

		private String lastName;

		private int age;

		private TestAddress address;


		public TestPerson(String firstName, String lastName, int age) {
			this.firstName = firstName;
			this.lastName = lastName;
			this.age = age;
		}

		public String getFirstName() {
			return firstName;
		}

		public String getLastName() {
			return lastName;
		}

		public int getAge() {
			return age;
		}

		public TestAddress getAddress() {
			return address;
		}

		public void setAddress(TestAddress address) {
			this.address = address;
		}
	}


	@SuppressWarnings("unused")
	private static class TestAddress {

		private int number;

		private String street;


		public TestAddress(int number, String street) {
			this.number = number;
			this.street = street;
		}

		public int getNumber() {
			return number;
		}

		public String getStreet() {
			return street;
		}
	}

}
