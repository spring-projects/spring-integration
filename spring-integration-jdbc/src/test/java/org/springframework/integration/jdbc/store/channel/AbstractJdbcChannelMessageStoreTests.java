/*
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.jdbc.store.channel;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.stream.Stream;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.serializer.support.SerializingConverter;
import org.springframework.integration.jdbc.store.JdbcChannelMessageStore;
import org.springframework.integration.store.BasicMessageGroupStore;
import org.springframework.integration.store.MessageGroup;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/**
 * @author Gunnar Hillert
 * @author Gary Russell
 * @author Meherzad Lahewala
 * @author Artem Bilan
 * @author Fan Kongyuan
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
	void inspectMessagesWithoutConsuming() {
		Message<?> first = MessageBuilder.withPayload("first queued message").build();
		Message<?> second = MessageBuilder.withPayload("second queued message").build();
		this.messageStore.addMessageToGroup(TEST_MESSAGE_GROUP, first);
		this.messageStore.addMessageToGroup(TEST_MESSAGE_GROUP, second);
		BasicMessageGroupStore store = this.messageStore;

		assertThat(store.getMessagesForGroup(TEST_MESSAGE_GROUP)).containsExactly(first, second);
		try (Stream<Message<?>> messages = store.streamMessagesForGroup(TEST_MESSAGE_GROUP)) {
			assertThat(messages).containsExactly(first, second);
		}
		assertThat(store.messageGroupSize(TEST_MESSAGE_GROUP)).isEqualTo(2);
		assertThat(store.pollMessageFromGroup(TEST_MESSAGE_GROUP)).isEqualTo(first);
		assertThat(store.pollMessageFromGroup(TEST_MESSAGE_GROUP)).isEqualTo(second);
		assertThat(store.getMessagesForGroup(TEST_MESSAGE_GROUP)).isEmpty();
		try (Stream<Message<?>> messages = store.streamMessagesForGroup(TEST_MESSAGE_GROUP)) {
			assertThat(messages).isEmpty();
		}
	}

	@Test
	void messageGroupsLoadMessagesOnlyOnAccess() {
		JdbcChannelMessageStore store = spy(this.messageStore);
		Message<?> message = MessageBuilder.withPayload("lazily inspected message").build();
		MessageGroup added = store.addMessageToGroup(TEST_MESSAGE_GROUP, message);
		MessageGroup retrieved = store.getMessageGroup(TEST_MESSAGE_GROUP);

		verify(store, never()).getMessagesForGroup(TEST_MESSAGE_GROUP);
		verify(store, never()).streamMessagesForGroup(TEST_MESSAGE_GROUP);
		assertThat(added.size()).isEqualTo(1);
		assertThat(retrieved.getMessages()).containsExactly(message);
		assertThat(retrieved.getOne()).isEqualTo(message);
		assertThat(store.pollMessageFromGroup(TEST_MESSAGE_GROUP)).isEqualTo(message);
	}

	@Test
	void inspectionRespectsGroupAndRegion() {
		Message<?> expected = MessageBuilder.withPayload("selected region and group").build();
		this.messageStore.addMessageToGroup(TEST_MESSAGE_GROUP, expected);
		String otherGroup = TEST_MESSAGE_GROUP + "-other";
		this.messageStore.addMessageToGroup(otherGroup, MessageBuilder.withPayload("other group").build());
		JdbcChannelMessageStore otherRegion = new JdbcChannelMessageStore(this.dataSource);
		otherRegion.setRegion(REGION + "-other");
		otherRegion.setChannelMessageStoreQueryProvider(this.queryProvider);
		otherRegion.afterPropertiesSet();
		otherRegion.addMessageToGroup(TEST_MESSAGE_GROUP, MessageBuilder.withPayload("other region").build());
		try {
			assertThat(this.messageStore.getMessagesForGroup(TEST_MESSAGE_GROUP)).containsExactly(expected);
			try (Stream<Message<?>> messages = this.messageStore.streamMessagesForGroup(TEST_MESSAGE_GROUP)) {
				assertThat(messages).containsExactly(expected);
			}
		}
		finally {
			this.messageStore.removeMessageGroup(otherGroup);
			otherRegion.removeMessageGroup(TEST_MESSAGE_GROUP);
		}
	}

	@Test
	void inspectionDoesNotChangePriorityPolling() {
		this.messageStore.setPriorityEnabled(true);
		Message<?> low = MessageBuilder.withPayload("low priority").setPriority(1).build();
		Message<?> high = MessageBuilder.withPayload("high priority").setPriority(9).build();
		this.messageStore.addMessageToGroup(TEST_MESSAGE_GROUP, low);
		this.messageStore.addMessageToGroup(TEST_MESSAGE_GROUP, high);

		assertThat(this.messageStore.getMessagesForGroup(TEST_MESSAGE_GROUP)).containsExactly(low, high);
		try (Stream<Message<?>> messages = this.messageStore.streamMessagesForGroup(TEST_MESSAGE_GROUP)) {
			assertThat(messages).containsExactly(low, high);
		}
		assertThat(this.messageStore.pollMessageFromGroup(TEST_MESSAGE_GROUP)).isEqualTo(high);
		assertThat(this.messageStore.pollMessageFromGroup(TEST_MESSAGE_GROUP)).isEqualTo(low);
	}

	@Test
	public void testAddAndGet() {
		final Message<String> message = MessageBuilder.withPayload("Cartman and Kenny")
				.setHeader("homeTown", "Southpark")
				.build();

		final TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

		transactionTemplate.setIsolationLevel(Isolation.READ_COMMITTED.value());
		transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);

		transactionTemplate.executeWithoutResult((status) ->
				messageStore.addMessageToGroup(TEST_MESSAGE_GROUP, message));

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

		transactionTemplate.executeWithoutResult((status) ->
				messageStore.addMessageToGroup(TEST_MESSAGE_GROUP, message));
		Message<?> messageFromDb = messageStore.pollMessageFromGroup(TEST_MESSAGE_GROUP);
		assertThat(messageFromDb).isNotNull();
		assertThat(messageFromDb.getHeaders().getId()).isEqualTo(message.getHeaders().getId());
	}

	private ChannelMessageStorePreparedStatementSetter getMessageGroupPreparedStatementSetter() {
		return new ChannelMessageStorePreparedStatementSetter() {

			private final SerializingConverter serializer = new SerializingConverter();

			@Override
			public void setValues(PreparedStatement preparedStatement, Message<?> requestMessage, Object groupId,
					String region, boolean priorityEnabled) throws SQLException {
				super.setValues(preparedStatement, requestMessage, groupId, region, priorityEnabled);
				byte[] messageBytes = this.serializer.convert(requestMessage);
				preparedStatement.setBytes(6, messageBytes);
			}

		};
	}

}
