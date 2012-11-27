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
package org.springframework.integration.aggregator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.store.MessageGroup;
import org.springframework.integration.store.MessageGroupStore;
import org.springframework.integration.store.SimpleMessageStore;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;

/**
 * @author Gary Russell
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

			public boolean send(Message<?> message) {
				return this.send(message, 0);
			}
		});
		handler.setReleaseStrategy(new ReleaseStrategy() {

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
			public boolean send(Message<?> message, long timeout) {
				outputMessages.add(message);
				return true;
			}

			public boolean send(Message<?> message) {
				return this.send(message, 0);
			}
		});
		handler.setReleaseStrategy(new ReleaseStrategy() {

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

}
