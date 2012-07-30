/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.integration.transformer;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.ConversionServiceFactory;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.integration.Message;
import org.springframework.integration.support.MessageBuilder;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;

/**
 * @author Oleg Zhurakousky
 * @author Gunnar Hillert
 *
 * @since 2.0
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class MapToObjectTransformerTests {


	@Test
	public void testMapToObjectTransformation(){
		Map map = new HashMap();
		map.put("fname", "Justin");
		map.put("lname", "Case");
		Address address = new Address();
		address.setStreet("1123 Main st");
		map.put("address", address);

		Message message = MessageBuilder.withPayload(map).build();

		MapToObjectTransformer transformer = new MapToObjectTransformer(Person.class);
		transformer.setBeanFactory(this.getBeanFactory());
		Message newMessage = transformer.transform(message);
		Person person = (Person) newMessage.getPayload();
		assertNotNull(person);
		assertEquals("Justin", person.getFname());
		assertEquals("Case", person.getLname());
		assertNull(person.getSsn());
		assertNotNull(person.getAddress());
		assertEquals("1123 Main st", person.getAddress().getStreet());
	}

	@Test
	public void testMapToObjectTransformationWithPrototype(){
		Map map = new HashMap();
		map.put("fname", "Justin");
		map.put("lname", "Case");
		Address address = new Address();
		address.setStreet("1123 Main st");
		map.put("address", address);

		Message message = MessageBuilder.withPayload(map).build();
		StaticApplicationContext ac = new StaticApplicationContext();
		ac.registerPrototype("person", Person.class);
		MapToObjectTransformer transformer = new MapToObjectTransformer("person");
		transformer.setBeanFactory(ac.getBeanFactory());
		Message newMessage = transformer.transform(message);
		Person person = (Person) newMessage.getPayload();
		assertNotNull(person);
		assertEquals("Justin", person.getFname());
		assertEquals("Case", person.getLname());
		assertNull(person.getSsn());
		assertNotNull(person.getAddress());
		assertEquals("1123 Main st", person.getAddress().getStreet());
	}

	@Test
	public void testMapToObjectTransformationWithConversionService(){
		Map map = new HashMap();
		map.put("fname", "Justin");
		map.put("lname", "Case");
		map.put("address", "1123 Main st");

		Message message = MessageBuilder.withPayload(map).build();

		MapToObjectTransformer transformer = new MapToObjectTransformer(Person.class);
		ConfigurableBeanFactory beanFactory = this.getBeanFactory();
		((GenericConversionService)beanFactory.getConversionService()).addConverter(new StringToAddressConverter());
		transformer.setBeanFactory(beanFactory);

		Message newMessage = transformer.transform(message);
		Person person = (Person) newMessage.getPayload();
		assertNotNull(person);
		assertEquals("Justin", person.getFname());
		assertEquals("Case", person.getLname());
		assertNotNull(person.getAddress());
		assertEquals("1123 Main st", person.getAddress().getStreet());
	}

	@SuppressWarnings("deprecation")
	private ConfigurableBeanFactory getBeanFactory(){
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		GenericConversionService conversionService = ConversionServiceFactory.createDefaultConversionService();
		beanFactory.setConversionService(conversionService);
		return beanFactory;
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

	public class StringToAddressConverter implements Converter<String, Address>{
		public Address convert(String source) {
			Address address = new Address();
			address.setStreet(source);
			return address;
		}
	}
}
