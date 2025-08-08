/*
 * Copyright © 2013 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2013-present the original author or authors.
 */

package org.springframework.integration.mongodb.store;

import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.handler.DelayHandler;
import org.springframework.integration.mongodb.MongoDbContainerTest;
import org.springframework.integration.store.MessageGroup;
import org.springframework.integration.store.MessageGroupStore;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Artem Bilan
 * @author Artem Vozhdayenko
 *
 * @since 3.0
 */
class DelayerHandlerRescheduleIntegrationTests implements MongoDbContainerTest {

	public static final String DELAYER_ID = "delayerWithMongoMS";

	@Test
	void testWithMongoDbMessageStore() throws Exception {
		testDelayerHandlerRescheduleWithMongoDbMessageStore("DelayerHandlerRescheduleIntegrationTests-context.xml");
	}

	@Test
	void testWithConfigurableMongoDbMessageStore() throws Exception {
		testDelayerHandlerRescheduleWithMongoDbMessageStore("DelayerHandlerRescheduleIntegrationConfigurableTests-context.xml");
	}

	@SuppressWarnings("unchecked")
	private void testDelayerHandlerRescheduleWithMongoDbMessageStore(String config) throws Exception {
		AbstractApplicationContext context = new ClassPathXmlApplicationContext(config, this.getClass());
		MessageChannel input = context.getBean("input", MessageChannel.class);
		MessageGroupStore messageStore = context.getBean("messageStore", MessageGroupStore.class);

		String delayerMessageGroupId = DELAYER_ID + ".messageGroupId";

		messageStore.removeMessageGroup(delayerMessageGroupId);

		Message<String> message1 = MessageBuilder.withPayload("test1").build();
		input.send(message1);
		input.send(MessageBuilder.withPayload("test2").build());

		// Emulate restart and check DB state before next start
		// Interrupt taskScheduler as quickly as possible
		ThreadPoolTaskScheduler taskScheduler =
				(ThreadPoolTaskScheduler) IntegrationContextUtils.getTaskScheduler(context);
		taskScheduler.shutdown();
		taskScheduler.getScheduledExecutor().awaitTermination(10, TimeUnit.SECONDS);

		assertThat(messageStore.messageGroupSize(delayerMessageGroupId)).isEqualTo(2);

		MessageGroup messageGroup = messageStore.getMessageGroup(delayerMessageGroupId);
		Iterator<Message<?>> iterator = messageGroup.getMessages().iterator();
		Message<?> messageInStore = iterator.next();
		Object payload = messageInStore.getPayload();

		// INT-3049
		assertThat(payload).isInstanceOf(DelayHandler.DelayedMessageWrapper.class);

		Message<String> original1 = (Message<String>) ((DelayHandler.DelayedMessageWrapper) payload).getOriginal();
		messageInStore = iterator.next();
		Message<String> original2 = (Message<String>) ((DelayHandler.DelayedMessageWrapper) messageInStore.getPayload())
				.getOriginal();
		assertThat(message1).isIn(original1, original2);

		context.close();

		context.refresh();

		PollableChannel output = context.getBean("output", PollableChannel.class);

		Message<?> message = output.receive(20000);
		assertThat(message).isNotNull();

		Object payload1 = message.getPayload();

		message = output.receive(20000);
		assertThat(message).isNotNull();
		Object payload2 = message.getPayload();
		assertThat(payload2).isNotSameAs(payload1);

		messageStore = context.getBean("messageStore", MessageGroupStore.class);

		assertThat(messageStore.messageGroupSize(delayerMessageGroupId)).isZero();
		context.close();
	}

}
