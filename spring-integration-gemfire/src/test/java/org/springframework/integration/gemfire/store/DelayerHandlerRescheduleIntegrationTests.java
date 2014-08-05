/*
 * Copyright 2013-2014 the original author or authors.
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

package org.springframework.integration.gemfire.store;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.concurrent.TimeUnit;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.data.gemfire.CacheFactoryBean;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.handler.DelayHandler;
import org.springframework.integration.store.MessageGroup;
import org.springframework.integration.store.MessageGroupStore;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.support.LongRunningIntegrationTest;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.util.Assert;

import com.gemstone.gemfire.cache.Cache;


/**
 * @author Artem Bilan
 * @author Gary Russell
 * @since 3.0
 */
public class DelayerHandlerRescheduleIntegrationTests {

	public static final String DELAYER_ID = "delayerWithGemfireMS";

	public static Cache cache;

	@ClassRule
	public static LongRunningIntegrationTest longTests = new LongRunningIntegrationTest();

	@BeforeClass
	public static void startUp() throws Exception {
		CacheFactoryBean cacheFactoryBean = new CacheFactoryBean();
		cache = cacheFactoryBean.getObject();
	}

	@AfterClass
	public static void cleanUp() {
		cache.close();
		Assert.isTrue(cache.isClosed(), "Cache did not close after close() call");
	}

	@Test
	public void testDelayerHandlerRescheduleWithGemfireMessageStore() throws Exception {
		AbstractApplicationContext context = new ClassPathXmlApplicationContext(
				"DelayerHandlerRescheduleIntegrationTests-context.xml", this.getClass());
		MessageChannel input = context.getBean("input", MessageChannel.class);
		MessageGroupStore messageStore = context.getBean("messageStore", MessageGroupStore.class);

		String delayerMessageGroupId = DELAYER_ID + ".messageGroupId";

		Message<String> message1 = MessageBuilder.withPayload("test1").build();
		input.send(message1);
		input.send(MessageBuilder.withPayload("test2").build());

		// Emulate restart and check Cache state before next start
		// Interrupt taskScheduler as quickly as possible
		ThreadPoolTaskScheduler taskScheduler = (ThreadPoolTaskScheduler) IntegrationContextUtils.getTaskScheduler(context);
		taskScheduler.shutdown();
		taskScheduler.getScheduledExecutor().awaitTermination(10, TimeUnit.SECONDS);
		context.destroy();

		try {
			context.getBean("input", MessageChannel.class);
			fail("IllegalStateException expected");
		}
		catch (Exception e) {
			assertTrue(e instanceof IllegalStateException);
			assertTrue(e.getMessage().contains("BeanFactory not initialized or already closed - call 'refresh'"));
		}

		assertEquals(1, messageStore.getMessageGroupCount());
		assertEquals(delayerMessageGroupId, messageStore.iterator().next().getGroupId());
		assertEquals(2, messageStore.messageGroupSize(delayerMessageGroupId));
		assertEquals(2, messageStore.getMessageCountForAllMessageGroups());
		MessageGroup messageGroup = messageStore.getMessageGroup(delayerMessageGroupId);
		Message<?> messageInStore = messageGroup.getMessages().iterator().next();
		Object payload = messageInStore.getPayload();

		// INT-3049
		assertTrue(payload instanceof DelayHandler.DelayedMessageWrapper);
		assertEquals(message1, ((DelayHandler.DelayedMessageWrapper) payload).getOriginal());

		context.refresh();

		PollableChannel output = context.getBean("output", PollableChannel.class);

		Message<?> message = output.receive(20000);
		assertNotNull(message);

		Object payload1 = message.getPayload();

		message = output.receive(20000);
		assertNotNull(message);
		Object payload2 = message.getPayload();
		assertNotSame(payload1, payload2);

		assertEquals(1, messageStore.getMessageGroupCount());
		int n = 0;
		while (n++ < 200 && messageStore.messageGroupSize(delayerMessageGroupId) > 0) {
			Thread.sleep(50);
		}
		assertEquals(0, messageStore.messageGroupSize(delayerMessageGroupId));

	}

}
