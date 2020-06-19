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

package org.springframework.integration.jdbc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.handler.DelayHandler;
import org.springframework.integration.store.MessageGroup;
import org.springframework.integration.store.MessageGroupStore;
import org.springframework.integration.store.SimpleMessageGroup;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.condition.LongRunningTest;
import org.springframework.integration.util.UUIDConverter;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.PollableChannel;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * @author Artem Bilan
 * @author Gary Russell
 */
@LongRunningTest
public class DelayerHandlerRescheduleIntegrationTests {

	public static final String DELAYER_ID = "delayerWithJdbcMS";

	public static EmbeddedDatabase dataSource;

	@BeforeAll
	public static void init() {
		dataSource = new EmbeddedDatabaseBuilder()
				.setType(EmbeddedDatabaseType.H2)
				.addScript("classpath:/org/springframework/integration/jdbc/schema-drop-h2.sql")
				.addScript("classpath:/org/springframework/integration/jdbc/schema-h2.sql")
				.build();
	}

	@AfterAll
	public static void destroy() {
		dataSource.shutdown();
	}

	@Test
	public void testDelayerHandlerRescheduleWithJdbcMessageStore() throws Exception {
		AbstractApplicationContext context =
				new ClassPathXmlApplicationContext("DelayerHandlerRescheduleIntegrationTests-context.xml", getClass());
		MessageChannel input = context.getBean("input", MessageChannel.class);
		MessageGroupStore messageStore = context.getBean("messageStore", MessageGroupStore.class);

		assertThat(messageStore.getMessageGroupCount()).isEqualTo(0);
		Message<String> message1 = MessageBuilder.withPayload("test1").build();
		input.send(message1);
		input.send(MessageBuilder.withPayload("test2").build());

		// Emulate restart and check DB state before next start
		// Interrupt taskScheduler as quickly as possible
		ThreadPoolTaskScheduler taskScheduler =
				(ThreadPoolTaskScheduler) IntegrationContextUtils.getTaskScheduler(context);
		taskScheduler.shutdown();
		taskScheduler.getScheduledExecutor().awaitTermination(10, TimeUnit.SECONDS);
		context.close();

		try {
			context.getBean("input", MessageChannel.class);
			fail("IllegalStateException expected");
		}
		catch (Exception e) {
			assertThat(e instanceof IllegalStateException).isTrue();
			assertThat(e.getMessage().contains("BeanFactory not initialized or already closed - call 'refresh'"))
					.isTrue();
		}

		String delayerMessageGroupId = UUIDConverter.getUUID(DELAYER_ID + ".messageGroupId").toString();

		assertThat(messageStore.getMessageGroupCount()).isEqualTo(1);
		assertThat(messageStore.iterator().next().getGroupId()).isEqualTo(delayerMessageGroupId);
		assertThat(messageStore.messageGroupSize(delayerMessageGroupId)).isEqualTo(2);
		assertThat(messageStore.getMessageCountForAllMessageGroups()).isEqualTo(2);
		MessageGroup messageGroup = messageStore.getMessageGroup(delayerMessageGroupId);
		// Ensure that with the lazyLoadMessageGroups = false the MessageStore doesn't return PersistentMessageGroup
		assertThat(messageGroup).isInstanceOf(SimpleMessageGroup.class);
		Message<?> messageInStore = messageGroup.getMessages().iterator().next();
		Object payload = messageInStore.getPayload();

		//INT-3049
		assertThat(payload instanceof DelayHandler.DelayedMessageWrapper).isTrue();
		assertThat(((DelayHandler.DelayedMessageWrapper) payload).getOriginal()).isEqualTo(message1);

		context.refresh();

		PollableChannel output = context.getBean("output", PollableChannel.class);

		Message<?> message = output.receive(20000);
		assertThat(message).isNotNull();

		Object payload1 = message.getPayload();

		message = output.receive(20000);
		assertThat(message).isNotNull();
		Object payload2 = message.getPayload();
		assertThat(payload2).isNotSameAs(payload1);

		assertThat(messageStore.getMessageGroupCount()).isEqualTo(1);
		assertThat(messageStore.messageGroupSize(delayerMessageGroupId)).isEqualTo(0);
		context.close();
	}

	@Test
	public void testRollbackOnDelayerHandlerReleaseTask() throws Exception {
		AbstractApplicationContext context =
				new ClassPathXmlApplicationContext("DelayerHandlerRescheduleIntegrationTests-context.xml", getClass());
		MessageChannel input = context.getBean("transactionalDelayerInput", MessageChannel.class);

		MessageGroupStore messageStore = context.getBean("messageStore", MessageGroupStore.class);
		String delayerMessageGroupId = UUIDConverter.getUUID("transactionalDelayer.messageGroupId").toString();
		assertThat(messageStore.messageGroupSize(delayerMessageGroupId)).isEqualTo(0);

		input.send(MessageBuilder.withPayload("test").build());

		Thread.sleep(1000);

		assertThat(messageStore.messageGroupSize(delayerMessageGroupId)).isEqualTo(1);

		//To check that 'rescheduling' works in the transaction boundaries too
		context.close();
		context.refresh();

		assertThat(RollbackTxSync.latch.await(20, TimeUnit.SECONDS)).isTrue();

		//On transaction rollback the delayed Message should remain in the persistent MessageStore
		assertThat(messageStore.messageGroupSize(delayerMessageGroupId)).isEqualTo(1);
		context.close();
	}

	@SuppressWarnings("unused")
	private static class ExceptionMessageHandler implements MessageHandler {

		ExceptionMessageHandler() {
		}

		@Override
		public void handleMessage(Message<?> message) throws MessagingException {
			TransactionSynchronizationManager.registerSynchronization(new RollbackTxSync());
			throw new RuntimeException("intentional");
		}

	}

	private static class RollbackTxSync implements TransactionSynchronization {

		public static CountDownLatch latch = new CountDownLatch(2);

		RollbackTxSync() {
			super();
		}

		@Override
		public void afterCompletion(int status) {
			if (TransactionSynchronization.STATUS_ROLLED_BACK == status) {
				latch.countDown();
			}
		}

	}

}
