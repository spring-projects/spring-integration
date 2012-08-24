/*
 * Copyright 2002-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.springframework.integration.jdbc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.MessagingException;
import org.springframework.integration.core.MessageHandler;
import org.springframework.integration.core.PollableChannel;
import org.springframework.integration.store.MessageGroup;
import org.springframework.integration.store.MessageGroupStore;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.util.UUIDConverter;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * @author Artem Bilan
 * @author Gary Russell
 */
public class DelayerHandlerRescheduleIntegrationTests {

	public static final String DELAYER_ID = "delayerWithJdbcMS";

	public static EmbeddedDatabase dataSource;

	@BeforeClass
	public static void init() {
		dataSource = new EmbeddedDatabaseBuilder()
				.setType(EmbeddedDatabaseType.H2)
				.addScript("classpath:/org/springframework/integration/jdbc/schema-h2.sql")
				.build();
	}

	@AfterClass
	public static void destroy() {
		dataSource.shutdown();
	}

	@Test //INT-1132
	public void testDelayerHandlerRescheduleWithJdbcMessageStore() throws Exception {
		AbstractApplicationContext context = new ClassPathXmlApplicationContext("DelayerHandlerRescheduleIntegrationTests-context.xml", this.getClass());
		MessageChannel input = context.getBean("input", MessageChannel.class);
		MessageGroupStore messageStore = context.getBean("messageStore", MessageGroupStore.class);

		assertEquals(0, messageStore.getMessageGroupCount());
		input.send(MessageBuilder.withPayload("test1").build());
		input.send(MessageBuilder.withPayload("test2").build());

		// Emulate restart and check DB state before next start
		context.destroy();

		Thread.sleep(100);

		try {
			context.getBean("input", MessageChannel.class);
			fail("IllegalStateException expected");
		}
		catch (Exception e) {
			assertTrue(e instanceof IllegalStateException);
			assertTrue(e.getMessage().contains("BeanFactory not initialized or already closed - call 'refresh'"));
		}

		String delayerMessageGroupId = UUIDConverter.getUUID(DELAYER_ID + ".messageGroupId").toString();

		assertEquals(1, messageStore.getMessageGroupCount());
		assertEquals(delayerMessageGroupId, messageStore.iterator().next().getGroupId());
		assertEquals(2, messageStore.messageGroupSize(delayerMessageGroupId));
		assertEquals(2, messageStore.getMessageCountForAllMessageGroups());
		MessageGroup messageGroup = messageStore.getMessageGroup(delayerMessageGroupId);
		Message<?> messageInStore = messageGroup.getMessages().iterator().next();
		Object payload = messageInStore.getPayload();
		assertEquals("DelayedMessageWrapper", payload.getClass().getSimpleName());
		assertEquals("test1", TestUtils.getPropertyValue(payload, "original.payload"));

		context.refresh();

		PollableChannel output = context.getBean("output", PollableChannel.class);

		Message<?> message = output.receive(10000);
		assertNotNull(message);

		Object payload1 = message.getPayload();

		message = output.receive(10000);
		assertNotNull(message);
		Object payload2 = message.getPayload();
		assertNotSame(payload1, payload2);

		assertEquals(1, messageStore.getMessageGroupCount());
		assertEquals(0, messageStore.messageGroupSize(delayerMessageGroupId));

	}

	@Test //INT-2649
	public void testRollbackOnDelayerHandlerReleaseTask() throws Exception {
		AbstractApplicationContext context = new ClassPathXmlApplicationContext("DelayerHandlerRescheduleIntegrationTests-context.xml", this.getClass());
		MessageChannel input = context.getBean("transactionalDelayerInput", MessageChannel.class);

		MessageGroupStore messageStore = context.getBean("messageStore", MessageGroupStore.class);
		String delayerMessageGroupId = UUIDConverter.getUUID("transactionalDelayer.messageGroupId").toString();
		assertEquals(0, messageStore.messageGroupSize(delayerMessageGroupId));

		input.send(MessageBuilder.withPayload("test").build());

		Thread.sleep(100);

		assertEquals(1, messageStore.messageGroupSize(delayerMessageGroupId));

		//To check that 'rescheduling' works in the transaction boundaries too
		context.destroy();
		context.refresh();

		assertTrue(RollbackTxSync.latch.await(20, TimeUnit.SECONDS));

		//On transaction rollback the delayed Message should remain in the persistent MessageStore
		assertEquals(1, messageStore.messageGroupSize(delayerMessageGroupId));
	}

	private static class TestJdbcMessageStore extends JdbcMessageStore {

		private TestJdbcMessageStore() {
			super();
			this.setDataSource(dataSource);
		}

	}

	private static class ExceptionMessageHandler implements MessageHandler {

		public void handleMessage(Message<?> message) throws MessagingException {
			TransactionSynchronizationManager.registerSynchronization(new RollbackTxSync());
			throw new RuntimeException("intentional");
		}

	}

	private static class RollbackTxSync extends TransactionSynchronizationAdapter {

		public static CountDownLatch latch = new CountDownLatch(2);

		@Override
		public void afterCompletion(int status) {
			if (TransactionSynchronization.STATUS_ROLLED_BACK == status) {
				latch.countDown();
			}
		}

	}

}
