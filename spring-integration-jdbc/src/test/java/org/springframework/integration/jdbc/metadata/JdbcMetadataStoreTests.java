/*
 * Copyright 2017-present the original author or authors.
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

package org.springframework.integration.jdbc.metadata;

import javax.sql.DataSource;

import org.apache.derby.shared.common.error.StandardException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;
import org.mockito.InOrder;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContextException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.AdditionalMatchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author Bojan Vukasovic
 * @author Artem Bilan
 * @author Sanghun Lee
 *
 * @since 5.0
 */
@SpringJUnitConfig
@DirtiesContext // close at the end after class
@Transactional
@EnabledForJreRange(min = JRE.JAVA_21, disabledReason = "Derby 10.17")
public class JdbcMetadataStoreTests {

	@Autowired
	private DataSource dataSource;

	private JdbcMetadataStore metadataStore;

	@BeforeEach
	public void init() {
		metadataStore = new JdbcMetadataStore(dataSource);
		metadataStore.afterPropertiesSet();
	}

	@Test
	public void keyAndValuesArePreservedOnPut() {
		metadataStore.put("foo", "bar");
		metadataStore.put("foo", "bar1");
		metadataStore.put("foo2", "bar2");
		String bar1 = metadataStore.get("foo");
		String bar2 = metadataStore.get("foo2");
		assertThat(bar1).isEqualTo("bar1");
		assertThat(bar2).isEqualTo("bar2");
	}

	@Test
	public void keyAndValuesAreNotPreservedOnRemove() {
		metadataStore.put("foo", "bar");
		metadataStore.put("foo2", "bar2");
		metadataStore.remove("foo");
		String bar = metadataStore.get("foo");
		metadataStore.remove("foo2");
		String bar2 = metadataStore.get("foo2");
		assertThat(bar).isNull();
		assertThat(bar2).isNull();
	}

	@Test
	public void keyAndValuesAreNotOverwrittenOnPutIfAbsent() {
		metadataStore.put("foo", "bar");
		metadataStore.putIfAbsent("foo", "bar1");
		String bar = metadataStore.get("foo");
		assertThat(bar).isEqualTo("bar");
	}

	@Test
	@Timeout(10)
	public void putIfAbsentReadsWithALockWhenTheSnapshotCannotSeeTheRow() {
		JdbcOperations operations = mock(JdbcOperations.class);
		given(operations.execute(any(ConnectionCallback.class))).willReturn("H2");
		// The insert reports the row as already present...
		given(operations.update(anyString(), any(PreparedStatementSetter.class))).willReturn(0);
		// ...while the non-locking read cannot see it, which is what a pinned snapshot looks like.
		// Only a locking read, which is not served from the snapshot, can break that tie.
		given(operations.queryForObject(anyString(), eq(String.class), any(), any()))
				.willAnswer(invocation -> {
					if (invocation.getArgument(0, String.class).contains("FOR UPDATE")) {
						return "committedByAnotherTransaction";
					}
					throw new EmptyResultDataAccessException(1);
				});

		JdbcMetadataStore store = new JdbcMetadataStore(operations);
		store.afterPropertiesSet();

		assertThat(store.putIfAbsent("someKey", "someValue")).isEqualTo("committedByAnotherTransaction");

		// The non-locking query must stay the first attempt; the locking one is a fallback only.
		InOrder inOrder = inOrder(operations);
		inOrder.verify(operations).queryForObject(not(contains("FOR UPDATE")), eq(String.class), any(), any());
		inOrder.verify(operations).queryForObject(contains("FOR UPDATE"), eq(String.class), any(), any());
	}

	@Test
	@Timeout(10)
	public void putIfAbsentDoesNotRepeatTheSameQueryWhenTheLockHintIsEmpty() {
		JdbcOperations operations = mock(JdbcOperations.class);
		given(operations.execute(any(ConnectionCallback.class))).willReturn("H2");
		given(operations.update(anyString(), any(PreparedStatementSetter.class))).willReturn(0, 1);
		given(operations.queryForObject(anyString(), eq(String.class), any(), any()))
				.willThrow(new EmptyResultDataAccessException(1));

		JdbcMetadataStore store = new JdbcMetadataStore(operations);
		store.setLockHint("");
		store.afterPropertiesSet();

		assertThat(store.putIfAbsent("someKey", "someValue")).isNull();

		// With an empty lock hint both queries are identical, so the empty re-read is not retried
		// with a lock; the loop goes straight back to the insert.
		verify(operations, times(1)).queryForObject(anyString(), eq(String.class), any(), any());
	}

	@Test
	@Timeout(10)
	public void putIfAbsentReturnsForAStoredNullValue() {
		new JdbcTemplate(this.dataSource)
				.update("INSERT INTO INT_METADATA_STORE(METADATA_KEY, METADATA_VALUE, REGION) VALUES (?, ?, ?)",
						"nullValued", null, "DEFAULT");
		// Documents a known limitation: METADATA_VALUE is nullable, and for a row that exists with
		// a null value putIfAbsent() also returns null, which the ConcurrentMetadataStore contract
		// reserves for "stored successfully". Conflating such a row with an absent one inside the
		// loop would be worse - putIfAbsent() would never return - so the read must keep the two
		// cases apart and this return value stays as it is.
		assertThat(metadataStore.putIfAbsent("nullValued", "someValue")).isNull();
	}

	@Test
	public void nonExistentKeyIsNotRemoved() {
		metadataStore.remove("non-existent");
		String ne = metadataStore.get("non-existent");
		assertThat(ne).isNull();
	}

	@Test
	public void existingKeyValueIsReplacedWithNewValueWhenOldValueMatches() {
		metadataStore.put("foo", "bar");
		metadataStore.replace("foo", "bar", "bar2");
		String bar2 = metadataStore.get("foo");
		assertThat(bar2).isEqualTo("bar2");
	}

	@Test
	public void existingKeyValueIsNotReplacedWithNewValueWhenOldValueDoesNotMatch() {
		metadataStore.put("foo", "bar");
		metadataStore.replace("foo", "bar1", "bar2");
		String bar = metadataStore.get("foo");
		assertThat(bar).isEqualTo("bar");
	}

	@Test
	void noTableThrowsExceptionOnStart() {
		try (TestUtils.TestApplicationContext testApplicationContext = TestUtils.createTestApplicationContext()) {
			JdbcMetadataStore jdbcMetadataStore = new JdbcMetadataStore(this.dataSource);
			jdbcMetadataStore.setTablePrefix("TEST_");
			testApplicationContext.registerBean("jdbcMetadataStore", jdbcMetadataStore);
			assertThatExceptionOfType(ApplicationContextException.class)
					.isThrownBy(testApplicationContext::refresh)
					.withRootCauseExactlyInstanceOf(StandardException.class)
					.withStackTraceContaining("Table/View 'TEST_METADATA_STORE' does not exist");
		}
	}

}
