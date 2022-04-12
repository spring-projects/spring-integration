/*
 * Copyright 2016-2022 the original author or authors.
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

import java.util.Date;
import java.util.UUID;

import javax.sql.DataSource;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.dao.DuplicateKeyException;
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
 *
 * @author Dave Syer
 * @author Artem Bilan
 * @author Glenn Renfro
 * @author Gary Russell
 * @author Alexandre Strubel
 * @author Ruslan Stelmachenko
 *
 * @since 4.3
 */
public class DefaultLockRepository
		implements LockRepository, InitializingBean, ApplicationContextAware, SmartInitializingSingleton {

	/**
	 * Default value for the table prefix property.
	 */
	public static final String DEFAULT_TABLE_PREFIX = "INT_";

	/**
	 * Default value for the time-to-live property.
	 */
	public static final int DEFAULT_TTL = 10000;

	private final String id;

	private final JdbcTemplate template;

	private int ttl = DEFAULT_TTL;

	private String prefix = DEFAULT_TABLE_PREFIX;

	private String region = "DEFAULT";

	private String deleteQuery = "DELETE FROM %sLOCK WHERE REGION=? AND LOCK_KEY=? AND CLIENT_ID=?";

	private String deleteExpiredQuery = "DELETE FROM %sLOCK WHERE REGION=? AND CREATED_DATE<?";

	private String deleteAllQuery = "DELETE FROM %sLOCK WHERE REGION=? AND CLIENT_ID=?";

	private String updateQuery =
			"UPDATE %sLOCK SET CLIENT_ID=?, CREATED_DATE=? WHERE REGION=? AND LOCK_KEY=? " +
					"AND (CLIENT_ID=? OR CREATED_DATE<?)";

	private String insertQuery = "INSERT INTO %sLOCK (REGION, LOCK_KEY, CLIENT_ID, CREATED_DATE) VALUES (?, ?, ?, ?)";

	private String countQuery =
			"SELECT COUNT(REGION) FROM %sLOCK WHERE REGION=? AND LOCK_KEY=? AND CLIENT_ID=? AND CREATED_DATE>=?";

	private String renewQuery = "UPDATE %sLOCK SET CREATED_DATE=? WHERE REGION=? AND LOCK_KEY=? AND CLIENT_ID=?";

	private ApplicationContext applicationContext;

	private PlatformTransactionManager transactionManager;

	private TransactionTemplate defaultTransactionTemplate;

	private TransactionTemplate readOnlyTransactionTemplate;

	private TransactionTemplate serializableTransactionTemplate;

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
	 * Specify the time (in milliseconds) to expire dead-locks.
	 * @param timeToLive the time to expire dead-locks.
	 */
	public void setTimeToLive(int timeToLive) {
		this.ttl = timeToLive;
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

	@Override
	public void afterPropertiesSet() {
		this.deleteQuery = String.format(this.deleteQuery, this.prefix);
		this.deleteExpiredQuery = String.format(this.deleteExpiredQuery, this.prefix);
		this.deleteAllQuery = String.format(this.deleteAllQuery, this.prefix);
		this.updateQuery = String.format(this.updateQuery, this.prefix);
		this.insertQuery = String.format(this.insertQuery, this.prefix);
		this.countQuery = String.format(this.countQuery, this.prefix);
		this.renewQuery = String.format(this.renewQuery, this.prefix);
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
		transactionDefinition.setIsolationLevel(TransactionDefinition.ISOLATION_SERIALIZABLE);

		this.serializableTransactionTemplate = new TransactionTemplate(this.transactionManager, transactionDefinition);
	}

	@Override
	public void close() {
		this.defaultTransactionTemplate.executeWithoutResult(
				transactionStatus -> this.template.update(this.deleteAllQuery, this.region, this.id));
	}

	@Override
	public void delete(String lock) {
		this.defaultTransactionTemplate.executeWithoutResult(
				transactionStatus -> this.template.update(this.deleteQuery, this.region, lock, this.id));
	}

	@Override
	public boolean acquire(String lock) {
		return this.serializableTransactionTemplate.execute(transactionStatus -> {
			if (this.template.update(this.updateQuery, this.id, new Date(), this.region, lock, this.id,
					new Date(System.currentTimeMillis() - this.ttl)) > 0) {
				return true;
			}
			try {
				return this.template.update(this.insertQuery, this.region, lock, this.id, new Date()) > 0;
			}
			catch (DuplicateKeyException e) {
				return false;
			}
		});
	}

	@Override
	public boolean isAcquired(String lock) {
		return this.readOnlyTransactionTemplate.execute(transactionStatus ->
				this.template.queryForObject(this.countQuery, // NOSONAR query never returns null
						Integer.class, this.region, lock, this.id, new Date(System.currentTimeMillis() - this.ttl))
						== 1);
	}

	@Override
	public void deleteExpired() {
		this.defaultTransactionTemplate.executeWithoutResult(
				transactionStatus ->
						this.template.update(this.deleteExpiredQuery, this.region,
								new Date(System.currentTimeMillis() - this.ttl)));
	}

	@Override
	public boolean renew(String lock) {
		return this.defaultTransactionTemplate.execute(
				transactionStatus -> this.template.update(this.renewQuery, new Date(), this.region, lock, this.id) > 0);
	}

}
