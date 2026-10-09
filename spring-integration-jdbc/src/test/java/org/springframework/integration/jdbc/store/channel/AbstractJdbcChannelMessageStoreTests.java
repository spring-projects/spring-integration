/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.integration.jdbc.store.channel;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.serializer.DefaultDeserializer;
import org.springframework.core.serializer.Deserializer;
import org.springframework.core.serializer.support.SerializationFailedException;
import org.springframework.core.serializer.support.SerializingConverter;
import org.springframework.integration.jdbc.store.JdbcChannelMessageStore;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author Gunnar Hillert
 * @author Gary Russell
 * @author Meherzad Lahewala
 * @author Artem Bilan
 * @author Glenn Renfro
 */

@SpringJUnitConfig
@DirtiesContext
public abstract class AbstractJdbcChannelMessageStoreTests {

	protected static final String TEST_MESSAGE_GROUP = "AbstractJdbcChannelMessageStoreTests";

	protected static final String REGION = "AbstractJdbcChannelMessageStoreTests";

	protected static final String[] MESSAGE_PATTERNS = {
			"org.springframework.messaging.support.GenericMessage",
			"org.springframework.messaging.MessageHeaders",
			"java.util.UUID",
			"java.util.HashMap",
			"java.lang.Boolean"
	};

	@Autowired
	protected DataSource dataSource;

	protected JdbcChannelMessageStore messageStore;

	@Autowired
	protected PlatformTransactionManager transactionManager;

	@Autowired
	protected ChannelMessageStoreQueryProvider queryProvider;

	@BeforeEach
	public void init() {
		messageStore = new JdbcChannelMessageStore(dataSource, MESSAGE_PATTERNS);
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

	@Test
	@SuppressWarnings("deprecation")
	public void legacyConstructorsAreUnrestricted() {
		JdbcChannelMessageStore noArgStore = new JdbcChannelMessageStore();
		noArgStore.setDataSource(this.dataSource);
		assertStoresAndPolls(configure(noArgStore), new UntrustedPayload());
		assertStoresAndPolls(configure(new JdbcChannelMessageStore(this.dataSource)), new UntrustedPayload());
	}

	@Test
	public void patternsConstructorsEnforceAllowList() {
		JdbcChannelMessageStore store = configure(new JdbcChannelMessageStore(this.dataSource, trustedPatterns()));
		assertStoresAndPolls(store, new TrustedPayload());
		assertUnauthorized(store);

		JdbcChannelMessageStore noDataSourceStore = new JdbcChannelMessageStore(trustedPatterns());
		noDataSourceStore.setDataSource(this.dataSource);
		configure(noDataSourceStore);
		assertStoresAndPolls(noDataSourceStore, new TrustedPayload());
		assertUnauthorized(noDataSourceStore);
	}

	@Test
	public void patternsConstructorsRejectInvalidPatterns() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new JdbcChannelMessageStore((String[]) null))
				.withMessage("'allowedPatterns' must not be empty");
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new JdbcChannelMessageStore(this.dataSource, new String[0]))
				.withMessage("'allowedPatterns' must not be empty");
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new JdbcChannelMessageStore(this.dataSource, "java.util.*", ""))
				.withMessageContaining("whitespace-only");
	}

	@Test
	@SuppressWarnings({"unchecked", "rawtypes", "deprecation"})
	public void patternsPreservedOnSetDeserializer() {
		JdbcChannelMessageStore store = new JdbcChannelMessageStore(this.dataSource, trustedPatterns());
		store.setDeserializer((Deserializer) new DefaultDeserializer(getClass().getClassLoader()));
		configure(store);
		assertStoresAndPolls(store, new TrustedPayload());
		assertUnauthorized(store);
		assertThatIllegalArgumentException().isThrownBy(store::addAllowedPatterns);
	}

	@Test
	@SuppressWarnings("deprecation")
	public void addAllowedPatternsAllowsPreviouslyRejectedClass() {
		JdbcChannelMessageStore store = configure(new JdbcChannelMessageStore(this.dataSource, trustedPatterns()));
		assertUnauthorized(store);
		assertThatIllegalArgumentException().isThrownBy(() -> store.addAllowedPatterns(" "));
		store.addAllowedPatterns(UntrustedPayload.class.getName());
		assertStoresAndPolls(store, new UntrustedPayload());
	}

	private JdbcChannelMessageStore configure(JdbcChannelMessageStore store) {
		store.setRegion(REGION);
		store.setChannelMessageStoreQueryProvider(this.queryProvider);
		store.afterPropertiesSet();
		store.removeMessageGroup(TEST_MESSAGE_GROUP);
		return store;
	}

	private void assertStoresAndPolls(JdbcChannelMessageStore store, Object payload) {
		new TransactionTemplate(this.transactionManager).executeWithoutResult((status) ->
				store.addMessageToGroup(TEST_MESSAGE_GROUP, new GenericMessage<>(payload)));
		Message<?> polled = store.pollMessageFromGroup(TEST_MESSAGE_GROUP);
		assertThat(polled).extracting(Message::getPayload).hasSameClassAs(payload);
	}

	private void assertUnauthorized(JdbcChannelMessageStore store) {
		new TransactionTemplate(this.transactionManager).executeWithoutResult((status) ->
				store.addMessageToGroup(TEST_MESSAGE_GROUP, new GenericMessage<>(new UntrustedPayload())));
		assertThatExceptionOfType(SerializationFailedException.class)
				.isThrownBy(() -> store.pollMessageFromGroup(TEST_MESSAGE_GROUP))
				.withCauseInstanceOf(SecurityException.class);
		store.removeMessageGroup(TEST_MESSAGE_GROUP);
	}

	private static String[] trustedPatterns() {
		return new String[] {
				"org.springframework.messaging.support.GenericMessage",
				"org.springframework.messaging.MessageHeaders",
				"java.util.UUID",
				"java.util.HashMap",
				TrustedPayload.class.getName()
		};
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

	@SuppressWarnings("serial")
	private static final class TrustedPayload implements Serializable {

	}

	@SuppressWarnings("serial")
	private static final class UntrustedPayload implements Serializable {

	}

}
