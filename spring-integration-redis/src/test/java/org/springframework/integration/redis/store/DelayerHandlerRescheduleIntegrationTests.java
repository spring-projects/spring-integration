/*
 * Copyright 2013-2022 the original author or authors.
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

package org.springframework.integration.redis.store;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.handler.DelayHandler;
import org.springframework.integration.redis.RedisContainerTest;
import org.springframework.integration.store.MessageGroup;
import org.springframework.integration.store.MessageGroupStore;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.condition.LongRunningTest;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * @author Artem Bilan
 * @author Gary Russell
 * @author Artem Vozhdayenko
 *
 * @since 3.0
 */
@LongRunningTest
class DelayerHandlerRescheduleIntegrationTests implements RedisContainerTest {
	public static final String DELAYER_ID = "delayerWithRedisMS" + UUID.randomUUID();

	@Test
	void testDelayerHandlerRescheduleWithRedisMessageStore() throws Exception {
		AbstractApplicationContext context = new ClassPathXmlApplicationContext(
				"DelayerHandlerRescheduleIntegrationTests-context.xml", this.getClass());
		MessageChannel input = context.getBean("input", MessageChannel.class);
		MessageGroupStore messageStore = context.getBean("messageStore", MessageGroupStore.class);

		String delayerMessageGroupId = DELAYER_ID + ".messageGroupId";

		messageStore.removeMessageGroup(delayerMessageGroupId);

		Message<String> message1 = MessageBuilder.withPayload("test1").build();
		input.send(message1);
		Thread.sleep(10);
		input.send(MessageBuilder.withPayload("test2").build());

		// Emulate restart and check DB state before next start
		// Interrupt taskScheduler as quickly as possible
		ThreadPoolTaskScheduler taskScheduler =
				(ThreadPoolTaskScheduler) IntegrationContextUtils.getTaskScheduler(context);
		taskScheduler.shutdown();
		assertThat(taskScheduler.getScheduledExecutor().awaitTermination(10, TimeUnit.SECONDS)).isTrue();
		context.close();

		try {
			context.getBean("input", MessageChannel.class);
			fail("IllegalStateException expected");
		}
		catch (Exception e) {
			assertThat(e).isInstanceOf(IllegalStateException.class);
			assertThat(e.getMessage()).contains("BeanFactory not initialized or already closed - call 'refresh'");
		}

		assertThat(messageStore.getMessageGroupCount()).isEqualTo(1);
		assertThat(messageStore.iterator().next().getGroupId()).isEqualTo(delayerMessageGroupId);
		assertThat(messageStore.messageGroupSize(delayerMessageGroupId)).isEqualTo(2);
		assertThat(messageStore.getMessageCountForAllMessageGroups()).isEqualTo(2);
		MessageGroup messageGroup = messageStore.getMessageGroup(delayerMessageGroupId);
		Message<?> messageInStore = messageGroup.getMessages().iterator().next();
		Object payload = messageInStore.getPayload();

		// INT-3049
		assertThat(payload).isInstanceOf(DelayHandler.DelayedMessageWrapper.class);
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
		int n = 0;
		while (n++ < 300 && messageStore.messageGroupSize(delayerMessageGroupId) > 0) {
			Thread.sleep(100);
		}
		assertThat(messageStore.messageGroupSize(delayerMessageGroupId)).isZero();

		messageStore.removeMessageGroup(delayerMessageGroupId);
		context.close();
	}

}
