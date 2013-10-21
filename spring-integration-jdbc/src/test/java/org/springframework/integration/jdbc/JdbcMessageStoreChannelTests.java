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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.integration.store.MessageGroup;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.transaction.annotation.Transactional;

@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext(classMode=ClassMode.AFTER_EACH_TEST_METHOD)
public class JdbcMessageStoreChannelTests {

	@Autowired
	private MessageChannel input;

	@Autowired
	private JdbcMessageStore messageStore;

	@BeforeTransaction
	public void clear() {
		for (MessageGroup group : messageStore) {
			messageStore.removeMessageGroup(group.getGroupId());
		}
	}

	@Test
	public void testSendAndActivate() throws Exception {
		Service.reset(1);
		input.send(new GenericMessage<String>("foo"));
		Service.await(10000);
		assertEquals(1, Service.messages.size());
		assertEquals(0, messageStore.getMessageGroup("JdbcMessageStoreChannelTests").size());
	}

	@Test
	public void testSendAndActivateWithRollback() throws Exception {
		Service.reset(1);
		Service.fail = true;
		input.send(new GenericMessage<String>("foo"));
		Service.await(10000);
		assertEquals(1, Service.messages.size());
		// After a rollback in the poller the message is still waiting to be delivered
		assertEquals(1, messageStore.getMessageGroup("JdbcMessageStoreChannelTests").size());
	}

	@Test
	@Transactional
	public void testSendAndActivateTransactionalSend() throws Exception {
		Service.reset(1);
		input.send(new GenericMessage<String>("foo"));
		// This will time out because the transaction has not committed yet
		try {
			Service.await(10000);
			fail("Expected timeout");
		} catch (IllegalStateException e) {
			// expected
		}
		// So no activation
		assertEquals(0, Service.messages.size());
		// But inside the transaction the message is still there
		assertEquals(1, messageStore.getMessageGroup("JdbcMessageStoreChannelTests").size());
	}

	public static class Service {
		private static boolean fail = false;
		private static boolean alreadyFailed = false;
		private static List<String> messages = new CopyOnWriteArrayList<String>();
		private static CountDownLatch latch = new CountDownLatch(0);
		public static void reset(int count) {
			fail = false;
			alreadyFailed = false;
			messages.clear();
			latch = new CountDownLatch(count);
		}
		public static void await(long timeout) throws InterruptedException {
			if (!latch.await(timeout, TimeUnit.MILLISECONDS)) {
				throw new IllegalStateException("Timed out waiting for message");
			}
		}
		public String echo(String input) {
			if (!alreadyFailed) {
				messages.add(input);
				latch.countDown();
			}
			if (fail) {
				alreadyFailed = true;
				throw new RuntimeException("Planned failure");
			}
			return input;
		}
	}

}
