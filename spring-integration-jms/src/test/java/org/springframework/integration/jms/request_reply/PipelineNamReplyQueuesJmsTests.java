/*
 * Copyright 2002-2012 the original author or authors.
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
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.MessageTimeoutException;
import org.springframework.integration.gateway.RequestReplyExchanger;
import org.springframework.integration.jms.config.ActiveMqTestUtils;
import org.springframework.integration.message.GenericMessage;
/**
 * @author Oleg Zhurakousky
 */
public class PipelineNamReplyQueuesJmsTests {

	private final Executor executor = Executors.newFixedThreadPool(30);

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
	 * jms:out(reply-destination-name="pipeline02-02") -> jms:in
	 */
	@Test
	public void testPipeline2() throws Exception{
		this.test("pipeline-named-queue-02.xml");
	}

	/**
	 * jms:out(reply-destination-name="pipeline03-01", correlation-key="JMSCorrelationID") -> jms:in -> randomTimeoutProcess ->
	 * jms:out(reply-destination-name="pipeline03-02") -> jms:in
	 */
	@Test
	public void testPipeline3() throws Exception{
		this.test("pipeline-named-queue-03.xml");
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

	public void test(String contextConfig) throws Exception{
		ActiveMqTestUtils.prepare();
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(contextConfig, this.getClass());
		final RequestReplyExchanger gateway = context.getBean(RequestReplyExchanger.class);
		final CountDownLatch latch = new CountDownLatch(requests);
		final AtomicInteger successCounter = new AtomicInteger();
		final AtomicInteger timeoutCounter = new AtomicInteger();
		final AtomicInteger failureCounter = new AtomicInteger();

		for (int i = 0; i < requests; i++) {
			final int y = i;
			executor.execute(new Runnable() {
				public void run() {
					try {
						assertEquals(y, gateway.exchange(new GenericMessage<Integer>(y)).getPayload());
						successCounter.incrementAndGet();
					} catch (MessageTimeoutException e) {
						timeoutCounter.incrementAndGet();
					} catch (Throwable t) {
						failureCounter.incrementAndGet();
					} finally {
						latch.countDown();
					}
				}
			});
		}
		latch.await();
		System.out.println("Success: " + successCounter.get());
		System.out.println("Timeout: " + timeoutCounter.get());
		System.out.println("Failure: " + failureCounter.get());
		// technically all we care that its > 0,
		// but reality of this test it has to be something more then 0
		assertTrue(successCounter.get() > 10);
		assertEquals(0, failureCounter.get());
		assertEquals(requests, successCounter.get() + timeoutCounter.get());
	}
}
