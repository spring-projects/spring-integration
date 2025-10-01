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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;

import org.springframework.integration.jdbc.oracle.OracleContainerTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.support.TransactionTemplate;

/**
 *
 * @author Gunnar Hillert
 * @author Artem Bilan
 *
 */
@ContextConfiguration
@DisabledIfSystemProperty(named = "os.arch", matches = ".*aarch64.*")
public class OracleTxTimeoutMessageStoreTests extends AbstractTxTimeoutMessageStoreTests implements OracleContainerTest {

	@AfterEach
	public void cleanTable() {
		final JdbcTemplate jdbcTemplate = new JdbcTemplate(this.dataSource);
		new TransactionTemplate(this.transactionManager)
				.executeWithoutResult(status -> {
					final int deletedChannelMessageRows = jdbcTemplate.update("delete from INT_CHANNEL_MESSAGE");
					log.info(String.format("Cleaning Database - Deleted Channel Messages: %s ",
							deletedChannelMessageRows));
				});
	}

}
