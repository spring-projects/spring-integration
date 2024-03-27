/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.integration.mongodb.store;

import java.io.Serializable;
import java.util.Properties;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.context.support.GenericApplicationContext;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.history.MessageHistory;
import org.springframework.integration.message.AdviceMessage;
import org.springframework.integration.mongodb.MongoDbContainerTest;
import org.springframework.integration.store.MessageStore;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.support.MutableMessage;
import org.springframework.integration.support.MutableMessageBuilder;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.messaging.support.GenericMessage;

import static org.assertj.core.api.Assertions.assertThat;

/**
 *
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 * @author Amol Nayak
 * @author Artem Vozhdayenko
 *
 */
public abstract class AbstractMongoDbMessageStoreTests implements MongoDbContainerTest {

	static MongoDatabaseFactory MONGO_DATABASE_FACTORY;

	@BeforeAll
	static void prepareMongoConnection() {
		MONGO_DATABASE_FACTORY = MongoDbContainerTest.createMongoDbFactory();
	}

	protected final GenericApplicationContext testApplicationContext = TestUtils.createTestApplicationContext();

	@BeforeEach
	public void setup() {
		this.testApplicationContext.refresh();
	}

	@AfterEach
	public void tearDown() {
		this.testApplicationContext.close();
		MongoDbContainerTest.cleanupCollections(MONGO_DATABASE_FACTORY);
	}

	@Test
	void testAddGetWithStringPayload() {
		MessageStore store = getMessageStore();
		Message<?> messageToStore = MessageBuilder.withPayload("Hello").build();
		store.addMessage(messageToStore);
		Message<?> retrievedMessage = store.getMessage(messageToStore.getHeaders().getId());
		assertThat(retrievedMessage).isNotNull();
		assertThat(retrievedMessage.getPayload()).isEqualTo(messageToStore.getPayload());
		assertThat(retrievedMessage.getHeaders()).isEqualTo(messageToStore.getHeaders());
		assertThat(retrievedMessage).isEqualTo(messageToStore);
	}

	@Test
	void testAddThenRemoveWithStringPayload() {
		MessageStore store = getMessageStore();
		Message<?> messageToStore = MessageBuilder.withPayload("Hello").build();
		store.addMessage(messageToStore);
		Message<?> retrievedMessage = store.getMessage(messageToStore.getHeaders().getId());
		assertThat(retrievedMessage).isNotNull();
		store.removeMessage(retrievedMessage.getHeaders().getId());
		retrievedMessage = store.getMessage(messageToStore.getHeaders().getId());
		assertThat(retrievedMessage).isNull();
	}

	@Test
	void testAddGetWithObjectDefaultConstructorPayload() {
		MessageStore store = getMessageStore();
		Person p = new Person();
		p.setFname("John");
		p.setLname("Doe");
		Message<?> messageToStore = MessageBuilder.withPayload(p).build();
		store.addMessage(messageToStore);
		Message<?> retrievedMessage = store.getMessage(messageToStore.getHeaders().getId());
		assertThat(retrievedMessage).isNotNull();
		assertThat(retrievedMessage.getPayload()).isEqualTo(messageToStore.getPayload());
		assertThat(retrievedMessage.getHeaders()).isEqualTo(messageToStore.getHeaders());
		assertThat(retrievedMessage).isEqualTo(messageToStore);
	}

	@Test
	void testWithMessageHistory() {
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
		assertThat(message).isNotNull();
		assertThat(message.getHeaders().get("foo")).isInstanceOf(Foo.class);
		assertThat(message.getHeaders().get("bar")).isInstanceOf(Bar.class);
		assertThat(message.getHeaders().get("baz")).isInstanceOf(Baz.class);
		assertThat(message.getHeaders().get("abc")).isInstanceOf(Abc.class);
		assertThat(message.getHeaders().get("xyz")).isInstanceOf(Xyz.class);
		MessageHistory messageHistory = MessageHistory.read(message);
		assertThat(messageHistory).isNotNull();
		assertThat(messageHistory).hasSize(2);
		Properties fooChannelHistory = messageHistory.get(0);
		assertThat(fooChannelHistory)
				.containsEntry("name", "fooChannel")
				.containsEntry("type", "channel");
	}

	@Test
	void testInt3153SequenceDetails() {
		MessageStore store = getMessageStore();
		Message<?> messageToStore = MessageBuilder.withPayload("test")
				.pushSequenceDetails(UUID.randomUUID(), 1, 1)
				.pushSequenceDetails(UUID.randomUUID(), 1, 1)
				.build();
		store.addMessage(messageToStore);
		Message<?> retrievedMessage = store.getMessage(messageToStore.getHeaders().getId());
		assertThat(retrievedMessage).isNotNull();
		assertThat(retrievedMessage.getPayload()).isEqualTo(messageToStore.getPayload());
		assertThat(retrievedMessage.getHeaders()).isEqualTo(messageToStore.getHeaders());
		assertThat(retrievedMessage).isEqualTo(messageToStore);
	}

	@Test
	void testInt3076MessageAsPayload() {
		MessageStore store = getMessageStore();
		Person p = new Person();
		p.setFname("John");
		p.setLname("Doe");
		Message<?> messageToStore = new GenericMessage<Message<?>>(MessageBuilder.withPayload(p).build());
		store.addMessage(messageToStore);
		Message<?> retrievedMessage = store.getMessage(messageToStore.getHeaders().getId());
		assertThat(retrievedMessage).isNotNull();
		assertThat(retrievedMessage.getPayload()).isInstanceOf(GenericMessage.class);
		assertThat(retrievedMessage.getPayload()).isEqualTo(messageToStore.getPayload());
		assertThat(retrievedMessage.getHeaders()).isEqualTo(messageToStore.getHeaders());
		assertThat(p).isEqualTo(((Message<?>) messageToStore.getPayload()).getPayload());
		assertThat(retrievedMessage).isEqualTo(messageToStore);
	}

	@Test
	void testInt3076AdviceMessage() {
		MessageStore store = getMessageStore();
		Person p = new Person();
		p.setFname("John");
		p.setLname("Doe");
		Message<Person> inputMessage = MessageBuilder.withPayload(p).build();
		Message<?> messageToStore = new AdviceMessage<>("foo", inputMessage);
		store.addMessage(messageToStore);
		Message<?> retrievedMessage = store.getMessage(messageToStore.getHeaders().getId());
		assertThat(retrievedMessage)
				.isNotNull()
				.isInstanceOf(AdviceMessage.class);
		assertThat(retrievedMessage.getPayload()).isEqualTo(messageToStore.getPayload());
		assertThat(retrievedMessage.getHeaders()).isEqualTo(messageToStore.getHeaders());
		assertThat(((AdviceMessage<?>) retrievedMessage).getInputMessage()).isEqualTo(inputMessage);
		assertThat(retrievedMessage).isEqualTo(messageToStore);
	}

	@Test
	void testAdviceMessageAsPayload() {
		MessageStore store = getMessageStore();
		Person p = new Person();
		p.setFname("John");
		p.setLname("Doe");
		Message<Person> inputMessage = MessageBuilder.withPayload(p).build();
		Message<?> messageToStore = new GenericMessage<Message<?>>(new AdviceMessage<>("foo", inputMessage));
		store.addMessage(messageToStore);
		Message<?> retrievedMessage = store.getMessage(messageToStore.getHeaders().getId());
		assertThat(retrievedMessage).isNotNull();
		assertThat(retrievedMessage.getPayload()).isInstanceOf(AdviceMessage.class);
		AdviceMessage<?> adviceMessage = (AdviceMessage<?>) retrievedMessage.getPayload();
		assertThat(adviceMessage.getPayload()).isEqualTo("foo");
		assertThat(retrievedMessage.getHeaders()).isEqualTo(messageToStore.getHeaders());
		assertThat(adviceMessage.getInputMessage()).isEqualTo(inputMessage);
		assertThat(retrievedMessage).isEqualTo(messageToStore);
	}

	@Test
	void testMutableMessageAsPayload() {
		MessageStore store = getMessageStore();
		Person p = new Person();
		p.setFname("John");
		p.setLname("Doe");
		Message<?> messageToStore = new GenericMessage<Message<?>>(MutableMessageBuilder.withPayload(p).build());
		store.addMessage(messageToStore);
		Message<?> retrievedMessage = store.getMessage(messageToStore.getHeaders().getId());
		assertThat(retrievedMessage).isNotNull();
		assertThat(retrievedMessage.getPayload()).isInstanceOf(MutableMessage.class);
		assertThat(retrievedMessage.getPayload()).isEqualTo(messageToStore.getPayload());
		assertThat(retrievedMessage.getHeaders()).isEqualTo(messageToStore.getHeaders());
		assertThat(p).isEqualTo(((Message<?>) messageToStore.getPayload()).getPayload());
		assertThat(retrievedMessage).isEqualTo(messageToStore);
	}

	@Test
	void testInt3076ErrorMessage() {
		MessageStore store = getMessageStore();
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
		assertThat(retrievedMessage)
				.isNotNull()
				.isInstanceOf(ErrorMessage.class);
		assertThat(retrievedMessage.getPayload()).isInstanceOf(MessagingException.class);
		assertThat(((MessagingException) retrievedMessage.getPayload()).getMessage())
				.contains("intentional MessagingException");
		assertThat(((MessagingException) retrievedMessage.getPayload()).getFailedMessage()).isEqualTo(failedMessage);
		assertThat(retrievedMessage.getHeaders()).isEqualTo(messageToStore.getHeaders());
	}

	@Test
	void testAddAndUpdateAlreadySaved() {
		MessageStore messageStore = getMessageStore();
		Message<String> message = MessageBuilder.withPayload("foo").build();
		message = messageStore.addMessage(message);
		Message<String> result = messageStore.addMessage(message);
		assertThat(result).isEqualTo(message);
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

	public static class Bar implements Serializable {

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;

		private final String name;

		public Bar(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}

	}

	public static class Baz implements Serializable {

		private static final long serialVersionUID = 1L;

		private final String name;

		Baz() {
			this("baz");
		}

		@PersistenceCreator
		Baz(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}

	}

	public static class Abc implements Serializable {

		private static final long serialVersionUID = 1L;

		private final String name;

		Abc() {
			this("abx");
		}

		@PersistenceCreator
		Abc(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}

	}

	public static class Xyz implements Serializable {

		private static final long serialVersionUID = 1L;

		@SuppressWarnings("unused")
		private final String name;

		Xyz() {
			this("xyz");
		}

		@PersistenceCreator
		Xyz(String name) {
			this.name = name;
		}

	}

	public static class Person implements Serializable {

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
				return other.lname == null;
			}
			else {
				return lname.equals(other.lname);
			}
		}

	}

	protected abstract MessageStore getMessageStore();

}
