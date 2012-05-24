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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.core.PollableChannel;
import org.springframework.integration.store.MessageGroup;
import org.springframework.integration.store.MessageGroupStore;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.util.UUIDConverter;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;



/**
 * @author Arte Bilan
 */
//INT-1132
public class DelayerHandlerRescheduleIntegrationTests {

	public static final String DELAYER_ID = "delayerWithJdbcMS";


	private static EmbeddedDatabase dataSource;

	@BeforeClass
	public static void init() {
		dataSource = new EmbeddedDatabaseBuilder()
				.setType(EmbeddedDatabaseType.DERBY)
				.addScript("classpath:/org/springframework/integration/jdbc/schema-derby.sql")
				.build();
	}

	@AfterClass
	public static void destroy() {
		dataSource.shutdown();
	}

	@Test
	public void testDelayerHandlerRescheduleWithJdbcMessageStore() {
		AbstractApplicationContext context = new ClassPathXmlApplicationContext("DelayerHandlerRescheduleIntegrationTests-context.xml", this.getClass());
		MessageChannel input = context.getBean("input", MessageChannel.class);
		MessageGroupStore messageStore = context.getBean("messageStore", MessageGroupStore.class);

		assertEquals(0, messageStore.getMessageGroupCount());
		String testPayload = "test";
		input.send(MessageBuilder.withPayload(testPayload).build());

		// Emulate restart and check DB state before next start
		context.destroy();

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
		assertEquals(1, messageStore.messageGroupSize(delayerMessageGroupId));
		assertEquals(1, messageStore.getMessageCountForAllMessageGroups());
		MessageGroup messageGroup = messageStore.getMessageGroup(delayerMessageGroupId);
		assertEquals(testPayload, messageGroup.getMessages().iterator().next().getPayload());

		context.refresh();
		PollableChannel output = context.getBean("output", PollableChannel.class);
		Message<?> message = output.receive();
		assertEquals(testPayload, message.getPayload());
		assertEquals(1, messageStore.getMessageGroupCount());
		assertEquals(0, messageStore.messageGroupSize(delayerMessageGroupId));

	}

	private static class TestJdbcMessageStore extends JdbcMessageStore {

		private TestJdbcMessageStore() {
			super();
			this.setDataSource(dataSource);
		}

	}

}
