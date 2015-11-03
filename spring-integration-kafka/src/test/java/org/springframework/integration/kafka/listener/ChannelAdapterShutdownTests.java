/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package org.springframework.integration.kafka.listener;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Rule;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.kafka.rule.KafkaEmbedded;
import org.springframework.integration.kafka.rule.KafkaRule;
import org.springframework.messaging.Message;
import org.springframework.util.StopWatch;

/**
 * @author Marius Bogoevici
 */
public class ChannelAdapterShutdownTests extends AbstractMessageListenerContainerTests {

	@Rule
	public final KafkaRule kafkaEmbeddedBrokerRule = new KafkaEmbedded(1);

	@Override
	public KafkaRule getKafkaRule() {
		return this.kafkaEmbeddedBrokerRule;
	}

	@Test
	public void testShutdownNotBlocking() throws Exception {

		System.setProperty("kafka.test.topic", TEST_TOPIC);

		int partitionCount = 1;

		createTopic(TEST_TOPIC, partitionCount, 1, 1);

		createMessageSender("none").send(createMessages(100, TEST_TOPIC, partitionCount));

		ClassPathXmlApplicationContext context =
				new ClassPathXmlApplicationContext("ChannelAdapterShutdownTests-context.xml",
						ChannelAdapterShutdownTests.class);
		TripLatchService tripLatchService = context.getBean(TripLatchService.class);
		assertTrue("Message reception hasn't started", tripLatchService.getLatch().await(10, TimeUnit.SECONDS));
		StopWatch stopWatch = new StopWatch();
		stopWatch.start();
		context.stop();
		stopWatch.stop();
		long shutDownTime = stopWatch.getTotalTimeMillis();
		if (shutDownTime > 25000) {
			fail("Context stopping only on timeout");
		}
	}

	/**
	 * A simple gateway latch holder that trips when a message passes through. Used to detect that the message listener
	 * container has started receiving messages
	 */
	public static class TripLatchService {

		private final CountDownLatch latch = new CountDownLatch(1);

		public CountDownLatch getLatch() {
			return latch;
		}

		@ServiceActivator
		public Message<?> trip(Message<?> message) {
			latch.countDown();
			return message;
		}
	}
}
