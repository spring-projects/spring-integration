/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.jdbc.lock;

import java.io.Closeable;
import java.util.Date;
import java.util.UUID;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

/**
 * Encapsulation of the SQL shunting that is needed for locks. A {@link JdbcLockRegistry}
 * needs a reference to a spring-managed (transactional) client service, so this component
 * has to be declared as a bean.
 *
 * @author Dave Syer
 *
 */
@Component
@Transactional
public class JdbcClient implements Closeable {

	/**
	 * Default value for the table prefix property.
	 */
	public static final String DEFAULT_TABLE_PREFIX = "INT_";

	private enum Query {

		DELETE("DELETE FROM %SLOCK WHERE REGION=? AND LOCK_KEY=? AND CLIENT_ID=?"),

		DELETE_EXPIRED("DELETE FROM %SLOCK WHERE REGION=? AND LOCK_KEY=? AND CLIENT_ID=? AND CREATED_DATE<?"),

		DELETE_ALL("DELETE FROM %SLOCK WHERE REGION=? AND CLIENT_ID=?"),

		UPDATE("UPDATE %SLOCK SET CREATED_DATE=? WHERE REGION=? AND LOCK_KEY=? AND CLIENT_ID=?"),

		INSERT("INSERT INTO %SLOCK (REGION, LOCK_KEY, CLIENT_ID, CREATED_DATE) VALUES (?, ?, ?, ?)"),

		COUNT("SELECT COUNT(REGION) FROM %SLOCK WHERE CLIENT_ID=? AND CREATED_DATE>=?");

		private String sql;

		Query(String sql) {
			this.sql = sql;
		}

		public String getSql(String prefix) {
			return String.format(this.sql, prefix);
		}
	}

	private String id = UUID.randomUUID().toString();

	private int ttl = 10000;

	private final JdbcTemplate template;

	private volatile String prefix = DEFAULT_TABLE_PREFIX;

	private volatile String region = "DEFAULT";

	@Autowired
	public JdbcClient(DataSource dataSource) {
		this.template = new JdbcTemplate(dataSource);
	}

	/**
	 * A unique grouping identifier for all locks persisted with this store. Using
	 * multiple regions allows the store to be partitioned (if necessary) for different
	 * purposes. Defaults to <code>DEFAULT</code>.
	 *
	 * @param region the region name to set
	 */
	public void setRegion(String region) {
		Assert.hasText(region, "Region must not be null or empty.");
		this.region = region;
	}

	/**
	 * @param prefix the prefix to set (default INT_)
	 */
	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}

	@Override
	public void close() {
		this.template.update(Query.DELETE_ALL.getSql(this.prefix), this.region, getId());
	}

	public void delete(String lock) {
		this.template.update(Query.DELETE.getSql(this.prefix), this.region, lock, getId());
	}

	@Transactional(isolation = Isolation.SERIALIZABLE, timeout = 1)
	public boolean acquire(String lock) {
		deleteExpired(lock);
		if (this.template.update(Query.UPDATE.getSql(this.prefix), new Date(), this.region, lock, getId()) > 0) {
			return true;
		}
		try {
			return this.template.update(Query.INSERT.getSql(this.prefix), this.region, lock, getId(), new Date()) > 0;
		}
		catch (DuplicateKeyException e) {
			return false;
		}
	}

	public boolean isAcquired(String lock) {
		deleteExpired(lock);
		return this.template.queryForObject(Query.COUNT.getSql(this.prefix), Integer.class, getId(),
				new Date(System.currentTimeMillis() - this.ttl)) == 1;
	}

	private int deleteExpired(String lock) {
		return this.template.update(Query.DELETE_EXPIRED.getSql(this.prefix), this.region, lock, getId(),
				new Date(System.currentTimeMillis() - this.ttl));
	}

	private String getId() {
		return this.id;
	}

}
