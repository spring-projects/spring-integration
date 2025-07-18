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

import java.util.concurrent.atomic.AtomicBoolean;

import javax.sql.DataSource;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.log.LogAccessor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.integration.metadata.ConcurrentMetadataStore;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

/**
 * Implementation of {@link ConcurrentMetadataStore} using a relational database via JDBC.
 * SQL scripts to create the necessary tables are packaged as
 * <code>org/springframework/integration/jdbc/schema-*.sql</code>,
 * where <code>*</code> is the target database type.
 * <p>
 * The transaction management is required to use this {@link ConcurrentMetadataStore}.
 * <p>
 * This class implements {@link SmartLifecycle} and calls
 * {@code SELECT COUNT(METADATA_KEY) FROM %sMETADATA_STORE} query
 * according to the provided prefix on {@link #start()} to check if required table is present in DB.
 * The application context will fail to start if the table is not present.
 * This check can be disabled via {@link #setCheckDatabaseOnStart(boolean)}.
 *
 * @author Bojan Vukasovic
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 5.0
 */
public class JdbcMetadataStore implements ConcurrentMetadataStore, InitializingBean, SmartLifecycle {

	private static final LogAccessor LOGGER = new LogAccessor(JdbcMetadataStore.class);

	private static final String KEY_CANNOT_BE_NULL = "'key' cannot be null";

	/**
	 * Default value for the table prefix property.
	 */
	public static final String DEFAULT_TABLE_PREFIX = "INT_";

	private final JdbcOperations jdbcTemplate;

	private final AtomicBoolean started = new AtomicBoolean();

	private String tablePrefix = DEFAULT_TABLE_PREFIX;

	private String region = "DEFAULT";

	private String lockHint = "FOR UPDATE";

	private String getValueQuery = """
			SELECT METADATA_VALUE FROM %sMETADATA_STORE
			WHERE METADATA_KEY=? AND REGION=?
			""";

	private String getValueForUpdateQuery = """
			SELECT METADATA_VALUE FROM %sMETADATA_STORE
			WHERE METADATA_KEY=? AND REGION=? %s
			""";

	private String replaceValueQuery = """
			UPDATE %sMETADATA_STORE
			SET METADATA_VALUE=?
			WHERE METADATA_KEY=? AND METADATA_VALUE=? AND REGION=?
			""";

	private String replaceValueByKeyQuery = """
			UPDATE %sMETADATA_STORE
			SET METADATA_VALUE=?
			WHERE METADATA_KEY=? AND REGION=?
			""";

	private String removeValueQuery = """
			DELETE FROM %sMETADATA_STORE
			WHERE METADATA_KEY=? AND REGION=?
			""";

	private String putIfAbsentValueQuery = """
			INSERT INTO %sMETADATA_STORE(METADATA_KEY, METADATA_VALUE, REGION)
			SELECT ?, ?, ?
				FROM %sMETADATA_STORE
				WHERE METADATA_KEY=? AND REGION=?
			HAVING COUNT(*)=0
			""";

	private String countQuery = """
			SELECT COUNT(METADATA_KEY) FROM %sMETADATA_STORE
			""";

	private boolean checkDatabaseOnStart = true;

	/**
	 * Instantiate a {@link JdbcMetadataStore} using provided dataSource {@link DataSource}.
	 * @param dataSource a {@link DataSource}
	 */
	public JdbcMetadataStore(DataSource dataSource) {
		this(new JdbcTemplate(dataSource));
	}

	/**
	 * Instantiate a {@link JdbcMetadataStore} using provided jdbcOperations {@link JdbcOperations}.
	 * @param jdbcOperations a {@link JdbcOperations}
	 */
	public JdbcMetadataStore(JdbcOperations jdbcOperations) {
		Assert.notNull(jdbcOperations, "'jdbcOperations' must not be null.");
		this.jdbcTemplate = jdbcOperations;
	}

	/**
	 * Public setter for the table prefix property.
	 * This will be prefixed to all the table names before queries are
	 * executed. Defaults to {@link #DEFAULT_TABLE_PREFIX}.
	 * @param tablePrefix the tablePrefix to set
	 */
	public void setTablePrefix(String tablePrefix) {
		Assert.notNull(tablePrefix, "'tablePrefix' must not be null.");
		this.tablePrefix = tablePrefix;
	}

	/**
	 * A unique grouping identifier for all messages persisted with this store.
	 * Using multiple regions allows the store
	 * to be partitioned (if necessary) for different purposes.
	 * Defaults to <code>DEFAULT</code>.
	 * @param region the region name to set
	 */
	public void setRegion(String region) {
		Assert.hasText(region, "'region' must not be null or empty.");
		this.region = region;
	}

	/**
	 * Specify a row lock hint for the query in the lock-based operations.
	 * Defaults to {@code FOR UPDATE}. Can be specified as an empty string,
	 * if the target RDBMS doesn't support locking on tables from queries.
	 * The value depends on the RDBMS vendor, e.g. SQL Server requires {@code WITH (ROWLOCK)}.
	 * @param lockHint the RDBMS vendor-specific lock hint.
	 * @since 5.0.7
	 */
	public void setLockHint(String lockHint) {
		Assert.notNull(lockHint, "'lockHint' cannot be null.");
		this.lockHint = lockHint;
	}

	@Override
	public void afterPropertiesSet() {
		String dataBaseVendor =
				this.jdbcTemplate.execute((ConnectionCallback<String>) connection ->
						connection.getMetaData().getDatabaseProductName());
		this.getValueQuery = String.format(this.getValueQuery, this.tablePrefix);
		this.getValueForUpdateQuery = String.format(this.getValueForUpdateQuery, this.tablePrefix, this.lockHint);
		this.replaceValueQuery = String.format(this.replaceValueQuery, this.tablePrefix);
		this.replaceValueByKeyQuery = String.format(this.replaceValueByKeyQuery, this.tablePrefix);
		this.removeValueQuery = String.format(this.removeValueQuery, this.tablePrefix);
		this.putIfAbsentValueQuery = String.format(this.putIfAbsentValueQuery, this.tablePrefix, this.tablePrefix);
		if ("PostgreSQL".equals(dataBaseVendor)) {
			this.putIfAbsentValueQuery += " ON CONFLICT DO NOTHING";
		}
		this.countQuery = String.format(this.countQuery, this.tablePrefix);
	}

	/**
	 * The flag to perform a database check query on start or not.
	 * @param checkDatabaseOnStart false to not perform the database check.
	 * @since 6.2
	 */
	public void setCheckDatabaseOnStart(boolean checkDatabaseOnStart) {
		this.checkDatabaseOnStart = checkDatabaseOnStart;
		if (!checkDatabaseOnStart) {
			LOGGER.info("The 'JdbcMetadataStore' won't be started automatically " +
					"and required table is not going be checked.");
		}
	}

	@Override
	public boolean isAutoStartup() {
		return this.checkDatabaseOnStart;
	}

	@Override
	public void start() {
		if (this.started.compareAndSet(false, true) && this.checkDatabaseOnStart) {
			this.jdbcTemplate.queryForObject(this.countQuery, Integer.class); // If no table in DB, an exception is thrown
		}
	}

	@Override
	public void stop() {
		this.started.set(false);
	}

	@Override
	public boolean isRunning() {
		return this.started.get();
	}

	@Override
	@Transactional
	public @Nullable String putIfAbsent(String key, String value) {
		Assert.notNull(key, KEY_CANNOT_BE_NULL);
		Assert.notNull(value, "'value' cannot be null");
		while (true) {
			//try to insert if the entry does not exist
			int affectedRows = tryToPutIfAbsent(key, value);
			if (affectedRows > 0) {
				//it was not in the table, so we have just inserted
				return null;
			}
			else {
				//value should be in table. try to return it
				try {
					return this.jdbcTemplate.queryForObject(this.getValueQuery, String.class, key, this.region);
				}
				catch (EmptyResultDataAccessException e) {
					//somebody deleted it between calls. try to insert again (go to beginning of while loop)
				}
			}
		}
	}

	private int tryToPutIfAbsent(String key, String value) {
		try {
			return this.jdbcTemplate.update(this.putIfAbsentValueQuery,
					ps -> {
						ps.setString(1, key);
						ps.setString(2, value);
						ps.setString(3, this.region); // NOSONAR magic number
						ps.setString(4, key); // NOSONAR magic number
						ps.setString(5, this.region); // NOSONAR magic number
					});
		}
		catch (TransientDataAccessException | DataIntegrityViolationException ex) {
			return 0;
		}
	}

	@Override
	@Transactional
	public boolean replace(String key, String oldValue, String newValue) {
		Assert.notNull(key, KEY_CANNOT_BE_NULL);
		Assert.notNull(oldValue, "'oldValue' cannot be null");
		Assert.notNull(newValue, "'newValue' cannot be null");
		int affectedRows = this.jdbcTemplate.update(this.replaceValueQuery,
				ps -> {
					ps.setString(1, newValue);
					ps.setString(2, key);
					ps.setString(3, oldValue); // NOSONAR magic number
					ps.setString(4, this.region); // NOSONAR magic number
				});
		return affectedRows > 0;
	}

	@Override
	@Transactional
	public void put(String key, String value) {
		Assert.notNull(key, KEY_CANNOT_BE_NULL);
		Assert.notNull(value, "'value' cannot be null");
		while (true) {
			//try to insert if the entry does not exist, if it exists we will try to update it
			int affectedRows = tryToPutIfAbsent(key, value);
			if (affectedRows == 0) {
				//since value is not inserted, means it is already present
				try {
					//lock row for updating
					this.jdbcTemplate.queryForObject(this.getValueForUpdateQuery, String.class, key, this.region);
				}
				catch (EmptyResultDataAccessException e) {
					//if there are no rows with this key, somebody deleted it in between two calls
					continue;    //try to insert again from beginning
				}
				//lock successful, so - replace
				this.jdbcTemplate.update(this.replaceValueByKeyQuery,
						ps -> {
							ps.setString(1, value);
							ps.setString(2, key);
							ps.setString(3, this.region); // NOSONAR magic number
						});
			}
			return;
		}
	}

	@Override
	@Transactional
	public @Nullable String get(String key) {
		Assert.notNull(key, KEY_CANNOT_BE_NULL);
		try {
			return this.jdbcTemplate.queryForObject(this.getValueQuery, String.class, key, this.region);
		}
		catch (EmptyResultDataAccessException e) {
			//if there are no rows with this key, return null
			return null;
		}
	}

	@Override
	@Transactional
	public @Nullable String remove(String key) {
		Assert.notNull(key, KEY_CANNOT_BE_NULL);
		String oldValue;
		try {
			//select old value and lock row for removal
			oldValue = this.jdbcTemplate.queryForObject(this.getValueForUpdateQuery, String.class, key, this.region);
		}
		catch (EmptyResultDataAccessException e) {
			//key is not present, so no need to delete it
			return null;
		}
		//delete row and return old value
		int updated = this.jdbcTemplate.update(this.removeValueQuery, key, this.region);
		if (updated != 0) {
			return oldValue;
		}
		return null;
	}

}
