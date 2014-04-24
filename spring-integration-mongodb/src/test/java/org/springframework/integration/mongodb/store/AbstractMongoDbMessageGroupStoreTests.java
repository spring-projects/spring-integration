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

import static org.junit.Assert.*;

import java.util.Collection;
import java.util.Iterator;
import java.util.Properties;
import java.util.UUID;

import com.mongodb.MongoClient;
import org.junit.Test;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.data.mongodb.core.SimpleMongoDbFactory;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.history.MessageHistory;
import org.springframework.integration.mongodb.rules.MongoDbAvailable;
import org.springframework.integration.mongodb.rules.MongoDbAvailableTests;
import org.springframework.integration.store.MessageGroup;
import org.springframework.integration.store.MessageGroupStore;
import org.springframework.integration.store.MessageStore;
import org.springframework.integration.store.SimpleMessageGroup;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.GenericMessage;

/**
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Amol Nayak
 *
 */
public abstract class AbstractMongoDbMessageGroupStoreTests extends MongoDbAvailableTests {

	@Test
	@MongoDbAvailable
	public void testNonExistingEmptyMessageGroup() throws Exception{
		this.cleanupCollections(new SimpleMongoDbFactory(new MongoClient(), "test"));
		MessageGroupStore store = getMessageGroupStore();
		MessageGroup messageGroup = store.getMessageGroup(1);
		assertNotNull(messageGroup);
		assertTrue(messageGroup instanceof SimpleMessageGroup);
		assertEquals(0, messageGroup.size());
	}

	@Test
	@MongoDbAvailable
	public void testMessageGroupWithAddedMessagePrimitiveGroupId() throws Exception{
		this.cleanupCollections(new SimpleMongoDbFactory(new MongoClient(), "test"));
		MessageGroupStore store = this.getMessageGroupStore();
		MessageStore messageStore = this.getMessageStore();
		MessageGroup messageGroup = store.getMessageGroup(1);
		Message<?> messageA = new GenericMessage<String>("A");
		Message<?> messageB = new GenericMessage<String>("B");
		store.addMessageToGroup(1, messageA);
		messageGroup = store.addMessageToGroup(1, messageB);
		assertNotNull(messageGroup);
		assertEquals(2, messageGroup.size());
		Message<?> retrievedMessage = messageStore.getMessage(messageA.getHeaders().getId());
		assertNotNull(retrievedMessage);
		assertEquals(retrievedMessage.getHeaders().getId(), messageA.getHeaders().getId());
		// ensure that 'message_group' header that is only used internally is not propagated
		assertNull(retrievedMessage.getHeaders().get("message_group"));
	}

	@Test
	@MongoDbAvailable
	public void testMessageGroupWithAddedMessageUUIDGroupIdAndUUIDHeader() throws Exception{
		this.cleanupCollections(new SimpleMongoDbFactory(new MongoClient(), "test"));
		MessageGroupStore store = this.getMessageGroupStore();
		MessageStore messageStore = this.getMessageStore();
	    Object id = UUID.randomUUID();
		MessageGroup messageGroup = store.getMessageGroup(id);
		UUID uuidA = UUID.randomUUID();
		Message<?> messageA = MessageBuilder.withPayload("A").setHeader("foo", uuidA).build();
		UUID uuidB = UUID.randomUUID();
		Message<?> messageB = MessageBuilder.withPayload("B").setHeader("foo", uuidB).build();
		store.addMessageToGroup(id, messageA);
		messageGroup = store.addMessageToGroup(id, messageB);
		assertNotNull(messageGroup);
		assertEquals(2, messageGroup.size());
		Message<?> retrievedMessage = messageStore.getMessage(messageA.getHeaders().getId());
		assertNotNull(retrievedMessage);
		assertEquals(retrievedMessage.getHeaders().getId(), messageA.getHeaders().getId());
		// ensure that 'message_group' header that is only used internally is not propagated
		assertNull(retrievedMessage.getHeaders().get("message_group"));
		Object fooHeader = retrievedMessage.getHeaders().get("foo");
		assertTrue(fooHeader instanceof UUID);
		assertEquals(uuidA, fooHeader);
	}

	@Test
	@MongoDbAvailable
	public void testCountMessagesInGroup() throws Exception{
		this.cleanupCollections(new SimpleMongoDbFactory(new MongoClient(), "test"));
		MessageGroupStore store = this.getMessageGroupStore();
		Message<?> messageA = new GenericMessage<String>("A");
		Message<?> messageB = new GenericMessage<String>("B");
		store.addMessageToGroup(1, messageA);
		store.addMessageToGroup(1, messageB);
		assertEquals(2, store.messageGroupSize(1));
	}

	@Test
	@MongoDbAvailable
	public void testPollMessages() throws Exception{
		this.cleanupCollections(new SimpleMongoDbFactory(new MongoClient(), "test"));
		MessageGroupStore store = this.getMessageGroupStore();
		Message<?> messageA = new GenericMessage<String>("A");
		Message<?> messageB = new GenericMessage<String>("B");
		store.addMessageToGroup(1, messageA);
		Thread.sleep(10);
		store.addMessageToGroup(1, messageB);
		assertEquals(2, store.messageGroupSize(1));
		Message<?> out = store.pollMessageFromGroup(1);
		assertNotNull(out);
		assertEquals("A", out.getPayload());
		assertEquals(1, store.messageGroupSize(1));
		out = store.pollMessageFromGroup(1);
		assertEquals("B", out.getPayload());
		assertEquals(0, store.messageGroupSize(1));
	}

	@Test
	@MongoDbAvailable
	public void testSameMessageMultipleGroupsPoll() throws Exception{
		this.cleanupCollections(new SimpleMongoDbFactory(new MongoClient(), "test"));
		MessageGroupStore store = this.getMessageGroupStore();
		Message<?> messageA = new GenericMessage<String>("A");
		store.addMessageToGroup(1, messageA);
		store.addMessageToGroup(2, messageA);
		store.addMessageToGroup(3, messageA);
		store.addMessageToGroup(4, messageA);
		assertEquals(1, store.messageGroupSize(1));
		assertEquals(1, store.messageGroupSize(2));
		assertEquals(1, store.messageGroupSize(3));
		assertEquals(1, store.messageGroupSize(4));
		store.pollMessageFromGroup(3);
		assertEquals(1, store.messageGroupSize(1));
		assertEquals(1, store.messageGroupSize(2));
		assertEquals(0, store.messageGroupSize(3));
		assertEquals(1, store.messageGroupSize(4));
		store.pollMessageFromGroup(4);
		assertEquals(1, store.messageGroupSize(1));
		assertEquals(1, store.messageGroupSize(2));
		assertEquals(0, store.messageGroupSize(3));
		assertEquals(0, store.messageGroupSize(4));
		store.pollMessageFromGroup(2);
		assertEquals(1, store.messageGroupSize(1));
		assertEquals(0, store.messageGroupSize(2));
		assertEquals(0, store.messageGroupSize(3));
		assertEquals(0, store.messageGroupSize(4));
		store.pollMessageFromGroup(1);
		assertEquals(0, store.messageGroupSize(1));
		assertEquals(0, store.messageGroupSize(2));
		assertEquals(0, store.messageGroupSize(3));
		assertEquals(0, store.messageGroupSize(4));
	}

	@Test
	@MongoDbAvailable
	public void testSameMessageMultipleGroupsRemove() throws Exception{
		this.cleanupCollections(new SimpleMongoDbFactory(new MongoClient(), "test"));
		MessageGroupStore store = this.getMessageGroupStore();

		Message<?> messageA = new GenericMessage<String>("A");
		store.addMessageToGroup(1, messageA);
		store.addMessageToGroup(2, messageA);
		store.addMessageToGroup(3, messageA);
		store.addMessageToGroup(4, messageA);
		assertEquals(1, store.messageGroupSize(1));
		assertEquals(1, store.messageGroupSize(2));
		assertEquals(1, store.messageGroupSize(3));
		assertEquals(1, store.messageGroupSize(4));
		store.removeMessageFromGroup(3, messageA);
		assertEquals(1, store.messageGroupSize(1));
		assertEquals(1, store.messageGroupSize(2));
		assertEquals(0, store.messageGroupSize(3));
		assertEquals(1, store.messageGroupSize(4));
		store.removeMessageFromGroup(4, messageA);
		assertEquals(1, store.messageGroupSize(1));
		assertEquals(1, store.messageGroupSize(2));
		assertEquals(0, store.messageGroupSize(3));
		assertEquals(0, store.messageGroupSize(4));
		store.removeMessageFromGroup(2, messageA);
		assertEquals(1, store.messageGroupSize(1));
		assertEquals(0, store.messageGroupSize(2));
		assertEquals(0, store.messageGroupSize(3));
		assertEquals(0, store.messageGroupSize(4));
		store.removeMessageFromGroup(1, messageA);
		assertEquals(0, store.messageGroupSize(1));
		assertEquals(0, store.messageGroupSize(2));
		assertEquals(0, store.messageGroupSize(3));
		assertEquals(0, store.messageGroupSize(4));
	}

	@Test
	@MongoDbAvailable
	public void testMessageGroupUpdatedDateChangesWithEachAddedMessage() throws Exception{
		this.cleanupCollections(new SimpleMongoDbFactory(new MongoClient(), "test"));
		MessageGroupStore store = this.getMessageGroupStore();

		MessageGroup messageGroup = store.getMessageGroup(1);
		Message<?> message = new GenericMessage<String>("Hello");
		messageGroup = store.addMessageToGroup(1, message);
		assertNotNull(messageGroup);
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
		store = this.getMessageGroupStore();

		messageGroup = store.getMessageGroup(1);
		assertEquals(2, messageGroup.size());
	}

	@Test
	@MongoDbAvailable
	public void testMessageGroupMarkingMessage() throws Exception{
		this.cleanupCollections(new SimpleMongoDbFactory(new MongoClient(), "test"));
		MessageGroupStore store = this.getMessageGroupStore();

		MessageGroup messageGroup = store.getMessageGroup(1);
		Message<?> messageA = new GenericMessage<String>("A");
		Message<?> messageB = new GenericMessage<String>("B");
		store.addMessageToGroup(1, messageA);
		messageGroup = store.addMessageToGroup(1, messageB);
		assertNotNull(messageGroup);
		assertEquals(2, messageGroup.size());

		messageGroup = store.removeMessageFromGroup(1, messageA);
		assertEquals(1, messageGroup.size());

		// validate that the updates were propagated to Mongo as well
		store = this.getMessageGroupStore();

		messageGroup = store.getMessageGroup(1);
		assertEquals(1, messageGroup.size());
	}

	@Test
	@MongoDbAvailable
	public void testRemoveMessageGroup() throws Exception{
		this.cleanupCollections(new SimpleMongoDbFactory(new MongoClient(), "test"));
		MessageGroupStore store = this.getMessageGroupStore();
		MessageStore messageStore = this.getMessageStore();

		MessageGroup messageGroup = store.getMessageGroup(1);
		Message<?> message = new GenericMessage<String>("Hello");
		UUID id = message.getHeaders().getId();
		messageGroup = store.addMessageToGroup(1, message);
		assertNotNull(messageGroup);
		assertEquals(1, messageGroup.size());
		message = messageStore.getMessage(id);
		assertNotNull(message);

		store.removeMessageGroup(1);
		MessageGroup messageGroupA = store.getMessageGroup(1);
		assertEquals(0, messageGroupA.size());
		assertFalse(messageGroupA.equals(messageGroup));

	}

	@Test
	@MongoDbAvailable
	public void testCompleteMessageGroup() throws Exception{
		this.cleanupCollections(new SimpleMongoDbFactory(new MongoClient(), "test"));
		MessageGroupStore store = this.getMessageGroupStore();

		MessageGroup messageGroup = store.getMessageGroup(1);
		assertNotNull(messageGroup);
		Message<?> message = new GenericMessage<String>("Hello");
		store.addMessageToGroup(messageGroup.getGroupId(), message);
		store.completeGroup(messageGroup.getGroupId());
		messageGroup = store.getMessageGroup(1);
		assertTrue(messageGroup.isComplete());
	}

	@Test
	@MongoDbAvailable
	public void testLastReleasedSequenceNumber() throws Exception{
		this.cleanupCollections(new SimpleMongoDbFactory(new MongoClient(), "test"));
		MessageGroupStore store = this.getMessageGroupStore();

		MessageGroup messageGroup = store.getMessageGroup(1);
		assertNotNull(messageGroup);
		Message<?> message = new GenericMessage<String>("Hello");
		store.addMessageToGroup(messageGroup.getGroupId(), message);
		store.setLastReleasedSequenceNumberForGroup(messageGroup.getGroupId(), 5);
		messageGroup = store.getMessageGroup(1);
		assertEquals(5, messageGroup.getLastReleasedMessageSequenceNumber());
	}

	@Test
	@MongoDbAvailable
	public void testRemoveMessageFromTheGroup() throws Exception{
		this.cleanupCollections(new SimpleMongoDbFactory(new MongoClient(), "test"));
		MessageGroupStore store = this.getMessageGroupStore();

		MessageGroup messageGroup = store.getMessageGroup(1);
		Message<?> message = new GenericMessage<String>("2");
		store.addMessageToGroup(1, new GenericMessage<String>("1"));
		store.addMessageToGroup(1, message);
		messageGroup = store.addMessageToGroup(1, new GenericMessage<String>("3"));
		assertNotNull(messageGroup);
		assertEquals(3, messageGroup.size());

		messageGroup = store.removeMessageFromGroup(1, message);
		assertEquals(2, messageGroup.size());
	}

	@Test
	@MongoDbAvailable
	public void testMultipleMessageStores() throws Exception{

		this.cleanupCollections(new SimpleMongoDbFactory(new MongoClient(), "test"));
		MessageGroupStore store1 = this.getMessageGroupStore();
		MessageGroupStore store2 = this.getMessageGroupStore();

		Message<?> message = new GenericMessage<String>("1");
		store1.addMessageToGroup(1, message);
		store2.addMessageToGroup(1, new GenericMessage<String>("2"));
		store1.addMessageToGroup(1, new GenericMessage<String>("3"));

		MessageGroupStore store3 = this.getMessageGroupStore();

		MessageGroup messageGroup = store3.getMessageGroup(1);

		assertNotNull(messageGroup);
		assertEquals(3, messageGroup.size());

		store3.removeMessageFromGroup(1, message);

		messageGroup = store2.getMessageGroup(1);
		assertEquals(2, messageGroup.size());
	}

	@Test
	@MongoDbAvailable
	public void testMessageGroupIterator() throws Exception{
		this.cleanupCollections(new SimpleMongoDbFactory(new MongoClient(), "test"));
		MessageGroupStore store1 = this.getMessageGroupStore();
		MessageGroupStore store2 = this.getMessageGroupStore();

		Message<?> message = new GenericMessage<String>("1");
		store2.addMessageToGroup(1, message);
		store1.addMessageToGroup(2, new GenericMessage<String>("2"));
		store2.addMessageToGroup(3, new GenericMessage<String>("3"));

		MessageGroupStore store3 = this.getMessageGroupStore();
		Iterator<MessageGroup> iterator = store3.iterator();
		assertNotNull(iterator);
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


	protected void testWithAggregatorWithShutdown(String config) throws Exception{
		this.cleanupCollections(new SimpleMongoDbFactory(new MongoClient(), "test"));

		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(config, this.getClass());
		context.refresh();

		MessageChannel input = context.getBean("inputChannel", MessageChannel.class);
		QueueChannel output = context.getBean("outputChannel", QueueChannel.class);

		Message<?> m1 = MessageBuilder.withPayload("1").setSequenceNumber(1).setSequenceSize(3).setCorrelationId(1).build();
		Message<?> m2 = MessageBuilder.withPayload("2").setSequenceNumber(2).setSequenceSize(3).setCorrelationId(1).build();
		input.send(m1);
		assertNull(output.receive(1000));
		input.send(m2);
		assertNull(output.receive(1000));
		context.close();

		context = new ClassPathXmlApplicationContext(config, this.getClass());
		input = context.getBean("inputChannel", MessageChannel.class);
		output = context.getBean("outputChannel", QueueChannel.class);

		Message<?> m3 = MessageBuilder.withPayload("3").setSequenceNumber(3).setSequenceSize(3).setCorrelationId(1).build();
		input.send(m3);
		assertNotNull(output.receive(2000));
	}

	@Test
	@MongoDbAvailable
	public void testWithMessageHistory() throws Exception{
		this.cleanupCollections(new SimpleMongoDbFactory(new MongoClient(), "test"));
		MessageGroupStore store = this.getMessageGroupStore();

		store.getMessageGroup(1);

		Message<?> message = new GenericMessage<String>("Hello");
		DirectChannel fooChannel = new DirectChannel();
		fooChannel.setBeanName("fooChannel");
		DirectChannel barChannel = new DirectChannel();
		barChannel.setBeanName("barChannel");

		message = MessageHistory.write(message, fooChannel);
		message = MessageHistory.write(message, barChannel);
		store.addMessageToGroup(1, message);
		MessageGroup group = store.getMessageGroup(1);
		assertNotNull(group);
		Collection<Message<?>> messages = group.getMessages();
		assertTrue(!messages.isEmpty());
		message = messages.iterator().next();

		MessageHistory messageHistory = MessageHistory.read(message);
		assertNotNull(messageHistory);
		assertEquals(2, messageHistory.size());
		Properties fooChannelHistory = messageHistory.get(0);
		assertEquals("fooChannel", fooChannelHistory.get("name"));
		assertEquals("channel", fooChannelHistory.get("type"));
	}

	protected abstract MessageGroupStore getMessageGroupStore() throws Exception;

	protected abstract MessageStore getMessageStore() throws Exception;

}
