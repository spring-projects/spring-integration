/*
 * Copyright 2016-2023 the original author or authors.
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
