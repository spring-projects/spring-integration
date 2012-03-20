/*
 * Copyright 2007-2012 the original author or authors
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

import java.util.Iterator;
import java.util.Properties;
import java.util.UUID;

import org.junit.Test;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.history.MessageHistory;
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
		
		MessageGroup messageGroup = store.getMessageGroup(1);
		assertNotNull(messageGroup);
		assertTrue(messageGroup instanceof SimpleMessageGroup);
		assertEquals(0, messageGroup.size());
	}
	
	@Test
	@MongoDbAvailable
	public void testMessageGroupWithAddedMessagePrimitiveGroupId() throws Exception{	
		MongoDbFactory mongoDbFactory = this.prepareMongoFactory();
		MongoDbMessageStore store = new MongoDbMessageStore(mongoDbFactory);
	
		MessageGroup messageGroup = store.getMessageGroup(1);
		Message<?> messageA = new GenericMessage<String>("A");
		Message<?> messageB = new GenericMessage<String>("B");
		store.addMessageToGroup(1, messageA);
		messageGroup = store.addMessageToGroup(1, messageB);
		assertEquals(2, messageGroup.size());
		Message<?> retrievedMessage = store.getMessage(messageA.getHeaders().getId());
		assertNotNull(retrievedMessage);
		assertEquals(retrievedMessage.getHeaders().getId(), messageA.getHeaders().getId());
		// ensure that 'message_group' header that is only used internally is not propagated 
		assertNull(retrievedMessage.getHeaders().get("message_group"));
	}
	
	@Test
	@MongoDbAvailable
	public void testMessageGroupWithAddedMessageUUIDGroupIdAndUUIDHeader() throws Exception{	
		MongoDbFactory mongoDbFactory = this.prepareMongoFactory();
		MongoDbMessageStore store = new MongoDbMessageStore(mongoDbFactory);
	    Object id = UUID.randomUUID();
		MessageGroup messageGroup = store.getMessageGroup(id);
		Message<?> messageA = MessageBuilder.withPayload("A").setHeader("foo", UUID.randomUUID()).build();
		Message<?> messageB = MessageBuilder.withPayload("B").setHeader("foo", UUID.randomUUID()).build();
		store.addMessageToGroup(id, messageA);
		messageGroup = store.addMessageToGroup(id, messageB);
		assertEquals(2, messageGroup.size());
		Message<?> retrievedMessage = store.getMessage(messageA.getHeaders().getId());
		assertNotNull(retrievedMessage);
		assertEquals(retrievedMessage.getHeaders().getId(), messageA.getHeaders().getId());
		// ensure that 'message_group' header that is only used internally is not propagated 
		assertNull(retrievedMessage.getHeaders().get("message_group"));
		assertTrue(retrievedMessage.getHeaders().get("foo") instanceof UUID);
	}
	
	@Test
	@MongoDbAvailable
	public void testCountMessagesInGroup() throws Exception{	
		MongoDbFactory mongoDbFactory = this.prepareMongoFactory();
		MongoDbMessageStore store = new MongoDbMessageStore(mongoDbFactory);
		
		Message<?> messageA = new GenericMessage<String>("A");
		Message<?> messageB = new GenericMessage<String>("B");
		store.addMessageToGroup(1, messageA);
		store.addMessageToGroup(1, messageB);
		assertEquals(2, store.messageGroupSize(1));
	}
	
	@Test
	@MongoDbAvailable
	public void testMessageGroupUpdatedDateChangesWithEachAddedMessage() throws Exception{	
		MongoDbFactory mongoDbFactory = this.prepareMongoFactory();
		MongoDbMessageStore store = new MongoDbMessageStore(mongoDbFactory);

		MessageGroup messageGroup = store.getMessageGroup(1);
		Message<?> message = new GenericMessage<String>("Hello");
		messageGroup = store.addMessageToGroup(1, message);
		assertEquals(1, messageGroup.size());
		long createdTimestamp = messageGroup.getTimestamp();
		long updatedTimestamp = messageGroup.getLastModified();
		assertEquals(createdTimestamp, updatedTimestamp);
		Thread.sleep(1000);
		message = new GenericMessage<String>("Hello again");
		messageGroup = store.addMessageToGroup(1, message);
		createdTimestamp = messageGroup.getTimestamp();
		updatedTimestamp = messageGroup.getLastModified();
		assertTrue(updatedTimestamp > createdTimestamp);
		assertEquals(2, messageGroup.size());
		
		// make sure the store is properly rebuild from MongoDB
		store = new MongoDbMessageStore(mongoDbFactory);

		messageGroup = store.getMessageGroup(1);
		assertEquals(2, messageGroup.size());
	}
	
	@Test
	@MongoDbAvailable
	public void testMessageGroupMarkingMessage() throws Exception{	
		MongoDbFactory mongoDbFactory = this.prepareMongoFactory();
		MongoDbMessageStore store = new MongoDbMessageStore(mongoDbFactory);
		
		MessageGroup messageGroup = store.getMessageGroup(1);
		Message<?> messageA = new GenericMessage<String>("A");
		Message<?> messageB = new GenericMessage<String>("B");
		store.addMessageToGroup(1, messageA);
		messageGroup = store.addMessageToGroup(1, messageB);
		assertEquals(2, messageGroup.size());
		
		messageGroup = store.removeMessageFromGroup(1, messageA);
		assertEquals(1, messageGroup.size());
		
		// validate that the updates were propagated to Mongo as well
		store = new MongoDbMessageStore(mongoDbFactory);
		
		messageGroup = store.getMessageGroup(1);
		assertEquals(1, messageGroup.size());
	}
	
	@Test
	@MongoDbAvailable
	public void testRemoveMessageGroup() throws Exception{	
		MongoDbFactory mongoDbFactory = this.prepareMongoFactory();
		MongoDbMessageStore store = new MongoDbMessageStore(mongoDbFactory);
		
		MessageGroup messageGroup = store.getMessageGroup(1);
		Message<?> message = new GenericMessage<String>("Hello");
		UUID id = message.getHeaders().getId();
		messageGroup = store.addMessageToGroup(1, message);
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
	public void testCompleteMessageGroup() throws Exception{	
		MongoDbFactory mongoDbFactory = this.prepareMongoFactory();
		MongoDbMessageStore store = new MongoDbMessageStore(mongoDbFactory);
		
		MessageGroup messageGroup = store.getMessageGroup(1);
		Message<?> message = new GenericMessage<String>("Hello");
		store.addMessageToGroup(messageGroup.getGroupId(), message);
		store.completeGroup(messageGroup.getGroupId());
		messageGroup = store.getMessageGroup(1);
		assertTrue(messageGroup.isComplete());
	}
	
	@Test
	@MongoDbAvailable
	public void testLastReleasedSequenceNumber() throws Exception{	
		MongoDbFactory mongoDbFactory = this.prepareMongoFactory();
		MongoDbMessageStore store = new MongoDbMessageStore(mongoDbFactory);
		
		MessageGroup messageGroup = store.getMessageGroup(1);
		Message<?> message = new GenericMessage<String>("Hello");
		store.addMessageToGroup(messageGroup.getGroupId(), message);
		store.setLastReleasedSequenceNumberForGroup(messageGroup.getGroupId(), 5);
		messageGroup = store.getMessageGroup(1);
		assertEquals(5, messageGroup.getLastReleasedMessageSequenceNumber());
	}
	
	@Test
	@MongoDbAvailable
	public void testRemoveMessageFromTheGroup() throws Exception{	
		MongoDbFactory mongoDbFactory = this.prepareMongoFactory();
		MongoDbMessageStore store = new MongoDbMessageStore(mongoDbFactory);
		
		MessageGroup messageGroup = store.getMessageGroup(1);
		Message<?> message = new GenericMessage<String>("2");
		store.addMessageToGroup(1, new GenericMessage<String>("1"));
		store.addMessageToGroup(1, message);
		messageGroup = store.addMessageToGroup(1, new GenericMessage<String>("3"));
	
		assertEquals(3, messageGroup.size());
		
		messageGroup = store.removeMessageFromGroup(1, message);
		assertEquals(2, messageGroup.size());
	}
	
	@Test
	@MongoDbAvailable
	public void testMultipleMessageStores() throws Exception{	
		MongoDbFactory mongoDbFactory = this.prepareMongoFactory();
		MongoDbMessageStore store1 = new MongoDbMessageStore(mongoDbFactory);
		MongoDbMessageStore store2 = new MongoDbMessageStore(mongoDbFactory);

		Message<?> message = new GenericMessage<String>("1");
		store1.addMessageToGroup(1, message);
		store2.addMessageToGroup(1, new GenericMessage<String>("2"));
		store1.addMessageToGroup(1, new GenericMessage<String>("3"));
		
		MongoDbMessageStore store3 = new MongoDbMessageStore(mongoDbFactory);
		
		MessageGroup messageGroup = store3.getMessageGroup(1);
		
		assertEquals(3, messageGroup.size());
		
		store3.removeMessageFromGroup(1, message);
		
		messageGroup = store2.getMessageGroup(1);
		assertEquals(2, messageGroup.size());
	}
	
	@Test
	@MongoDbAvailable
	public void testMessageGroupIterator() throws Exception{	
		MongoDbFactory mongoDbFactory = this.prepareMongoFactory();
		MongoDbMessageStore store1 = new MongoDbMessageStore(mongoDbFactory);
		MongoDbMessageStore store2 = new MongoDbMessageStore(mongoDbFactory);
		
		Message<?> message = new GenericMessage<String>("1");
		store2.addMessageToGroup(1, message);
		store1.addMessageToGroup(2, new GenericMessage<String>("2"));
		store2.addMessageToGroup(3, new GenericMessage<String>("3"));
		
		MongoDbMessageStore store3 = new MongoDbMessageStore(mongoDbFactory);
		
		Iterator<MessageGroup> iterator = store3.iterator();
		int counter = 0;
		while (iterator.hasNext()) {
			iterator.next();
			counter++;
		}
		assertEquals(3, counter);
		
		store2.removeMessageFromGroup(1, message);
		
		iterator = store3.iterator();
		counter = 0;
		while (iterator.hasNext()) {
			iterator.next();
			counter++;
		}
		assertEquals(2, counter);
	}
	
//	@Test
//	@MongoDbAvailable
//	public void testConcurrentModifications() throws Exception{	
//		MongoDbFactory mongoDbFactory = this.prepareMongoFactory();
//		final MongoDbMessageStore store1 = new MongoDbMessageStore(mongoDbFactory);
//		final MongoDbMessageStore store2 = new MongoDbMessageStore(mongoDbFactory);
//		
//		final Message<?> message = new GenericMessage<String>("1"); 
//
//		ExecutorService executor = null;
//		
//		final List<Object> failures = new ArrayList<Object>();
//		
//		for (int i = 0; i < 100; i++) {
//			executor = Executors.newCachedThreadPool();
//			
//			executor.execute(new Runnable() {	
//				public void run() {			
//					MessageGroup group = store1.addMessageToGroup(1, message);
//					if (group.getUnmarked().size() != 1){
//						failures.add("ADD");
//						throw new AssertionFailedError("Failed on ADD");
//					}	
//				}
//			});
//			executor.execute(new Runnable() {	
//				public void run() {
//					MessageGroup group = store2.removeMessageFromGroup(1, message);
//					if (group.getUnmarked().size() != 0){
//						failures.add("REMOVE");
//						throw new AssertionFailedError("Failed on Remove");
//					}	
//				}
//			});
//			
//			executor.shutdown();
//			executor.awaitTermination(10, TimeUnit.SECONDS);
//			store2.removeMessageFromGroup(1, message); // ensures that if ADD thread executed after REMOVE, the store is empty for the next cycle
//		}
//		assertTrue(failures.size() == 0);
//	}
		
	
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
	
	@Test
	@MongoDbAvailable
	public void testWithMessageHistory() throws Exception{	
		MongoDbFactory mongoDbFactory = this.prepareMongoFactory();
		MongoDbMessageStore store = new MongoDbMessageStore(mongoDbFactory);
		
		store.getMessageGroup(1);
		
		Message<?> message = new GenericMessage<String>("Hello");
		DirectChannel fooChannel = new DirectChannel();
		fooChannel.setBeanName("fooChannel");
		DirectChannel barChannel = new DirectChannel();
		barChannel.setBeanName("barChannel");
		
		message = MessageHistory.write(message, fooChannel);
		message = MessageHistory.write(message, barChannel);
		store.addMessageToGroup(1, message);
		
		message = store.getMessageGroup(1).getMessages().iterator().next();
		
		MessageHistory messageHistory = MessageHistory.read(message);
		assertNotNull(messageHistory);
		assertEquals(2, messageHistory.size());
		Properties fooChannelHistory = messageHistory.get(0);
		assertEquals("fooChannel", fooChannelHistory.get("name"));
		assertEquals("channel", fooChannelHistory.get("type"));
	}
	
}
