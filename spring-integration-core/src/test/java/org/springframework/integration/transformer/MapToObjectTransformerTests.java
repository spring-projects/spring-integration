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

package org.springframework.integration.transformer;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterRegistry;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.support.utils.IntegrationUtils;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;


/**
 * @author Oleg Zhurakousky
 * @author Gunnar Hillert
 * @author Artem Bilan
 *
 * @since 2.0
 */
public class MapToObjectTransformerTests {

	private final GenericApplicationContext context = TestUtils.createTestApplicationContext();

	@BeforeEach
	public void prepare() {
		this.context.registerBeanDefinition(IntegrationUtils.INTEGRATION_CONVERSION_SERVICE_BEAN_NAME,
				new RootBeanDefinition("org.springframework.integration.config.CustomConversionServiceFactoryBean"));
		this.context.refresh();
	}

	@AfterEach
	public void terDown() {
		this.context.close();
	}

	@Test
	public void testMapToObjectTransformation() {
		Map<String, Object> map = new HashMap<>();
		map.put("fname", "Justin");
		map.put("lname", "Case");
		Address address = new Address();
		address.setStreet("1123 Main st");
		map.put("address", address);

		Message<?> message = MessageBuilder.withPayload(map).build();

		MapToObjectTransformer transformer = new MapToObjectTransformer(Person.class);
		transformer.setBeanFactory(this.context);
		Message<?> newMessage = transformer.transform(message);
		Person person = (Person) newMessage.getPayload();
		assertThat(person).isNotNull();
		assertThat(person.getFname()).isEqualTo("Justin");
		assertThat(person.getLname()).isEqualTo("Case");
		assertThat(person.getSsn()).isNull();
		assertThat(person.getAddress()).isNotNull();
		assertThat(person.getAddress().getStreet()).isEqualTo("1123 Main st");
	}

	@Test
	public void testMapToObjectTransformationWithPrototype() {
		Map<String, Object> map = new HashMap<>();
		map.put("fname", "Justin");
		map.put("lname", "Case");
		Address address = new Address();
		address.setStreet("1123 Main st");
		map.put("address", address);

		Message<?> message = MessageBuilder.withPayload(map).build();
		StaticApplicationContext ac = new StaticApplicationContext();
		ac.registerPrototype("person", Person.class);
		MapToObjectTransformer transformer = new MapToObjectTransformer("person");
		transformer.setBeanFactory(ac.getBeanFactory());
		Message<?> newMessage = transformer.transform(message);
		Person person = (Person) newMessage.getPayload();
		assertThat(person).isNotNull();
		assertThat(person.getFname()).isEqualTo("Justin");
		assertThat(person.getLname()).isEqualTo("Case");
		assertThat(person.getSsn()).isNull();
		assertThat(person.getAddress()).isNotNull();
		assertThat(person.getAddress().getStreet()).isEqualTo("1123 Main st");
		ac.close();
	}

	@Test
	public void testMapToObjectTransformationWithConversionService() {
		Map<String, Object> map = new HashMap<>();
		map.put("fname", "Justin");
		map.put("lname", "Case");
		map.put("address", "1123 Main st");

		Message<?> message = MessageBuilder.withPayload(map).build();

		MapToObjectTransformer transformer = new MapToObjectTransformer(Person.class);
		ConverterRegistry conversionService =
				this.context.getBean(IntegrationUtils.INTEGRATION_CONVERSION_SERVICE_BEAN_NAME,
						ConverterRegistry.class);
		conversionService.addConverter(new StringToAddressConverter());
		transformer.setBeanFactory(this.context);

		Message<?> newMessage = transformer.transform(message);
		Person person = (Person) newMessage.getPayload();
		assertThat(person).isNotNull();
		assertThat(person.getFname()).isEqualTo("Justin");
		assertThat(person.getLname()).isEqualTo("Case");
		assertThat(person.getAddress()).isNotNull();
		assertThat(person.getAddress().getStreet()).isEqualTo("1123 Main st");
	}

	public static class Person {

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

	public class StringToAddressConverter implements Converter<String, Address> {

		@Override
		public Address convert(String source) {
			Address address = new Address();
			address.setStreet(source);
			return address;
		}

	}

}
