/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.integration.channel.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.config.TestChannelInterceptor;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Mark Fisher
 * @author Dave Syer
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class ThreadLocalChannelParserTests {

	@Autowired @Qualifier("simpleChannel")
	private PollableChannel simpleChannel;

	@Autowired @Qualifier("channelWithInterceptor")
	private PollableChannel channelWithInterceptor;

	@Autowired
	private TestChannelInterceptor interceptor;

	@Test
	public void testSendInAnotherThread() throws Exception {
		simpleChannel.send(new GenericMessage<String>("test"));
		Executor otherThreadExecutor = Executors.newSingleThreadExecutor();
		final CountDownLatch latch = new CountDownLatch(1);
		otherThreadExecutor.execute(new Runnable() {
			public void run() {
				simpleChannel.send(new GenericMessage<String>("crap"));
				latch.countDown();
			}
		});
		latch.await(1, TimeUnit.SECONDS);
		assertEquals("test", simpleChannel.receive(10).getPayload());
		// Message sent on another thread is not collected here
		assertEquals(null, simpleChannel.receive(10));
	}

	@Test
	public void testReceiveInAnotherThread() throws Exception {
		simpleChannel.send(new GenericMessage<String>("test-1.1"));
		simpleChannel.send(new GenericMessage<String>("test-1.2"));
		simpleChannel.send(new GenericMessage<String>("test-1.3"));
		channelWithInterceptor.send(new GenericMessage<String>("test-2.1"));
		channelWithInterceptor.send(new GenericMessage<String>("test-2.2"));
		Executor otherThreadExecutor = Executors.newSingleThreadExecutor();
		final List<Object> otherThreadResults = new ArrayList<Object>();
		final CountDownLatch latch = new CountDownLatch(2);
		otherThreadExecutor.execute(new Runnable() {
			public void run() {
				otherThreadResults.add(simpleChannel.receive(0));
				latch.countDown();
			}
		});
		otherThreadExecutor.execute(new Runnable() {
			public void run() {
				otherThreadResults.add(channelWithInterceptor.receive(0));
				latch.countDown();
			}
		});
		latch.await(1, TimeUnit.SECONDS);
		assertEquals(2, otherThreadResults.size());
		assertNull(otherThreadResults.get(0));
		assertNull(otherThreadResults.get(1));
		assertEquals("test-1.1", simpleChannel.receive(0).getPayload());
		assertEquals("test-1.2", simpleChannel.receive(0).getPayload());
		assertEquals("test-1.3", simpleChannel.receive(0).getPayload());
		assertNull(simpleChannel.receive(0));
		assertEquals("test-2.1", channelWithInterceptor.receive(0).getPayload());
		assertEquals("test-2.2", channelWithInterceptor.receive(0).getPayload());
		assertNull(channelWithInterceptor.receive(0));
	}

	@Test
	public void testInterceptor() {
		int before = interceptor.getSendCount();
		channelWithInterceptor.send(new GenericMessage<String>("test"));
		assertEquals(before+1, interceptor.getSendCount());
	}

}
