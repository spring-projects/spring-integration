/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.integration.aggregator.scenarios;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 *
 */
@SpringJUnitConfig
@DirtiesContext
public class AggregatorWithCustomReleaseStrategyTests {


	@Autowired
	private ExecutorService executor;

	@Autowired
	@Qualifier("aggregationChannelCustomCorrelation")
	private MessageChannel inputChannel;

	@Autowired
	private QueueChannel resultChannel;

	@Autowired
	@Qualifier("in")
	private MessageChannel inChannel;

	@Test
	public void validateSequenceSizeHasNoAffectCustomCorrelator() throws Exception {
		CountDownLatch latch = new CountDownLatch(1800);

		for (int i = 0; i < 600; i++) {
			final int counter = i;
			this.executor.execute(() -> {
				this.inputChannel.send(MessageBuilder.withPayload("foo").
						setHeader("correlation", "foo" + counter).build());
				latch.countDown();
			});
			this.executor.execute(() -> {
				this.inputChannel.send(MessageBuilder.withPayload("bar").
						setHeader("correlation", "foo" + counter).build());
				latch.countDown();
			});
			this.executor.execute(() -> {
				this.inputChannel.send(MessageBuilder.withPayload("baz").
						setHeader("correlation", "foo" + counter).build());
				latch.countDown();
			});
		}

		assertThat(latch.await(120, TimeUnit.SECONDS)).as("Sends failed to complete: " + latch.getCount() + " remain")
				.isTrue();

		Message<?> message = this.resultChannel.receive(1000);
		int counter = 0;
		while (message != null) {
			counter++;
			message = this.resultChannel.receive(1000);
		}
		assertThat(counter).isEqualTo(600);
	}

	@Test
	public void validateSequenceSizeHasNoAffectWithSplitter() throws Exception {
		final CountDownLatch latch = new CountDownLatch(1800);

		for (int i = 0; i < 600; i++) {
			this.executor.execute(() -> {
				this.inChannel.send(MessageBuilder.withPayload(new Integer[]{ 1, 2, 3, 4, 5, 6, 7, 8 }).build());
				latch.countDown();
			});
			executor.execute(() -> {
				this.inChannel.send(MessageBuilder.withPayload(new Integer[]{ 9, 10, 11, 12, 13, 14, 15, 16 }).build());
				latch.countDown();
			});
			executor.execute(() -> {
				this.inChannel.send(MessageBuilder.withPayload(new Integer[]{ 17, 18, 19, 20, 21, 22, 23, 24 }).build());
				latch.countDown();
			});
		}

		assertThat(latch.await(60, TimeUnit.SECONDS)).as("Sends failed to complete: " + latch.getCount() + " remain")
				.isTrue();

		Message<?> message = this.resultChannel.receive(1000);
		int counter = 0;
		while (message != null && ++counter < 7200) {
			message = this.resultChannel.receive(1000);
		}
		assertThat(counter).isEqualTo(7200);
	}

}
