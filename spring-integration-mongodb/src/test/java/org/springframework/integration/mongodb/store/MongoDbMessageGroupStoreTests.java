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
package org.springframework.integration.mongodb.store;

import java.util.UUID;

import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.mongodb.rules.MongoDbAvailable;
import org.springframework.integration.mongodb.rules.MongoDbAvailableTests;
import org.springframework.integration.store.MessageGroup;
import org.springframework.integration.store.SimpleMessageGroup;
import org.springframework.integration.support.MessageBuilder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Oleg Zhurakousky
 *
 */
public class MongoDbMessageGroupStoreTests extends MongoDbAvailableTests {

	@Test
	@MongoDbAvailable
	public void testNonExistingEmptyMessageGroup() throws Exception{	
		MongoDbFactory mongoDbFactory = this.prepareMongoFactory();
		MongoDbMessageStore store = new MongoDbMessageStore(mongoDbFactory);
		store.afterPropertiesSet();
		
		MessageGroup messageGroup = store.getMessageGroup(1);
		assertNotNull(messageGroup);
		assertTrue(messageGroup instanceof SimpleMessageGroup);
		assertEquals(0, messageGroup.size());
		MessageGroup messageGroupA = store.getMessageGroup(1);
		assertEquals(messageGroup, messageGroupA);
	}
	
	@Test
	@MongoDbAvailable
	public void testMessageGroupWithAddedMessage() throws Exception{	
		MongoDbFactory mongoDbFactory = this.prepareMongoFactory();
		MongoDbMessageStore store = new MongoDbMessageStore(mongoDbFactory);
		store.afterPropertiesSet();

		MessageGroup messageGroup = store.getMessageGroup(1);
		Message<?> messageA = new GenericMessage<String>("A");
		Message<?> messageB = new GenericMessage<String>("B");
		store.addMessageToGroup(1, messageA);
		store.addMessageToGroup(1, messageB);
		assertEquals(2, messageGroup.size());
		Message<?> retrievedMessage = store.getMessage(messageA.getHeaders().getId());
		assertNotNull(retrievedMessage);
		assertEquals(retrievedMessage.getHeaders().getId(), messageA.getHeaders().getId());
		// ensure that 'message_group' header that is only used internally is not propagated 
		assertNull(retrievedMessage.getHeaders().get("message_group"));
	}
	
	@Test
	@MongoDbAvailable
	public void testMessageGroupMarkingMessage() throws Exception{	
		MongoDbFactory mongoDbFactory = this.prepareMongoFactory();
		MongoDbMessageStore store = new MongoDbMessageStore(mongoDbFactory);
		store.afterPropertiesSet();

		MessageGroup messageGroup = store.getMessageGroup(1);
		Message<?> messageA = new GenericMessage<String>("A");
		Message<?> messageB = new GenericMessage<String>("B");
		store.addMessageToGroup(1, messageA);
		store.addMessageToGroup(1, messageB);
		assertEquals(0, messageGroup.getMarked().size());
		assertEquals(2, messageGroup.getUnmarked().size());
		
		store.markMessageFromGroup(1, messageA);
		assertEquals(1, messageGroup.getMarked().size());
		assertEquals(1, messageGroup.getUnmarked().size());
		
		// validate that the updates were propagated to Mongo as well
		store = new MongoDbMessageStore(mongoDbFactory);
		store.afterPropertiesSet();
		messageGroup = store.getMessageGroup(1);
		assertEquals(1, messageGroup.getMarked().size());
		assertEquals(1, messageGroup.getUnmarked().size());
	}
	
	@Test
	@MongoDbAvailable
	public void testRemoveMessageGroup() throws Exception{	
		MongoDbFactory mongoDbFactory = this.prepareMongoFactory();
		MongoDbMessageStore store = new MongoDbMessageStore(mongoDbFactory);
		store.afterPropertiesSet();

		MessageGroup messageGroup = store.getMessageGroup(1);
		Message<?> message = new GenericMessage<String>("Hello");
		UUID id = message.getHeaders().getId();
		store.addMessageToGroup(1, message);
		assertEquals(1, messageGroup.size());
		message = store.getMessage(id);
		assertNotNull(message);
		
		store.removeMessageGroup(1);
		MessageGroup messageGroupA = store.getMessageGroup(1);
		assertEquals(0, messageGroupA.size());
		assertFalse(messageGroupA.equals(messageGroup));
		
	}
	
	@Test
	@MongoDbAvailable
	public void testRemoveMessageFromTheGroup() throws Exception{	
		MongoDbFactory mongoDbFactory = this.prepareMongoFactory();
		MongoDbMessageStore store = new MongoDbMessageStore(mongoDbFactory);
		store.afterPropertiesSet();

		MessageGroup messageGroup = store.getMessageGroup(1);
		Message<?> message = new GenericMessage<String>("2");
		store.addMessageToGroup(1, new GenericMessage<String>("1"));
		store.addMessageToGroup(1, message);
		store.addMessageToGroup(1, new GenericMessage<String>("3"));
	
		assertEquals(3, messageGroup.size());
		
		store.removeMessageFromGroup(1, message);
		assertEquals(2, messageGroup.size());
	}
	
	@Test
	@MongoDbAvailable
	public void testMarkAllMessagesInMessageGroup() throws Exception {	
		MongoDbFactory mongoDbFactory = this.prepareMongoFactory();
		MongoDbMessageStore store = new MongoDbMessageStore(mongoDbFactory);
		store.afterPropertiesSet();
		
		MessageGroup messageGroup = store.getMessageGroup(1);
		
		store.addMessageToGroup(1, new GenericMessage<String>("1"));
		store.addMessageToGroup(1, new GenericMessage<String>("2"));
		store.addMessageToGroup(1, new GenericMessage<String>("3"));
		
		assertEquals(3, messageGroup.getUnmarked().size());
		assertEquals(0, messageGroup.getMarked().size());
		
		store.markMessageGroup(messageGroup);

		assertEquals(0, messageGroup.getUnmarked().size());
		assertEquals(3, messageGroup.getMarked().size());
		
		store = new MongoDbMessageStore(mongoDbFactory);
		store.afterPropertiesSet();
		messageGroup = store.getMessageGroup(1);
		assertEquals(0, messageGroup.getUnmarked().size());
		assertEquals(3, messageGroup.getMarked().size());
	}
	
	@Test
	@MongoDbAvailable
	public void testGetOneFromMessageGroup() throws Exception{	
		MongoDbFactory mongoDbFactory = this.prepareMongoFactory();
		MongoDbMessageStore store = new MongoDbMessageStore(mongoDbFactory);
		store.afterPropertiesSet();

		store.addMessageToGroup(1, new GenericMessage<String>("1"));
		store.addMessageToGroup(1, new GenericMessage<String>("1"));
		store.addMessageToGroup(1, new GenericMessage<String>("1"));
		
		MessageGroup messageGroup = store.getMessageGroup(1);
		
		Message<?> message =  messageGroup.getOne(); 
		assertEquals("1", message.getPayload());
	}
	
	@Test
	@MongoDbAvailable
	public void testWithAggregatorWithShutdown() throws Exception{	
		this.prepareMongoFactory(); // for this test it only ensures that DB was flushed before test
		
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("mongo-aggregator-config.xml", this.getClass());
		MessageChannel input = context.getBean("inputChannel", MessageChannel.class);
		QueueChannel output = context.getBean("outputChannel", QueueChannel.class);
		
		Message<?> m1 = MessageBuilder.withPayload("1").setSequenceNumber(1).setSequenceSize(3).setCorrelationId(1).build();
		Message<?> m2 = MessageBuilder.withPayload("2").setSequenceNumber(2).setSequenceSize(3).setCorrelationId(1).build();
		input.send(m1);
		assertNull(output.receive(1000));
		input.send(m2);
		assertNull(output.receive(1000));
		context.close();
		
		context = new ClassPathXmlApplicationContext("mongo-aggregator-config.xml", this.getClass());
		input = context.getBean("inputChannel", MessageChannel.class);
		output = context.getBean("outputChannel", QueueChannel.class);
		
		Message<?> m3 = MessageBuilder.withPayload("3").setSequenceNumber(3).setSequenceSize(3).setCorrelationId(1).build();
		input.send(m3);
		assertNotNull(output.receive(2000));
	}
	
}
