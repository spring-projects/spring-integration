/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.integration.aggregator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.store.MessageGroup;
import org.springframework.integration.store.MessageGroupStore;
import org.springframework.integration.store.SimpleMessageGroup;
import org.springframework.integration.store.SimpleMessageStore;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * @author Gary Russell
 * @author Artem Bilan
 * @author Meherzad Lahewala
 *
 * @since 2.2
 *
 */
public class AbstractCorrelatingMessageHandlerTests {

	@Test
	public void testReaperDoesntReapAProcessingGroup() throws Exception {
		final MessageGroupStore groupStore = new SimpleMessageStore();
		final CountDownLatch waitForSendLatch = new CountDownLatch(1);
		final CountDownLatch waitReapStartLatch = new CountDownLatch(1);
		final CountDownLatch waitReapCompleteLatch = new CountDownLatch(1);
		AbstractCorrelatingMessageHandler handler = new AbstractCorrelatingMessageHandler(group -> group, groupStore) {

			@Override
			protected void afterRelease(MessageGroup group, Collection<Message<?>> completedMessages) {
			}

		};
		handler.setReleasePartialSequences(true);

		/*
		 * Runs "reap" when group 'bar' is in completion
		 */
		ExecutorService exec = Executors.newSingleThreadExecutor();
		exec.execute(() -> {
			try {
				waitReapStartLatch.await(10, TimeUnit.SECONDS);
			}
			catch (InterruptedException e1) {
				Thread.currentThread().interrupt();
			}
			waitForSendLatch.countDown();
			try {
				Thread.sleep(100);
			}
			catch (InterruptedException e2) {
				Thread.currentThread().interrupt();
			}
			groupStore.expireMessageGroups(50);
			waitReapCompleteLatch.countDown();
		});

		final List<Message<?>> outputMessages = new ArrayList<>();
		handler.setOutputChannel((message, timeout) -> {
			/*
			 * Executes when group 'bar' completes normally
			 */
			outputMessages.add(message);
			// wake reaper
			waitReapStartLatch.countDown();
			try {
				waitForSendLatch.await(10, TimeUnit.SECONDS);
				// wait a little longer for reaper to grab groups
				Thread.sleep(2000);
				// simulate tx commit
				groupStore.removeMessageGroup("bar");
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			return true;
		});
		handler.setReleaseStrategy(group -> group.size() == 2);

		QueueChannel discards = new QueueChannel();
		handler.setDiscardChannel(discards);
		handler.setSendPartialResultOnExpiry(true);

		Message<String> message = MessageBuilder.withPayload("foo")
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

		assertThat(waitReapCompleteLatch.await(20, TimeUnit.SECONDS)).isTrue();
		// Before INT-2751 we got bar + bar + qux
		assertThat(outputMessages.size()).isEqualTo(2); // bar + qux
		// normal release
		assertThat(((MessageGroup) outputMessages.get(0).getPayload()).size()).isEqualTo(2); // 'bar'
		// reaper release
		assertThat(((MessageGroup) outputMessages.get(1).getPayload()).size()).isEqualTo(1); // 'qux'

		assertThat(discards.receive(0)).isNull();
		exec.shutdownNow();
	}

	@Test
	public void testReaperReapsAnEmptyGroup() {
		final MessageGroupStore groupStore = new SimpleMessageStore();
		AggregatingMessageHandler handler = new AggregatingMessageHandler(group -> group, groupStore);

		final List<Message<?>> outputMessages = new ArrayList<>();
		handler.setOutputChannel((message, timeout) -> {
			/*
			 * Executes when group 'bar' completes normally
			 */
			outputMessages.add(message);
			return true;
		});
		handler.setReleaseStrategy(group -> group.size() == 1);

		Message<String> message = MessageBuilder.withPayload("foo")
				.setCorrelationId("bar")
				.build();
		handler.handleMessage(message);

		assertThat(outputMessages.size()).isEqualTo(1);

		assertThat(TestUtils.getPropertyValue(handler, "messageStore.groupIdToMessageGroup", Map.class).size())
				.isEqualTo(1);
		groupStore.expireMessageGroups(0);
		assertThat(TestUtils.getPropertyValue(handler, "messageStore.groupIdToMessageGroup", Map.class).size())
				.isEqualTo(0);
	}

	@Test
	public void testReaperReapsAnEmptyGroupAfterConfiguredDelay() throws Exception {
		final MessageGroupStore groupStore = new SimpleMessageStore();
		AggregatingMessageHandler handler = new AggregatingMessageHandler(group -> group, groupStore);

		final List<Message<?>> outputMessages = new ArrayList<>();
		handler.setOutputChannel((message, timeout) -> {
			/*
			 * Executes when group 'bar' completes normally
			 */
			outputMessages.add(message);
			return true;
		});
		handler.setReleaseStrategy(group -> group.size() == 1);

		Message<String> message = MessageBuilder.withPayload("foo")
				.setCorrelationId("bar")
				.build();
		handler.handleMessage(message);

		handler.setMinimumTimeoutForEmptyGroups(10_000);

		assertThat(outputMessages.size()).isEqualTo(1);

		assertThat(TestUtils.getPropertyValue(handler, "messageStore.groupIdToMessageGroup", Map.class).size())
				.isEqualTo(1);
		groupStore.expireMessageGroups(0);
		assertThat(TestUtils.getPropertyValue(handler, "messageStore.groupIdToMessageGroup", Map.class).size())
				.isEqualTo(1);

		handler.setMinimumTimeoutForEmptyGroups(10);

		int n = 0;

		while (n++ < 200) {
			groupStore.expireMessageGroups(0);
			if (TestUtils.getPropertyValue(handler, "messageStore.groupIdToMessageGroup", Map.class).size() > 0) {
				Thread.sleep(50);
			}
			else {
				break;
			}
		}

		assertThat(n < 200).isTrue();
		assertThat(TestUtils.getPropertyValue(handler, "messageStore.groupIdToMessageGroup", Map.class).size())
				.isEqualTo(0);
	}

	@Test
	public void testReapWithChangeInSameMillisecond() throws Exception {
		MessageGroupProcessor mgp = new DefaultAggregatingMessageGroupProcessor();
		AggregatingMessageHandler handler = new AggregatingMessageHandler(mgp);
		handler.setReleaseStrategy(group -> true);
		QueueChannel outputChannel = new QueueChannel();
		handler.setOutputChannel(outputChannel);
		MessageGroupStore mgs = TestUtils.getPropertyValue(handler, "messageStore", MessageGroupStore.class);
		Method forceComplete =
				AbstractCorrelatingMessageHandler.class.getDeclaredMethod("forceComplete", MessageGroup.class);
		forceComplete.setAccessible(true);
		GenericMessage<String> secondMessage = new GenericMessage<>("bar");
		mgs.addMessagesToGroup("foo", new GenericMessage<>("foo"), secondMessage);
		MessageGroup group = mgs.getMessageGroup("foo");
		// remove a message
		mgs.removeMessagesFromGroup("foo", secondMessage);
		// force lastModified to be the same
		MessageGroup groupNow = mgs.getMessageGroup("foo");
		new DirectFieldAccessor(group).setPropertyValue("lastModified", groupNow.getLastModified());
		forceComplete.invoke(handler, group);
		Message<?> message = outputChannel.receive(0);
		assertThat(message).isNotNull();
		Collection<?> payload = (Collection<?>) message.getPayload();
		assertThat(payload.size()).isEqualTo(1);
	}

	@Test
	public void testDontReapIfAlreadyComplete() throws Exception {
		MessageGroupProcessor mgp = new DefaultAggregatingMessageGroupProcessor();
		AggregatingMessageHandler handler = new AggregatingMessageHandler(mgp);
		handler.setReleaseStrategy(group -> true);
		QueueChannel outputChannel = new QueueChannel();
		handler.setOutputChannel(outputChannel);
		MessageGroupStore mgs = TestUtils.getPropertyValue(handler, "messageStore", MessageGroupStore.class);
		mgs.addMessagesToGroup("foo", new GenericMessage<>("foo"));
		mgs.completeGroup("foo");
		mgs = spy(mgs);
		new DirectFieldAccessor(handler).setPropertyValue("messageStore", mgs);
		Method forceComplete =
				AbstractCorrelatingMessageHandler.class.getDeclaredMethod("forceComplete", MessageGroup.class);
		forceComplete.setAccessible(true);
		MessageGroup group = (MessageGroup) TestUtils.getPropertyValue(mgs, "groupIdToMessageGroup", Map.class)
				.get("foo");
		assertThat(group.isComplete()).isTrue();
		forceComplete.invoke(handler, group);
		verify(mgs, never()).getMessageGroup("foo");
		assertThat(outputChannel.receive(0)).isNull();
	}

	/*
	 * INT-3216 - Verifies the complete early exit is taken after a refresh.
	 */
	@Test
	public void testDontReapIfAlreadyCompleteAfterRefetch() throws Exception {
		MessageGroupProcessor mgp = new DefaultAggregatingMessageGroupProcessor();
		AggregatingMessageHandler handler = new AggregatingMessageHandler(mgp);
		handler.setReleaseStrategy(group -> true);
		QueueChannel outputChannel = new QueueChannel();
		handler.setOutputChannel(outputChannel);
		MessageGroupStore mgs = TestUtils.getPropertyValue(handler, "messageStore", MessageGroupStore.class);
		mgs.addMessagesToGroup("foo", new GenericMessage<>("foo"));
		MessageGroup group = new SimpleMessageGroup(mgs.getMessageGroup("foo"));
		mgs.completeGroup("foo");
		mgs = spy(mgs);
		new DirectFieldAccessor(handler).setPropertyValue("messageStore", mgs);
		Method forceComplete =
				AbstractCorrelatingMessageHandler.class.getDeclaredMethod("forceComplete", MessageGroup.class);
		forceComplete.setAccessible(true);
		MessageGroup groupInStore = (MessageGroup) TestUtils.getPropertyValue(mgs, "groupIdToMessageGroup", Map.class)
				.get("foo");
		assertThat(groupInStore.isComplete()).isTrue();
		assertThat(group.isComplete()).isFalse();
		new DirectFieldAccessor(group).setPropertyValue("lastModified", groupInStore.getLastModified());
		forceComplete.invoke(handler, group);
		verify(mgs).getMessageGroup("foo");
		assertThat(outputChannel.receive(0)).isNull();
	}

	/*
	 * INT-3216 - Verifies we don't complete if it's a completely new group (different timestamp).
	 */
	@Test
	public void testDontReapIfNewGroupFoundDuringRefetch() throws Exception {
		MessageGroupProcessor mgp = new DefaultAggregatingMessageGroupProcessor();
		AggregatingMessageHandler handler = new AggregatingMessageHandler(mgp);
		handler.setReleaseStrategy(group -> true);
		QueueChannel outputChannel = new QueueChannel();
		handler.setOutputChannel(outputChannel);
		MessageGroupStore mgs = TestUtils.getPropertyValue(handler, "messageStore", MessageGroupStore.class);
		mgs.addMessagesToGroup("foo", new GenericMessage<>("foo"));
		MessageGroup group = new SimpleMessageGroup(mgs.getMessageGroup("foo"));
		mgs = spy(mgs);
		new DirectFieldAccessor(handler).setPropertyValue("messageStore", mgs);
		Method forceComplete =
				AbstractCorrelatingMessageHandler.class.getDeclaredMethod("forceComplete", MessageGroup.class);
		forceComplete.setAccessible(true);
		MessageGroup groupInStore = (MessageGroup) TestUtils.getPropertyValue(mgs, "groupIdToMessageGroup", Map.class)
				.get("foo");
		assertThat(groupInStore.isComplete()).isFalse();
		assertThat(group.isComplete()).isFalse();
		DirectFieldAccessor directFieldAccessor = new DirectFieldAccessor(group);
		directFieldAccessor.setPropertyValue("lastModified", groupInStore.getLastModified());
		directFieldAccessor.setPropertyValue("timestamp", groupInStore.getTimestamp() - 1);
		forceComplete.invoke(handler, group);
		verify(mgs).getMessageGroup("foo");
		assertThat(outputChannel.receive(0)).isNull();
	}

	@Test
	public void testInt3483DeadlockOnMessageStoreRemoveMessageGroup() throws InterruptedException {
		final AggregatingMessageHandler handler =
				new AggregatingMessageHandler(new DefaultAggregatingMessageGroupProcessor());
		handler.setOutputChannel(new QueueChannel());
		QueueChannel discardChannel = new QueueChannel();
		handler.setDiscardChannel(discardChannel);
		handler.setReleaseStrategy(group -> true);
		handler.setExpireGroupsUponTimeout(false);
		SimpleMessageStore messageStore = new SimpleMessageStore() {

			@Override
			public void removeMessageGroup(Object groupId) {
				throw new RuntimeException("intentional");
			}
		};
		handler.setMessageStore(messageStore);
		// test UniqueExpiryCallback error message
		// new AggregatingMessageHandler(new DefaultAggregatingMessageGroupProcessor()).setMessageStore(messageStore);
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
		executorService.execute(() -> handler.handleMessage(MessageBuilder.withPayload("foo")
				.setCorrelationId(1)
				.setSequenceNumber(2)
				.setSequenceSize(2)
				.build()));
		executorService.shutdown();
		/* Previously lock for the groupId hasn't been unlocked from the 'forceComplete', because it wasn't
		 reachable in case of exception from the BasicMessageGroupStore.removeMessageGroup
		  */
		assertThat(executorService.awaitTermination(10, TimeUnit.SECONDS)).isTrue();

		/* Since MessageGroup had been marked as 'complete', but hasn't been removed because of exception,
		 the second message is discarded
		  */
		Message<?> receive = discardChannel.receive(10000);
		assertThat(receive).isNotNull();
	}

	@Test
	@Disabled("Time sensitive: the empty group might be removed before main thread reaches assertion for size")
	public void testScheduleRemoveAnEmptyGroupAfterConfiguredDelay() throws Exception {
		final MessageGroupStore groupStore = new SimpleMessageStore();
		AggregatingMessageHandler handler = new AggregatingMessageHandler(group -> group, groupStore);

		final List<Message<?>> outputMessages = new ArrayList<>();
		handler.setOutputChannel((message, timeout) -> {
			/*
			 * Executes when group 'bar' completes normally
			 */
			outputMessages.add(message);
			return true;
		});
		handler.setReleaseStrategy(group -> group.size() == 1);

		handler.setMinimumTimeoutForEmptyGroups(100);

		ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
		taskScheduler.afterPropertiesSet();
		handler.setTaskScheduler(taskScheduler);

		Message<String> message = MessageBuilder.withPayload("foo")
				.setCorrelationId("bar")
				.build();
		handler.handleMessage(message);

		assertThat(outputMessages.size()).isEqualTo(1);

		assertThat(TestUtils.getPropertyValue(handler, "messageStore.groupIdToMessageGroup", Map.class).size())
				.isEqualTo(1);

		Thread.sleep(100);

		int n = 0;

		while (TestUtils.getPropertyValue(handler, "messageStore.groupIdToMessageGroup", Map.class).size() > 0
				&& n++ < 200) {
			Thread.sleep(50);
		}

		assertThat(n < 200).isTrue();
		assertThat(TestUtils.getPropertyValue(handler, "messageStore.groupIdToMessageGroup", Map.class).size())
				.isEqualTo(0);
	}

	@Test
	@Disabled("Until 5.2 with new 'owner' feature on groups")
	public void testDontReapMessageOfOtherHandler() {
		MessageGroupStore groupStore = new SimpleMessageStore();

		AggregatingMessageHandler handler1 = new AggregatingMessageHandler(group -> group, groupStore);
		AggregatingMessageHandler handler2 = new AggregatingMessageHandler(group -> group, groupStore);

		QueueChannel handler1DiscardChannel = new QueueChannel();
		handler1.setDiscardChannel(handler1DiscardChannel);

		QueueChannel handler2DiscardChannel = new QueueChannel();
		handler2.setDiscardChannel(handler2DiscardChannel);

		handler1.setReleaseStrategy(group -> false);
		handler2.setReleaseStrategy(group -> false);

		handler1.handleMessage(MessageBuilder.withPayload("foo").setCorrelationId("foo").build());
		handler1.handleMessage(MessageBuilder.withPayload("foo").setCorrelationId("foo").build());
		handler2.handleMessage(MessageBuilder.withPayload("foo").setCorrelationId("bar").build());

		groupStore.expireMessageGroups(0);

		assertThat(handler1DiscardChannel.getQueueSize()).isEqualTo(2);
		assertThat(handler2DiscardChannel.getQueueSize()).isEqualTo(1);
	}

	@Test
	public void testNoPopSequenceDetails() {
		MessageGroupProcessor mgp = new DefaultAggregatingMessageGroupProcessor();
		AggregatingMessageHandler handler = new AggregatingMessageHandler(mgp);
		handler.setReleaseStrategy(group -> true);
		handler.setPopSequence(false);
		QueueChannel outputChannel = new QueueChannel();
		handler.setOutputChannel(outputChannel);

		Message<?> testMessage = MessageBuilder.withPayload("foo")
				.setCorrelationId(1)
				.setSequenceNumber(1)
				.setSequenceSize(1)
				.pushSequenceDetails(2, 2, 2)
				.build();

		handler.handleMessage(testMessage);

		Message<?> receive = outputChannel.receive(10_000);

		assertThat(receive).isNotNull();

		assertThat(receive.getHeaders().get(IntegrationMessageHeaderAccessor.CORRELATION_ID)).isEqualTo(2);
		assertThat(receive.getHeaders().get(IntegrationMessageHeaderAccessor.SEQUENCE_NUMBER)).isEqualTo(2);
		assertThat(receive.getHeaders().get(IntegrationMessageHeaderAccessor.SEQUENCE_SIZE)).isEqualTo(2);
		assertThat(receive.getHeaders().containsKey(IntegrationMessageHeaderAccessor.SEQUENCE_DETAILS)).isTrue();
	}

	@Test
	public void testPurgeOrphanedGroupsOnStartup() throws InterruptedException {
		MessageGroupStore groupStore = new SimpleMessageStore();
		AggregatingMessageHandler handler = new AggregatingMessageHandler(group -> group, groupStore);
		handler.setReleaseStrategy(group -> false);
		QueueChannel discardChannel = new QueueChannel();
		handler.setDiscardChannel(discardChannel);
		handler.setExpireTimeout(1);
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.afterPropertiesSet();
		handler.handleMessageInternal(MessageBuilder.withPayload("test").setCorrelationId("test").build());
		Thread.sleep(100);
		handler.start();
		Message<?> receive = discardChannel.receive(10000);
		assertThat(receive).isNotNull();
		assertThat(groupStore.getMessageGroupCount()).isEqualTo(0);
	}

	@Test
	public void testPurgeOrphanedGroupsScheduled() {
		MessageGroupStore groupStore = spy(new SimpleMessageStore());
		AggregatingMessageHandler handler = new AggregatingMessageHandler(group -> group, groupStore);
		handler.setReleaseStrategy(group -> false);
		QueueChannel discardChannel = new QueueChannel();
		handler.setDiscardChannel(discardChannel);
		handler.setExpireTimeout(100);
		handler.setExpireDuration(Duration.ofMillis(10));
		handler.setBeanFactory(mock(BeanFactory.class));
		ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
		taskScheduler.afterPropertiesSet();
		handler.setTaskScheduler(taskScheduler);
		handler.afterPropertiesSet();
		handler.handleMessageInternal(MessageBuilder.withPayload("test").setCorrelationId("test").build());
		handler.start();
		Message<?> receive = discardChannel.receive(10000);
		assertThat(receive).isNotNull();
		await().until(groupStore::getMessageGroupCount, (count) -> count == 0);
		verify(groupStore, atLeast(2)).expireMessageGroups(100);
		taskScheduler.destroy();
	}

}
