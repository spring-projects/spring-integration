/*
 * Copyright 2002-present the original author or authors.
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
