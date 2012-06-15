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
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assert.assertThat;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.hamcrest.Matchers;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.serializer.support.DeserializingConverter;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.core.PollableChannel;
import org.springframework.integration.store.MessageGroup;
import org.springframework.integration.store.MessageGroupStore;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.util.UUIDConverter;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.jdbc.support.lob.DefaultLobHandler;
import org.springframework.jdbc.support.lob.LobHandler;
import org.springframework.test.annotation.Repeat;

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

		long timeBeforeReceive = System.currentTimeMillis();
		Message<?> message = output.receive();
		long timeAfterReceive = System.currentTimeMillis();
		assertThat(timeAfterReceive - timeBeforeReceive, Matchers.lessThanOrEqualTo(100L));

		Object payload1 = message.getPayload();

		message = output.receive();
		Object payload2 = message.getPayload();
		assertNotSame(payload1, payload2);

		assertEquals(1, messageStore.getMessageGroupCount());
		assertEquals(0, messageStore.messageGroupSize(delayerMessageGroupId));

	}

	private static class TestJdbcMessageStore extends JdbcMessageStore {

		private volatile LobHandler lobHandler = new DefaultLobHandler();

		private volatile MessageMapper mapper = new MessageMapper();

		private volatile DeserializingConverter deserializer = new DeserializingConverter();

		private TestJdbcMessageStore() {
			super();
			this.setDataSource(dataSource);
		}


//		LIFO polling
		@Override
		protected Message<?> doPollForMessage(String groupIdKey) {
			List<Message<?>> messages = this.getJdbcOperations()
					.query("SELECT INT_MESSAGE.MESSAGE_ID, INT_MESSAGE.MESSAGE_BYTES from INT_MESSAGE " +
							"where INT_MESSAGE.MESSAGE_ID = " +
							"(SELECT max(MESSAGE_ID) from INT_MESSAGE where CREATED_DATE = " +
							"(SELECT max(CREATED_DATE) from INT_MESSAGE, INT_GROUP_TO_MESSAGE " +
							"where INT_MESSAGE.MESSAGE_ID = INT_GROUP_TO_MESSAGE.MESSAGE_ID " +
							"and INT_GROUP_TO_MESSAGE.GROUP_KEY = ?))", new Object[]{groupIdKey}, mapper);
			if (messages.size() > 0) {
				System.out.println(messages.get(0));
				return messages.get(0);
			}
			return null;
		}

		private class MessageMapper implements RowMapper<Message<?>> {

			public Message<?> mapRow(ResultSet rs, int rowNum) throws SQLException {
				return (Message<?>) deserializer.convert(lobHandler.getBlobAsBytes(rs, "MESSAGE_BYTES"));
			}
		}

	}

}
