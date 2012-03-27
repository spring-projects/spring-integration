/*
 * Copyright 2002-2012 the original author or authors.
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
package org.springframework.integration.aggregator.scenarios;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.support.MessageBuilder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Oleg Zhurakousky
 *
 */
public class AggregatorWithCustomReleaseStrategyTests {

	@Test
	public void validateSequenceSizeHasNoAffect() throws Exception{
		ApplicationContext context =
				new ClassPathXmlApplicationContext("aggregator-with-custom-release-strategy.xml", this.getClass());
		final MessageChannel inputChannel = context.getBean("in", MessageChannel.class);
		QueueChannel resultChannel = context.getBean("resultChannel", QueueChannel.class);

		final CountDownLatch latch = new CountDownLatch(2);

		new Thread(new Runnable() {
			public void run() {
				inputChannel.send(MessageBuilder.withPayload(new Integer[]{1, 2, 3, 4, 5, 6, 7, 8}).
						setHeader("correlation", "foo").build());
				latch.countDown();
			}
		}).start();

		new Thread(new Runnable() {
			public void run() {
				inputChannel.send(MessageBuilder.withPayload(new Integer[]{10, 20, 30, 40, 50, 60, 70, 80}).
						setHeader("correlation", "foo").build());
				latch.countDown();
			}
		}).start();

		assertTrue("Sends failed to complete", latch.await(10, TimeUnit.SECONDS));

		Message<?> message = resultChannel.receive(1000);
		int counter = 0;
		while(message != null){
			counter++;
			message = resultChannel.receive(1000);
		}
		assertEquals(8, counter);
	}
}
