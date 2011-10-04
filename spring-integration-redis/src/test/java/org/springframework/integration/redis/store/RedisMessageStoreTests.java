/*
 * Copyright 2007-2011 the original author or authors
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 */
package org.springframework.integration.redis.store;

import java.io.Serializable;
import java.util.UUID;

import org.junit.Test;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.integration.Message;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.redis.rules.RedisAvailable;
import org.springframework.integration.redis.rules.RedisAvailableTests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;

/**
 * @author Oleg Zhurakousky
 *
 */
public class RedisMessageStoreTests extends RedisAvailableTests {

	@Test
	@RedisAvailable
	public void testGetNonExistingMessage(){	
		JedisConnectionFactory jcf = this.getConnectionFactoryForTest();
		RedisMessageStore store = new RedisMessageStore(jcf);
		Message<?> message = store.getMessage(UUID.randomUUID());
		assertNull(message);
	}
	
	@Test
	@RedisAvailable
	public void testGetMessageCountWhenEmpty(){	
		JedisConnectionFactory jcf = this.getConnectionFactoryForTest();
		RedisMessageStore store = new RedisMessageStore(jcf);
		assertEquals(0, store.getMessageCount());
	}
	
	@Test
	@RedisAvailable
	public void testAddStringMessage(){	
		JedisConnectionFactory jcf = this.getConnectionFactoryForTest();
		RedisMessageStore store = new RedisMessageStore(jcf);
		Message<String> stringMessage = new GenericMessage<String>("Hello Redis");
		Message<String> storedMessage =  store.addMessage(stringMessage);
		assertNotSame(stringMessage, storedMessage);
		assertEquals("Hello Redis", storedMessage.getPayload());
	}
	
	@Test
	@RedisAvailable
	public void testAddSerializableObjectMessage(){	
		JedisConnectionFactory jcf = this.getConnectionFactoryForTest();
		RedisMessageStore store = new RedisMessageStore(jcf);
		Address address = new Address();
		address.setAddress("1600 Pennsylvania Av, Washington, DC");
		Person person = new Person(address, "Barak Obama");
		
		Message<Person> objectMessage = new GenericMessage<Person>(person);
		Message<Person> storedMessage =  store.addMessage(objectMessage);
		assertNotSame(objectMessage, storedMessage);
		assertEquals("Barak Obama", storedMessage.getPayload().getName());
	}
	
	@Test(expected=IllegalArgumentException.class)
	@RedisAvailable
	public void testAddNonSerializableObjectMessage(){	
		JedisConnectionFactory jcf = this.getConnectionFactoryForTest();
		RedisMessageStore store = new RedisMessageStore(jcf);
		
		Message<Foo> objectMessage = new GenericMessage<Foo>(new Foo());
		store.addMessage(objectMessage);
	}
	
	@SuppressWarnings("unchecked")
	@Test
	@RedisAvailable
	public void testAddAndGetStringMessage(){	
		JedisConnectionFactory jcf = this.getConnectionFactoryForTest();
		RedisMessageStore store = new RedisMessageStore(jcf);
		Message<String> stringMessage = new GenericMessage<String>("Hello Redis");
		store.addMessage(stringMessage);
		Message<String> retrievedMessage = (Message<String>) store.getMessage(stringMessage.getHeaders().getId());
		assertNotNull(retrievedMessage);
		assertEquals("Hello Redis", retrievedMessage.getPayload());
	}
	@SuppressWarnings("unchecked")
	@Test
	@RedisAvailable
	public void testAddAndRemoveStringMessage(){	
		JedisConnectionFactory jcf = this.getConnectionFactoryForTest();
		RedisMessageStore store = new RedisMessageStore(jcf);
		Message<String> stringMessage = new GenericMessage<String>("Hello Redis");
		store.addMessage(stringMessage);
		Message<String> retrievedMessage = (Message<String>) store.removeMessage(stringMessage.getHeaders().getId());
		assertNotNull(retrievedMessage);
		assertEquals("Hello Redis", retrievedMessage.getPayload());
		assertNull(store.getMessage(stringMessage.getHeaders().getId()));
	}
	
	@SuppressWarnings("serial")
	public static class Person implements Serializable{
		private Address address;
		public Address getAddress() {
			return address;
		}
		public void setAddress(Address address) {
			this.address = address;
		}
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		private String name;
		public Person(Address address, String name){
			this.address = address;
			this.name = name;
		}
	}
	@SuppressWarnings("serial")
	public static class Address implements Serializable{
		private String address;

		public String getAddress() {
			return address;
		}

		public void setAddress(String address) {
			this.address = address;
		}
	}
	
	public static class Foo{
		
	}
}
