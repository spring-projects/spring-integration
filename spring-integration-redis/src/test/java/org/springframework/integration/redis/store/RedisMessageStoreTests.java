/*
 * Copyright 2007-2013 the original author or authors
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;

import java.io.Serializable;
import java.util.Properties;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.history.MessageHistory;
import org.springframework.integration.redis.rules.RedisAvailable;
import org.springframework.integration.redis.rules.RedisAvailableTests;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;

/**
 * @author Oleg Zhurakousky
 *
 */
public class RedisMessageStoreTests extends RedisAvailableTests {

	@Before
	@After
	public void setUpTearDown() {
		StringRedisTemplate template = this.createStringRedisTemplate(this.getConnectionFactoryForTest());
		template.delete(template.keys("MESSAGE_*"));
	}

	@Test
	@RedisAvailable
	public void testGetNonExistingMessage(){
		RedisConnectionFactory jcf = this.getConnectionFactoryForTest();
		RedisMessageStore store = new RedisMessageStore(jcf);
		Message<?> message = store.getMessage(UUID.randomUUID());
		assertNull(message);
	}

	@Test
	@RedisAvailable
	public void testGetMessageCountWhenEmpty(){
		RedisConnectionFactory jcf = this.getConnectionFactoryForTest();
		RedisMessageStore store = new RedisMessageStore(jcf);
		assertEquals(0, store.getMessageCount());
	}

	@Test
	@RedisAvailable
	public void testAddStringMessage(){
		RedisConnectionFactory jcf = this.getConnectionFactoryForTest();
		RedisMessageStore store = new RedisMessageStore(jcf);
		Message<String> stringMessage = new GenericMessage<String>("Hello Redis");
		Message<String> storedMessage =  store.addMessage(stringMessage);
		assertNotSame(stringMessage, storedMessage);
		assertEquals("Hello Redis", storedMessage.getPayload());
	}

	@Test
	@RedisAvailable
	public void testAddSerializableObjectMessage(){
		RedisConnectionFactory jcf = this.getConnectionFactoryForTest();
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
		RedisConnectionFactory jcf = this.getConnectionFactoryForTest();
		RedisMessageStore store = new RedisMessageStore(jcf);

		Message<Foo> objectMessage = new GenericMessage<Foo>(new Foo());
		store.addMessage(objectMessage);
	}

	@SuppressWarnings("unchecked")
	@Test
	@RedisAvailable
	public void testAddAndGetStringMessage(){
		RedisConnectionFactory jcf = this.getConnectionFactoryForTest();
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
		RedisConnectionFactory jcf = this.getConnectionFactoryForTest();
		RedisMessageStore store = new RedisMessageStore(jcf);
		Message<String> stringMessage = new GenericMessage<String>("Hello Redis");
		store.addMessage(stringMessage);
		Message<String> retrievedMessage = (Message<String>) store.removeMessage(stringMessage.getHeaders().getId());
		assertNotNull(retrievedMessage);
		assertEquals("Hello Redis", retrievedMessage.getPayload());
		assertNull(store.getMessage(stringMessage.getHeaders().getId()));
	}

	@Test
	@RedisAvailable
	public void testWithMessageHistory() throws Exception{
		RedisConnectionFactory jcf = this.getConnectionFactoryForTest();
		RedisMessageStore store = new RedisMessageStore(jcf);

		Message<?> message = new GenericMessage<String>("Hello");
		DirectChannel fooChannel = new DirectChannel();
		fooChannel.setBeanName("fooChannel");
		DirectChannel barChannel = new DirectChannel();
		barChannel.setBeanName("barChannel");

		message = MessageHistory.write(message, fooChannel);
		message = MessageHistory.write(message, barChannel);
		store.addMessage(message);
		message = store.getMessage(message.getHeaders().getId());
		MessageHistory messageHistory = MessageHistory.read(message);
		assertNotNull(messageHistory);
		assertEquals(2, messageHistory.size());
		Properties fooChannelHistory = messageHistory.get(0);
		assertEquals("fooChannel", fooChannelHistory.get("name"));
		assertEquals("channel", fooChannelHistory.get("type"));
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
