/*
 * Copyright 2016-2020 the original author or authors.
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

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

/**
 * The default implementation of the {@link LockRepository} based on the
 * table from the script presented in the {@code org/springframework/integration/jdbc/schema-*.sql}.
 * <p>
 * This repository can't be shared between different {@link JdbcLockRegistry} instances.
 * Otherwise it opens a possibility to break {@link java.util.concurrent.locks.Lock} contract,
 * where {@link JdbcLockRegistry} uses non-shared {@link java.util.concurrent.locks.ReentrantLock}s
 * for local synchronizations.
 *
 * @author Dave Syer
 * @author Artem Bilan
 * @author Glenn Renfro
 * @author Gary Russell
 * @author Alexandre Strubel
 *
 * @since 4.3
 */
@Repository
@Transactional
public class DefaultLockRepository implements LockRepository, InitializingBean {

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

	/**
	 * Constructor that initializes the client id that will be associated for
	 * all the locks persisted by the store instance to a random {@link UUID}.
	 * @param dataSource the {@link DataSource} used to maintain the lock repository.
	 */
	@Autowired
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
	 * Specify the prefix for target data base table used from queries.
	 * @param prefix the prefix to set (default INT_).
	 */
	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}

	/**
	 * Specify the time (in milliseconds) to expire dead locks.
	 * @param timeToLive the time to expire dead locks.
	 */
	public void setTimeToLive(int timeToLive) {
		this.ttl = timeToLive;
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
	public void close() {
		this.template.update(this.deleteAllQuery, this.region, this.id);
	}

	@Override
	public void delete(String lock) {
		this.template.update(this.deleteQuery, this.region, lock, this.id);
	}

	@Transactional(isolation = Isolation.SERIALIZABLE)
	@Override
	public boolean acquire(String lock) {
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
	}

	@Override
	public boolean isAcquired(String lock) {
		return this.template.queryForObject(this.countQuery, Integer.class, // NOSONAR query never returns null
				this.region, lock, this.id, new Date(System.currentTimeMillis() - this.ttl)) == 1;
	}

	@Override
	public void deleteExpired() {
		this.template.update(this.deleteExpiredQuery, this.region, new Date(System.currentTimeMillis() - this.ttl));
	}

	@Override
	public boolean renew(String lock) {
		return this.template.update(this.renewQuery, new Date(), this.region, lock, this.id) > 0;
	}

}
