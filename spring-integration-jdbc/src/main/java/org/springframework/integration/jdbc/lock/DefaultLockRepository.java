/*
 * Copyright 2016-2025 the original author or authors.
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

package org.springframework.integration.jdbc.lock;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.sql.DataSource;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.log.LogAccessor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.Assert;

/**
 * The default implementation of the {@link LockRepository} based on the
 * table from the script presented in the {@code org/springframework/integration/jdbc/schema-*.sql}.
 * <p>
 * This repository can't be shared between different {@link JdbcLockRegistry} instances.
 * Otherwise, it opens a possibility to break {@link java.util.concurrent.locks.Lock} contract,
 * where {@link JdbcLockRegistry} uses non-shared {@link java.util.concurrent.locks.ReentrantLock}s
 * for local synchronizations.
 * <p>
 * This class implements {@link SmartLifecycle} and calls
 * {@code SELECT COUNT(REGION) FROM %sLOCK} query
 * according to the provided prefix on {@link #start()} to check if required table is present in DB.
 * The application context will fail to start if the table is not present.
 * This check can be disabled via {@link #setCheckDatabaseOnStart(boolean)}.
 *
 * @author Dave Syer
 * @author Artem Bilan
 * @author Glenn Renfro
 * @author Gary Russell
 * @author Alexandre Strubel
 * @author Ruslan Stelmachenko
 * @author Eddie Cho
 *
 * @since 4.3
 */
public class DefaultLockRepository
		implements LockRepository, InitializingBean, ApplicationContextAware, SmartInitializingSingleton,
		SmartLifecycle {

	private static final LogAccessor LOGGER = new LogAccessor(DefaultLockRepository.class);

	/**
	 * Default value for the table prefix property.
	 */
	public static final String DEFAULT_TABLE_PREFIX = "INT_";

	/**
	 * Default value for the time-to-live property.
	 */
	public static final Duration DEFAULT_TTL = Duration.ofSeconds(10);

	private final String id;

	private final JdbcTemplate template;

	private final AtomicBoolean started = new AtomicBoolean();

	private Duration ttl = DEFAULT_TTL;

	private String prefix = DEFAULT_TABLE_PREFIX;

	private String region = "DEFAULT";

	private String deleteQuery = """
			DELETE FROM %sLOCK
			WHERE REGION=? AND LOCK_KEY=? AND CLIENT_ID=?
			""";

	private String deleteExpiredQuery = """
			DELETE FROM %sLOCK
			WHERE REGION=? AND CREATED_DATE<?
			""";

	private String deleteAllQuery = """
			DELETE FROM %sLOCK
			WHERE REGION=? AND CLIENT_ID=?
			""";

	private String updateQuery = """
			UPDATE %sLOCK
			SET CLIENT_ID=?, CREATED_DATE=?
			WHERE REGION=? AND LOCK_KEY=? AND (CLIENT_ID=? OR CREATED_DATE<?)
			""";

	private String insertQuery = """
			INSERT INTO %sLOCK (REGION, LOCK_KEY, CLIENT_ID, CREATED_DATE)
			VALUES (?, ?, ?, ?)
			""";

	private String countQuery = """
			SELECT COUNT(REGION)
			FROM %sLOCK
			WHERE REGION=? AND LOCK_KEY=? AND CLIENT_ID=? AND CREATED_DATE>=?
			""";

	private String renewQuery = """
			UPDATE %sLOCK
			SET CREATED_DATE=?
			WHERE REGION=? AND LOCK_KEY=? AND CLIENT_ID=?
			""";

	private String countAllQuery = """
			SELECT COUNT(REGION) FROM %sLOCK
			""";

	private ApplicationContext applicationContext;

	private PlatformTransactionManager transactionManager;

	private TransactionTemplate defaultTransactionTemplate;

	private TransactionTemplate readOnlyTransactionTemplate;

	private TransactionTemplate readCommittedTransactionTemplate;

	private boolean checkDatabaseOnStart = true;

	/**
	 * Constructor that initializes the client id that will be associated for
	 * all the locks persisted by the store instance to a random {@link UUID}.
	 * @param dataSource the {@link DataSource} used to maintain the lock repository.
	 */
	public DefaultLockRepository(DataSource dataSource) {
		this(dataSource, UUID.randomUUID().toString());
	}

	/**
	 * Constructor that allows the user to specify a client id that will
	 * be associated for all the locks persisted by the store instance.
	 * @param dataSource the {@link DataSource} used to maintain the lock repository.
	 * @param id the client id to be associated with locks handled by the repository.
	 * @since 4.3.13
	 */
	public DefaultLockRepository(DataSource dataSource, String id) {
		Assert.hasText(id, "id must not be null nor empty");
		this.template = new JdbcTemplate(dataSource);
		this.id = id;
	}

	/**
	 * A unique grouping identifier for all locks persisted with this store. Using
	 * multiple regions allows the store to be partitioned (if necessary) for different
	 * purposes. Defaults to {@code DEFAULT}.
	 * @param region the region name to set
	 */
	public void setRegion(String region) {
		Assert.hasText(region, "Region must not be null or empty.");
		this.region = region;
	}

	/**
	 * Specify the prefix for target database table used from queries.
	 * @param prefix the prefix to set (default {@code INT_}).
	 */
	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}

	/**
	 * Specify the time (in milliseconds) to expire deadlocks.
	 * @param timeToLive the time to expire deadlocks.
	 */
	public void setTimeToLive(int timeToLive) {
		this.ttl = Duration.ofMillis(timeToLive);
	}

	/**
	 * Set a {@link PlatformTransactionManager} for operations.
	 * Otherwise, a primary {@link PlatformTransactionManager} bean is obtained
	 * from the application context.
	 * @param transactionManager the {@link PlatformTransactionManager} to use.
	 * @since 6.0
	 */
	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	/**
	 * Set a custom {@code UPDATE} query for a lock record.
	 * The {@link #getUpdateQuery()} can be used as a template for customization.
	 * The default query is:
	 * <pre class="code">
	 * {@code
	 *  UPDATE %sLOCK
	 * 			SET CLIENT_ID=?, CREATED_DATE=?
	 * 			WHERE REGION=? AND LOCK_KEY=? AND (CLIENT_ID=? OR CREATED_DATE<?)
	 * }
	 * </pre>
	 * @param updateQuery the query to update a lock record.
	 * @since 6.1
	 * @see #getUpdateQuery()
	 * @see #setPrefix(String)
	 */
	public void setUpdateQuery(String updateQuery) {
		this.updateQuery = updateQuery;
	}

	/**
	 * Return the current update query.
	 * Can be used in a setter as a concatenation of the default query and some extra hint.
	 * @return the current update query.
	 * @since 6.1
	 * @see #setUpdateQuery(String)
	 */
	public String getUpdateQuery() {
		return this.updateQuery;
	}

	/**
	 * Set a custom {@code INSERT} query for a lock record.
	 * The {@link #getInsertQuery()} can be used as a template for customization.
	 * The default query is
	 * {@code INSERT INTO %sLOCK (REGION, LOCK_KEY, CLIENT_ID, CREATED_DATE) VALUES (?, ?, ?, ?)}.
	 * For example a PostgreSQL {@code ON CONFLICT DO NOTHING} hint can be provided like this:
	 * <pre class="code">
	 * {@code
	 *  lockRepository.setInsertQuery(lockRepository.getInsertQuery() + " ON CONFLICT DO NOTHING");
	 * }
	 * </pre>
	 * @param insertQuery the insert query for a lock record.
	 * @since 6.1
	 * @see #getInsertQuery()
	 * @see #setPrefix(String)
	 */
	public void setInsertQuery(String insertQuery) {
		this.insertQuery = insertQuery;
	}

	/**
	 * Return the current insert query.
	 * Can be used in a setter as a concatenation of the default query and some extra hint.
	 * @return the current insert query.
	 * @since 6.1
	 * @see #setInsertQuery(String)
	 */
	public String getInsertQuery() {
		return this.insertQuery;
	}

	/**
	 * Set a custom {@code INSERT} query for a lock record.
	 * The {@link #getRenewQuery()} can be used as a template for customization.
	 * The default query is:
	 * <pre class="code">
	 * {@code
	 *  UPDATE %sLOCK
	 * 			SET CREATED_DATE=?
	 * 			WHERE REGION=? AND LOCK_KEY=? AND CLIENT_ID=?
	 * }
	 * </pre>
	 * @param renewQuery the update query to renew a lock record.
	 * @since 6.1
	 * @see #getRenewQuery()
	 * @see #setPrefix(String)
	 */
	public void setRenewQuery(String renewQuery) {
		this.renewQuery = renewQuery;
	}

	/**
	 * Return the current renew query.
	 * Can be used in a setter as a concatenation of a default query and some extra hint.
	 * @return the current renew query.
	 * @since 6.1
	 * @see #setRenewQuery(String)
	 */
	public String getRenewQuery() {
		return this.renewQuery;
	}

	/**
	 * The flag to perform a database check query on start or not.
	 * @param checkDatabaseOnStart false to not perform the database check.
	 * @since 6.2
	 */
	public void setCheckDatabaseOnStart(boolean checkDatabaseOnStart) {
		this.checkDatabaseOnStart = checkDatabaseOnStart;
		if (!checkDatabaseOnStart) {
			LOGGER.info("The 'DefaultLockRepository' won't be started automatically " +
					"and required table is not going be checked.");
		}
	}

	@Override
	public void afterPropertiesSet() {
		this.deleteQuery = String.format(this.deleteQuery, this.prefix);
		this.deleteExpiredQuery = String.format(this.deleteExpiredQuery, this.prefix);
		this.deleteAllQuery = String.format(this.deleteAllQuery, this.prefix);
		this.updateQuery = String.format(this.updateQuery, this.prefix);
		this.insertQuery = String.format(this.insertQuery, this.prefix);
		this.countQuery = String.format(this.countQuery, this.prefix);
		this.renewQuery = String.format(this.renewQuery, this.prefix);
		this.countAllQuery = String.format(this.countAllQuery, this.prefix);
	}

	@Override
	public void afterSingletonsInstantiated() {
		if (this.transactionManager == null) {
			try {
				this.transactionManager = this.applicationContext.getBean(PlatformTransactionManager.class);
			}
			catch (BeansException ex) {
				throw new BeanInitializationException(
						"A unique or primary 'PlatformTransactionManager' bean " +
								"must be present in the application context.", ex);
			}
		}

		DefaultTransactionDefinition transactionDefinition =
				new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

		this.defaultTransactionTemplate =
				new TransactionTemplate(this.transactionManager, transactionDefinition);

		// It is safe to reuse the transactionDefinition - the TransactionTemplate makes copy of its properties.
		transactionDefinition.setReadOnly(true);

		this.readOnlyTransactionTemplate = new TransactionTemplate(this.transactionManager, transactionDefinition);

		transactionDefinition.setReadOnly(false);
		transactionDefinition.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);

		this.readCommittedTransactionTemplate = new TransactionTemplate(this.transactionManager, transactionDefinition);
	}

	@Override
	public boolean isAutoStartup() {
		return this.checkDatabaseOnStart;
	}

	@Override
	public void start() {
		if (this.started.compareAndSet(false, true) && this.checkDatabaseOnStart) {
			this.template.queryForObject(this.countAllQuery, Integer.class); // If no table in DB, an exception is thrown
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
	public void close() {
		this.defaultTransactionTemplate.executeWithoutResult(
				transactionStatus -> this.template.update(this.deleteAllQuery, this.region, this.id));
	}

	@Override
	public boolean delete(String lock) {
		return this.defaultTransactionTemplate.execute(
				transactionStatus -> this.template.update(this.deleteQuery, this.region, lock, this.id)) == 1;
	}

	@Override
	public boolean acquire(String lock) {
		Boolean result =
				this.readCommittedTransactionTemplate.execute(
						transactionStatus -> {
							if (this.template.update(this.updateQuery, this.id, epochMillis(),
									this.region, lock, this.id, ttlEpochMillis()) > 0) {
								return true;
							}
							try {
								return this.template.update(this.insertQuery, this.region, lock, this.id,
										epochMillis()) > 0;
							}
							catch (DataIntegrityViolationException ex) {
								return false;
							}
						});
		return Boolean.TRUE.equals(result);
	}

	@Override
	public boolean isAcquired(String lock) {
		final Boolean result = this.readOnlyTransactionTemplate.execute(
				transactionStatus ->
						Integer.valueOf(1).equals(
								this.template.queryForObject(this.countQuery,
										Integer.class, this.region, lock, this.id, ttlEpochMillis())));
		return Boolean.TRUE.equals(result);
	}

	@Override
	public void deleteExpired() {
		this.defaultTransactionTemplate.executeWithoutResult(
				transactionStatus ->
						this.template.update(this.deleteExpiredQuery, this.region, ttlEpochMillis()));
	}

	@Override
	public boolean renew(String lock) {
		final Boolean result = this.defaultTransactionTemplate.execute(
				transactionStatus ->
						this.template.update(this.renewQuery, epochMillis(), this.region, lock, this.id) == 1);
		return Boolean.TRUE.equals(result);
	}

	private Timestamp ttlEpochMillis() {
		return Timestamp.valueOf(currentTime().minus(this.ttl));
	}

	private static Timestamp epochMillis() {
		return Timestamp.valueOf(currentTime());
	}

	private static LocalDateTime currentTime() {
		return LocalDateTime.now(ZoneOffset.UTC);
	}

}
