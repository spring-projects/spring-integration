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

package org.springframework.integration.jdbc;

import java.util.Map;

import org.springframework.integration.endpoint.AbstractMessageSource;
import org.springframework.messaging.MessagingException;
import org.springframework.util.Assert;

/**
 * A polling channel adapter that creates messages from the payload returned by
 * executing a stored procedure or Sql function. Optionally an update can be executed
 * after the execution of the Stored Procedure or Function in order to update
 * processed rows.
 *
 * @author Gunnar Hillert
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 2.1
 */
public class StoredProcPollingChannelAdapter extends AbstractMessageSource<Object> {

	private final StoredProcExecutor executor;

	private volatile boolean expectSingleResult = false;

	/**
	 * Constructor taking {@link StoredProcExecutor}.
	 * @param storedProcExecutor Must not be null.
	 */
	public StoredProcPollingChannelAdapter(StoredProcExecutor storedProcExecutor) {
		Assert.notNull(storedProcExecutor, "storedProcExecutor must not be null.");
		this.executor = storedProcExecutor;

	}

	/**
	 * This parameter indicates that only one result object shall be returned from
	 * the Stored Procedure/Function Call. If set to true, a resultMap that contains
	 * only 1 element, will have that 1 element extracted and returned as payload.
	 * If the resultMap contains more than 1 element and expectSingleResult is true,
	 * then a {@link MessagingException} is thrown.
	 * Otherwise the complete resultMap is returned as the
	 * {@link org.springframework.messaging.Message} payload.
	 * Important Note: Several databases such as H2 are not fully supported.
	 * The H2 database, for example, does not fully support the
	 * {@link java.sql.CallableStatement}
	 * semantics and when executing function calls against H2, a result list is
	 * returned rather than a single value.
	 * Therefore, even if you set expectSingleResult = true, you may end up with
	 * a collection being returned.
	 * @param expectSingleResult true if a single result is expected.
	 */
	public void setExpectSingleResult(boolean expectSingleResult) {
		this.expectSingleResult = expectSingleResult;
	}

	/**
	 * Execute the select query and the update query if provided. Returns the
	 * rows returned by the select query. If a RowMapper has been provided, the
	 * mapped results are returned.
	 */
	@Override
	protected Object doReceive() {
		Object payload;

		Map<String, ?> resultMap = this.executor.executeStoredProcedure();

		if (resultMap.isEmpty()) {
			payload = null;
		}
		else {
			if (this.expectSingleResult && resultMap.size() == 1) {
				payload = resultMap.values().iterator().next();
			}
			else if (this.expectSingleResult && resultMap.size() > 1) {
				throw new MessagingException(
						"Stored Procedure/Function call returned more than "
								+ "1 result object and expectSingleResult was 'true'.");
			}
			else {
				payload = resultMap;
			}
		}

		return payload;
	}

	@Override
	public String getComponentType() {
		return "stored-proc:inbound-channel-adapter";
	}

}
