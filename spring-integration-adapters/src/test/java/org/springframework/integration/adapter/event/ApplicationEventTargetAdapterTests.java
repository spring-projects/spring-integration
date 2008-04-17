/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.integration.adapter.event;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.integration.bus.MessageBus;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.channel.SimpleChannel;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.message.StringMessage;
import org.springframework.integration.scheduling.Subscription;

/**
 * @author Mark Fisher
 */
public class ApplicationEventTargetAdapterTests {

	@Test
	public void testSendingEvent() throws InterruptedException {
		final CountDownLatch latch = new CountDownLatch(1);
		ApplicationEventPublisher publisher = new ApplicationEventPublisher() {
			public void publishEvent(ApplicationEvent event) {
				latch.countDown();
			}
		};
		MessageChannel channel = new SimpleChannel();
		ApplicationEventTargetAdapter adapter = new ApplicationEventTargetAdapter();
		adapter.setApplicationEventPublisher(publisher);
		MessageBus bus = new MessageBus();
		bus.registerChannel("channel", channel);
		bus.registerTarget("adapter", adapter, new Subscription(channel));
		bus.start();
		assertEquals(1, latch.getCount());
		channel.send(new StringMessage("123", "testing"));
		latch.await(100, TimeUnit.MILLISECONDS);
		assertEquals(0, latch.getCount());
		bus.stop();
	}

}
