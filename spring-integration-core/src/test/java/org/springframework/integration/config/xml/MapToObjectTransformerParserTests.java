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

package org.springframework.integration.config.xml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.convert.converter.Converter;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;


/**
 * @author Oleg Zhurakousky
 * @since 2.0
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class MapToObjectTransformerParserTests {
	@Autowired
	@Qualifier("input")
	private MessageChannel input;
	@Autowired
	@Qualifier("output")
	private PollableChannel output;

	@Autowired
	@Qualifier("inputA")
	private MessageChannel inputA;
	@Autowired
	@Qualifier("outputA")
	private PollableChannel outputA;

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void testMapToObjectTransformationWithType(){
		Map map = new HashMap();
		map.put("fname", "Justin");
		map.put("lname", "Case");
		Address address = new Address();
		address.setStreet("1123 Main st");
		map.put("address", address);

		Message message = MessageBuilder.withPayload(map).build();
		input.send(message);

		Message outMessage = output.receive();

		Person person = (Person) outMessage.getPayload();
		assertNotNull(person);
		assertEquals("Justin", person.getFname());
		assertEquals("Case", person.getLname());
		assertNull(person.getSsn());
		assertNotNull(person.getAddress());
		assertEquals("1123 Main st", person.getAddress().getStreet());
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void testMapToObjectTransformationWithRef(){
		Map map = new HashMap();
		map.put("fname", "Justin");
		map.put("lname", "Case");
		Address address = new Address();
		address.setStreet("1123 Main st");
		map.put("address", address);

		Message message = MessageBuilder.withPayload(map).build();
		inputA.send(message);
		Message<?> newMessage =  outputA.receive();
		Person person = (Person) newMessage.getPayload();
		assertNotNull(person);
		assertEquals("Justin", person.getFname());
		assertEquals("Case", person.getLname());
		assertNull(person.getSsn());
		assertNotNull(person.getAddress());
		assertEquals("1123 Main st", person.getAddress().getStreet());
	}


	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void testMapToObjectTransformationWithConversionService(){
		Map map = new HashMap();
		map.put("fname", "Justin");
		map.put("lname", "Case");
		map.put("address", "1123 Main st");

		Message message = MessageBuilder.withPayload(map).build();
		inputA.send(message);

		Message newMessage = outputA.receive();
		Person person = (Person) newMessage.getPayload();
		assertNotNull(person);
		assertEquals("Justin", person.getFname());
		assertEquals("Case", person.getLname());
		assertNotNull(person.getAddress());
		assertEquals("1123 Main st", person.getAddress().getStreet());
	}
	@Test(expected=BeanCreationException.class)
	public void testNonPrototypeFailure(){
		new ClassPathXmlApplicationContext("MapToObjectTransformerParserTests-context-fail.xml", MapToObjectTransformerParserTests.class);
	}

	public static class Person{
		private String fname;
		private String lname;
		private String ssn;
		private Address address;
		public String getSsn() {
			return ssn;
		}
		public void setSsn(String ssn) {
			this.ssn = ssn;
		}
		public String getFname() {
			return fname;
		}
		public void setFname(String fname) {
			this.fname = fname;
		}
		public String getLname() {
			return lname;
		}
		public void setLname(String lname) {
			this.lname = lname;
		}
		public Address getAddress() {
			return address;
		}
		public void setAddress(Address address) {
			this.address = address;
		}
	}

	public static class Address {
		private String street;

		public String getStreet() {
			return street;
		}

		public void setStreet(String street) {
			this.street = street;
		}
	}

	public static class StringToAddressConverter implements Converter<String, Address>{
		public StringToAddressConverter(){}
		public Address convert(String source) {
			Address address = new Address();
			address.setStreet(source);
			return address;
		}
	}
}
