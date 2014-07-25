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

package org.springframework.integration.aggregator;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.store.MessageGroup;
import org.springframework.integration.store.MessageGroupStore;
import org.springframework.integration.store.SimpleMessageGroup;
import org.springframework.integration.store.SimpleMessageStore;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.GenericMessage;

/**
 * @author Gary Russell
 * @author Artem Bilan
 * @since 2.2
 *
 */
public class AbstractCorrelatingMessageHandlerTests {

	@Test // INT-2751
	public void testReaperDoesntReapAProcessingGroup() throws Exception {
		final MessageGroupStore groupStore = new SimpleMessageStore();
		final CountDownLatch waitForSendlatch = new CountDownLatch(1);
		final CountDownLatch waitReapStartLatch = new CountDownLatch(1);
		final CountDownLatch waitReapCompleteLatch = new CountDownLatch(1);
		AbstractCorrelatingMessageHandler handler = new AbstractCorrelatingMessageHandler(
				new MessageGroupProcessor() {

					@Override
					public Object processMessageGroup(MessageGroup group) {
						return group;
					}
				}, groupStore) {

			@Override
			protected void afterRelease(MessageGroup group, Collection<Message<?>> completedMessages) {
			}
		};
		handler.setReleasePartialSequences(true);

		/*
		 * Runs "reap" when group 'bar' is in completion
		 */
		Executors.newSingleThreadExecutor().execute(new Runnable() {

			@Override
			public void run() {
				try {
					waitReapStartLatch.await(10, TimeUnit.SECONDS);
				}
				catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
				waitForSendlatch.countDown();
				try {
					Thread.sleep(100);
				}
				catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
				groupStore.expireMessageGroups(50);
				waitReapCompleteLatch.countDown();
			}
		});

		final List<Message<?>> outputMessages = new ArrayList<Message<?>>();
		handler.setOutputChannel(new MessageChannel() {

			/*
			 * Executes when group 'bar' completes normally
			 */
			@Override
			public boolean send(Message<?> message, long timeout) {
				outputMessages.add(message);
				// wake reaper
				waitReapStartLatch.countDown();
				try {
					waitForSendlatch.await(10, TimeUnit.SECONDS);
					// wait a little longer for reaper to grab groups
					Thread.sleep(2000);
					// simulate tx commit
					groupStore.removeMessageGroup("bar");
				}
				catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
				return true;
			}

			@Override
			public boolean send(Message<?> message) {
				return this.send(message, 0);
			}
		});
		handler.setReleaseStrategy(new ReleaseStrategy() {

			@Override
			public boolean canRelease(MessageGroup group) {
				return group.size() == 2;
			}
		});

		QueueChannel discards = new QueueChannel();
		handler.setDiscardChannel(discards);
		handler.setSendPartialResultOnExpiry(true);

		Message<String>	message = MessageBuilder.withPayload("foo")
				.setCorrelationId("qux")
				.build();
		// partial group that will be reaped
		handler.handleMessage(message);
		message = MessageBuilder.withPayload("foo")
				.setCorrelationId("bar")
				.build();
		// full group that should not be reaped
		handler.handleMessage(message);
		message = MessageBuilder.withPayload("baz")
				.setCorrelationId("bar")
				.build();
		handler.handleMessage(message);

		assertTrue(waitReapCompleteLatch.await(10, TimeUnit.SECONDS));
		// Before INT-2751 we got bar + bar + qux
		assertEquals(2, outputMessages.size()); // bar + qux
		// normal release
		assertEquals(2, ((MessageGroup) outputMessages.get(0).getPayload()).size()); // 'bar'
		// reaper release
		assertEquals(1, ((MessageGroup) outputMessages.get(1).getPayload()).size()); // 'qux'

		assertNull(discards.receive(0));
	}

	@Test // INT-2833
	public void testReaperReapsAnEmptyGroup() throws Exception {
		final MessageGroupStore groupStore = new SimpleMessageStore();
		AggregatingMessageHandler handler = new AggregatingMessageHandler(
				new MessageGroupProcessor() {

					@Override
					public Object processMessageGroup(MessageGroup group) {
						return group;
					}
				}, groupStore) {
		};

		final List<Message<?>> outputMessages = new ArrayList<Message<?>>();
		handler.setOutputChannel(new MessageChannel() {

			/*
			 * Executes when group 'bar' completes normally
			 */
			@Override
			public boolean send(Message<?> message, long timeout) {
				outputMessages.add(message);
				return true;
			}

			@Override
			public boolean send(Message<?> message) {
				return this.send(message, 0);
			}
		});
		handler.setReleaseStrategy(new ReleaseStrategy() {

			@Override
			public boolean canRelease(MessageGroup group) {
				return group.size() == 1;
			}
		});

		Message<String>	message = MessageBuilder.withPayload("foo")
				.setCorrelationId("bar")
				.build();
		handler.handleMessage(message);

		assertEquals(1, outputMessages.size());

		assertEquals(1, TestUtils.getPropertyValue(handler, "messageStore.groupIdToMessageGroup", Map.class).size());
		groupStore.expireMessageGroups(0);
		assertEquals(0, TestUtils.getPropertyValue(handler, "messageStore.groupIdToMessageGroup", Map.class).size());
	}

	@Test // INT-2833
	public void testReaperReapsAnEmptyGroupAfterConfiguredDelay() throws Exception {
		final MessageGroupStore groupStore = new SimpleMessageStore();
		AggregatingMessageHandler handler = new AggregatingMessageHandler(
				new MessageGroupProcessor() {

					@Override
					public Object processMessageGroup(MessageGroup group) {
						return group;
					}
				}, groupStore) {
		};

		final List<Message<?>> outputMessages = new ArrayList<Message<?>>();
		handler.setOutputChannel(new MessageChannel() {

			/*
			 * Executes when group 'bar' completes normally
			 */
			@Override
			public boolean send(Message<?> message, long timeout) {
				outputMessages.add(message);
				return true;
			}

			@Override
			public boolean send(Message<?> message) {
				return this.send(message, 0);
			}
		});
		handler.setReleaseStrategy(new ReleaseStrategy() {

			@Override
			public boolean canRelease(MessageGroup group) {
				return group.size() == 1;
			}
		});

		handler.setMinimumTimeoutForEmptyGroups(1000);

		Message<String>	message = MessageBuilder.withPayload("foo")
				.setCorrelationId("bar")
				.build();
		handler.handleMessage(message);

		assertEquals(1, outputMessages.size());

		assertEquals(1, TestUtils.getPropertyValue(handler, "messageStore.groupIdToMessageGroup", Map.class).size());
		groupStore.expireMessageGroups(0);
		assertEquals(1, TestUtils.getPropertyValue(handler, "messageStore.groupIdToMessageGroup", Map.class).size());
		Thread.sleep(1010);
		groupStore.expireMessageGroups(0);
		assertEquals(0, TestUtils.getPropertyValue(handler, "messageStore.groupIdToMessageGroup", Map.class).size());
	}

	@Test
	public void testReapWithChangeInSameMillisecond() throws Exception {
		MessageGroupProcessor mgp = new DefaultAggregatingMessageGroupProcessor();
		AggregatingMessageHandler handler = new AggregatingMessageHandler(mgp);
		handler.setReleaseStrategy(new ReleaseStrategy() {
			@Override
			public boolean canRelease(MessageGroup group) {
				return true;
			}
		});
		QueueChannel outputChannel = new QueueChannel();
		handler.setOutputChannel(outputChannel);
		MessageGroupStore mgs = TestUtils.getPropertyValue(handler, "messageStore", MessageGroupStore.class);
		Method forceComplete =
				AbstractCorrelatingMessageHandler.class.getDeclaredMethod("forceComplete", MessageGroup.class);
		forceComplete.setAccessible(true);
		mgs.addMessageToGroup("foo", new GenericMessage<String>("foo"));
		GenericMessage<String> secondMessage = new GenericMessage<String>("bar");
		mgs.addMessageToGroup("foo", secondMessage);
		MessageGroup group = mgs.getMessageGroup("foo");
		// remove a message
		mgs.removeMessageFromGroup("foo", secondMessage);
		// force lastModified to be the same
		MessageGroup groupNow = mgs.getMessageGroup("foo");
		new DirectFieldAccessor(group).setPropertyValue("lastModified", groupNow.getLastModified());
		forceComplete.invoke(handler, group);
		Message<?> message = outputChannel.receive(0);
		assertNotNull(message);
		Collection<?> payload = (Collection<?>) message.getPayload();
		assertEquals(1, payload.size());
	}

	@Test /* INT-3216 */
	public void testDontReapIfAlreadyComplete() throws Exception {
		MessageGroupProcessor mgp = new DefaultAggregatingMessageGroupProcessor();
		AggregatingMessageHandler handler = new AggregatingMessageHandler(mgp);
		handler.setReleaseStrategy(new ReleaseStrategy() {

			@Override
			public boolean canRelease(MessageGroup group) {
				return true;
			}

		});
		QueueChannel outputChannel = new QueueChannel();
		handler.setOutputChannel(outputChannel);
		MessageGroupStore mgs = TestUtils.getPropertyValue(handler, "messageStore", MessageGroupStore.class);
		mgs.addMessageToGroup("foo", new GenericMessage<String>("foo"));
		mgs.completeGroup("foo");
		mgs = spy(mgs);
		new DirectFieldAccessor(handler).setPropertyValue("messageStore", mgs);
		Method forceComplete =
				AbstractCorrelatingMessageHandler.class.getDeclaredMethod("forceComplete", MessageGroup.class);
		forceComplete.setAccessible(true);
		MessageGroup group = (MessageGroup) TestUtils.getPropertyValue(mgs, "groupIdToMessageGroup", Map.class)
				.get("foo");
		assertTrue(group.isComplete());
		forceComplete.invoke(handler, group);
		verify(mgs, never()).getMessageGroup("foo");
		assertNull(outputChannel.receive(0));
	}

	/*
	 * INT-3216 - Verifies the complete early exit is taken after a refresh.
	 */
	@Test
	public void testDontReapIfAlreadyCompleteAfterRefetch() throws Exception {
		MessageGroupProcessor mgp = new DefaultAggregatingMessageGroupProcessor();
		AggregatingMessageHandler handler = new AggregatingMessageHandler(mgp);
		handler.setReleaseStrategy(new ReleaseStrategy() {

			@Override
			public boolean canRelease(MessageGroup group) {
				return true;
			}

		});
		QueueChannel outputChannel = new QueueChannel();
		handler.setOutputChannel(outputChannel);
		MessageGroupStore mgs = TestUtils.getPropertyValue(handler, "messageStore", MessageGroupStore.class);
		mgs.addMessageToGroup("foo", new GenericMessage<String>("foo"));
		MessageGroup group = new SimpleMessageGroup(mgs.getMessageGroup("foo"));
		mgs.completeGroup("foo");
		mgs = spy(mgs);
		new DirectFieldAccessor(handler).setPropertyValue("messageStore", mgs);
		Method forceComplete =
				AbstractCorrelatingMessageHandler.class.getDeclaredMethod("forceComplete", MessageGroup.class);
		forceComplete.setAccessible(true);
		MessageGroup groupInStore = (MessageGroup) TestUtils.getPropertyValue(mgs, "groupIdToMessageGroup", Map.class)
				.get("foo");
		assertTrue(groupInStore.isComplete());
		assertFalse(group.isComplete());
		new DirectFieldAccessor(group).setPropertyValue("lastModified", groupInStore.getLastModified());
		forceComplete.invoke(handler, group);
		verify(mgs).getMessageGroup("foo");
		assertNull(outputChannel.receive(0));
	}

	/*
	 * INT-3216 - Verifies we don't complete if it's a completely new group (different timestamp).
	 */
	@Test
	public void testDontReapIfNewGroupFoundDuringRefetch() throws Exception {
		MessageGroupProcessor mgp = new DefaultAggregatingMessageGroupProcessor();
		AggregatingMessageHandler handler = new AggregatingMessageHandler(mgp);
		handler.setReleaseStrategy(new ReleaseStrategy() {

			@Override
			public boolean canRelease(MessageGroup group) {
				return true;
			}

		});
		QueueChannel outputChannel = new QueueChannel();
		handler.setOutputChannel(outputChannel);
		MessageGroupStore mgs = TestUtils.getPropertyValue(handler, "messageStore", MessageGroupStore.class);
		mgs.addMessageToGroup("foo", new GenericMessage<String>("foo"));
		MessageGroup group = new SimpleMessageGroup(mgs.getMessageGroup("foo"));
		mgs = spy(mgs);
		new DirectFieldAccessor(handler).setPropertyValue("messageStore", mgs);
		Method forceComplete =
				AbstractCorrelatingMessageHandler.class.getDeclaredMethod("forceComplete", MessageGroup.class);
		forceComplete.setAccessible(true);
		MessageGroup groupInStore = (MessageGroup) TestUtils.getPropertyValue(mgs, "groupIdToMessageGroup", Map.class)
				.get("foo");
		assertFalse(groupInStore.isComplete());
		assertFalse(group.isComplete());
		DirectFieldAccessor directFieldAccessor = new DirectFieldAccessor(group);
		directFieldAccessor.setPropertyValue("lastModified", groupInStore.getLastModified());
		directFieldAccessor.setPropertyValue("timestamp", groupInStore.getTimestamp() - 1);
		forceComplete.invoke(handler, group);
		verify(mgs).getMessageGroup("foo");
		assertNull(outputChannel.receive(0));
	}

	@Test
	public void testInt3483DeadlockOnMessageStoreRemoveMessageGroup() throws InterruptedException {
		final AggregatingMessageHandler handler =
				new AggregatingMessageHandler(new DefaultAggregatingMessageGroupProcessor());
		handler.setOutputChannel(new QueueChannel());
		QueueChannel discardChannel = new QueueChannel();
		handler.setDiscardChannel(discardChannel);
		handler.setReleaseStrategy(new ReleaseStrategy() {

			@Override
			public boolean canRelease(MessageGroup group) {
				return true;
			}

		});
		handler.setExpireGroupsUponTimeout(false);
		SimpleMessageStore messageStore = new SimpleMessageStore() {
			@Override
			public void removeMessageGroup(Object groupId) {
				throw new RuntimeException("intentional");
			}
		};
		handler.setMessageStore(messageStore);
		handler.handleMessage(MessageBuilder.withPayload("foo")
				.setCorrelationId(1)
				.setSequenceNumber(1)
				.setSequenceSize(2)
				.build());

		try {
			messageStore.expireMessageGroups(0);
		}
		catch (Exception e) {
			//suppress an intentional 'removeMessageGroup' exception
		}
		ExecutorService executorService = Executors.newSingleThreadExecutor();
		executorService.execute(new Runnable() {
			@Override
			public void run() {
				handler.handleMessage(MessageBuilder.withPayload("foo")
						.setCorrelationId(1)
						.setSequenceNumber(2)
						.setSequenceSize(2)
						.build());
			}
		});
		executorService.shutdown();
		/* Previously lock for the groupId hasn't been unlocked from the 'forceComplete', because it wasn't
		 reachable in case of exception from the BasicMessageGroupStore.removeMessageGroup
		  */
		assertTrue(executorService.awaitTermination(10, TimeUnit.SECONDS));

		/* Since MessageGroup had been marked as 'complete', but hasn't been removed because of exception,
		 the second message is discarded
		  */
		Message<?> receive = discardChannel.receive(1000);
		assertNotNull(receive);
	}

}
