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

import org.codehaus.jackson.JsonParser.Feature;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Test;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 * @since 2.0
 */
public class JsonToObjectTransformerTests {

	@Test
	public void objectPayload() throws Exception {
		JsonToObjectTransformer<TestPerson> transformer = new JsonToObjectTransformer<TestPerson>(TestPerson.class);
		String jsonString = "{\"firstName\":\"John\",\"lastName\":\"Doe\",\"age\":42,\"address\":{\"number\":123,\"street\":\"Main Street\"}}";
		TestPerson person = transformer.transformPayload(jsonString);
		assertEquals("John", person.getFirstName());
		assertEquals("Doe", person.getLastName());
		assertEquals(42, person.getAge());
		assertEquals("123 Main Street", person.getAddress().toString());
	}

	@Test
	public void objectPayloadWithCustomMapper() throws Exception {
		ObjectMapper customMapper = new ObjectMapper();
		customMapper.configure(Feature.ALLOW_UNQUOTED_FIELD_NAMES, Boolean.TRUE);
		customMapper.configure(Feature.ALLOW_SINGLE_QUOTES, Boolean.TRUE);
		JsonToObjectTransformer<TestPerson> transformer =
				new JsonToObjectTransformer<TestPerson>(TestPerson.class, new JacksonJsonObjectMapper(customMapper));
		String jsonString = "{firstName:'John', lastName:'Doe', age:42, address:{number:123, street:'Main Street'}}";
		TestPerson person = transformer.transformPayload(jsonString);
		assertEquals("John", person.getFirstName());
		assertEquals("Doe", person.getLastName());
		assertEquals(42, person.getAge());
		assertEquals("123 Main Street", person.getAddress().toString());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testInt2831IllegalArgument() throws Exception {
		new JsonToObjectTransformer<String>(String.class, new Object());
	}


	@SuppressWarnings("unused")
	private static class TestPerson {

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


	@SuppressWarnings("unused")
	private static class TestAddress {

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

}
