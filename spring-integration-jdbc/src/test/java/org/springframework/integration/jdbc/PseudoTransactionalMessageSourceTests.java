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
package org.springframework.integration.jdbc;

import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.integration.Message;
import org.springframework.integration.core.PseudoTransactionalMessageSource;
import org.springframework.integration.message.GenericMessage;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Gary Russell
 * @since 2.2
 *
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class PseudoTransactionalMessageSourceTests {

	private static CountDownLatch latch1 = new CountDownLatch(1);

	private static boolean committed;

	private static CountDownLatch latch2 = new CountDownLatch(1);

	private static boolean rolledBack;

	private static boolean doRollback;

	@Test
	public void testCommit() throws Exception {
		assertTrue(latch1.await(10, TimeUnit.SECONDS));
		assertTrue(committed);
	}

	@Test
	public void testRollback() throws Exception {
		doRollback = true;
		assertTrue(latch2.await(10, TimeUnit.SECONDS));
		assertTrue(rolledBack);
	}

	public static class MessageSource implements PseudoTransactionalMessageSource<String> {

		public Message<String> receive() {
			if (doRollback) {
				throw new RuntimeException("Expected");
			}
			return new GenericMessage<String>("foo");
		}

		public Object getResource() {
			return new Object();
		}

		public void afterCommit(Object resource) {
			committed = true;
			latch1.countDown();
		}

		public void afterRollback(Object resource) {
			rolledBack = true;
			latch2.countDown();
		}

		public void afterReceiveNoTX(Object resource) {
		}

		public void afterSendNoTX(Object resource) {
		}

	}
}
