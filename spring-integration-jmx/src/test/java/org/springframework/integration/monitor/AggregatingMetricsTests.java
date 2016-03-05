/*
 * Copyright 2015-2016 the original author or authors.
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

package org.springframework.integration.monitor;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.channel.AbstractMessageChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.handler.BridgeHandler;
import org.springframework.integration.handler.ServiceActivatingHandler;
import org.springframework.integration.support.management.AggregatingMessageChannelMetrics;
import org.springframework.integration.support.management.AggregatingMessageHandlerMetrics;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Gary Russell
 * @since 4.2
 *
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext
public class AggregatingMetricsTests {

	@Autowired
	private MessageChannel input;

	@Autowired
	private AbstractMessageChannel delay;

	@Autowired
	private QueueChannel output;

	@Autowired
	private BridgeHandler handler;

	@Autowired
	private ServiceActivatingHandler delayer;

	@Test
	public void testCounts() {
		Message<?> message = new GenericMessage<String>("foo");
		int count = 2000;
		for (int i = 0; i < count; i++) {
			input.send(message);
		}
		assertEquals(count, this.output.getQueueSize());
		assertEquals(count, this.output.getSendCount());
		assertEquals(Long.valueOf(count / 1000).longValue(), this.output.getSendDuration().getCountLong());
		assertEquals(count, this.handler.getHandleCount());
		assertEquals(Long.valueOf(count / 1000).longValue(), this.handler.getDuration().getCountLong());
	}

	@Test
	public void testElapsed() {
		int sampleSize = 2;
		this.delay.configureMetrics(new AggregatingMessageChannelMetrics("foo", sampleSize));
		this.delay.setStatsEnabled(true);
		this.delayer.configureMetrics(new AggregatingMessageHandlerMetrics("bar", sampleSize));
		this.delayer.setStatsEnabled(true);
		GenericMessage<String> message = new GenericMessage<String>("foo");
		int count = 4;
		for (int i = 0; i < count; i++) {
			this.delay.send(message);
		}
		assertEquals(count, this.delay.getSendCount());
		assertEquals(count / sampleSize, this.delay.getSendDuration().getCount());
		assertThat((int) this.delay.getMeanSendDuration() / sampleSize, greaterThanOrEqualTo(50));
		assertEquals(count, this.delayer.getHandleCount());
		assertEquals(count / sampleSize, this.delayer.getDuration().getCount());
		assertThat((int) this.delayer.getMeanDuration() / sampleSize, greaterThanOrEqualTo(50));
	}

	@Test @Ignore
	public void perf() {
		AggregatingMessageHandlerMetrics metrics = new AggregatingMessageHandlerMetrics();
		for (int i = 0; i < 100000000; i++) {
			metrics.afterHandle(metrics.beforeHandle(), true);
		}
	}

}
