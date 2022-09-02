/*
 * Copyright 2022 the original author or authors.
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

package org.springframework.integration.jdbc.channel;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import org.apache.commons.dbcp2.BasicDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.postgresql.jdbc.PgConnection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.jdbc.store.JdbcChannelMessageStore;
import org.springframework.integration.jdbc.store.channel.PostgresChannelMessageStoreQueryProvider;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * @author Rafael Winterhalter
 * @author Artem Bilan
 *
 * @since 6.0
 */
@SpringJUnitConfig
@DirtiesContext
public class PostgresChannelMessageTableSubscriberTests implements PostgresContainerTest {

	private static final String INTEGRATION_DB_SCRIPTS = """
			CREATE SEQUENCE INT_MESSAGE_SEQ START WITH 1 INCREMENT BY 1 NO CYCLE;
			^^^ END OF SCRIPT ^^^

			CREATE TABLE INT_CHANNEL_MESSAGE (
				MESSAGE_ID CHAR(36) NOT NULL,
				GROUP_KEY CHAR(36) NOT NULL,
				CREATED_DATE BIGINT NOT NULL,
				MESSAGE_PRIORITY BIGINT,
				MESSAGE_SEQUENCE BIGINT NOT NULL DEFAULT nextval('INT_MESSAGE_SEQ'),
				MESSAGE_BYTES BYTEA,
				REGION VARCHAR(100) NOT NULL,
				constraint INT_CHANNEL_MESSAGE_PK primary key (REGION, GROUP_KEY, CREATED_DATE, MESSAGE_SEQUENCE)
			);
			^^^ END OF SCRIPT ^^^

			CREATE FUNCTION INT_CHANNEL_MESSAGE_NOTIFY_FCT()
			RETURNS TRIGGER AS
			$BODY$
			BEGIN
				PERFORM pg_notify('int_channel_message_notify', NEW.REGION || ' ' || NEW.GROUP_KEY);
				RETURN NEW;
			END;
			$BODY$
			LANGUAGE PLPGSQL;
			^^^ END OF SCRIPT ^^^

			CREATE TRIGGER INT_CHANNEL_MESSAGE_NOTIFY_TRG
				AFTER INSERT ON INT_CHANNEL_MESSAGE
				FOR EACH ROW
				EXECUTE PROCEDURE INT_CHANNEL_MESSAGE_NOTIFY_FCT();
			^^^ END OF SCRIPT ^^^
			""";

	@Autowired
	private JdbcChannelMessageStore messageStore;

	private PostgresChannelMessageTableSubscriber postgresChannelMessageTableSubscriber;

	@BeforeEach
	void setUp() {
		// Not initiated as a bean to allow for registrations prior and post the life cycle
		this.postgresChannelMessageTableSubscriber = new PostgresChannelMessageTableSubscriber(
				() -> DriverManager.getConnection(POSTGRES_CONTAINER.getJdbcUrl(),
								POSTGRES_CONTAINER.getUsername(),
								POSTGRES_CONTAINER.getPassword())
						.unwrap(PgConnection.class)
		);
	}

	@Test
	public void testMessagePollMessagesAddedAfterStart() throws Exception {
		CountDownLatch latch = new CountDownLatch(2);
		List<Object> payloads = new ArrayList<>();
		postgresChannelMessageTableSubscriber.start();
		try {
			PostgresSubscribableChannel channel = new PostgresSubscribableChannel(messageStore,
					"testMessagePollMessagesAddedAfterStart",
					postgresChannelMessageTableSubscriber);
			channel.subscribe(message -> {
				payloads.add(message.getPayload());
				latch.countDown();
			});
			messageStore.addMessageToGroup("testMessagePollMessagesAddedAfterStart", new GenericMessage<>("1"));
			messageStore.addMessageToGroup("testMessagePollMessagesAddedAfterStart", new GenericMessage<>("2"));
			assertThat(latch.await(3, TimeUnit.SECONDS))
					.as("Expected Postgres notification within 3 seconds")
					.isTrue();
		}
		finally {
			postgresChannelMessageTableSubscriber.stop();
		}
		assertThat(payloads).containsExactly("1", "2");
	}

	@Test
	public void testMessagePollMessagesAddedBeforeStart() throws InterruptedException {
		CountDownLatch latch = new CountDownLatch(2);
		List<Object> payloads = new ArrayList<>();
		PostgresSubscribableChannel channel =
				new PostgresSubscribableChannel(messageStore,
						"testMessagePollMessagesAddedBeforeStart",
						postgresChannelMessageTableSubscriber);
		channel.subscribe(message -> {
			payloads.add(message.getPayload());
			latch.countDown();
		});
		messageStore.addMessageToGroup("testMessagePollMessagesAddedBeforeStart", new GenericMessage<>("1"));
		messageStore.addMessageToGroup("testMessagePollMessagesAddedBeforeStart", new GenericMessage<>("2"));
		postgresChannelMessageTableSubscriber.start();
		try {
			assertThat(latch.await(3, TimeUnit.SECONDS))
					.as("Expected Postgres notification within 3 seconds")
					.isTrue();
		}
		finally {
			postgresChannelMessageTableSubscriber.stop();
		}
		assertThat(payloads).containsExactly("1", "2");
	}

	@Configuration
	@EnableIntegration
	public static class Config {

		@Bean
		public DataSource dataSource() {
			BasicDataSource dataSource = new BasicDataSource();
			dataSource.setUrl(PostgresContainerTest.getJdbcUrl());
			dataSource.setUsername(PostgresContainerTest.getUsername());
			dataSource.setPassword(PostgresContainerTest.getPassword());
			return dataSource;
		}

		@Bean
		DataSourceInitializer dataSourceInitializer(DataSource dataSource) {
			DataSourceInitializer dataSourceInitializer = new DataSourceInitializer();
			dataSourceInitializer.setDataSource(dataSource);
			ResourceDatabasePopulator databasePopulator =
					new ResourceDatabasePopulator(new ByteArrayResource(INTEGRATION_DB_SCRIPTS.getBytes()));
			databasePopulator.setSeparator(ScriptUtils.EOF_STATEMENT_SEPARATOR);
			dataSourceInitializer.setDatabasePopulator(
					databasePopulator);
			return dataSourceInitializer;
		}

		@Bean
		public JdbcChannelMessageStore jdbcChannelMessageStore(DataSource dataSource) {
			JdbcChannelMessageStore messageStore = new JdbcChannelMessageStore(dataSource);
			messageStore.setRegion("PostgresChannelMessageTableSubscriberTest");
			messageStore.setChannelMessageStoreQueryProvider(new PostgresChannelMessageStoreQueryProvider());
			return messageStore;
		}

	}

}
