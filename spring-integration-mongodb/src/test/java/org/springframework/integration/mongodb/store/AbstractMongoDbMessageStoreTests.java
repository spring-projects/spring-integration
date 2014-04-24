/*
 * Copyright 2002-2014 the original author or authors.
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

import static org.junit.Assert.*;

import java.io.Serializable;
import java.util.Properties;
import java.util.UUID;

import com.mongodb.MongoClient;
import org.hamcrest.Matchers;
import org.junit.Test;

import org.springframework.data.mongodb.core.SimpleMongoDbFactory;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.history.MessageHistory;
import org.springframework.integration.message.AdviceMessage;
import org.springframework.integration.mongodb.rules.MongoDbAvailable;
import org.springframework.integration.mongodb.rules.MongoDbAvailableTests;
import org.springframework.integration.store.MessageStore;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.support.MutableMessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.messaging.support.GenericMessage;

/**
 *
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 * @author Amol Nayak
 *
 */
public abstract class AbstractMongoDbMessageStoreTests extends MongoDbAvailableTests {


	@Test
	@MongoDbAvailable
	public void testAddGetWithStringPayload() throws Exception {
		cleanupCollections(new SimpleMongoDbFactory(new MongoClient(), "test"));
		MessageStore store = getMessageStore();
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
	public void testAddThenRemoveWithStringPayload() throws Exception {
		cleanupCollections(new SimpleMongoDbFactory(new MongoClient(), "test"));
		MessageStore store = getMessageStore();
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
	public void testAddGetWithObjectDefaultConstructorPayload() throws Exception {
		cleanupCollections(new SimpleMongoDbFactory(new MongoClient(), "test"));
		MessageStore store = getMessageStore();
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
		cleanupCollections(new SimpleMongoDbFactory(new MongoClient(), "test"));
		MessageStore store = getMessageStore();
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
		assertNotNull(message);
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
		cleanupCollections(new SimpleMongoDbFactory(new MongoClient(), "test"));
		MessageStore store = getMessageStore();
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

	@Test
	@MongoDbAvailable
	public void testInt3076MessageAsPayload() throws Exception{
		MessageStore store = this.getMessageStore();
		Person p = new Person();
		p.setFname("John");
		p.setLname("Doe");
		Message<?> messageToStore = new GenericMessage<Message<?>>(MessageBuilder.withPayload(p).build());
		store.addMessage(messageToStore);
		Message<?> retrievedMessage = store.getMessage(messageToStore.getHeaders().getId());
		assertNotNull(retrievedMessage);
		assertTrue(retrievedMessage.getPayload() instanceof GenericMessage);
		assertEquals(messageToStore.getPayload(), retrievedMessage.getPayload());
		assertEquals(messageToStore.getHeaders(), retrievedMessage.getHeaders());
		assertEquals(((Message<?>) messageToStore.getPayload()).getPayload(), p);
		assertEquals(messageToStore, retrievedMessage);
	}

	@Test
	@MongoDbAvailable
	public void testInt3076AdviceMessage() throws Exception{
		MessageStore store = this.getMessageStore();
		Person p = new Person();
		p.setFname("John");
		p.setLname("Doe");
		Message<Person> inputMessage = MessageBuilder.withPayload(p).build();
		Message<?> messageToStore = new AdviceMessage("foo", inputMessage);
		store.addMessage(messageToStore);
		Message<?> retrievedMessage = store.getMessage(messageToStore.getHeaders().getId());
		assertNotNull(retrievedMessage);
		assertTrue(retrievedMessage instanceof AdviceMessage);
		assertEquals(messageToStore.getPayload(), retrievedMessage.getPayload());
		assertEquals(messageToStore.getHeaders(), retrievedMessage.getHeaders());
		assertEquals(inputMessage, ((AdviceMessage) retrievedMessage).getInputMessage());
		assertEquals(messageToStore, retrievedMessage);
	}

	@Test
	@MongoDbAvailable
	public void testAdviceMessageAsPayload() throws Exception{
		MessageStore store = this.getMessageStore();
		Person p = new Person();
		p.setFname("John");
		p.setLname("Doe");
		Message<Person> inputMessage = MessageBuilder.withPayload(p).build();
		Message<?> messageToStore = new GenericMessage<Message<?>>(new AdviceMessage("foo", inputMessage));
		store.addMessage(messageToStore);
		Message<?> retrievedMessage = store.getMessage(messageToStore.getHeaders().getId());
		assertNotNull(retrievedMessage);
		assertTrue(retrievedMessage.getPayload() instanceof AdviceMessage);
		AdviceMessage adviceMessage = (AdviceMessage) retrievedMessage.getPayload();
		assertEquals("foo", adviceMessage.getPayload());
		assertEquals(messageToStore.getHeaders(), retrievedMessage.getHeaders());
		assertEquals(inputMessage, adviceMessage.getInputMessage());
		assertEquals(messageToStore, retrievedMessage);
	}

	@Test
	@MongoDbAvailable
	public void testMutableMessageAsPayload() throws Exception{
		MessageStore store = this.getMessageStore();
		Person p = new Person();
		p.setFname("John");
		p.setLname("Doe");
		Message<?> messageToStore = new GenericMessage<Message<?>>(MutableMessageBuilder.withPayload(p).build());
		store.addMessage(messageToStore);
		Message<?> retrievedMessage = store.getMessage(messageToStore.getHeaders().getId());
		assertNotNull(retrievedMessage);
		assertEquals("org.springframework.integration.support.MutableMessage", retrievedMessage.getPayload().getClass().getName());
		assertEquals(messageToStore.getPayload(), retrievedMessage.getPayload());
		assertEquals(messageToStore.getHeaders(), retrievedMessage.getHeaders());
		assertEquals(((Message<?>) messageToStore.getPayload()).getPayload(), p);
		assertEquals(messageToStore, retrievedMessage);
	}

	@Test
	@MongoDbAvailable
	public void testInt3076ErrorMessage() throws Exception{
		MessageStore store = this.getMessageStore();
		Person p = new Person();
		p.setFname("John");
		p.setLname("Doe");
		Message<Person> failedMessage = MessageBuilder.withPayload(p).build();
		MessagingException messagingException;
		try {
			throw new RuntimeException("intentional");
		}
		catch (Exception e) {
			messagingException = new MessagingException(failedMessage, "intentional MessagingException", e);
		}
		Message<?> messageToStore = new ErrorMessage(messagingException);
		store.addMessage(messageToStore);
		Message<?> retrievedMessage = store.getMessage(messageToStore.getHeaders().getId());
		assertNotNull(retrievedMessage);
		assertTrue(retrievedMessage instanceof ErrorMessage);
		assertThat(retrievedMessage.getPayload(), Matchers.instanceOf(MessagingException.class));
		assertEquals("intentional MessagingException", ((MessagingException) retrievedMessage.getPayload()).getMessage());
		assertEquals(failedMessage, ((MessagingException) retrievedMessage.getPayload()).getFailedMessage());
		assertEquals(messageToStore.getHeaders(), retrievedMessage.getHeaders());
	}


	public static class Foo implements Serializable {
		/**
		 *
		 */
		private static final long serialVersionUID = 1L;
		private String name;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	public static class Bar implements Serializable{
		/**
		 *
		 */
		private static final long serialVersionUID = 1L;
		private final String name;

		public Bar(String name){
			this.name = name;
		}

		public String getName() {
			return name;
		}
	}

	public static class Baz implements Serializable {
		/**
		 *
		 */
		private static final long serialVersionUID = 1L;
		private final String name = "baz";

		public String getName() {
			return name;
		}
	}

	public static class Abc implements Serializable {
		/**
		 *
		 */
		private static final long serialVersionUID = 1L;

		private final String name = "abx";

		private Abc(){}

		public String getName() {
			return name;
		}
	}

	public static class Xyz implements Serializable {
		/**
		 *
		 */
		private static final long serialVersionUID = 1L;

		@SuppressWarnings("unused")
		private final String name = "xyz";

		private Xyz(){}
	}


	public static class Person implements Serializable{

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;

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

	protected abstract MessageStore getMessageStore() throws Exception;
}
