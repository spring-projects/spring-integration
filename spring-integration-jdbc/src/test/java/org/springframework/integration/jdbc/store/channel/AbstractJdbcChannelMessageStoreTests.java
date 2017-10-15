/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.integration.jdbc.store.channel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.sql.Types;

import javax.sql.DataSource;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.serializer.support.SerializingConverter;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.jdbc.store.JdbcChannelMessageStore;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.util.UUIDConverter;
import org.springframework.jdbc.support.lob.DefaultLobHandler;
import org.springframework.messaging.Message;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * @author Gunnar Hillert
 * @author Gary Russell
 * @author Meherzad Lahewala
 */

@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext // close at the end after class
public abstract class AbstractJdbcChannelMessageStoreTests {

	protected static final String TEST_MESSAGE_GROUP = "AbstractJdbcChannelMessageStoreTests";

	private static final String REGION = "AbstractJdbcChannelMessageStoreTests";

	@Autowired
	protected DataSource dataSource;

	protected JdbcChannelMessageStore messageStore;

	@Autowired
	protected PlatformTransactionManager transactionManager;

	@Autowired
	protected ChannelMessageStoreQueryProvider queryProvider;

	@Before
	public void init() throws Exception {
		messageStore = new JdbcChannelMessageStore(dataSource);
		messageStore.setRegion(REGION);
		messageStore.setChannelMessageStoreQueryProvider(queryProvider);
		messageStore.afterPropertiesSet();
		messageStore.removeMessageGroup("AbstractJdbcChannelMessageStoreTests");
	}

	@Test
	public void testGetNonExistentMessageFromGroup() throws Exception {
		Message<?> result = messageStore.pollMessageFromGroup(TEST_MESSAGE_GROUP);
		assertNull(result);
	}

	@Test
	public void testAddAndGet() throws Exception {
		final Message<String> message = MessageBuilder.withPayload("Cartman and Kenny")
				.setHeader("homeTown", "Southpark")
				.build();

		final TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

		transactionTemplate.setIsolationLevel(Isolation.READ_COMMITTED.value());
		transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);

		transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				messageStore.addMessageToGroup(TEST_MESSAGE_GROUP, message);
			}
		});

		Message<?> messageFromDb = messageStore.pollMessageFromGroup(TEST_MESSAGE_GROUP);

		assertNotNull(messageFromDb);
		assertEquals(message.getHeaders().getId(), messageFromDb.getHeaders().getId());
	}

	@Test
	public void testAddAndGetCustomStatementSetter() {
		messageStore.setMessageGroupPreparedStatementSetter(getMessageGroupPreparedStatementSetter());
		final Message<String> message = MessageBuilder.withPayload("Cartman and Kenny")
				.setHeader("homeTown", "Southpark")
				.build();

		final TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

		transactionTemplate.setIsolationLevel(Isolation.READ_COMMITTED.value());
		transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);

		transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				messageStore.addMessageToGroup(TEST_MESSAGE_GROUP, message);
			}
		});
		Message<?> messageFromDb = messageStore.pollMessageFromGroup(TEST_MESSAGE_GROUP);
		assertNotNull(messageFromDb);
		assertEquals(message.getHeaders().getId(), messageFromDb.getHeaders().getId());
	}

	private String getKey(Object input) {
		return input == null ? null : UUIDConverter.getUUID(input).toString();
	}

	private MessageGroupPreparedStatementSetter getMessageGroupPreparedStatementSetter() {
		return (preparedStatement, message, groupId) -> {
			String groupKey = getKey(groupId);
			long createdDate = System.currentTimeMillis();
			String messageId = getKey(message.getHeaders().getId());
			SerializingConverter serializer = new SerializingConverter();
			byte[] messageBytes = serializer.convert(message);

			preparedStatement.setString(1, messageId);
			preparedStatement.setString(2, groupKey);
			preparedStatement.setString(3, REGION);
			preparedStatement.setLong(4, createdDate);
			Integer priority = message.getHeaders().get(IntegrationMessageHeaderAccessor.PRIORITY, Integer.class);

			if (priority != null) {
				preparedStatement.setInt(5, priority);
			}
			else {
				preparedStatement.setNull(5, Types.NUMERIC);
			}
			DefaultLobHandler lobHandler = new DefaultLobHandler();
			lobHandler.getLobCreator().setBlobAsBytes(preparedStatement, 6, messageBytes);
		};
	}
}
