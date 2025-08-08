/*
 * Copyright © 2016 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2016-present the original author or authors.
 */

package org.springframework.integration.jdbc.store.channel;

import org.h2.jdbc.JdbcSQLSyntaxErrorException;
import org.junit.jupiter.api.Test;

import org.springframework.context.ApplicationContextException;
import org.springframework.integration.jdbc.store.JdbcChannelMessageStore;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Gunnar Hillert
 * @author Manuel Jordan
 * @since 4.3
 */
@ContextConfiguration
public class H2JdbcChannelMessageStoreTests extends AbstractJdbcChannelMessageStoreTests {

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
