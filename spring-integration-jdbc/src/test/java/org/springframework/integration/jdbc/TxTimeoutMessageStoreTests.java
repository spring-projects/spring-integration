/*
 * Copyright 2002-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.springframework.integration.jdbc;

import javax.sql.DataSource;

import junit.framework.Assert;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.core.PollableChannel;
import org.springframework.integration.jdbc.test.ServiceActivator;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class TxTimeoutMessageStoreTests {

	private static final Log log = LogFactory.getLog(TxTimeoutMessageStoreTests.class);

	@Autowired
	DataSource dataSource;

	@Autowired
	private MessageChannel inputChannel;

	@Autowired
	private PollableChannel errorChannel;

	@Autowired
	PlatformTransactionManager transactionManager;

	@Autowired
	ServiceActivator sa;

//	@Before
//	@Transactional @Rollback(false)
//	public void populateDb() throws SQLException {
//		//new JdbcTemplate
//
//		final TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
//
//		transactionTemplate.setIsolationLevel(Isolation.READ_COMMITTED.value());
//		transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
//
//		transactionTemplate.execute(new TransactionCallbackWithoutResult() {
//			@Override
//			protected void doInTransactionWithoutResult(TransactionStatus status) {
//				ResourceDatabasePopulator p = new ResourceDatabasePopulator();
//				p.addScript(
//						new ClassPathResource("org/springframework/integration/jdbc/messagestore/channel/schema-derby.sql"));
//				try {
//					p.populate(dataSource.getConnection());
//				} catch (SQLException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//			}
//		});
//	}

	@Test
	public void test() throws InterruptedException {

		final TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

		transactionTemplate.setIsolationLevel(Isolation.READ_COMMITTED.value());
		transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);

		int maxMessages = 10;

		for (int i = 0; i < maxMessages; ++i) {
			final String message = "TEST MESSAGE " + i;
			log.info("Sending message: " + message);

			transactionTemplate.execute(new TransactionCallbackWithoutResult() {
				@Override
				protected void doInTransactionWithoutResult(TransactionStatus status) {
					inputChannel.send(MessageBuilder.withPayload(message).build());
				}
			});

			log.info("Done sending message: "  + message);
		}

		log.info("Done sending " + maxMessages + " messages.");

		Assert.assertTrue("Contdown latch did not count down from 10 to 0 in 10000ms.", sa.await(30000));

	}

}
