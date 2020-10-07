/*
 * Copyright 2002-2020 the original author or authors.
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

import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;


/**
 * A message handler that executes Stored Procedures for update purposes.
 *
 * Stored procedure parameter values are by default automatically extracted from
 * the Payload if the payload's bean properties match the parameters of the Stored
 * Procedure.
 *
 * This may be sufficient for basic use cases. For more sophisticated options
 * consider passing in one or more
 * {@link org.springframework.integration.jdbc.storedproc.ProcedureParameter}.
 *
 * If you need to handle the return parameters of the called stored procedure
 * explicitly, please consider using a {@link StoredProcOutboundGateway} instead.
 *
 * Also, if you need to execute SQL Functions, please also use the
 * {@link StoredProcOutboundGateway}. As functions are typically used to look up
 * values, only, the Stored Procedure message handler purposefully does not support
 * SQL function calls. If you believe there are valid use-cases for that, please file a
 * feature request at https://jira.springsource.org.
 *
 * @author Gunnar Hillert
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.1
 */
public class StoredProcMessageHandler extends AbstractMessageHandler {

	private final StoredProcExecutor executor;

	/**
	 * Constructor passing in the {@link StoredProcExecutor}.
	 * @param storedProcExecutor Must not be null.
	 */
	public StoredProcMessageHandler(StoredProcExecutor storedProcExecutor) {
		Assert.notNull(storedProcExecutor, "storedProcExecutor must not be null.");
		this.executor = storedProcExecutor;

	}

	@Override
	public String getComponentType() {
		return "jdbc:stored-proc-outbound-channel-adapter";
	}

	/**
	 * Executes the Stored procedure, delegates to executeStoredProcedure(...).
	 * Any return values from the Stored procedure are ignored.
	 * Return values are logged at debug level, though.
	 */
	@Override
	protected void handleMessageInternal(Message<?> message) {
		Map<String, Object> resultMap = this.executor.executeStoredProcedure(message);
		if (!CollectionUtils.isEmpty(resultMap)) {
			logger.debug(() -> String.format("The StoredProcMessageHandler ignores return "
							+ "values, but the called Stored Procedure '%s' returned data: '%s'",
					this.executor.getStoredProcedureName(), resultMap));
		}

	}

}
