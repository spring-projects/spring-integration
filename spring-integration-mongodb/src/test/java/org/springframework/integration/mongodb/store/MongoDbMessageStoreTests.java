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

package org.springframework.integration.mongodb.store;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Properties;
import java.util.UUID;

import org.junit.Test;

import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.core.SimpleMongoDbFactory;
import org.springframework.messaging.Message;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.history.MessageHistory;
import org.springframework.integration.mongodb.rules.MongoDbAvailable;
import org.springframework.integration.mongodb.rules.MongoDbAvailableTests;
import org.springframework.integration.support.MessageBuilder;

import com.mongodb.Mongo;

/**
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 */
public class MongoDbMessageStoreTests extends MongoDbAvailableTests{

	@Test
	@MongoDbAvailable
	public void addGetWithStringPayload() throws Exception {
		MongoDbFactory mongoDbFactory = new SimpleMongoDbFactory(new Mongo(), "test");
		MongoDbMessageStore store = new MongoDbMessageStore(mongoDbFactory);
		Message<?> messageToStore = MessageBuilder.withPayload("Hello").build();
		store.addMessage(messageToStore);
		Message<?> retrievedMessage = store.getMessage(messageToStore.getHeaders().getId());
		assertNotNull(retrievedMessage);
		assertEquals(messageToStore.getPayload(), retrievedMessage.getPayload());
		assertEquals(messageToStore.getHeaders(), retrievedMessage.getHeaders());
		assertEquals(messageToStore, retrievedMessage);
	}
	@Test
	@MongoDbAvailable
	public void addThenRemoveWithStringPayload() throws Exception {
		MongoDbFactory mongoDbFactory = new SimpleMongoDbFactory(new Mongo(), "test");
		MongoDbMessageStore store = new MongoDbMessageStore(mongoDbFactory);
		Message<?> messageToStore = MessageBuilder.withPayload("Hello").build();
		store.addMessage(messageToStore);
		Message<?> retrievedMessage = store.getMessage(messageToStore.getHeaders().getId());
		assertNotNull(retrievedMessage);
		store.removeMessage(retrievedMessage.getHeaders().getId());
		retrievedMessage = store.getMessage(messageToStore.getHeaders().getId());
		assertNull(retrievedMessage);
	}

	@Test
	@MongoDbAvailable
	public void addGetWithObjectDefaultConstructorPayload() throws Exception {
		MongoDbFactory mongoDbFactory = new SimpleMongoDbFactory(new Mongo(), "test");
		MongoDbMessageStore store = new MongoDbMessageStore(mongoDbFactory);
		Person p = new Person();
		p.setFname("John");
		p.setLname("Doe");
		Message<?> messageToStore = MessageBuilder.withPayload(p).build();
		store.addMessage(messageToStore);
		Message<?> retrievedMessage = store.getMessage(messageToStore.getHeaders().getId());
		assertNotNull(retrievedMessage);
		assertEquals(messageToStore.getPayload(), retrievedMessage.getPayload());
		assertEquals(messageToStore.getHeaders(), retrievedMessage.getHeaders());
		assertEquals(messageToStore, retrievedMessage);
	}

	@Test
	@MongoDbAvailable
	public void testWithMessageHistory() throws Exception{
		MongoDbFactory mongoDbFactory = this.prepareMongoFactory();
		MongoDbMessageStore store = new MongoDbMessageStore(mongoDbFactory);

		Foo foo = new Foo();
		foo.setName("foo");
		Message<?> message = MessageBuilder.withPayload(foo).
				setHeader("foo", foo).
				setHeader("bar", new Bar("bar")).
				setHeader("baz", new Baz()).
				setHeader("abc", new Abc()).
				setHeader("xyz", new Xyz()).
				build();
		DirectChannel fooChannel = new DirectChannel();
		fooChannel.setBeanName("fooChannel");
		DirectChannel barChannel = new DirectChannel();
		barChannel.setBeanName("barChannel");

		message = MessageHistory.write(message, fooChannel);
		message = MessageHistory.write(message, barChannel);
		store.addMessage(message);
		message = store.getMessage(message.getHeaders().getId());
		assertTrue(message.getHeaders().get("foo") instanceof Foo);
		assertTrue(message.getHeaders().get("bar") instanceof Bar);
		assertTrue(message.getHeaders().get("baz") instanceof Baz);
		assertTrue(message.getHeaders().get("abc") instanceof Abc);
		assertTrue(message.getHeaders().get("xyz") instanceof Xyz);
		MessageHistory messageHistory = MessageHistory.read(message);
		assertNotNull(messageHistory);
		assertEquals(2, messageHistory.size());
		Properties fooChannelHistory = messageHistory.get(0);
		assertEquals("fooChannel", fooChannelHistory.get("name"));
		assertEquals("channel", fooChannelHistory.get("type"));
	}

	@Test
	@MongoDbAvailable
	public void testInt3153SequenceDetails() throws Exception{
		MongoDbFactory mongoDbFactory = this.prepareMongoFactory();
		MongoDbMessageStore store = new MongoDbMessageStore(mongoDbFactory);
		Message<?> messageToStore = MessageBuilder.withPayload("test")
				.pushSequenceDetails(UUID.randomUUID(), 1, 1)
				.pushSequenceDetails(UUID.randomUUID(), 1, 1)
				.build();
		store.addMessage(messageToStore);
		Message<?> retrievedMessage = store.getMessage(messageToStore.getHeaders().getId());
		assertNotNull(retrievedMessage);
		assertEquals(messageToStore.getPayload(), retrievedMessage.getPayload());
		assertEquals(messageToStore.getHeaders(), retrievedMessage.getHeaders());
		assertEquals(messageToStore, retrievedMessage);
	}

	public static class Foo{
		private String name;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	public static class Bar{
		private String name;

		public Bar(String name){
			this.name = name;
		}

		public String getName() {
			return name;
		}
	}

	public static class Baz{
		private String name = "baz";

		public String getName() {
			return name;
		}
	}

	public static class Abc{
		private String name = "abx";

		private Abc(){}

		public String getName() {
			return name;
		}
	}

	public static class Xyz{
		@SuppressWarnings("unused")
		private String name = "xyz";

		private Xyz(){}
	}


	public static class Person {

		private String fname;

		private String lname;

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

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((fname == null) ? 0 : fname.hashCode());
			result = prime * result + ((lname == null) ? 0 : lname.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			Person other = (Person) obj;
			if (fname == null) {
				if (other.fname != null) {
					return false;
				}
			}
			else if (!fname.equals(other.fname)) {
				return false;
			}
			if (lname == null) {
				if (other.lname != null) {
					return false;
				}
			}
			else if (!lname.equals(other.lname)) {
				return false;
			}
			return true;
		}
	}

}
