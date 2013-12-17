/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.integration.jdbc.mysql;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

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
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * This test was created to reproduce INT-2980.
 *
 * @author Gunnar Hillert
 *
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext(classMode=ClassMode.AFTER_EACH_TEST_METHOD)
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
		new TransactionTemplate(this.transactionManager).execute(new TransactionCallback<Void>() {
			public Void doInTransaction(TransactionStatus status) {
				final int deletedGroupToMessageRows = jdbcTemplate.update("delete from INT_GROUP_TO_MESSAGE");
				final int deletedMessages = jdbcTemplate.update("delete from INT_MESSAGE");
				final int deletedMessageGroups = jdbcTemplate.update("delete from INT_MESSAGE_GROUP");

				LOG.info(String.format("Cleaning Database - Deleted Messages: %s, " +
						"Deleted GroupToMessage Rows: %s, Deleted Message Groups: %s",
						deletedMessages, deletedGroupToMessageRows, deletedMessageGroups));

				return null;
			}
		});
	}

	@Test
	public void testSendAndActivateTransactionalSend() throws Exception {

		new TransactionTemplate(this.transactionManager).execute(new TransactionCallback<Void>() {
			public Void doInTransaction(TransactionStatus status) {
				requestChannel.send(MessageBuilder.withPayload("Hello ").build());
				return null;
			}
		});

		assertTrue("countDownLatch1 was " + countDownLatch1.getCount(), countDownLatch1.await(10000, TimeUnit.MILLISECONDS));
		assertTrue("countDownLatch2 was " + countDownLatch2.getCount(), countDownLatch2.await(10000, TimeUnit.MILLISECONDS));

		assertTrue("Wrong Sequence Number handled.", success.get());
		assertNull(errorChannel.receive(0));
	}

	public static class Splitter {

		public Splitter() {
			super();
		}

		public List<Object> duplicate(Message<?> message) {
			ArrayList<Object> res = new ArrayList<Object>();
			res.add(message);
			res.add(message);

			System.out.println("Split Complete");

			return res;
		}
	}

	public static class ServiceActivator {

		public ServiceActivator() {
			super();
		}

		public void first(Message<?> message ) {

			int sequenceNumber = new IntegrationMessageHeaderAccessor(message).getSequenceNumber();

			LOG.info("First handling sequence number: " + sequenceNumber + "; Message ID: " + message.getHeaders().getId());

			if (sequenceNumber != 1) {
				success.set(false);
			}

			countDownLatch1.countDown();
		}

		public void second(Message<?> message ) {

			int sequenceNumber = new IntegrationMessageHeaderAccessor(message).getSequenceNumber();
			LOG.info("Second handling sequence number: " + sequenceNumber + "; Message ID: " + message.getHeaders().getId());

			if (sequenceNumber != 2) {
				success.set(false);
			}

			countDownLatch2.countDown();
		}
	}
}
