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
package org.springframework.integration.mongodb.store;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.hamcrest.Matchers;
import org.junit.Test;

import org.springframework.context.support.GenericApplicationContext;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.core.SimpleMongoDbFactory;
import org.springframework.integration.Message;
import org.springframework.integration.MessagingException;
import org.springframework.integration.message.AdviceMessage;
import org.springframework.integration.message.ErrorMessage;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.mongodb.rules.MongoDbAvailable;
import org.springframework.integration.store.MessageStore;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;

import com.mongodb.Mongo;

/**
 * @author Amol Nayak
 * @author Artem Bilan
 */
public class ConfigurableMongoDbMessageStoreTests extends AbstractMongoDbMessageStoreTests {

	@Override
	protected MessageStore getMessageStore() throws Exception {
		MongoDbFactory mongoDbFactory = new SimpleMongoDbFactory(new Mongo(), "test");
		ConfigurableMongoDbMessageStore mongoDbMessageStore = new ConfigurableMongoDbMessageStore(mongoDbFactory);
		GenericApplicationContext testApplicationContext = TestUtils.createTestApplicationContext();
		testApplicationContext.refresh();
		mongoDbMessageStore.setApplicationContext(testApplicationContext);
		mongoDbMessageStore.afterPropertiesSet();
		return mongoDbMessageStore;
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

}
