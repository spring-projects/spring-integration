/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.integration.jms.request_reply;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.gateway.RequestReplyExchanger;
import org.springframework.integration.jms.ActiveMQMultiContextTests;
import org.springframework.integration.test.condition.LongRunningTest;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.util.StopWatch;

/**
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 */
@LongRunningTest
public class MiscellaneousTests extends ActiveMQMultiContextTests {

	/**
	 * Asserts that receive-timeout is honored even if
	 * requests (once in process), takes less then receive-timeout value
	 * when requests are queued up (e.g., single consumer receiver)
	 */
	@Test
	public void testTimeoutHonoringWhenRequestsQueuedUp() throws Exception {
		try (ClassPathXmlApplicationContext context =
				new ClassPathXmlApplicationContext("honor-timeout.xml", getClass())) {

			final RequestReplyExchanger gateway = context.getBean(RequestReplyExchanger.class);
			final CountDownLatch latch = new CountDownLatch(3);
			final AtomicInteger replies = new AtomicInteger();
			StopWatch stopWatch = new StopWatch();
			stopWatch.start();
			for (int i = 0; i < 3; i++) {
				this.exchange(latch, gateway, replies);
			}
			latch.await();
			stopWatch.stop();
			assertThat(stopWatch.getTotalTimeMillis()).isLessThanOrEqualTo(18000);
			assertThat(replies.get()).isEqualTo(1);
		}
	}


	private void exchange(CountDownLatch latch, RequestReplyExchanger gateway, AtomicInteger replies) {
		new Thread(() -> {
			try {
				gateway.exchange(new GenericMessage<>(""));
				replies.incrementAndGet();
			}
			catch (Exception e) {
				//ignore
			}
			latch.countDown();
		}).start();
	}

}
