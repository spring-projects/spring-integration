/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.integration.channel.config;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.config.TestChannelInterceptor;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @author Dave Syer
 * @author Artem Bilan
 */
@SpringJUnitConfig
@DirtiesContext
public class ThreadLocalChannelParserTests {

	@Autowired @Qualifier("simpleChannel")
	private PollableChannel simpleChannel;

	@Autowired @Qualifier("channelWithInterceptor")
	private PollableChannel channelWithInterceptor;

	@Autowired
	private TestChannelInterceptor interceptor;

	@Test
	public void testSendInAnotherThread() throws Exception {
		simpleChannel.send(new GenericMessage<>("test"));
		ExecutorService otherThreadExecutor = Executors.newSingleThreadExecutor();
		final CountDownLatch latch = new CountDownLatch(1);
		otherThreadExecutor.execute(() -> {
			simpleChannel.send(new GenericMessage<>("crap"));
			latch.countDown();
		});
		assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(simpleChannel.receive(10).getPayload()).isEqualTo("test");
		// Message sent on another thread is not collected here
		assertThat(simpleChannel.receive(1)).isEqualTo(null);
		otherThreadExecutor.shutdown();
	}

	@Test
	public void testReceiveInAnotherThread() throws Exception {
		simpleChannel.send(new GenericMessage<>("test-1.1"));
		simpleChannel.send(new GenericMessage<>("test-1.2"));
		simpleChannel.send(new GenericMessage<>("test-1.3"));
		channelWithInterceptor.send(new GenericMessage<>("test-2.1"));
		channelWithInterceptor.send(new GenericMessage<>("test-2.2"));
		ExecutorService otherThreadExecutor = Executors.newSingleThreadExecutor();
		final List<Object> otherThreadResults = new ArrayList<>();
		final CountDownLatch latch = new CountDownLatch(2);
		otherThreadExecutor.execute(() -> {
			otherThreadResults.add(simpleChannel.receive(0));
			latch.countDown();
		});
		otherThreadExecutor.execute(() -> {
			otherThreadResults.add(channelWithInterceptor.receive(0));
			latch.countDown();
		});
		assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(otherThreadResults.size()).isEqualTo(2);
		assertThat(otherThreadResults.get(0)).isNull();
		assertThat(otherThreadResults.get(1)).isNull();
		assertThat(simpleChannel.receive(0).getPayload()).isEqualTo("test-1.1");
		assertThat(simpleChannel.receive(0).getPayload()).isEqualTo("test-1.2");
		assertThat(simpleChannel.receive(0).getPayload()).isEqualTo("test-1.3");
		assertThat(simpleChannel.receive(0)).isNull();
		assertThat(channelWithInterceptor.receive(0).getPayload()).isEqualTo("test-2.1");
		assertThat(channelWithInterceptor.receive(0).getPayload()).isEqualTo("test-2.2");
		assertThat(channelWithInterceptor.receive(0)).isNull();

		otherThreadExecutor.shutdown();
	}

	@Test
	public void testInterceptor() {
		int before = interceptor.getSendCount();
		channelWithInterceptor.send(new GenericMessage<>("test"));
		assertThat(interceptor.getSendCount()).isEqualTo(before + 1);
	}

}
