/*
 * Copyright 2016-2022 the original author or authors.
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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.postgresql.jdbc.PgConnection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.jdbc.store.JdbcChannelMessageStore;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;

@SpringJUnitConfig
@DirtiesContext
public class PostgresChannelMessageTableSubscriberTest implements PostgresContainerTest {

	@Autowired
	private DataSource dataSource;

	private JdbcChannelMessageStore messageStore;

	private PostgresChannelMessageTableSubscriber postgresChannelMessageTableSubscriber;

	@Autowired
	private PlatformTransactionManager transactionManager;

	@BeforeEach
	public void init() {
		messageStore = new JdbcChannelMessageStore(dataSource);
		messageStore.setRegion("PostgresChannelMessageTableSubscriberTest");
		postgresChannelMessageTableSubscriber = new PostgresChannelMessageTableSubscriber(
				() -> DriverManager.getConnection(POSTGRES_CONTAINER.getJdbcUrl(),
						POSTGRES_CONTAINER.getUsername(),
						POSTGRES_CONTAINER.getPassword()).unwrap(PgConnection.class)
		);
	}

	@Test
	public void testMessagePollMessagesAddedAfterStart() throws InterruptedException {
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
			assertThat(latch.await(3, TimeUnit.SECONDS)).isTrue();
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
			PostgresSubscribableChannel channel = new PostgresSubscribableChannel(messageStore,
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
			assertThat(latch.await(3, TimeUnit.SECONDS)).isTrue();
		}
		finally {
			postgresChannelMessageTableSubscriber.stop();
		}
		assertThat(payloads).containsExactly("1", "2");
	}
}
