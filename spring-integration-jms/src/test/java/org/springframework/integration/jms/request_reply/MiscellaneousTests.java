package org.springframework.integration.jms.request_reply;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.gateway.RequestReplyExchanger;
import org.springframework.integration.jms.config.ActiveMqTestUtils;
import org.springframework.integration.message.GenericMessage;
import org.springframework.util.StopWatch;

public class MiscellaneousTests {

	/**
	 * jms:out -> jms:in -> randomTimeoutProcess ->
	 * jms:out -> jms:in
	 * All reply queues are TEMPORARY
	 */
	@Test
	public void testTimeoutHonoringWhenRequestsQueuedUp() throws Exception{
		ActiveMqTestUtils.prepare();
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("honor-timeout.xml", this.getClass());
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
		assertTrue(stopWatch.getTotalTimeMillis() <= 11000);
		assertEquals(1, replies.get());
	}


	private void exchange(final CountDownLatch latch, final RequestReplyExchanger gateway, final AtomicInteger replies) {
		new Thread(new Runnable() {
			public void run() {
				try {
					gateway.exchange(new GenericMessage<String>(""));
					replies.incrementAndGet();
				} catch (Exception e) {
					//ignore
				}
				latch.countDown();
			}
		}).start();
	}
}
