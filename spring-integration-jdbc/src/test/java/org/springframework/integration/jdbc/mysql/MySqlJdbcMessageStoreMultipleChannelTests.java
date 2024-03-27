/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.integration.jdbc.mysql;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * This test was created to reproduce INT-2980.
 *
 * @author Gunnar Hillert
 *
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
@Ignore
public class MySqlJdbcMessageStoreMultipleChannelTests {

	private static final Log LOG = LogFactory.getLog(MySqlJdbcMessageStoreMultipleChannelTests.class);

	private static final CountDownLatch countDownLatch1 = new CountDownLatch(1);

	private static final CountDownLatch countDownLatch2 = new CountDownLatch(1);

	private static AtomicBoolean success = new AtomicBoolean(true);

	@Autowired
	@Qualifier("requestChannel")
	private MessageChannel requestChannel;

	@Autowired
	@Qualifier("errorChannel")
	private QueueChannel errorChannel;

	@Autowired
	private PlatformTransactionManager transactionManager;

	private JdbcTemplate jdbcTemplate;

	@Autowired
	private DataSource dataSource;

	@Before
	public void beforeTest() {
		this.jdbcTemplate = new JdbcTemplate(dataSource);
	}

	@After
	public void afterTest() {
		new TransactionTemplate(this.transactionManager).execute(status -> {
			this.jdbcTemplate.update("delete from INT_GROUP_TO_MESSAGE");
			this.jdbcTemplate.update("delete from INT_MESSAGE");
			this.jdbcTemplate.update("delete from INT_MESSAGE_GROUP");
			return null;
		});
	}

	@Test
	public void testSendAndActivateTransactionalSend() throws Exception {

		new TransactionTemplate(this.transactionManager).execute(status -> {
			requestChannel.send(MessageBuilder.withPayload("Hello ").build());
			return null;
		});

		assertThat(countDownLatch1.await(10000, TimeUnit.MILLISECONDS))
				.as("countDownLatch1 was " + countDownLatch1.getCount()).isTrue();
		assertThat(countDownLatch2.await(10000, TimeUnit.MILLISECONDS))
				.as("countDownLatch2 was " + countDownLatch2.getCount()).isTrue();

		assertThat(success.get()).as("Wrong Sequence Number handled.").isTrue();
		assertThat(errorChannel.receive(0)).isNull();
	}

	public static class Splitter {

		public Splitter() {
			super();
		}

		public List<Object> duplicate(Message<?> message) {
			ArrayList<Object> res = new ArrayList<Object>();
			res.add(message);
			res.add(message);
			return res;
		}

	}

	public static class ServiceActivator {

		public ServiceActivator() {
			super();
		}

		public void first(Message<?> message) {

			int sequenceNumber = new IntegrationMessageHeaderAccessor(message).getSequenceNumber();

			LOG.info("First handling sequence number: " + sequenceNumber + "; Message ID: " + message.getHeaders().getId());

			if (sequenceNumber != 1) {
				success.set(false);
			}

			countDownLatch1.countDown();
		}

		public void second(Message<?> message) {

			int sequenceNumber = new IntegrationMessageHeaderAccessor(message).getSequenceNumber();
			LOG.info("Second handling sequence number: " + sequenceNumber + "; Message ID: " + message.getHeaders().getId());

			if (sequenceNumber != 2) {
				success.set(false);
			}

			countDownLatch2.countDown();
		}

	}

}
