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

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.NullChannel;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.endpoint.AbstractMessageSource;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.support.management.DefaultMessageChannelMetrics;
import org.springframework.integration.support.management.MetricsContext;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Gary Russell
 * @author Artem Bilan
 * @since 4.2
 *
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext
public class MonitorTests {

	@Autowired
	private QueueChannel input;

	@Autowired
	private DirectChannel next;

	@Autowired
	private TestHandler handler;

	@Autowired
	private TestSource source;

	@Autowired
	private PublishSubscribeChannel pubsub;

	@Autowired
	private QueueChannel output;

	@Autowired
	private NullChannel nullChannel;


	@Test
	public void testStats() throws InterruptedException {
		final CountDownLatch afterSendLatch = new CountDownLatch(1);

		DefaultMessageChannelMetrics channelMetrics =
				TestUtils.getPropertyValue(this.next, "channelMetrics", DefaultMessageChannelMetrics.class);
		channelMetrics = Mockito.spy(channelMetrics);

		Mockito.doAnswer(invocation -> {
			Object result = invocation.callRealMethod();
			afterSendLatch.countDown();
			return result;
		}).when(channelMetrics).afterSend(Mockito.any(MetricsContext.class), Mockito.eq(Boolean.TRUE));

		new DirectFieldAccessor(this.next).setPropertyValue("channelMetrics", channelMetrics);

		MessagingTemplate messagingTemplate = new MessagingTemplate(this.input);
		messagingTemplate.setReceiveTimeout(100000);
		Integer active = messagingTemplate.convertSendAndReceive("foo", Integer.class);
		assertEquals(1, active.intValue());
		assertTrue(afterSendLatch.await(10, TimeUnit.SECONDS));
		assertEquals(0, this.handler.getActiveCount());
		assertEquals(1, this.handler.getHandleCount());
		assertThat(this.handler.getDuration().getMax(), greaterThan(99.0));
		assertThat(this.handler.getDuration().getMax(), lessThan(10000.0));
		assertEquals(1, this.input.getSendCount());
		assertEquals(1, this.input.getReceiveCount());
		assertEquals(1, this.next.getSendCount());
		assertThat(this.next.getSendDuration().getMax(), greaterThan(99.0));
		assertThat(this.next.getSendDuration().getMax(), lessThan(10000.0));
		Message<?> fromInbound = this.output.receive(100000);
		assertNotNull(fromInbound);
		assertEquals(0, fromInbound.getPayload());
		fromInbound = this.output.receive(10000);
		assertNotNull(fromInbound);
		assertEquals(1, fromInbound.getPayload());
		assertThat(this.source.getMessageCount(), greaterThanOrEqualTo(2));
		assertThat(this.nullChannel.getSendCount(), greaterThanOrEqualTo(2));
		assertThat(this.pubsub.getSendCount(), greaterThanOrEqualTo(2));
	}

	public static class TestHandler extends AbstractReplyProducingMessageHandler {

		@Override
		protected Object handleRequestMessage(Message<?> requestMessage) {
			try {
				Thread.sleep(100);
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			return getActiveCount();
		}

	}

	public static class TestSource extends AbstractMessageSource<Integer> {

		@Override
		public String getComponentType() {
			return "foo";
		}

		@Override
		protected Object doReceive() {
			return getMessageCount();
		}

	}

}
