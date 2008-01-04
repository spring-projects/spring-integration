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

package org.springframework.integration.bus;

import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

import org.springframework.integration.channel.PointToPointChannel;
import org.springframework.integration.endpoint.GenericMessageEndpoint;
import org.springframework.integration.endpoint.MessageEndpoint;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.StringMessage;

/**
 * @author Mark Fisher
 */
public class UnicastMessageDispatcherTests {

	@Test
	public void testDispatcherSendsToExactlyOneEndpoint() throws InterruptedException {
		final AtomicBoolean endpoint1Received = new AtomicBoolean();
		final AtomicBoolean endpoint2Received = new AtomicBoolean();
		final CountDownLatch latch = new CountDownLatch(1);
		MessageEndpoint<String> endpoint1 = new GenericMessageEndpoint<String>() {
			@Override
			public void messageReceived(Message<String> message) {
				endpoint1Received.set(true);
				latch.countDown();
			}
		};
		MessageEndpoint<String> endpoint2 = new GenericMessageEndpoint<String>() {
			@Override
			public void messageReceived(Message<String> message) {
				endpoint2Received.set(true);
				latch.countDown();
			}
		};
		ConsumerPolicy policy = new ConsumerPolicy();
		PointToPointChannel channel = new PointToPointChannel();
		channel.send(new StringMessage(1, "test"));
		MessageRetriever retriever = new ChannelPollingMessageRetriever(channel, policy);
		UnicastMessageDispatcher dispatcher = new UnicastMessageDispatcher(retriever, policy);
		dispatcher.addExecutor(new MessageReceivingExecutor(endpoint1, 1, 1));
		dispatcher.addExecutor(new MessageReceivingExecutor(endpoint2, 1, 1));
		dispatcher.dispatch();
		latch.await(100, TimeUnit.MILLISECONDS);
		assertTrue("exactly one endpoint should have received message",
				endpoint1Received.get() ^ endpoint2Received.get());
	}

}
