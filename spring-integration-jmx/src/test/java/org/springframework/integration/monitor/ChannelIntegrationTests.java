/*
 * Copyright 2009-2020 the original author or authors.
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

package org.springframework.integration.monitor;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.support.context.NamedComponent;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Dave Syer
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.0
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext
public class ChannelIntegrationTests {

	@Autowired
	private MessageChannel requests;

	@Autowired
	private PollableChannel intermediate;

	@Autowired
	private PollableChannel sourceChannel;

	@Autowired
	private IntegrationMBeanExporter messageChannelsMonitor;

	@Test
	public void testMessageChannelStatistics() {
		this.requests.send(new GenericMessage<>("foo"));

		String intermediateChannelName = ((NamedComponent) this.intermediate).getBeanName();

		assertThat(messageChannelsMonitor.getChannelSendCount(intermediateChannelName)).isEqualTo(1);

		double rate =
				messageChannelsMonitor.getChannelSendRate(((NamedComponent) this.requests).getBeanName()).getMean();
		assertThat(rate).as("No statistics for requests channel").isGreaterThanOrEqualTo(0);

		rate = messageChannelsMonitor.getChannelSendRate(intermediateChannelName).getMean();
		assertThat(rate).as("No statistics for intermediate channel").isGreaterThanOrEqualTo(0);

		assertThat(intermediate.receive(100L)).isNotNull();
		assertThat(messageChannelsMonitor.getChannelReceiveCount(intermediateChannelName)).isEqualTo(1);

		requests.send(new GenericMessage<>("foo"));
		try {
			requests.send(new GenericMessage<>("foo"));
		}
		catch (@SuppressWarnings("unused") MessageDeliveryException e) {
		}

		assertThat(messageChannelsMonitor.getChannelSendCount(intermediateChannelName)).isEqualTo(3);

		assertThat(messageChannelsMonitor.getChannelSendErrorCount(intermediateChannelName)).isEqualTo(1);

		assertThat(messageChannelsMonitor.getChannelMetrics(intermediateChannelName)).isSameAs(intermediate);

		@SuppressWarnings("deprecation")
		org.springframework.integration.support.management.BaseHandlerMetrics handlerMetrics = messageChannelsMonitor
				.getHandlerMetrics("bridge");

		assertThat(handlerMetrics.handleCount()).isEqualTo(3);
		assertThat(handlerMetrics.errorCount()).isEqualTo(1);

		assertThat(this.sourceChannel.receive(10000)).isNotNull();

		assertThat(messageChannelsMonitor.getSourceMessageCount("source")).isGreaterThan(0);
		assertThat(messageChannelsMonitor.getSourceMetrics("source").getMessageCount()).isGreaterThan(0);
	}

}
