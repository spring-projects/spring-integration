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

package org.springframework.integration.mongodb.store;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.function.BiFunction;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.integration.aggregator.ReleaseStrategy;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.history.MessageHistory;
import org.springframework.integration.mongodb.rules.MongoDbAvailable;
import org.springframework.integration.mongodb.rules.MongoDbAvailableTests;
import org.springframework.integration.store.AbstractBatchingMessageGroupStore;
import org.springframework.integration.store.MessageGroup;
import org.springframework.integration.store.MessageGroupStore;
import org.springframework.integration.store.MessageStore;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.GenericMessage;

/**
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Amol Nayak
 * @author Artem Bilan
 */
public abstract class AbstractMongoDbMessageGroupStoreTests extends MongoDbAvailableTests {

	public static final BiFunction<Message<?>, String, String> CONDITION_SUPPLIER = (m, c) -> "10";

	public static final ReleaseStrategy RELEASE_STRATEGY =
			group -> group.size() == Integer.parseInt(group.getCondition());

	protected final GenericApplicationContext testApplicationContext = TestUtils.createTestApplicationContext();

	@Before
	public void setup() {
		this.testApplicationContext.refresh();
	}

	@After
	public void tearDown() {
		this.testApplicationContext.close();
		cleanupCollections(MONGO_DATABASE_FACTORY);
	}

	@Test
	@MongoDbAvailable
	public void testNonExistingEmptyMessageGroup() {
		MessageGroupStore store = getMessageGroupStore();
		store.addMessagesToGroup(1, new GenericMessage<Object>("foo"));
		MessageGroup messageGroup = store.getMessageGroup(1);
		assertThat(messageGroup).isNotNull();
		assertThat(messageGroup.getClass().getName()).contains("PersistentMessageGroup");
		assertThat(messageGroup.size()).isEqualTo(1);
	}

	@Test
	@MongoDbAvailable
	public void testMessageGroupWithAddedMessagePrimitiveGroupId() {
		MessageGroupStore store = getMessageGroupStore();
		MessageStore messageStore = getMessageStore();
		MessageGroup messageGroup = store.getMessageGroup(1);
		Message<?> messageA = new GenericMessage<>("A");
		Message<?> messageB = new GenericMessage<>("B");
		store.addMessagesToGroup(1, messageA);
		messageGroup = store.addMessageToGroup(1, messageB);
		assertThat(messageGroup).isNotNull();
		assertThat(messageGroup.size()).isEqualTo(2);
		Message<?> retrievedMessage = messageStore.getMessage(messageA.getHeaders().getId());
		assertThat(retrievedMessage).isNotNull();
		assertThat(messageA.getHeaders().getId()).isEqualTo(retrievedMessage.getHeaders().getId());
		// ensure that 'message_group' header that is only used internally is not propagated
		assertThat(retrievedMessage.getHeaders().get("message_group")).isNull();
		assertThat(store.getMessageGroupCount()).isEqualTo(1);
	}

	@Test
	@MongoDbAvailable
	public void testMessageGroupWithAddedMessageUUIDGroupIdAndUUIDHeader() {
		MessageGroupStore store = getMessageGroupStore();
		MessageStore messageStore = getMessageStore();
		Object id = UUID.randomUUID();
		MessageGroup messageGroup = store.getMessageGroup(id);
		UUID uuidA = UUID.randomUUID();
		Message<?> messageA = MessageBuilder.withPayload("A").setHeader("foo", uuidA).build();
		UUID uuidB = UUID.randomUUID();
		Message<?> messageB = MessageBuilder.withPayload("B").setHeader("foo", uuidB).build();
		store.addMessagesToGroup(id, messageA);
		messageGroup = store.addMessageToGroup(id, messageB);
		assertThat(messageGroup).isNotNull();
		assertThat(messageGroup.size()).isEqualTo(2);
		Message<?> retrievedMessage = messageStore.getMessage(messageA.getHeaders().getId());
		assertThat(retrievedMessage).isNotNull();
		assertThat(messageA.getHeaders().getId()).isEqualTo(retrievedMessage.getHeaders().getId());
		// ensure that 'message_group' header that is only used internally is not propagated
		assertThat(retrievedMessage.getHeaders().get("message_group")).isNull();
		Object fooHeader = retrievedMessage.getHeaders().get("foo");
		assertThat(fooHeader instanceof UUID).isTrue();
		assertThat(fooHeader).isEqualTo(uuidA);
	}

	@Test
	@MongoDbAvailable
	public void testCountMessagesInGroup() {
		MessageGroupStore store = getMessageGroupStore();
		Message<?> messageA = new GenericMessage<>("A");
		Message<?> messageB = new GenericMessage<>("B");
		store.addMessagesToGroup(1, messageA, messageB);
		assertThat(store.messageGroupSize(1)).isEqualTo(2);
	}

	@Test
	@MongoDbAvailable
	public void testPollMessages() throws InterruptedException {
		MessageGroupStore store = getMessageGroupStore();
		Message<?> messageA = new GenericMessage<>("A");
		Message<?> messageB = new GenericMessage<>("B");
		store.addMessagesToGroup(1, messageA);
		Thread.sleep(10);
		store.addMessagesToGroup(1, messageB);
		assertThat(store.messageGroupSize(1)).isEqualTo(2);
		Message<?> out = store.pollMessageFromGroup(1);
		assertThat(out).isNotNull();
		assertThat(out.getPayload()).isEqualTo("A");
		assertThat(store.messageGroupSize(1)).isEqualTo(1);
		out = store.pollMessageFromGroup(1);
		assertThat(out.getPayload()).isEqualTo("B");
		assertThat(store.messageGroupSize(1)).isEqualTo(0);
	}

	@Test
	@MongoDbAvailable
	public void testSameMessageMultipleGroupsPoll() {
		MessageGroupStore store = getMessageGroupStore();
		Message<?> messageA = new GenericMessage<>("A");
		store.addMessagesToGroup(1, messageA);
		store.addMessagesToGroup(2, messageA);
		store.addMessagesToGroup(3, messageA);
		store.addMessagesToGroup(4, messageA);
		assertThat(store.messageGroupSize(1)).isEqualTo(1);
		assertThat(store.messageGroupSize(2)).isEqualTo(1);
		assertThat(store.messageGroupSize(3)).isEqualTo(1);
		assertThat(store.messageGroupSize(4)).isEqualTo(1);
		store.pollMessageFromGroup(3);
		assertThat(store.messageGroupSize(1)).isEqualTo(1);
		assertThat(store.messageGroupSize(2)).isEqualTo(1);
		assertThat(store.messageGroupSize(3)).isEqualTo(0);
		assertThat(store.messageGroupSize(4)).isEqualTo(1);
		store.pollMessageFromGroup(4);
		assertThat(store.messageGroupSize(1)).isEqualTo(1);
		assertThat(store.messageGroupSize(2)).isEqualTo(1);
		assertThat(store.messageGroupSize(3)).isEqualTo(0);
		assertThat(store.messageGroupSize(4)).isEqualTo(0);
		store.pollMessageFromGroup(2);
		assertThat(store.messageGroupSize(1)).isEqualTo(1);
		assertThat(store.messageGroupSize(2)).isEqualTo(0);
		assertThat(store.messageGroupSize(3)).isEqualTo(0);
		assertThat(store.messageGroupSize(4)).isEqualTo(0);
		store.pollMessageFromGroup(1);
		assertThat(store.messageGroupSize(1)).isEqualTo(0);
		assertThat(store.messageGroupSize(2)).isEqualTo(0);
		assertThat(store.messageGroupSize(3)).isEqualTo(0);
		assertThat(store.messageGroupSize(4)).isEqualTo(0);
	}

	@Test
	@MongoDbAvailable
	public void testSameMessageMultipleGroupsRemove() {
		MessageGroupStore store = getMessageGroupStore();
		Message<?> messageA = new GenericMessage<>("A");
		store.addMessagesToGroup(1, messageA);
		store.addMessagesToGroup(2, messageA);
		store.addMessagesToGroup(3, messageA);
		store.addMessagesToGroup(4, messageA);
		assertThat(store.messageGroupSize(1)).isEqualTo(1);
		assertThat(store.messageGroupSize(2)).isEqualTo(1);
		assertThat(store.messageGroupSize(3)).isEqualTo(1);
		assertThat(store.messageGroupSize(4)).isEqualTo(1);
		store.removeMessagesFromGroup(3, messageA);
		assertThat(store.messageGroupSize(1)).isEqualTo(1);
		assertThat(store.messageGroupSize(2)).isEqualTo(1);
		assertThat(store.messageGroupSize(3)).isEqualTo(0);
		assertThat(store.messageGroupSize(4)).isEqualTo(1);
		store.removeMessagesFromGroup(4, messageA);
		assertThat(store.messageGroupSize(1)).isEqualTo(1);
		assertThat(store.messageGroupSize(2)).isEqualTo(1);
		assertThat(store.messageGroupSize(3)).isEqualTo(0);
		assertThat(store.messageGroupSize(4)).isEqualTo(0);
		store.removeMessagesFromGroup(2, messageA);
		assertThat(store.messageGroupSize(1)).isEqualTo(1);
		assertThat(store.messageGroupSize(2)).isEqualTo(0);
		assertThat(store.messageGroupSize(3)).isEqualTo(0);
		assertThat(store.messageGroupSize(4)).isEqualTo(0);
		store.removeMessagesFromGroup(1, messageA);
		assertThat(store.messageGroupSize(1)).isEqualTo(0);
		assertThat(store.messageGroupSize(2)).isEqualTo(0);
		assertThat(store.messageGroupSize(3)).isEqualTo(0);
		assertThat(store.messageGroupSize(4)).isEqualTo(0);
	}

	@Test
	@MongoDbAvailable
	public void testMessageGroupUpdatedDateChangesWithEachAddedMessage() throws InterruptedException {
		MessageGroupStore store = getMessageGroupStore();

		MessageGroup messageGroup = store.getMessageGroup(1);
		Message<?> message = new GenericMessage<>("Hello");
		messageGroup = store.addMessageToGroup(1, message);
		assertThat(messageGroup).isNotNull();
		assertThat(messageGroup.size()).isEqualTo(1);
		long createdTimestamp = messageGroup.getTimestamp();
		long updatedTimestamp = messageGroup.getLastModified();
		assertThat(updatedTimestamp).isEqualTo(createdTimestamp);
		Thread.sleep(10);
		message = new GenericMessage<>("Hello again");
		messageGroup = store.addMessageToGroup(1, message);
		createdTimestamp = messageGroup.getTimestamp();
		updatedTimestamp = messageGroup.getLastModified();
		assertThat(updatedTimestamp > createdTimestamp).isTrue();
		assertThat(messageGroup.size()).isEqualTo(2);

		// make sure the store is properly rebuild from MongoDB
		store = getMessageGroupStore();

		messageGroup = store.getMessageGroup(1);
		assertThat(messageGroup.size()).isEqualTo(2);
	}

	@Test
	@MongoDbAvailable
	public void testMessageGroupMarkingMessage() {
		MessageGroupStore store = getMessageGroupStore();

		MessageGroup messageGroup = store.getMessageGroup(1);
		Message<?> messageA = new GenericMessage<>("A");
		Message<?> messageB = new GenericMessage<>("B");
		store.addMessagesToGroup(1, messageA);
		messageGroup = store.addMessageToGroup(1, messageB);
		assertThat(messageGroup).isNotNull();
		assertThat(messageGroup.size()).isEqualTo(2);

		store.removeMessagesFromGroup(1, messageA);
		messageGroup = store.getMessageGroup(1);
		assertThat(messageGroup.size()).isEqualTo(1);

		// validate that the updates were propagated to Mongo as well
		store = this.getMessageGroupStore();

		messageGroup = store.getMessageGroup(1);
		assertThat(messageGroup.size()).isEqualTo(1);
	}

	@Test
	@MongoDbAvailable
	public void testRemoveMessageGroup() {
		MessageGroupStore store = getMessageGroupStore();
		MessageStore messageStore = getMessageStore();

		MessageGroup messageGroup = store.getMessageGroup(1);
		Message<?> message = new GenericMessage<>("Hello");
		UUID id = message.getHeaders().getId();
		messageGroup = store.addMessageToGroup(1, message);
		assertThat(messageGroup).isNotNull();
		assertThat(messageGroup.size()).isEqualTo(1);
		message = messageStore.getMessage(id);
		assertThat(message).isNotNull();

		store.removeMessageGroup(1);
		MessageGroup messageGroupA = store.getMessageGroup(1);
		assertThat(messageGroupA.size()).isEqualTo(0);
		assertThat(messageGroupA.equals(messageGroup)).isFalse();

	}

	@Test
	@MongoDbAvailable
	public void testCompleteMessageGroup() {
		MessageGroupStore store = getMessageGroupStore();

		MessageGroup messageGroup = store.getMessageGroup(1);
		assertThat(messageGroup).isNotNull();
		Message<?> message = new GenericMessage<>("Hello");
		store.addMessagesToGroup(messageGroup.getGroupId(), message);
		store.completeGroup(messageGroup.getGroupId());
		messageGroup = store.getMessageGroup(1);
		assertThat(messageGroup.isComplete()).isTrue();
	}

	@Test
	@MongoDbAvailable
	public void testLastReleasedSequenceNumber() {
		MessageGroupStore store = getMessageGroupStore();

		MessageGroup messageGroup = store.getMessageGroup(1);
		assertThat(messageGroup).isNotNull();
		Message<?> message = new GenericMessage<>("Hello");
		store.addMessagesToGroup(messageGroup.getGroupId(), message);
		store.setLastReleasedSequenceNumberForGroup(messageGroup.getGroupId(), 5);
		messageGroup = store.getMessageGroup(1);
		assertThat(messageGroup.getLastReleasedMessageSequenceNumber()).isEqualTo(5);
	}

	@Test
	@MongoDbAvailable
	public void testRemoveMessageFromTheGroup() {
		MessageGroupStore store = getMessageGroupStore();

		MessageGroup messageGroup = store.getMessageGroup(1);
		Message<?> message = new GenericMessage<>("2");
		store.addMessagesToGroup(1, new GenericMessage<>("1"), message);
		messageGroup = store.addMessageToGroup(1, new GenericMessage<>("3"));
		assertThat(messageGroup).isNotNull();
		assertThat(messageGroup.size()).isEqualTo(3);

		store.removeMessagesFromGroup(1, message);
		messageGroup = store.getMessageGroup(1);
		assertThat(messageGroup.size()).isEqualTo(2);
	}

	@Test
	@MongoDbAvailable
	public void testMultipleMessageStores() {
		MessageGroupStore store1 = getMessageGroupStore();
		MessageGroupStore store2 = getMessageGroupStore();

		Message<?> message = new GenericMessage<>("1");
		store1.addMessagesToGroup(1, message, new GenericMessage<>("2"), new GenericMessage<>("3"));

		MessageGroupStore store3 = getMessageGroupStore();

		MessageGroup messageGroup = store3.getMessageGroup(1);

		assertThat(messageGroup).isNotNull();
		assertThat(messageGroup.size()).isEqualTo(3);

		store3.removeMessagesFromGroup(1, message);

		messageGroup = store2.getMessageGroup(1);
		assertThat(messageGroup.size()).isEqualTo(2);
	}

	@Test
	@MongoDbAvailable
	public void testMessageGroupIterator() {
		MessageGroupStore store1 = getMessageGroupStore();
		MessageGroupStore store2 = getMessageGroupStore();

		Message<?> message = new GenericMessage<>("1");
		store2.addMessagesToGroup("1", message);
		store1.addMessagesToGroup("2", new GenericMessage<>("2"));
		store2.addMessagesToGroup("3", new GenericMessage<>("3"));

		MessageGroupStore store3 = this.getMessageGroupStore();
		Iterator<MessageGroup> iterator = store3.iterator();
		assertThat(iterator).isNotNull();
		int counter = 0;
		while (iterator.hasNext()) {
			iterator.next();
			counter++;
		}
		assertThat(counter).isEqualTo(3);

		store2.removeMessagesFromGroup("1", message);

		iterator = store3.iterator();
		counter = 0;
		while (iterator.hasNext()) {
			iterator.next();
			counter++;
		}
		assertThat(counter).isEqualTo(2);
	}

	@Test
	@MongoDbAvailable
	public void testAddAndRemoveMessagesFromMessageGroup() {
		MessageGroupStore messageStore = (MessageGroupStore) getMessageStore();
		String groupId = "X";
		messageStore.removeMessageGroup("X");
		((AbstractBatchingMessageGroupStore) messageStore).setRemoveBatchSize(10);
		List<Message<?>> messages = new ArrayList<>();
		for (int i = 0; i < 25; i++) {
			Message<String> message = MessageBuilder.withPayload("foo").setCorrelationId(groupId).build();
			messageStore.addMessagesToGroup(groupId, message);
			messages.add(message);
		}
		MessageGroup group = messageStore.getMessageGroup(groupId);
		assertThat(group.size()).isEqualTo(25);
		messageStore.removeMessagesFromGroup(groupId, messages);
		group = messageStore.getMessageGroup(groupId);
		assertThat(group.size()).isEqualTo(0);
	}

	//	@Test
	//	@MongoDbAvailable
	//	public void testConcurrentModifications() throws Exception{
	//		MongoDatabaseFactory mongoDbFactory = this.prepareMongoFactory();
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
	//			store2.removeMessageFromGroup(1, message); // ensures that if ADD thread executed after REMOVE, the
	//			store is empty for the next cycle
	//		}
	//		assertTrue(failures.size() == 0);
	//	}


	protected void testWithAggregatorWithShutdown(String config) {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(config, this.getClass());
		context.refresh();

		MessageChannel input = context.getBean("inputChannel", MessageChannel.class);
		QueueChannel output = context.getBean("outputChannel", QueueChannel.class);

		Message<?> m1 = MessageBuilder.withPayload("1")
				.setSequenceNumber(1)
				.setSequenceSize(10)
				.setCorrelationId(1)
				.build();
		Message<?> m2 = MessageBuilder.withPayload("2")
				.setSequenceNumber(2)
				.setSequenceSize(10)
				.setCorrelationId(1)
				.build();
		input.send(m1);
		assertThat(output.receive(1000)).isNull();
		input.send(m2);
		assertThat(output.receive(1000)).isNull();

		for (int i = 3; i < 10; i++) {
			input.send(MessageBuilder.withPayload("" + i)
					.setSequenceNumber(i)
					.setSequenceSize(10)
					.setCorrelationId(1)
					.build());
		}

		context.close();

		context = new ClassPathXmlApplicationContext(config, this.getClass());
		input = context.getBean("inputChannel", MessageChannel.class);
		output = context.getBean("outputChannel", QueueChannel.class);

		Message<?> m10 = MessageBuilder.withPayload("10")
				.setSequenceNumber(10)
				.setSequenceSize(10)
				.setCorrelationId(1)
				.build();
		input.send(m10);
		assertThat(output.receive(2000)).isNotNull();
		context.close();
	}

	@Test
	@MongoDbAvailable
	public void testWithMessageHistory() {
		MessageGroupStore store = getMessageGroupStore();

		store.getMessageGroup(1);

		Message<?> message = new GenericMessage<>("Hello");
		DirectChannel fooChannel = new DirectChannel();
		fooChannel.setBeanName("fooChannel");
		DirectChannel barChannel = new DirectChannel();
		barChannel.setBeanName("barChannel");

		message = MessageHistory.write(message, fooChannel);
		message = MessageHistory.write(message, barChannel);
		store.addMessagesToGroup(1, message);
		MessageGroup group = store.getMessageGroup(1);
		assertThat(group).isNotNull();
		Collection<Message<?>> messages = group.getMessages();
		assertThat(!messages.isEmpty()).isTrue();
		message = messages.iterator().next();

		MessageHistory messageHistory = MessageHistory.read(message);
		assertThat(messageHistory).isNotNull();
		assertThat(messageHistory.size()).isEqualTo(2);
		Properties fooChannelHistory = messageHistory.get(0);
		assertThat(fooChannelHistory.get("name")).isEqualTo("fooChannel");
		assertThat(fooChannelHistory.get("type")).isEqualTo("channel");
	}

	protected abstract MessageGroupStore getMessageGroupStore();

	protected abstract MessageStore getMessageStore();

}
