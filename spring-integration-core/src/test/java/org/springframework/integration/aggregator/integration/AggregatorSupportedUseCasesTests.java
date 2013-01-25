/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.springframework.integration.aggregator.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.integration.aggregator.AggregatingMessageHandler;
import org.springframework.integration.aggregator.DefaultAggregatingMessageGroupProcessor;
import org.springframework.integration.aggregator.ReleaseStrategy;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.store.MessageGroup;
import org.springframework.integration.store.MessageGroupStore;
import org.springframework.integration.store.SimpleMessageStore;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;

/**
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 *
 */
public class AggregatorSupportedUseCasesTests {

	private MessageGroupStore store = new SimpleMessageStore(100);

	private ApplicationContext applicationContext = TestUtils.createTestApplicationContext();

	private DefaultAggregatingMessageGroupProcessor processor = new DefaultAggregatingMessageGroupProcessor();

	private AggregatingMessageHandler defaultHandler;

	private QueueChannel outputChannel;

	private QueueChannel discardChannel;

	@Before
	public void setupAggregator() {
		this.defaultHandler = new AggregatingMessageHandler(processor, store);
		this.outputChannel = new QueueChannel();
		this.discardChannel = new QueueChannel();
		this.defaultHandler.setOutputChannel(this.outputChannel);
		this.defaultHandler.setDiscardChannel(this.discardChannel);
		this.defaultHandler.setBeanFactory(applicationContext);
		this.defaultHandler.afterPropertiesSet();
	}

	@Test
	public void waitForAllDefaultReleaseStrategyWithLateArrivals(){
		for (int i = 0; i < 5; i++) {
			this.defaultHandler.handleMessage(MessageBuilder.withPayload(i).setSequenceSize(5).setCorrelationId("A").setSequenceNumber(i).build());
		}
		assertEquals(5, ((List<?>) this.outputChannel.receive(0).getPayload()).size());
		assertNull(this.discardChannel.receive(0));
		assertEquals(0, this.store.getMessageGroup("A").getMessages().size());

		// send another message with the same correlation id and see it in the discard channel
		this.defaultHandler.handleMessage(MessageBuilder.withPayload("foo").setSequenceSize(5).setCorrelationId("A").setSequenceNumber(3).build());
		assertNotNull(this.discardChannel.receive(0));

		// set 'expireGroupsUponCompletion' to 'true' and the messages should start accumulating again
		this.defaultHandler.setExpireGroupsUponCompletion(true);
		this.defaultHandler.handleMessage(MessageBuilder.withPayload("foo").setSequenceSize(5).setCorrelationId("A").setSequenceNumber(3).build());
		assertNull(this.discardChannel.receive(0));
		assertEquals(1, this.store.getMessageGroup("A").getMessages().size());
	}

	@Test
	public void waitForAllCustomReleaseStrategyWithLateArrivals(){
		this.defaultHandler.setReleaseStrategy(new SampleSizeReleaseStrategy());

		for (int i = 0; i < 5; i++) {
			this.defaultHandler.handleMessage(MessageBuilder.withPayload(i).setCorrelationId("A").build());
		}
		assertEquals(5, ((List<?>) this.outputChannel.receive(0).getPayload()).size());
		assertNull(this.discardChannel.receive(0));
		assertEquals(0, this.store.getMessageGroup("A").getMessages().size());

		// send another message with the same correlation id and see it in the discard channel
		this.defaultHandler.handleMessage(MessageBuilder.withPayload("foo").setCorrelationId("A").build());
		assertNotNull(this.discardChannel.receive(0));

		// set 'expireGroupsUponCompletion' to 'true' and the messages should start accumulating again
		this.defaultHandler.setExpireGroupsUponCompletion(true);
		this.defaultHandler.handleMessage(MessageBuilder.withPayload("foo").setCorrelationId("A").build());
		assertNull(this.discardChannel.receive(0));
		assertEquals(1, this.store.getMessageGroup("A").getMessages().size());
	}

	@Test
	public void firstBest(){
		this.defaultHandler.setReleaseStrategy(new FirstBestReleaseStrategy());

		for (int i = 0; i < 5; i++) {
			this.defaultHandler.handleMessage(MessageBuilder.withPayload(i).setCorrelationId("A").build());
		}
		assertEquals(1, ((List<?>) this.outputChannel.receive(0).getPayload()).size());
		assertNotNull(this.discardChannel.receive(0));
		assertNotNull(this.discardChannel.receive(0));
		assertNotNull(this.discardChannel.receive(0));
		assertNotNull(this.discardChannel.receive(0));
	}

	@Test
	public void batchingWithoutLeftovers(){
		this.defaultHandler.setReleaseStrategy(new SampleSizeReleaseStrategy());
		this.defaultHandler.setExpireGroupsUponCompletion(true);

		for (int i = 0; i < 10; i++) {
			this.defaultHandler.handleMessage(MessageBuilder.withPayload(i).setCorrelationId("A").build());
		}
		assertEquals(5, ((List<?>) this.outputChannel.receive(0).getPayload()).size());
		assertEquals(5, ((List<?>) this.outputChannel.receive(0).getPayload()).size());
		assertNull(this.discardChannel.receive(0));
	}

	@Test
	public void batchingWithLeftovers(){
		this.defaultHandler.setReleaseStrategy(new SampleSizeReleaseStrategy());
		this.defaultHandler.setExpireGroupsUponCompletion(true);

		for (int i = 0; i < 12; i++) {
			this.defaultHandler.handleMessage(MessageBuilder.withPayload(i).setCorrelationId("A").build());
		}
		assertEquals(5, ((List<?>) this.outputChannel.receive(0).getPayload()).size());
		assertEquals(5, ((List<?>) this.outputChannel.receive(0).getPayload()).size());
		assertNull(this.discardChannel.receive(0));
		assertEquals(2, this.store.getMessageGroup("A").getMessages().size());
	}

	@Test
	public void testInt2899VerifyMessageStoreCalls() throws InterruptedException {
		MessageGroupStore store = Mockito.spy(this.store);
		this.defaultHandler.setMessageStore(store);

		for (int i = 0; i < 4; i++) {
			this.defaultHandler.handleMessage(MessageBuilder.withPayload(i).setCorrelationId("A").build());
		}
		assertNull(this.outputChannel.receive(0));
		assertNull(this.discardChannel.receive(0));
		assertEquals(1, store.getMessageGroupCount());
		assertEquals(4, store.getMessageGroup("A").getMessages().size());

		this.defaultHandler.setExpireGroupsUponCompletion(true);
		Mockito.verify(store, Mockito.never()).removeMessageGroup(Mockito.any());

		store.completeGroup("A");

		this.defaultHandler.onApplicationEvent(new ContextRefreshedEvent(this.applicationContext));
		this.defaultHandler.handleMessage(MessageBuilder.withPayload(5).setCorrelationId("A").build());

		assertEquals(1, store.getMessageGroupCount());
		assertEquals(1, store.getMessageGroup("A").getMessages().size());
	}

	private class SampleSizeReleaseStrategy implements ReleaseStrategy {

		public boolean canRelease(MessageGroup group) {
			return group.getMessages().size() == 5;
		}

	}

	private class FirstBestReleaseStrategy implements ReleaseStrategy {

		public boolean canRelease(MessageGroup group) {
			return true;
		}

	}

}
