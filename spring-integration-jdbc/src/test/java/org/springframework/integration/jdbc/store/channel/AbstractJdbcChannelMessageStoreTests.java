/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.jdbc.store.channel;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.serializer.support.SerializingConverter;
import org.springframework.integration.jdbc.store.JdbcChannelMessageStore;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.jdbc.support.lob.DefaultLobHandler;
import org.springframework.jdbc.support.lob.LobHandler;
import org.springframework.messaging.Message;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Gunnar Hillert
 * @author Gary Russell
 * @author Meherzad Lahewala
 * @author Artem Bilan
 */

@SpringJUnitConfig
@DirtiesContext
public abstract class AbstractJdbcChannelMessageStoreTests {

	protected static final String TEST_MESSAGE_GROUP = "AbstractJdbcChannelMessageStoreTests";

	protected static final String REGION = "AbstractJdbcChannelMessageStoreTests";

	@Autowired
	protected DataSource dataSource;

	protected JdbcChannelMessageStore messageStore;

	@Autowired
	protected PlatformTransactionManager transactionManager;

	@Autowired
	protected ChannelMessageStoreQueryProvider queryProvider;

	@BeforeEach
	public void init() {
		messageStore = new JdbcChannelMessageStore(dataSource);
		messageStore.setRegion(REGION);
		messageStore.setChannelMessageStoreQueryProvider(queryProvider);
		messageStore.afterPropertiesSet();
		messageStore.removeMessageGroup("AbstractJdbcChannelMessageStoreTests");
	}

	@Test
	public void testGetNonExistentMessageFromGroup() {
		Message<?> result = messageStore.pollMessageFromGroup(TEST_MESSAGE_GROUP);
		assertThat(result).isNull();
	}

	@Test
	public void testAddAndGet() {
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

		assertThat(messageFromDb).isNotNull();
		assertThat(messageFromDb.getHeaders().getId()).isEqualTo(message.getHeaders().getId());
	}

	@Test
	public void testAddAndGetCustomStatementSetter() {
		messageStore.setPreparedStatementSetter(getMessageGroupPreparedStatementSetter());
		final Message<String> message = MessageBuilder.withPayload("Cartman and Kenny").build();

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
		assertThat(messageFromDb).isNotNull();
		assertThat(messageFromDb.getHeaders().getId()).isEqualTo(message.getHeaders().getId());
	}

	private ChannelMessageStorePreparedStatementSetter getMessageGroupPreparedStatementSetter() {
		return new ChannelMessageStorePreparedStatementSetter() {

			private SerializingConverter serializer = new SerializingConverter();

			private LobHandler lobHandler = new DefaultLobHandler();

			@Override
			public void setValues(PreparedStatement preparedStatement, Message<?> requestMessage, Object groupId,
					String region, boolean priorityEnabled) throws SQLException {
				super.setValues(preparedStatement, requestMessage, groupId, region, priorityEnabled);
				byte[] messageBytes = this.serializer.convert(requestMessage);
				this.lobHandler.getLobCreator().setBlobAsBytes(preparedStatement, 6, messageBytes);
			}

		};
	}

}
