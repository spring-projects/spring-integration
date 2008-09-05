/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.integration.endpoint;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import org.springframework.integration.bus.DefaultMessageBus;
import org.springframework.integration.dispatcher.PollingDispatcher;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageTarget;
import org.springframework.integration.message.PollableSource;
import org.springframework.integration.message.StringMessage;
import org.springframework.integration.scheduling.PollingSchedule;

/**
 * @author Mark Fisher
 */
public class MessagingBridgeTests {

	@Test
	public void simplePassThrough() throws InterruptedException {
		final CountDownLatch latch = new CountDownLatch(1);
		DefaultMessageBus bus = new DefaultMessageBus();
		MessagingBridge bridge = new MessagingBridge(new MessageTarget() {
			public boolean send(Message<?> message) {
				latch.countDown();
				return true;
			}
		});
		bridge.setBeanName("bridge");
		PollableSource<String> source = new PollableSource<String>() {
			public Message<String> receive() {
				return new StringMessage("test");
			}
		};
		PollingDispatcher poller = new PollingDispatcher(source, new PollingSchedule(1000));
		poller.setMaxMessagesPerPoll(1);
		bridge.setSource(poller);
		bus.registerEndpoint(bridge);
		bus.start();
		latch.await(1, TimeUnit.SECONDS);
		bus.stop();
		assertEquals(0, latch.getCount());
	}

}
