/*
 * Copyright 2002-2020 the original author or authors.
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.MessageTimeoutException;
import org.springframework.integration.gateway.RequestReplyExchanger;
import org.springframework.integration.jms.ActiveMQMultiContextTests;
import org.springframework.integration.jms.config.ActiveMqTestUtils;
import org.springframework.integration.test.support.LongRunningIntegrationTest;
import org.springframework.messaging.support.GenericMessage;
/**
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Ali Moghadam
 */
public class PipelineJmsTests extends ActiveMQMultiContextTests {

	private final ExecutorService executor = Executors.newFixedThreadPool(30);

	private static final Log logger = LogFactory.getLog(PipelineJmsTests.class);

	@Rule
	public LongRunningIntegrationTest longTests = new LongRunningIntegrationTest();

	@After
	public void tearDown() {
		this.executor.shutdownNow();
	}

	int requests = 5;

	/**
	 * jms:out -> jms:in -> randomTimeoutProcess ->
	 * jms:out -> jms:in
	 * All reply queues are TEMPORARY
	 */
	@Test
	public void testPipeline1() throws Exception {
		this.test("pipeline-01.xml");
	}

	/**
	 * jms:out(correlation-key="JMSCorrelationID") -> jms:in -> randomTimeoutProcess ->
	 * jms:out -> jms:in
	 * All reply queues are TEMPORARY
	 */
	@Test
	public void testPipeline2() throws Exception {
		this.test("pipeline-02.xml");
	}

	/**
	 * jms:out(correlation-key="JMSCorrelationID") -> jms:in -> randomTimeoutProcess ->
	 * jms:out(correlation-key="JMSCorrelationID") -> jms:in
	 * All reply queues are TEMPORARY
	 */
	@Test
	public void testPipeline3() throws Exception {
		this.test("pipeline-03.xml");
	}

	/**
	 * jms:out -> jms:in -> randomTimeoutProcess ->
	 * jms:out(correlation-key="JMSCorrelationID") -> jms:in
	 * All reply queues are TEMPORARY
	 */
	@Test
	public void testPipeline4() throws Exception {
		this.test("pipeline-04.xml");
	}

	/**
	 * jms:out(correlation-key="foo") -> jms:in(correlation-key="foo") -> randomTimeoutProcess ->
	 * jms:out -> jms:in
	 * All reply queues are TEMPORARY
	 */
	@Test
	public void testPipeline5() throws Exception {
		this.test("pipeline-05.xml");
	}

	/**
	 * jms:out -> jms:in -> randomTimeoutProcess ->
	 * jms:out(correlation-key="foo") -> jms:in(correlation-key="foo")
	 * All reply queues are TEMPORARY
	 */
	@Test
	public void testPipeline6() throws Exception {
		this.test("pipeline-06.xml");
	}

	/**
	 * jms:out(correlation-key="JMSCorrelationID") -> jms:in -> randomTimeoutProcess ->
	 * jms:out(correlation-key="foo") -> jms:in(correlation-key="foo")
	 * All reply queues are TEMPORARY
	 */
	@Test
	public void testPipeline7() throws Exception {
		this.test("pipeline-07.xml");
	}

	/**
	 * jms:out(correlation-key="foo") -> jms:in(correlation-key="foo") -> randomTimeoutProcess ->
	 * jms:out(correlation-key="JMSCorrelationID") -> jms:in
	 * All reply queues are TEMPORARY
	 */
	@Test
	public void testPipeline8() throws Exception {
		this.test("pipeline-08.xml");
	}

	/**
	 * jms:out(correlation-key="foo") -> jms:in(correlation-key="foo") -> randomTimeoutProcess ->
	 * jms:out(correlation-key="bar") -> jms:in(correlation-key="bar")
	 * All reply queues are TEMPORARY
	 */
	@Test
	public void testPipeline9() throws Exception {
		this.test("pipeline-09.xml");
	}

	public void test(String contextConfig) throws Exception {
		ActiveMqTestUtils.prepare();
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(contextConfig, this.getClass());
		final RequestReplyExchanger gateway = context.getBean(RequestReplyExchanger.class);
		final CountDownLatch latch = new CountDownLatch(requests);
		final AtomicInteger successCounter = new AtomicInteger();
		final AtomicInteger timeoutCounter = new AtomicInteger();
		final AtomicInteger failureCounter = new AtomicInteger();
		try {
			for (int i = 0; i < requests; i++) {
				final int y = i;
				executor.execute(() -> {
					try {
						assertThat(gateway.exchange(new GenericMessage<Integer>(y)).getPayload()).isEqualTo(y);
						successCounter.incrementAndGet();
					}
					catch (MessageTimeoutException e) {
						timeoutCounter.incrementAndGet();
					}
					catch (Throwable t) {
						failureCounter.incrementAndGet();
					}
					finally {
						latch.countDown();
					}
				});
			}
			latch.await();
		}
		finally {
			logger.info("Test config: " + contextConfig);
			logger.info("Success: " + successCounter.get());
			logger.info("Timeout: " + timeoutCounter.get());
			logger.info("Failure: " + failureCounter.get());
			// technically all we care that its > 0,
			// but reality of this test it has to be something more then 0
			assertThat(successCounter.get() > 1).isTrue();
			assertThat(failureCounter.get()).isEqualTo(0);
			assertThat(successCounter.get() + timeoutCounter.get()).isEqualTo(requests);
			context.close();
		}
	}
}
