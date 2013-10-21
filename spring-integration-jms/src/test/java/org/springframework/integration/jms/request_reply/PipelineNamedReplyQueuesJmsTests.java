/*
 * Copyright 2002-2013 the original author or authors.
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
package org.springframework.integration.jms.request_reply;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.MessageTimeoutException;
import org.springframework.integration.gateway.RequestReplyExchanger;
import org.springframework.integration.jms.ActiveMQMultiContextTests;
import org.springframework.integration.jms.config.ActiveMqTestUtils;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.integration.test.support.LongRunningIntegrationTest;
/**
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 * @author Ali Moghadam
 */
public class PipelineNamedReplyQueuesJmsTests extends ActiveMQMultiContextTests {

	private final Executor executor = Executors.newFixedThreadPool(30);

	private static final Log logger = LogFactory.getLog(PipelineJmsTests.class);

	@Rule
	public LongRunningIntegrationTest longTests = new LongRunningIntegrationTest();

	@Before
	public void setLogLevel() {
		LogManager.getLogger(getClass()).setLevel(Level.INFO);
	}

	int requests = 50;

	/**
	 * jms:out(reply-destination-name="pipeline01-01") -> jms:in -> randomTimeoutProcess ->
	 * jms:out -> jms:in
	 */
	@Test
	public void testPipeline1() throws Exception{
		this.test("pipeline-named-queue-01.xml");
	}

	/**
	 * jms:out(reply-destination-name="pipeline02-01") -> jms:in -> randomTimeoutProcess ->
	 * jms:out(reply-destination-name="pipeline02-02") -> jms:in ->
	 * jms:out(reply-destination-name="pipeline02-03") -> jms:in
	 */
	@Test
	public void testPipeline2() throws Exception{
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
	public void testPipeline2a() throws Exception{
		int timeouts = this.test("pipeline-named-queue-02a.xml");
		assertEquals(0, timeouts);
	}

	/**
	 * jms:out(reply-destination-name="pipeline03-01", correlation-key="JMSCorrelationID") -> jms:in -> randomTimeoutProcess ->
	 * jms:out(reply-destination-name="pipeline03-02") -> jms:in ->
	 * jms:out(reply-destination-name="pipeline03-03") -> jms:in
	 */
	@Test
	public void testPipeline3() throws Exception{
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
	public void testPipeline3a() throws Exception{
		int timeouts = this.test("pipeline-named-queue-03a.xml", 20000);
		assertEquals(0, timeouts);
	}

	/**
	 * jms:out(reply-destination-name="pipeline04-01", correlation-key="foo") -> jms:in(correlation-key="foo") -> randomTimeoutProcess ->
	 * jms:out(reply-destination-name="pipeline04-02") -> jms:in
	 */
	@Test
	public void testPipeline4() throws Exception{
		this.test("pipeline-named-queue-04.xml");
	}

	/**
	 * jms:out(reply-destination-name="pipeline05-01", correlation-key="foo") -> jms:in(correlation-key="foo") -> randomTimeoutProcess ->
	 * jms:out(reply-destination-name="pipeline05-02", correlation-key="JMSCorrelationID") -> jms:in
	 */
	@Test
	public void testPipeline5() throws Exception{
		this.test("pipeline-named-queue-05.xml");
	}

	/**
	 * jms:out(reply-destination-name="pipeline06-01", correlation-key="JMSCorrelationID") -> jms:in -> randomTimeoutProcess ->
	 * jms:out(reply-destination-name="pipeline06-02", correlation-key="foo") -> jms:in(correlation-key="foo")
	 */
	@Test
	public void testPipeline6() throws Exception{
		this.test("pipeline-named-queue-06.xml");
	}

	/**
	 * jms:out(reply-destination-name="pipeline07-01", correlation-key="JMSCorrelationID") -> jms:in -> randomTimeoutProcess ->
	 * jms:out(reply-destination-name="pipeline07-02", correlation-key="foo") -> jms:in(correlation-key="foo")
	 */
	@Test
	public void testPipeline7() throws Exception{
		this.test("pipeline-named-queue-07.xml");
	}

	public int test(String contextConfig) throws Exception {
		return test(contextConfig, 0);
	}

	public int test(String contextConfig, final int offset) throws Exception {
		ActiveMqTestUtils.prepare();
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(contextConfig, this.getClass());
		final AtomicInteger successCounter = new AtomicInteger();
		final AtomicInteger timeoutCounter = new AtomicInteger();
		final AtomicInteger failureCounter = new AtomicInteger();
		try {
			final RequestReplyExchanger gateway = context.getBean(RequestReplyExchanger.class);
			final CountDownLatch latch = new CountDownLatch(requests);

			for (int i = 0; i < requests; i++) {
				final int y = i;
				executor.execute(new Runnable() {
					public void run() {
						try {
							assertEquals(y + offset, gateway.exchange(new GenericMessage<Integer>(y)).getPayload());
							successCounter.incrementAndGet();
						} catch (MessageTimeoutException e) {
							timeoutCounter.incrementAndGet();
						} catch (Throwable t) {
							t.printStackTrace();
							failureCounter.incrementAndGet();
						} finally {
							latch.countDown();
						}
					}
				});
			}
			assertTrue(latch.await(120, TimeUnit.SECONDS));
			// technically all we care that its > 0,
			// but reality of this test it has to be something more then 0
			assertTrue(successCounter.get() > 10);
			assertEquals(0, failureCounter.get());
			assertEquals(requests, successCounter.get() + timeoutCounter.get());
			return timeoutCounter.get();
		}
		finally {
			logger.info("Test config: " + contextConfig);
			logger.info("Success: " + successCounter.get());
			logger.info("Timeout: " + timeoutCounter.get());
			logger.info("Failure: " + failureCounter.get());
			context.destroy();
		}
	}
}
