/*
 * Copyright 2016-present the original author or authors.
 */

package org.springframework.integration.jdbc.store.channel;

import java.util.stream.Stream;

import org.apache.commons.dbcp2.BasicDataSource;
import org.h2.jdbc.JdbcSQLSyntaxErrorException;
import org.junit.jupiter.api.Test;

import org.springframework.context.ApplicationContextException;
import org.springframework.integration.jdbc.store.JdbcChannelMessageStore;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Gunnar Hillert
 * @author Manuel Jordan
 * @author 樊孔缘
 * @since 4.3
 */
@ContextConfiguration
public class H2JdbcChannelMessageStoreTests extends AbstractJdbcChannelMessageStoreTests {

	@Test
	void closingPartiallyConsumedStreamReleasesConnection() {
		this.messageStore.addMessageToGroup(TEST_MESSAGE_GROUP, new GenericMessage<>("first message"));
		this.messageStore.addMessageToGroup(TEST_MESSAGE_GROUP, new GenericMessage<>("second message"));
		BasicDataSource pool = (BasicDataSource) this.dataSource;
		int activeConnections = pool.getNumActive();

		try (Stream<Message<?>> messages = this.messageStore.streamMessagesForGroup(TEST_MESSAGE_GROUP)) {
			assertThat(pool.getNumActive()).isEqualTo(activeConnections + 1);
			assertThat(messages.limit(1)).hasSize(1);
		}

		assertThat(pool.getNumActive()).isEqualTo(activeConnections);
		assertThat(this.messageStore.messageGroupSize(TEST_MESSAGE_GROUP)).isEqualTo(2);
	}

	@Test
	void noTableThrowsExceptionOnStart() {
		try (TestUtils.TestApplicationContext testApplicationContext = TestUtils.createTestApplicationContext()) {
			JdbcChannelMessageStore jdbcChannelMessageStore = new JdbcChannelMessageStore(this.dataSource);
			jdbcChannelMessageStore.setTablePrefix("TEST_");
			jdbcChannelMessageStore.setRegion(REGION);
			jdbcChannelMessageStore.setChannelMessageStoreQueryProvider(this.queryProvider);
			testApplicationContext.registerBean("jdbcChannelMessageStore", jdbcChannelMessageStore);
			assertThatExceptionOfType(ApplicationContextException.class)
					.isThrownBy(testApplicationContext::refresh)
					.withRootCauseExactlyInstanceOf(JdbcSQLSyntaxErrorException.class)
					.withStackTraceContaining("Table \"TEST_CHANNEL_MESSAGE\" not found");
		}
	}

}
