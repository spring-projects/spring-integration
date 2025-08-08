/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.jms.request_reply;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.gateway.RequestReplyExchanger;
import org.springframework.integration.jms.ActiveMQMultiContextTests;
import org.springframework.integration.test.condition.LongRunningTest;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.util.StopWatch;

import static org.assertj.core.api.Assertions.assertThat;

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
		try (ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"honor-timeout.xml", getClass())) {

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
