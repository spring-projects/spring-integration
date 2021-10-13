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

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.MessageTimeoutException;
import org.springframework.integration.gateway.RequestReplyExchanger;
import org.springframework.integration.jms.ActiveMQMultiContextTests;
import org.springframework.integration.test.condition.LongRunningTest;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;

/**
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 * @author Ali Moghadam
 */
@LongRunningTest
public class PipelineNamedReplyQueuesJmsTests extends ActiveMQMultiContextTests {

	private final ExecutorService executor = Executors.newFixedThreadPool(30);

	private static final Log logger = LogFactory.getLog(PipelineJmsTests.class);

	@AfterEach
	public void tearDown() {
		this.executor.shutdownNow();
	}

	int requests = 5;

	/**
	 * jms:out(reply-destination-name="pipeline01-01") -> jms:in -> randomTimeoutProcess ->
	 * jms:out -> jms:in
	 */
	@Test
	public void testPipeline1() throws Exception {
		this.test("pipeline-named-queue-01.xml");
	}

	/**
	 * jms:out(reply-destination-name="pipeline02-01") -> jms:in -> randomTimeoutProcess ->
	 * jms:out(reply-destination-name="pipeline02-02") -> jms:in ->
	 * jms:out(reply-destination-name="pipeline02-03") -> jms:in
	 */
	@Test
	public void testPipeline2() throws Exception {
		this.test("pipeline-named-queue-02.xml");
	}

	/**
	 * Same as {@link #testPipeline2()} except all gateways use the same reply queue.
	 * and zero failures expected (no timeouts on server).
	 * jms:out(reply-destination-name="pipeline02a-01") -> jms:in -> zeroTimeoutProcess ->
	 * jms:out(reply-destination-name="pipeline02a-01") -> jms:in ->
	 * jms:out(reply-destination-name="pipeline02a-01") -> jms:in
	 */
	@Test
	public void testPipeline2a() throws Exception {
		int timeouts = this.test("pipeline-named-queue-02a.xml");
		assertThat(timeouts).isEqualTo(0);
	}

	/**
	 * jms:out(reply-destination-name="pipeline03-01", correlation-key="JMSCorrelationID") -> jms:in -> randomTimeoutProcess ->
	 * jms:out(reply-destination-name="pipeline03-02") -> jms:in ->
	 * jms:out(reply-destination-name="pipeline03-03") -> jms:in
	 */
	@Test
	public void testPipeline3() throws Exception {
		this.test("pipeline-named-queue-03.xml");
	}

	/**
	 * Same as {@link #testPipeline3()} except all gateways use the same reply queue.
	 * Ensures the correlation id is not propagated. No timeouts expected.
	 * jms:out(reply-destination-name="pipeline03a-01", correlation-key="JMSCorrelationID") -> jms:in -> zeroTimeoutProcess ->
	 * jms:out(reply-destination-name="pipeline03a-01") -> jms:in ->
	 * jms:out(reply-destination-name="pipeline03a-01") -> jms:in
	 * Ensures reply came from service after third gateway
	 */
	@Test
	public void testPipeline3a() throws Exception {
		int timeouts = this.test("pipeline-named-queue-03a.xml", 50000);
		assertThat(timeouts).isEqualTo(0);
	}

	/**
	 * jms:out(reply-destination-name="pipeline04-01", correlation-key="foo") -> jms:in(correlation-key="foo") -> randomTimeoutProcess ->
	 * jms:out(reply-destination-name="pipeline04-02") -> jms:in
	 */
	@Test
	public void testPipeline4() throws Exception {
		int timeouts = this.test("pipeline-named-queue-04.xml", 30000);
		assertThat(timeouts).isEqualTo(0);
	}

	/**
	 * jms:out(reply-destination-name="pipeline05-01", correlation-key="foo") -> jms:in(correlation-key="foo") -> randomTimeoutProcess ->
	 * jms:out(reply-destination-name="pipeline05-02", correlation-key="JMSCorrelationID") -> jms:in
	 */
	@Test
	public void testPipeline5() throws Exception {
		this.test("pipeline-named-queue-05.xml");
	}

	/**
	 * jms:out(reply-destination-name="pipeline06-01", correlation-key="JMSCorrelationID") -> jms:in -> randomTimeoutProcess ->
	 * jms:out(reply-destination-name="pipeline06-02", correlation-key="foo") -> jms:in(correlation-key="foo")
	 */
	@Test
	public void testPipeline6() throws Exception {
		this.test("pipeline-named-queue-06.xml");
	}

	/**
	 * jms:out(reply-destination-name="pipeline07-01", correlation-key="JMSCorrelationID") -> jms:in -> randomTimeoutProcess ->
	 * jms:out(reply-destination-name="pipeline07-02", correlation-key="foo") -> jms:in(correlation-key="foo")
	 */
	@Test
	public void testPipeline7() throws Exception {
		this.test("pipeline-named-queue-07.xml");
	}

	public int test(String contextConfig) throws Exception {
		return test(contextConfig, 0);
	}

	public int test(String contextConfig, final int offset) throws Exception {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(contextConfig, this.getClass());
		final AtomicInteger successCounter = new AtomicInteger();
		final AtomicInteger timeoutCounter = new AtomicInteger();
		final AtomicInteger failureCounter = new AtomicInteger();
		try {
			final RequestReplyExchanger gateway = context.getBean(RequestReplyExchanger.class);
			final CountDownLatch latch = new CountDownLatch(requests);

			for (int i = 1000000; i < 1000000 + requests * 100000; i += 100000) {
				final int y = i;
				executor.execute(() -> {
					try {
						assertThat(gateway.exchange(new GenericMessage<>(y)).getPayload()).isEqualTo(y + offset);
						successCounter.incrementAndGet();
					}
					catch (MessageTimeoutException e) {
						timeoutCounter.incrementAndGet();
					}
					catch (Throwable t) {
						logger.error("gateway invocation failed", t);
						failureCounter.incrementAndGet();
					}
					finally {
						latch.countDown();
					}
				});
			}
			assertThat(latch.await(120, TimeUnit.SECONDS)).isTrue();
			// technically all we care that its > 0,
			// but reality of this test it has to be something more then 0
			assertThat(successCounter.get() > 1).isTrue();
			assertThat(failureCounter.get()).isEqualTo(0);
			assertThat(successCounter.get() + timeoutCounter.get()).isEqualTo(requests);
			return timeoutCounter.get();
		}
		finally {
			logger.info("Test config: " + contextConfig);
			logger.info("Success: " + successCounter.get());
			logger.info("Timeout: " + timeoutCounter.get());
			logger.info("Failure: " + failureCounter.get());
			if (timeoutCounter.get() > 0 && context.containsBean("capture")) {
				logger.info(context.getBean(Capture.class).messages);
			}
			context.close();
		}
	}

	public static class Capture {

		private final BlockingQueue<String> messages = new LinkedBlockingQueue<>();

		public Message<?> capture(Message<?> message) {
			messages.add("\n[" + Thread.currentThread().getName() + "] " + message);
			return message;
		}

	}

}
