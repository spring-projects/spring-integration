/*
 * Copyright 2022-2024 the original author or authors.
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

import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javax.sql.DataSource;

import org.apache.commons.dbcp2.BasicDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.postgresql.jdbc.PgConnection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.jdbc.store.JdbcChannelMessageStore;
import org.springframework.integration.jdbc.store.channel.PostgresChannelMessageStoreQueryProvider;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Rafael Winterhalter
 * @author Artem Bilan
 * @author Igor Lovich
 * @author Adama Sorho
 * @author Johannes Edmeier
 *
 * @since 6.0
 */
@SpringJUnitConfig
@DirtiesContext
public class PostgresChannelMessageTableSubscriberTests implements PostgresContainerTest {

	private static final String INTEGRATION_DB_SCRIPTS = """
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

	@Autowired
	private PlatformTransactionManager transactionManager;

	private PostgresChannelMessageTableSubscriber postgresChannelMessageTableSubscriber;

	private PostgresSubscribableChannel postgresSubscribableChannel;

	private ThreadPoolTaskExecutor taskExecutor;

	private String groupId;

	private ConnectionSupplier connectionSupplier;

	@BeforeEach
	void setUp(TestInfo testInfo) {
		// Not initiated as a bean to allow for registrations prior and post the life cycle
		this.connectionSupplier = new ConnectionSupplier();
		this.postgresChannelMessageTableSubscriber = new PostgresChannelMessageTableSubscriber(connectionSupplier);
		this.postgresChannelMessageTableSubscriber.setNotificationTimeout(Duration.ofSeconds(5));

		this.taskExecutor = new ThreadPoolTaskExecutor();
		this.taskExecutor.setCorePoolSize(10);
		this.taskExecutor.setWaitForTasksToCompleteOnShutdown(true);
		this.taskExecutor.setAwaitTerminationSeconds(10);
		this.taskExecutor.afterPropertiesSet();

		this.groupId = testInfo.getDisplayName();

		this.postgresSubscribableChannel =
				new PostgresSubscribableChannel(messageStore, groupId, postgresChannelMessageTableSubscriber);
		this.postgresSubscribableChannel.setBeanName("testPostgresChannel");
		this.postgresSubscribableChannel.setDispatcherExecutor(this.taskExecutor);
		this.postgresSubscribableChannel.afterPropertiesSet();
	}

	@AfterEach
	void tearDown() {
		this.postgresChannelMessageTableSubscriber.stop();
		this.taskExecutor.shutdown();
	}

	@Test
	public void testMessagePollMessagesAddedAfterStart() throws Exception {
		CountDownLatch latch = new CountDownLatch(2);
		List<Object> payloads = new ArrayList<>();
		postgresChannelMessageTableSubscriber.start();
		postgresSubscribableChannel.subscribe(message -> {
			payloads.add(message.getPayload());
			latch.countDown();
		});
		messageStore.addMessageToGroup(groupId, new GenericMessage<>("1"));
		messageStore.addMessageToGroup(groupId, new GenericMessage<>("2"));
		assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(payloads).containsExactly("1", "2");
	}

	@Test
	public void testMessagePollMessagesAddedBeforeStart() throws InterruptedException {
		CountDownLatch latch = new CountDownLatch(2);
		List<Object> payloads = new ArrayList<>();

		postgresSubscribableChannel.subscribe(message -> {
			payloads.add(message.getPayload());
			latch.countDown();
		});
		messageStore.addMessageToGroup(groupId, new GenericMessage<>("1"));
		messageStore.addMessageToGroup(groupId, new GenericMessage<>("2"));
		postgresChannelMessageTableSubscriber.start();
		assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(payloads).containsExactly("1", "2");
	}

	@Test
	void testMessagesDispatchedInTransaction() throws InterruptedException {
		CountDownLatch latch = new CountDownLatch(2);
		postgresSubscribableChannel.setTransactionManager(transactionManager);

		postgresChannelMessageTableSubscriber.start();
		MessageHandler messageHandler =
				message -> {
					try {
						throw new RuntimeException("An error has occurred");
					}
					finally {
						latch.countDown();
					}
				};
		postgresSubscribableChannel.subscribe(messageHandler);

		messageStore.addMessageToGroup(groupId, new GenericMessage<>("1"));
		messageStore.addMessageToGroup(groupId, new GenericMessage<>("2"));

		assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();

		// Stop subscriber to unlock records from TX for the next verification
		postgresChannelMessageTableSubscriber.stop();
		postgresSubscribableChannel.unsubscribe(messageHandler);
		this.taskExecutor.shutdown();

		assertThat(messageStore.messageGroupSize(groupId)).isEqualTo(2);
		assertThat(messageStore.pollMessageFromGroup(groupId).getPayload()).isEqualTo("1");
		assertThat(messageStore.pollMessageFromGroup(groupId).getPayload()).isEqualTo("2");
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void errorHandlerIsCalled(boolean transactionsEnabled) throws InterruptedException {
		if (transactionsEnabled) {
			postgresSubscribableChannel.setTransactionManager(transactionManager);
		}

		AtomicReference<Throwable> exceptionReference = new AtomicReference<>();
		CountDownLatch errorHandlerLatch = new CountDownLatch(1);
		postgresSubscribableChannel.setErrorHandler(ex -> {
			exceptionReference.set(ex);
			errorHandlerLatch.countDown();
		});

		postgresChannelMessageTableSubscriber.start();

		postgresSubscribableChannel.subscribe(message -> {
			throw new RuntimeException("An error has occurred");
		});

		messageStore.addMessageToGroup(groupId, new GenericMessage<>("1"));

		assertThat(errorHandlerLatch.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(exceptionReference.get())
				.isInstanceOf(MessagingException.class)
				.hasStackTraceContaining("An error has occurred");
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void testRetryOnErrorDuringDispatch(boolean transactionsEnabled) throws InterruptedException {
		CountDownLatch latch = new CountDownLatch(2);
		List<Object> payloads = new ArrayList<>();
		AtomicInteger actualTries = new AtomicInteger();

		int maxAttempts = 2;
		postgresSubscribableChannel.setRetryTemplate(RetryTemplate.builder().maxAttempts(maxAttempts).build());

		if (transactionsEnabled) {
			postgresSubscribableChannel.setTransactionManager(transactionManager);
		}

		postgresChannelMessageTableSubscriber.start();

		postgresSubscribableChannel.subscribe(message -> {
			try {
				//fail once
				if (actualTries.getAndIncrement() == 0) {
					throw new RuntimeException("An error has occurred");
				}
				payloads.add(message.getPayload());
			}
			finally {
				latch.countDown();
			}
		});

		messageStore.addMessageToGroup(groupId, new GenericMessage<>("1"));

		assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();

		assertThat(actualTries.get()).isEqualTo(maxAttempts);
		assertThat(payloads).containsExactly("1");
	}

	@Test
	public void testRenewConnection() throws Exception {
		CountDownLatch latch = new CountDownLatch(2);
		List<Object> payloads = new ArrayList<>();
		CountDownLatch connectionLatch = new CountDownLatch(2);
		connectionSupplier.onGetConnection = connectionLatch::countDown;
		postgresChannelMessageTableSubscriber.start();
		postgresSubscribableChannel.subscribe(message -> {
			payloads.add(message.getPayload());
			latch.countDown();
		});

		assertThat(connectionLatch.await(10, TimeUnit.SECONDS)).isTrue();

		messageStore.addMessageToGroup(groupId, new GenericMessage<>("1"));
		messageStore.addMessageToGroup(groupId, new GenericMessage<>("2"));
		assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(payloads).containsExactlyInAnyOrder("1", "2");
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
			dataSourceInitializer.setDatabasePopulator(databasePopulator);
			return dataSourceInitializer;
		}

		@Bean
		PlatformTransactionManager transactionManager(DataSource dataSource) {
			return new DataSourceTransactionManager(dataSource);
		}

		@Bean
		public JdbcChannelMessageStore jdbcChannelMessageStore(DataSource dataSource) {
			JdbcChannelMessageStore messageStore = new JdbcChannelMessageStore(dataSource);
			messageStore.setRegion("PostgresChannelMessageTableSubscriberTest");
			messageStore.setChannelMessageStoreQueryProvider(new PostgresChannelMessageStoreQueryProvider());
			return messageStore;
		}

	}

	private static class ConnectionSupplier implements PgConnectionSupplier {

		Runnable onGetConnection;

		@Override
		public PgConnection get() throws SQLException {
			var conn = DriverManager.getConnection(POSTGRES_CONTAINER.getJdbcUrl(),
							POSTGRES_CONTAINER.getUsername(),
							POSTGRES_CONTAINER.getPassword())
					.unwrap(PgConnection.class);
			if (this.onGetConnection != null) {
				this.onGetConnection.run();
			}
			return conn;
		}

	}
}
