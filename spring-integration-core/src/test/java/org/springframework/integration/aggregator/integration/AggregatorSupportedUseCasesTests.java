/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.integration.aggregator.integration;

import java.util.List;

import org.junit.Test;

import org.springframework.integration.aggregator.AggregatingMessageHandler;
import org.springframework.integration.aggregator.DefaultAggregatingMessageGroupProcessor;
import org.springframework.integration.aggregator.ReleaseStrategy;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.store.MessageGroup;
import org.springframework.integration.store.MessageGroupStore;
import org.springframework.integration.store.SimpleMessageStore;
import org.springframework.integration.support.MessageBuilder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 *
 */
public class AggregatorSupportedUseCasesTests {

	private final MessageGroupStore store = new SimpleMessageStore(100);

	private final DefaultAggregatingMessageGroupProcessor processor = new DefaultAggregatingMessageGroupProcessor();

	private final AggregatingMessageHandler defaultHandler = new AggregatingMessageHandler(processor, store);

	@Test
	public void waitForAllDefaultReleaseStrategyWithLateArrivals() {
		QueueChannel outputChannel = new QueueChannel();
		QueueChannel discardChannel = new QueueChannel();
		defaultHandler.setOutputChannel(outputChannel);
		defaultHandler.setDiscardChannel(discardChannel);

		for (int i = 0; i < 5; i++) {
			defaultHandler.handleMessage(MessageBuilder.withPayload(i).setSequenceSize(5).setCorrelationId("A").setSequenceNumber(i).build());
		}
		assertThat(((List<?>) outputChannel.receive(0).getPayload()).size()).isEqualTo(5);
		assertThat(discardChannel.receive(0)).isNull();
		assertThat(store.getMessageGroup("A").getMessages().size()).isEqualTo(0);

		// send another message with the same correlation id and see it in the discard channel
		defaultHandler.handleMessage(MessageBuilder.withPayload("foo").setSequenceSize(5).setCorrelationId("A").setSequenceNumber(3).build());
		assertThat(discardChannel.receive(0)).isNotNull();

		// expireMessageGroups from aggregator MessageStore and the messages should start accumulating again
		store.expireMessageGroups(0);
		defaultHandler.handleMessage(MessageBuilder.withPayload("foo").setSequenceSize(5).setCorrelationId("A").setSequenceNumber(3).build());
		assertThat(discardChannel.receive(0)).isNull();
		assertThat(store.getMessageGroup("A").getMessages().size()).isEqualTo(1);
	}

	@Test
	public void waitForAllCustomReleaseStrategyWithLateArrivals() {
		QueueChannel outputChannel = new QueueChannel();
		QueueChannel discardChannel = new QueueChannel();
		defaultHandler.setOutputChannel(outputChannel);
		defaultHandler.setDiscardChannel(discardChannel);
		defaultHandler.setReleaseStrategy(new SampleSizeReleaseStrategy());

		for (int i = 0; i < 5; i++) {
			defaultHandler.handleMessage(MessageBuilder.withPayload(i).setCorrelationId("A").build());
		}
		assertThat(((List<?>) outputChannel.receive(0).getPayload()).size()).isEqualTo(5);
		assertThat(discardChannel.receive(0)).isNull();
		assertThat(store.getMessageGroup("A").getMessages().size()).isEqualTo(0);

		// send another message with the same correlation id and see it in the discard channel
		defaultHandler.handleMessage(MessageBuilder.withPayload("foo").setCorrelationId("A").build());
		assertThat(discardChannel.receive(0)).isNotNull();

		// expireMessageGroups from aggregator MessageStore and the messages should start accumulating again
		store.expireMessageGroups(0);
		defaultHandler.handleMessage(MessageBuilder.withPayload("foo").setCorrelationId("A").build());
		assertThat(discardChannel.receive(0)).isNull();
		assertThat(store.getMessageGroup("A").getMessages().size()).isEqualTo(1);
	}

	@Test
	public void firstBest() {
		QueueChannel outputChannel = new QueueChannel();
		QueueChannel discardChannel = new QueueChannel();
		defaultHandler.setOutputChannel(outputChannel);
		defaultHandler.setDiscardChannel(discardChannel);
		defaultHandler.setReleaseStrategy(new FirstBestReleaseStrategy());

		for (int i = 0; i < 5; i++) {
			defaultHandler.handleMessage(MessageBuilder.withPayload(i).setCorrelationId("A").build());
		}
		assertThat(((List<?>) outputChannel.receive(0).getPayload()).size()).isEqualTo(1);
		assertThat(discardChannel.receive(0)).isNotNull();
		assertThat(discardChannel.receive(0)).isNotNull();
		assertThat(discardChannel.receive(0)).isNotNull();
		assertThat(discardChannel.receive(0)).isNotNull();
	}

	@Test
	public void batchingWithoutLeftovers() {
		QueueChannel outputChannel = new QueueChannel();
		QueueChannel discardChannel = new QueueChannel();
		defaultHandler.setOutputChannel(outputChannel);
		defaultHandler.setDiscardChannel(discardChannel);
		defaultHandler.setReleaseStrategy(new SampleSizeReleaseStrategy());
		defaultHandler.setExpireGroupsUponCompletion(true);

		for (int i = 0; i < 10; i++) {
			defaultHandler.handleMessage(MessageBuilder.withPayload(i).setCorrelationId("A").build());
		}
		assertThat(((List<?>) outputChannel.receive(0).getPayload()).size()).isEqualTo(5);
		assertThat(((List<?>) outputChannel.receive(0).getPayload()).size()).isEqualTo(5);
		assertThat(discardChannel.receive(0)).isNull();
	}

	@Test
	public void batchingWithLeftovers() {
		QueueChannel outputChannel = new QueueChannel();
		QueueChannel discardChannel = new QueueChannel();
		defaultHandler.setOutputChannel(outputChannel);
		defaultHandler.setDiscardChannel(discardChannel);
		defaultHandler.setReleaseStrategy(new SampleSizeReleaseStrategy());
		defaultHandler.setExpireGroupsUponCompletion(true);

		for (int i = 0; i < 12; i++) {
			defaultHandler.handleMessage(MessageBuilder.withPayload(i).setCorrelationId("A").build());
		}
		assertThat(((List<?>) outputChannel.receive(0).getPayload()).size()).isEqualTo(5);
		assertThat(((List<?>) outputChannel.receive(0).getPayload()).size()).isEqualTo(5);
		assertThat(discardChannel.receive(0)).isNull();
		assertThat(store.getMessageGroup("A").getMessages().size()).isEqualTo(2);
	}

	private class SampleSizeReleaseStrategy implements ReleaseStrategy {

		SampleSizeReleaseStrategy() {
			super();
		}

		@Override
		public boolean canRelease(MessageGroup group) {
			return group.getMessages().size() == 5;
		}

	}

	private class FirstBestReleaseStrategy implements ReleaseStrategy {

		FirstBestReleaseStrategy() {
			super();
		}

		@Override
		public boolean canRelease(MessageGroup group) {
			return true;
		}

	}

}
