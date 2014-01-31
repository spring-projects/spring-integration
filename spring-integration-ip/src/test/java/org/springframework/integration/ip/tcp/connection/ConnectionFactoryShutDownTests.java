/*
 * Copyright 2002-2014 the original author or authors.
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
package org.springframework.integration.ip.tcp.connection;

import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import org.springframework.messaging.MessagingException;
import org.springframework.util.StopWatch;

/**
 * @author Gary Russell
 * @since 2.2
 *
 */
public class ConnectionFactoryShutDownTests {

	@Test
	public void testShutdownDoesntDeadlock() throws Exception {
		final AbstractConnectionFactory factory = new AbstractConnectionFactory(0) {

			@Override
			public TcpConnection getConnection() throws Exception {
				return null;
			}

		};
		factory.setActive(true);
		Executor executor = factory.getTaskExecutor();
		final CountDownLatch latch1 = new CountDownLatch(1);
		final CountDownLatch latch2 = new CountDownLatch(1);
		executor.execute(new Runnable() {

			@Override
			public void run() {
				latch1.countDown();
				try {
					while (true) {
						factory.getTaskExecutor();
						Thread.sleep(100);
					}
				}
				catch (MessagingException e) {
				}
				catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
				latch2.countDown();
			}
		});
		assertTrue(latch1.await(10, TimeUnit.SECONDS));
		StopWatch watch = new StopWatch();
		watch.start();
		factory.stop();
		watch.stop();
		assertTrue("Expected < 1000, was:" + watch.getLastTaskTimeMillis(), watch.getLastTaskTimeMillis() < 1000);
		assertTrue(latch1.await(10, TimeUnit.SECONDS));
	}
}
